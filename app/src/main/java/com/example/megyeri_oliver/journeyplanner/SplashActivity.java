package com.example.megyeri_oliver.journeyplanner;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Db.init(this);
        new CheckForDatabaseUpdate().execute();

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);

        finish();
    }

    private class CheckForDatabaseUpdate extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            return Db.isDatabaseUpToDate();
        }

        @Override
        protected void onPostExecute(Boolean isUpToDate) {
            if( ! isUpToDate) {
                Intent intent = new Intent(SplashActivity.this, DatabaseUpdateActivity.class);
                startActivity(intent);
            }
        }
    }
}