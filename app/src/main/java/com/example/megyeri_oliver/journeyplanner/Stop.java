package com.example.megyeri_oliver.journeyplanner;

import android.util.Log;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;

public class Stop implements Comparable<Stop>, Serializable {
	public final static String ORIGIN_NAME = "Origin";
	public final static String DESTINATION_NAME = "Destination";
	public final static long DEFAULT_ID = 0;
	private final static int ENDPOINT_RADIUS = 400;	//meter
	private final static int ON_JOURNEY_RADIUS = 300;	//meter
	private final static double WALK_SPEED = 1.4; //m/s

	public final static double SZEGED[] = {46.252649, 20.153506};
	private final static double NE_SZEGED[] = {46.294712, 20.229383};
	private final static double SE_SZEGED[] = {46.191621, 20.220031};
	private final static double SW_SZEGED[] = {46.189542, 19.995670};
	private final static double NW_SZEGED[] = {46.306572, 20.006395};

	private long ID;
	private String name;
	private double longitude;
	private double latitude;
	private Calendar date;
	private Path path;
	private Stop first;

	private Stop destination;
	private ArrayList<Stop> destinationStops;
	private int change;
	private int sequence;

	private int sumWalkTime;
	private int stopCount;

	private boolean isFastForwardPossible;

	public Stop(long ID, String name, double latitude, double longitude) {
		this.ID = ID;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.change = 0;
		this.date = Calendar.getInstance();
		this.first = this;
		this.destinationStops = new ArrayList<>();

		this.sumWalkTime = 0;
		this.stopCount = 0;

		this.isFastForwardPossible = false;
	}

	public Stop(long ID, String name, double latitude, double longitude, Path path) {
		this.ID = ID;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.date = path.getArrivalTime();
		this.path = path;

		this.first = path.getDepartureStop().getFirst();
		this.destination = path.getDepartureStop().getFirst().getDestination();
		this.destinationStops = path.getDepartureStop().getDestinationStops();

		this.isFastForwardPossible = false;

		this.change = 0;
		if( path.getDepartureStop().getPath() != null ) {
			if( !path.getDepartureStop().getPath().getServiceName().equals(this.path.getServiceName()) ) {
				this.change = path.getDepartureStop().getChange() + 1;

				if(Db.isTripHasDestination(this)) {
					Log.i("Journey Planner","Destination found!");
					isFastForwardPossible = true;
					fastForward();
				}
			}
			else {
				this.change = path.getDepartureStop().getChange();
			}
		}

		this.sumWalkTime = path.getDepartureStop().getSumWalkTime();
		this.stopCount = path.getDepartureStop().getStopCount() + 1;
	}

	public long getID() {
		return ID;
	}

	public int getChange() {
		return change;
	}

