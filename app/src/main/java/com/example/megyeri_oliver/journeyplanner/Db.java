package com.example.megyeri_oliver.journeyplanner;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class Db {
	private static DatabaseOpenHelper openHelper;
	private static SQLiteDatabase database;
	private static Db instance;

	public static void init(Context context) {
		openHelper = new DatabaseOpenHelper(context);
		try {
			openHelper.createDatabase();
		}
		catch (Exception e) {
			System.err.println(e);
		};
		try {
			database = openHelper.openDatabase();
		}
		catch (Exception e) {};
		//database = openHelper.getReadableDatabase();
	}

	public static List<String> test() {
		List<String> list = new ArrayList<>();
		Cursor cursor = null;
		try {
			cursor = database.rawQuery("SELECT service_id FROM calendar", null);
		} catch (Exception e) {
			System.out.println(e);
		}
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			list.add(cursor.getString(0));
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	public static Stop getStopByStopID(long stopID) {
		String query = "SELECT * FROM stops WHERE stop_id = " + stopID;
		Stop result = null;

		Cursor resultSet = database.rawQuery(query, null);
		resultSet.moveToFirst();

		long stopId = resultSet.getLong(0);
		String stopName = resultSet.getString(1);
		double stopLat = resultSet.getDouble(2);
		double stopLon = resultSet.getDouble(3);

		result = new Stop(stopId, stopName, stopLat, stopLon);

		return result;
	}

	public static ArrayList<Stop> getNexts(Stop s) {
		Cursor resultSet = null;
		ArrayList<Stop> result = null;

		long stopID = s.getID();
		Calendar calendar = s.getDate();

		String dayName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);

		String date = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
		String time = new SimpleDateFormat("HH:mm").format(calendar.getTime());

		String query =
				"SELECT 	stop_times.stop_id, stops.stop_name, stops.stop_lat, stops.stop_lon, " +
						"			routes.route_short_name, sub_stop_times.departure_time, stop_times.arrival_time, trips.direction_id, stop_times.stop_sequence, routes.route_type " +
						"FROM 		stops, " +
						"			stop_times, " +
						"			trips, " +
						"			routes, " +
						"			( SELECT trip_id, stop_sequence, departure_time FROM stop_times WHERE stop_id = " + stopID + ") AS sub_stop_times, " +
						"           ( SELECT service_id FROM calendar WHERE '" + date + "' >= date(start_date) AND '" + date + "' <= date(end_date) AND " + dayName + " = 1 ) AS sub_calendar " +
						"WHERE 		time(stop_times.departure_time) >= '" + time + "' " +
						"AND		stop_times.stop_sequence = sub_stop_times.stop_sequence + 1 " +
						"AND		stop_times.trip_id = trips.trip_id " +
						"AND		trips.route_id = routes.route_id " +
						"AND		stop_times.trip_id = sub_stop_times.trip_id " +
						"AND		trips.service_id = sub_calendar.service_id " +
						"AND		stop_times.stop_id = stops.stop_id " +
						"ORDER BY 	sub_stop_times.departure_time";

		resultSet = database.rawQuery(query, null);
		try {
			result = getStopsFromResultSet(resultSet, s);
		}
		catch (ParseException e) {
			System.out.println(e);
		}

		return result;
	}

	public static ArrayList<Stop> getAllStops() {
		Cursor resultSet = null;
		ArrayList<Stop> result = null;

		String query = "SELECT * FROM stops";

		resultSet = database.rawQuery(query, null);
		result = getStopsFromResultSet(resultSet);

		return result;
	}


	private static ArrayList<Stop> getStopsFromResultSet(Cursor rs, Stop s) throws ParseException{
		ArrayList<Stop> result = new ArrayList<Stop>();
		Calendar calendar = s.getDate();
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

		Boolean hasPathWithCurrentService = false;
		if (rs.moveToFirst()) {
			do {
				Calendar departureTime = new GregorianCalendar();
				Calendar arrivalTime = new GregorianCalendar();

				long stopID = rs.getLong(0);
				String stopName = rs.getString(1);
				double stopLat = rs.getDouble(2);
				double stopLon = rs.getDouble(3);
				String serviceName = rs.getString(4);
				departureTime.setTime(timeFormat.parse(rs.getString(5)));
				arrivalTime.setTime(timeFormat.parse(rs.getString(6)));
				int directionId = rs.getInt(7);
				int stopSequence = rs.getInt(8);
				int routeType = rs.getInt(9);

				departureTime.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
				arrivalTime.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));

				//A legkorábbiak leválogatásához, járatszámonként
				boolean ok = true;
				for (Stop stop : result) {
					if (stop.getPath().getServiceName().equals(serviceName)) ok = false;
				}

				//Lehetőség szerint vonalon maradás, még ha lassabb is a járat
				if (!hasPathWithCurrentService
						&& s.getPath() != null
						&& s.getPath().getServiceName().equals(serviceName)) {
					for (Stop stop : result) {
						if (stop.getID() == stopID) {
							result.remove(stop);
							break;
						}
					}
					hasPathWithCurrentService = true;
				}

				if (ok) {
					Path path = new Path(serviceName, departureTime, arrivalTime, s, routeType);
					path.setDirectionId(directionId);
					Stop r = new Stop(stopID, stopName, stopLat, stopLon, path);
					r.setSequence(stopSequence);
					result.add(r);
				}
			} while (rs.moveToNext());
		}

		return result;
	}

	private static ArrayList<Stop> getStopsFromResultSet(Cursor rs) {
		ArrayList<Stop> result = new ArrayList<Stop>();

		if (rs.moveToFirst()) {
			do {
				long stopID = rs.getLong(0);
				String stopName = rs.getString(1);
				double stopLat = rs.getDouble(2);
				double stopLon = rs.getDouble(3);

				result.add(new Stop(stopID, stopName, stopLat, stopLon));
			} while (rs.moveToNext());
		}

		return result;
	}
}