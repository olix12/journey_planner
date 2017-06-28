package com.example.megyeri_oliver.journeyplanner;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/*
To-do: Investigation needed: _id, etc.
 */

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "timetable5.db";
    private static final String DATABASE_NAME2 = "databases/timetable5.db";
    private static final int DATABASE_VERSION = 14; //not used?
    private static final String DATABASE_PATH = "/data/data/com.example.megyeri_oliver.journeyplanner/databases/";

    private final Context context;
    private SQLiteDatabase database;

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public void createDatabase() throws IOException {
        this.getReadableDatabase();

        InputStream input = context.getAssets().open(DATABASE_NAME2);
        String outputFileName = DATABASE_PATH + DATABASE_NAME;
        OutputStream output = new FileOutputStream(outputFileName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        output.flush();
        output.close();
        input.close();
    }

    public SQLiteDatabase openDatabase() {
        if(!isDatabaseExist()) {
            try {
                this.createDatabase();
            } catch (Exception e) {
                System.err.println("Error creating database.");
            }
        }
        try {
            this.database = SQLiteDatabase.openDatabase(DATABASE_PATH+DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
        }
        catch(Exception e) {
            System.out.print("Error opening database.");
        }
        return this.database;
    }

    private boolean isDatabaseExist() {
        SQLiteDatabase db;

        try {
            db = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
        }
        catch (SQLiteException e) {
            return false;
        }

        db.close();
        return true;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

}
