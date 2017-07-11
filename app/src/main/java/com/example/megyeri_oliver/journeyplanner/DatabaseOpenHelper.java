package com.example.megyeri_oliver.journeyplanner;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/*
To-do: Investigation needed: _id, etc.
 */

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "timetable.db";
    private static final int DATABASE_VERSION = 15; //not used?
//    private static final String DATABASE_PATH = "/data/data/com.example.megyeri_oliver.journeyplanner/databases/";

    private final Context context;

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE `calendar` ( `service_id` TEXT, `monday` INTEGER, `tuesday` INTEGER, `wednesday` INTEGER, `thursday` INTEGER, `friday` INTEGER, `saturday` INTEGER, `sunday` INTEGER, `start_date` TEXT, `end_date` TEXT, PRIMARY KEY(`service_id`) )");
        db.execSQL("CREATE TABLE `routes` ( `route_id` INTEGER, `agency_id` INTEGER, `route_short_name` TEXT, `route_long_name` TEXT, `route_desc` TEXT, `route_type` INTEGER, `route_url` TEXT, `route_color` TEXT, `route_text_color` TEXT, PRIMARY KEY(`route_id`) )");
        db.execSQL("CREATE TABLE `stop_times` ( `trip_id` TEXT, `arrival_time` TEXT, `departure_time` TEXT, `stop_id` INTEGER, `stop_sequence` INTEGER, `pickup_type` INTEGER, `drop_off_type` INTEGER, `shape_dist_traveled` INTEGER, PRIMARY KEY(`trip_id`,`stop_sequence`) )");
        db.execSQL("CREATE TABLE \"stops\" ( `stop_id` INTEGER, `stop_name` TEXT, `stop_lat` REAL, `stop_lon` REAL, PRIMARY KEY(`stop_id`) )");
        db.execSQL("CREATE TABLE `trips` ( `route_id` INTEGER, `service_id` TEXT, `trip_id` TEXT, `trip_headsign` TEXT, `direction_id` INTEGER, `shape_id` INTEGER, `wheelchair_accessible` INTEGER, PRIMARY KEY(`trip_id`) )");
        db.execSQL("CREATE TABLE `version` ( `version` TEXT, PRIMARY KEY(`version`) )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

}
