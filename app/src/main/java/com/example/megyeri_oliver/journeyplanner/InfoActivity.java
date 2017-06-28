package com.example.megyeri_oliver.journeyplanner;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.tram_bell);
        mp.start();
    }
}
