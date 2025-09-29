package com.example.lotoaclick

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvFile: TextView
    private lateinit var btnPickFile: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var edtXStart: EditText
    private lateinit var edtYStart: EditText
    private lateinit var edtDX: EditText
    private lateinit var edtDY: EditText
    private lateinit var edtCols: EditText
    private lateinit var edtDelayBetween: EditText
    private lateinit var edtTapDelay: EditText

    private var pickedUri: Uri? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickedUri = uri
            tvFile.text = "Fișier: ${uri.path}"
            tvStatus.text = "Status: fișier selectat"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvFile = findViewById(R.id.tvFile)
        btnPickFile = findViewById(R.id.btnPickFile)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        edtXStart = findViewById(R.id.edtXStart)
        edtYStart = findViewById(R.id.edtYStart)
        edtDX = findViewById(R.id.edtDX)
        edtDY = findViewById(R.id.edtDY)
        edtCols = findViewById(R.id.edtCols)
        edtDelayBetween = findViewById(R.id.edtDelayBetween)
        edtTapDelay = findViewById(R.id.edtTapDelay)

        btnPickFile.setOnClickListener {
            pickFileLauncher.launch(arrayOf("text/plain"))
        }

        btnStart.setOnClickListener {
            if (pickedUri == null) {
                tvStatus.text = "Selectează mai întâi fișierul."
                return@setOnClickListener
            }
            if (!isAccessibilityEnabled(this)) {
                // prompt to enable accessibility
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                tvStatus.text = "Activează serviciul Accessibility pentru LotoAutoClick."
                return@setOnClickListener
            }
            // read file lines (on background)
            scope.launch {
                val lines = readLinesFromUri(pickedUri!!)
                if (lines.isEmpty()) {
                    tvStatus.text = "Fișier gol sau invalid."
                    return@launch
                }
                // build config
                val cfg = Config(
                    lines = lines,
                    xStart = edtXStart.text.toString().toIntOrNull() ?: 152,
                    yStart = edtYStart.text.toString().toIntOrNull() ?: 620,
                    dX = edtDX.text.toString().toIntOrNull() ?: 120,
                    dY = edtDY.text.toString().toIntOrNull() ?: 110,
                    cols = edtCols.text.toString().toIntOrNull() ?: 10,
                    delayBetweenSec = edtDelayBetween.text.toString().toIntOrNull() ?: 10,
                    tapDelayMs = edtTapDelay.text.toString().toLongOrNull() ?: 250L,
                    confirmX = (edtXStart.text.toString().toIntOrNull() ?: 152),
                    confirmY = (edtYStart.text.toString().toIntOrNull() ?: 620) + (edtDY.text.toString().toIntOrNull() ?: 110) * 6
                )
                // pass config to service holder
                ClickAccessibilityService.config = cfg
                // start service action
                val i = Intent(this@MainActivity, ClickAccessibilityService::class.java)
                i.action = ClickAccessibilityService.ACTION_START
                startService(i)
                tvStatus.text = "Start requested — treci în aplicația loto."
            }
        }

        btnStop.setOnClickListener {
            val i = Intent(this, ClickAccessibilityService::class.java)
            i.action = ClickAccessibilityService.ACTION_STOP
            startService(i)
            tvStatus.text = "Stop requested."
        }
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val am = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return am?.contains(packageName) ?: false
    }

    private suspend fun readLinesFromUri(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val res = mutableListOf<String>()
        try {
            contentResolver.openInputStream(uri)?.use { inStream ->
                BufferedReader(InputStreamReader(inStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) res.add(trimmed)
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        res
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

/** simple config data holder */
data class Config(
    val lines: List<String> = emptyList(),
    val xStart: Int = 152,
    val yStart: Int = 620,
    val dX: Int = 120,
    val dY: Int = 110,
    val cols: Int = 10,
    val delayBetweenSec: Int = 10,
    val tapDelayMs: Long = 250L,
    val confirmX: Int = 152,
    val confirmY: Int = 620
)