	public String getName() {
		return name;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public Calendar getDate() {
		return this.date;
	}

	public void setDate(Calendar date) {
		this.date = date;
	}

	public Path getPath() {
		return path;
	}

	public Stop getFirst() {
		return first;
	}

	public Stop getDestination() {
		return destination;
	}

	public ArrayList<Stop> getDestinationStops() {
		return this.destinationStops;
	}

	public void setDestinationAndDestinationStops(Stop destination) {
		this.destination = destination;
		this.destinationStops = destination.getNexts();
	}

	public int getSumWalkTime() { return sumWalkTime; }

	public void setSumWalkTime(int sumWalkTime) { this.sumWalkTime = sumWalkTime; }

	public int getStopCount() {
		return stopCount;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public boolean isFastForwardPossible() { return isFastForwardPossible; }

	public Stop fastForward() {
	  System.out.println("Fast forwarding!");
	  Stop result = null;
	  try {
      result = Db.goThroughTrip(this);
    }
    catch(ParseException e) {
	    System.err.println("Parse error: something really went wrong.");
    }
    return result;
  }

	public ArrayList<Stop> getNexts() {
		ArrayList<Stop> result = new ArrayList<Stop>();
		ArrayList<Stop> tempArray;

		tempArray = Db.getNexts(this);
		if(tempArray != null) {
			result.addAll(tempArray);
		}

		if(this.path == null) {
			tempArray = getNearbyStops(ENDPOINT_RADIUS);
			if (tempArray.size() == 0) {
				tempArray = getNearbyStops((int)(1.5*ENDPOINT_RADIUS));
			}
			result.addAll(tempArray);
		}
		else if( ! this.path.getServiceName().equals(Path.WALK_SERVICE_NAME) ) {	//A sétafikálás kiiktatására
			tempArray = getNearbyStops(ON_JOURNEY_RADIUS);
			if (tempArray != null) {
				result.addAll(tempArray);
			}
		}

		return result;
	}

	public int compareTo(Stop x) {
		return this.date.compareTo(x.date);
	}

	public Stop getPrev() {
		return (this.path != null) ? this.path.getDepartureStop() : null;
	}

	public String toString() {
		return this.name + ", " + this.date.getTime().getHours() + ":" + this.date.getTime().getMinutes();
	}

	//Megadott sugáron belüli szomszédos megállók
	private ArrayList<Stop> getNearbyStops(int radius) {
		ArrayList<Stop> result = new ArrayList<Stop>();

		Calendar arrival = (Calendar)this.getDate().clone();
		arrival.set(Calendar.MINUTE, arrival.get(Calendar.MINUTE)+1);

		for(Stop s: Db.getAllStops()) {
			if( getDistance(s) <= radius && s.ID != this.ID ) {
				Calendar arrivalDate = (Calendar)this.date.clone();

				int walkTime = (int)Math.ceil( getDistance(s) / WALK_SPEED / 60 );

				arrivalDate.set(Calendar.MINUTE, date.get(Calendar.MINUTE) + walkTime);

				Path path = new Path(Path.WALK_SERVICE_NAME, (Calendar)this.date.clone(), arrivalDate,
                this, Path.ROUTE_TYPE_WALK, "");
				Stop nearbyStop = new Stop(s.getID(), s.getName(), s.getLatitude(), s.getLongitude(), path);
				nearbyStop.setSumWalkTime(this.sumWalkTime + walkTime);	//logolás

				result.add(nearbyStop);
			}
		}

		return result;
	}

	public boolean isTiszaCrossed(Stop s) {
		if( intersect(this.latitude, this.longitude, s.latitude, s.longitude, 46.226370, 20.142686, 46.254330, 20.157019)
				&& intersect(this.latitude, this.longitude, s.latitude, s.longitude, 46.254330, 20.157019, 46.256289, 20.169722)
				&& intersect(this.latitude, this.longitude, s.latitude, s.longitude, 46.256289, 20.169722, 46.252431, 20.193841) ) {
			return true;
		}
		else {
			return false;
		}
	}

	public boolean isInSzeged() {
		if( !intersect(this.latitude, this.longitude, SZEGED[0], SZEGED[1], SW_SZEGED[0], SW_SZEGED[1], NW_SZEGED[0], NW_SZEGED[1])
				&& !intersect(this.latitude, this.longitude, SZEGED[0], SZEGED[1], NW_SZEGED[0], NW_SZEGED[1], NE_SZEGED[0], NE_SZEGED[1])
				&& !intersect(this.latitude, this.longitude, SZEGED[0], SZEGED[1], NE_SZEGED[0], NE_SZEGED[1], SE_SZEGED[0], SE_SZEGED[1])
				&& !intersect(this.latitude, this.longitude, SZEGED[0], SZEGED[1], SE_SZEGED[0], SE_SZEGED[1], SW_SZEGED[0], SW_SZEGED[1]) ) {
			return true;
		}
		else {
			return false;
		}
	}

	/*Haversine formula*/
	public double getDistance(Stop stop) {
		int R = 6371;	//meter
		double phi1 = Math.toRadians(this.latitude);
		double phi2 = Math.toRadians(stop.latitude);
		double deltaPhi = Math.toRadians(this.latitude-stop.latitude);
		double deltaLambda = Math.toRadians(this.longitude-stop.longitude);

		double a = Math.pow(Math.sin(deltaPhi/2), 2) + Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(deltaLambda/2), 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

		return R * c * 1000;	//meter
	}

	private boolean intersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
		return turn(x1, y1, x2, y2, x3, y3)* turn(x1, y1, x2, y2, x4, y4) < 0	//(A,B,C)*(A,B,D)
				&& turn(x3, y3, x4, y4, x1, y1)* turn(x3, y3, x4, y4, x2, y2) < 0;	//(C,D,A)*(C,D,B)
	}

	private int direction(double x1, double y1, double x2, double y2) {
		double S = y1*x2-y2*x1;
		if(S<0) return -1;
		else if(S==0) return 0;
		else return 1;
	}

	private int turn(double x1, double y1, double x2, double y2, double x3, double y3) {
		return direction(x2-x1, y2-y1, x3-x1, y3-y1);	//B-A, C-A
	}
}

