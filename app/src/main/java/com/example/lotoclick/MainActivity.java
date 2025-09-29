package com.example.lotoaclick;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private Button startButton;
    private List<String> combinatii = new ArrayList<>();
    private int currentIndex = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);

        // Citește combinațiile dintr-un fișier aflat în res/raw/combinatii.txt
        loadCombinatii();

        startButton.setOnClickListener(v -> startAutoClick());
    }

    private void loadCombinatii() {
        InputStream inputStream = getResources().openRawResource(R.raw.combinatii);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                combinatii.add(line);
            }
            statusText.setText("Combinatii incarcate: " + combinatii.size());
        } catch (IOException e) {
            e.printStackTrace();
            statusText.setText("Eroare la citire!");
        }
    }

    private void startAutoClick() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentIndex < combinatii.size()) {
                    String combo = combinatii.get(currentIndex);
                    statusText.setText("Rulez: " + combo);

                    // Aici ar veni partea de auto-click real (simulare touch).
                    // Momentan doar afisam pe ecran.

                    currentIndex++;
                    handler.postDelayed(this, 10000); // delay 10 secunde
                } else {
                    statusText.setText("Toate combinatiile au fost rulate!");
                }
            }
        }, 1000);
    }
}
