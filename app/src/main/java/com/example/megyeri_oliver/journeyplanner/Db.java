package com.example.megyeri_oliver.journeyplanner;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import net.maritimecloud.internal.core.javax.json.Json;
import net.maritimecloud.internal.core.javax.json.JsonArray;
import net.maritimecloud.internal.core.javax.json.JsonObject;
import net.maritimecloud.internal.core.javax.json.JsonReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Db {
	private static DatabaseOpenHelper openHelper;
	private static SQLiteDatabase database;
    private static String version;
    private static String latestVersion;

	public static void init(Context context) {
		openHelper = new DatabaseOpenHelper(context);

		try {
			database = openHelper.getReadableDatabase();
		}
		catch (Exception e) {
			System.err.println(e);
		};
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
		Cursor resultSet;
		ArrayList<Stop> result = null;

		long stopID = s.getID();
		Calendar calendar = s.getDate();

		String dayName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);

		String date = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
		String time = new SimpleDateFormat("HH:mm").format(calendar.getTime());

		String query =
				"SELECT 	stop_times.stop_id, stops.stop_name, stops.stop_lat, stops.stop_lon, " +
						"			routes.route_short_name, sub_stop_times.departure_time, stop_times.arrival_time, " +
            "     trips.direction_id, stop_times.stop_sequence, routes.route_type, stop_times.trip_id " +
						"FROM stops, " +
						"			stop_times, " +
						"			trips, " +
						"			routes, " +
						"			( SELECT trip_id, stop_sequence, departure_time FROM stop_times WHERE stop_id = " + stopID + ") AS sub_stop_times, " +
						"     ( SELECT service_id FROM calendar WHERE '" + date + "' >= date(start_date) AND '" + date + "' <= date(end_date) AND " + dayName + " = 1 ) AS sub_calendar " +
						"WHERE 	time(stop_times.departure_time) >= '" + time + "' " +
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

	public static boolean isTripHasDestination(Stop startStop) {
		Cursor resultSet;
		ArrayList<Long> result = new ArrayList<>();

		Calendar arrivalDate = startStop.getDate();
		String tripId = startStop.getPath().getTripId();

		if( ! tripId.isEmpty() ) {
      String time = new SimpleDateFormat("HH:mm::ss").format(arrivalDate.getTime());

      String query = "SELECT stop_id FROM stop_times WHERE trip_id = '" + tripId + "' AND time(arrival_time) > '" + time + "'";

      resultSet = database.rawQuery(query, null);
      if (resultSet.moveToFirst()) {
        do {
          long stopId = resultSet.getLong(0);
          result.add(stopId);
        } while (resultSet.moveToNext());
      }

      for(Long r: result) {
        for(Stop s: startStop.getDestinationStops()) {
          if(s.getID() == r) {
            return true;
          }
        }
      }
    }

    return false;
	}

	public static Stop goThroughTrip(Stop startStop) throws ParseException {
		Cursor resultSet;

		Calendar arrivalDate = startStop.getDate();
		String tripId = startStop.getPath().getTripId();

		if( ! tripId.isEmpty() ) {
			String time = new SimpleDateFormat("HH:mm::ss").format(arrivalDate.getTime());

			String query = "SELECT stops.stop_id, stop_name, stops.stop_lat, stops.stop_lon, " +
                      "stop_times.arrival_time, stop_times.stop_sequence " +
                      "FROM stop_times, stops " +
                      "WHERE trip_id='" + tripId + "' AND time(arrival_time) > '" + time + "' " +
                      "AND stops.stop_id = stop_times.stop_id " +
                      "ORDER BY stop_times.stop_sequence";

			resultSet = database.rawQuery(query, null);
      if (resultSet.moveToFirst()) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Stop prevStop = startStop;

        do {
          Calendar arrivalTime = new GregorianCalendar();

					long stopId = resultSet.getLong(0);
					String stopName = resultSet.getString(1);
					double stopLat = resultSet.getDouble(2);
					double stopLon = resultSet.getDouble(3);
          arrivalTime.setTime(timeFormat.parse(resultSet.getString(4)));
          int stopSequence = resultSet.getInt(5);

          Path path = new Path(startStop.getPath().getServiceName(), prevStop.getPath().getArrivalTime(),
                                arrivalTime, prevStop, startStop.getPath().getRouteType(), tripId);
          path.setDirectionId(startStop.getPath().getDirectionId());
          Stop r = new Stop(stopId, stopName, stopLat, stopLon, path);
          r.setSequence(stopSequence);

          for(Stop ds: startStop.getDestinationStops()) {
            if(ds.getID() == r.getID()) {
              return r;
            }
          }
				} while (resultSet.moveToNext());
			}
		}

		return null;
	}

	public static ArrayList<Stop> getAllStops() {
		Cursor resultSet;
		ArrayList<Stop> result = null;

		String query = "SELECT * FROM stops";

		resultSet = database.rawQuery(query, null);
		result = getStopsFromResultSet(resultSet);

		return result;
	}

    public static String getVersion() {
        return version;
    }

    public static boolean isDatabaseUpToDate() {
        version = "x";  //if version table is empty (application is running for the first time)

        Cursor resultSet = database.rawQuery("SELECT version FROM version", null);
        if( resultSet.moveToFirst() ) {
            version = resultSet.getString(0);
        }

        try {
            URL url = new URL("https://api.transitfeeds.com/v1/getFeedVersions?key=ee5f58ac-25ae-465a-b39d-9e7bb8cc662f&feed=dakk%2F625&page=1&limit=10&err=1&warn=1");
            InputStream inputStream = url.openStream();
            JsonReader jsonReader = Json.createReader(inputStream);

            JsonObject obj = jsonReader.readObject();
            obj = obj.getJsonObject("results");
            JsonArray array = obj.getJsonArray("versions");
            obj = array.getValuesAs(JsonObject.class).get(0);
            latestVersion = obj.getString("id");
        }
        catch(Exception e) {
            System.err.println(e);
        }

        return version.equals(latestVersion) ? true : false;
    }

    public static void update(Context context) {
        final String ZIP_FILE_NAME = "db.zip";

        try {
            byte[] buffer = new byte[1024];
            int length;

            URL url = new URL("https://transitfeeds.com/p/dakk/625/latest/download");

            InputStream inputStream = new BufferedInputStream( url.openStream() );
            OutputStream outputStream = context.openFileOutput(ZIP_FILE_NAME, Context.MODE_PRIVATE);

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            ZipInputStream zis = new ZipInputStream(context.openFileInput(ZIP_FILE_NAME));

            for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
                FileOutputStream fos = context.openFileOutput(ze.getName(), Context.MODE_PRIVATE);

                while ((length = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }

                fos.close();
            }

            zis.closeEntry();
            zis.close();
        } catch (Exception e) {
            System.err.println(e);
        }

        try {
            database.close();
            database = openHelper.getWritableDatabase();

            Db.updateStopTimesTable(context);
            Db.updateCalendarTable(context);
            Db.updateRoutesTable(context);
            Db.updateStopsTable(context);
            Db.updateTripsTable(context);

            String sql = "DELETE FROM version";
            database.execSQL(sql);
            sql = "INSERT INTO version VALUES (\"" + latestVersion + "\")";
            database.execSQL(sql);

            Db.version = latestVersion;

            database.close();
            database = openHelper.getReadableDatabase();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private static void updateStopTimesTable(Context context) throws SQLException, IOException {
        database.execSQL("DELETE FROM stop_times");

        String sql = "INSERT INTO stop_times VALUES (?,?,?,?,?,?,?,?)";

		CSVParser csvParser = new CSVParser( new InputStreamReader(context.openFileInput("stop_times.txt")),
				CSVFormat.RFC4180.
				withHeader() );

        database.beginTransaction();

        SQLiteStatement pstmt = database.compileStatement(sql);

		for(CSVRecord csvRecord: csvParser) {
            pstmt.bindString(1, csvRecord.get(0));
            pstmt.bindString(2, csvRecord.get(1));
            pstmt.bindString(3, csvRecord.get(2));
            pstmt.bindLong(4, Long.parseLong(csvRecord.get(3)));
            pstmt.bindLong(5, Long.parseLong(csvRecord.get(4)));
            pstmt.bindLong(6, Long.parseLong(csvRecord.get(5)));
            pstmt.bindLong(7, Long.parseLong(csvRecord.get(6)));
            pstmt.bindLong(8, Long.parseLong(csvRecord.get(7)));

            pstmt.execute();
            pstmt.clearBindings();
		}
        csvParser.close();

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    private static void updateCalendarTable(Context context) throws SQLException, IOException {
        database.execSQL("DELETE FROM calendar");

        String sql = "INSERT INTO calendar VALUES (?,?,?,?,?,?,?,?,?,?)";

        CSVParser csvParser = new CSVParser( new InputStreamReader(context.openFileInput("calendar.txt")),
                CSVFormat.RFC4180.
                        withHeader() );

        database.beginTransaction();

        SQLiteStatement pstmt = database.compileStatement(sql);

        for(CSVRecord csvRecord: csvParser) {
            String startDate = csvRecord.get(8).substring(0,4) + "-" + csvRecord.get(8).substring(4,6) + "-" + csvRecord.get(8).substring(6,8);
            String endDate = csvRecord.get(9).substring(0,4) + "-" + csvRecord.get(9).substring(4,6) + "-" + csvRecord.get(9).substring(6,8);

            pstmt.bindString(1, csvRecord.get(0));
            pstmt.bindLong(2, Long.parseLong(csvRecord.get(1)));
            pstmt.bindLong(3, Long.parseLong(csvRecord.get(2)));
            pstmt.bindLong(4, Long.parseLong(csvRecord.get(3)));
            pstmt.bindLong(5, Long.parseLong(csvRecord.get(4)));
            pstmt.bindLong(6, Long.parseLong(csvRecord.get(5)));
            pstmt.bindLong(7, Long.parseLong(csvRecord.get(6)));
            pstmt.bindLong(8, Long.parseLong(csvRecord.get(7)));
            pstmt.bindString(9, startDate);
            pstmt.bindString(10, endDate);

            pstmt.execute();
            pstmt.clearBindings();
        }
        csvParser.close();

        database.setTransactionSuccessful();
        database.endTransaction();
	}

	private static void updateRoutesTable(Context context) throws SQLException, IOException {
		database.execSQL("DELETE FROM routes");

		String sql = "INSERT INTO routes VALUES (?,?,?,?,?,?,?,?,?)";

        CSVParser csvParser = new CSVParser( new InputStreamReader(context.openFileInput("routes.txt")),
                CSVFormat.RFC4180.
                        withHeader() );

		database.beginTransaction();

		SQLiteStatement pstmt = database.compileStatement(sql);
        for(CSVRecord csvRecord: csvParser) {
			pstmt.bindLong(1, Long.parseLong(csvRecord.get(0)));
			pstmt.bindLong(2, Long.parseLong(csvRecord.get(1)));
			pstmt.bindString(3, csvRecord.get(2));
			pstmt.bindString(4, csvRecord.get(3));
			pstmt.bindString(5, csvRecord.get(4));
			pstmt.bindLong(6, Long.parseLong(csvRecord.get(5)));
			pstmt.bindString(7, csvRecord.get(6));
			pstmt.bindString(8, csvRecord.get(7));
			pstmt.bindString(9, csvRecord.get(8));

			pstmt.execute();
			pstmt.clearBindings();
		}

		database.setTransactionSuccessful();
		database.endTransaction();
	}

	private static void updateStopsTable(Context context) throws SQLException, IOException {
        database.execSQL("DELETE FROM stops");

        String sql = "INSERT INTO stops VALUES (?,?,?,?)";

        CSVParser csvParser = new CSVParser( new InputStreamReader(context.openFileInput("stops.txt")),
                CSVFormat.RFC4180.
                        withHeader() );

        database.beginTransaction();

        SQLiteStatement pstmt = database.compileStatement(sql);
        for(CSVRecord csvRecord: csvParser) {
            pstmt.bindLong(1, Long.parseLong(csvRecord.get(0)));
            pstmt.bindString(2, csvRecord.get(1));
            pstmt.bindDouble(3, Double.parseDouble(csvRecord.get(2)));
            pstmt.bindDouble(4, Double.parseDouble(csvRecord.get(3)));

            pstmt.execute();
            pstmt.clearBindings();
        }

        database.setTransactionSuccessful();
        database.endTransaction();
	}

	private static void updateTripsTable(Context context) throws SQLException, IOException {
        database.execSQL("DELETE FROM trips");

        String sql = "INSERT INTO trips VALUES (?,?,?,?,?,?,?)";

        CSVParser csvParser = new CSVParser( new InputStreamReader(context.openFileInput("trips.txt")),
                CSVFormat.RFC4180.
                        withHeader() );

        database.beginTransaction();

        SQLiteStatement pstmt = database.compileStatement(sql);
        for(CSVRecord csvRecord: csvParser) {
            pstmt.bindLong(1, Long.parseLong(csvRecord.get(0)));
            pstmt.bindString(2, csvRecord.get(1));
            pstmt.bindString(3, csvRecord.get(2));
            pstmt.bindString(4, csvRecord.get(3));
            pstmt.bindLong(5, Long.parseLong(csvRecord.get(4)));
            pstmt.bindLong(6, Long.parseLong(csvRecord.get(5)));
            pstmt.bindLong(7, Long.parseLong(csvRecord.get(6)));

            pstmt.execute();
            pstmt.clearBindings();
        }

        database.setTransactionSuccessful();
        database.endTransaction();
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
				String tripId = rs.getString(10);

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
					Path path = new Path(serviceName, departureTime, arrivalTime, s, routeType, tripId);
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