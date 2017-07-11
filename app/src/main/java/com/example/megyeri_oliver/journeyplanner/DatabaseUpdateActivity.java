package com.example.megyeri_oliver.journeyplanner;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class DatabaseUpdateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_update);

        new UpdateDatabase().execute();
    }

    private class UpdateDatabase extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Db.update(getApplicationContext());
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            finish();
        }
    }
}
