package com.example.lotoaclick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import java.lang.Runnable
import kotlin.math.max

class ClickAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        // will be filled by MainActivity before starting
        @Volatile
        var config: Config? = null
    }

    private var job: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("ClickService", "onServiceConnected")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProcess()
            ACTION_STOP -> stopProcess()
            else -> { /* no-op */ }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startProcess() {
        val cfg = config
        if (cfg == null) {
            Log.e("ClickService", "No config set")
            return
        }
        if (job?.isActive == true) {
            Log.i("ClickService", "Already running")
            return
        }

        job = CoroutineScope(Dispatchers.Default).launch {
            for ((index, line) in cfg.lines.withIndex()) {
                if (!isActive) break

                // parse numbers: accept "5, 12,23,41,59" or "5 12 23 41 59"
                val nums = line.split(Regex("[,;\\s]+"))
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..(cfg.cols * 6) } // basic guard

                if (nums.isEmpty()) {
                    Log.w("ClickService", "Empty/invalid line at ${index+1}")
                    continue
                }

                // log / broadcast progress optionally
                Log.i("ClickService", "Processing line ${index+1}/${cfg.lines.size}: $nums")

                // tap each number
                for (n in nums) {
                    if (!isActive) break
                    val row = (n - 1) / cfg.cols
                    val col = (n - 1) % cfg.cols
                    val x = cfg.xStart + col * cfg.dX
                    val y = cfg.yStart + row * cfg.dY
                    performTapSafe(x.toFloat(), y.toFloat())
                    delay(max(50L, cfg.tapDelayMs))
                }

                // optional: tap confirm
                performTapSafe(cfg.confirmX.toFloat(), cfg.confirmY.toFloat())

                // delay between combinations (seconds)
                delay(max(0L, cfg.delayBetweenSec.toLong() * 1000L))
            }
            Log.i("ClickService", "Finished all lines or stopped")
        }
    }

    private fun stopProcess() {
        job?.cancel()
        job = null
    }

    private fun performTapSafe(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                .build()
            // dispatchGesture is asynchronous; we don't need callback here
            val success = dispatchGesture(gesture, object : GestureDescription.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    // no-op
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w("ClickService", "Gesture cancelled")
                }
            }, null)
            if (!success) {
                Log.w("ClickService", "dispatchGesture returned false")
            }
            // small sleep to let UI react; dispatchGesture is async anyway
            try { Thread.sleep(30) } catch (e: InterruptedException) { /* ignore */ }
        } else {
            Log.e("ClickService", "Dispatch gestures require API >= N")
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) { /* no-op */ }
    override fun onInterrupt() { stopProcess() }
}
