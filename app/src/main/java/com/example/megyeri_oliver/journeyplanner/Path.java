package com.example.megyeri_oliver.journeyplanner;

import java.io.Serializable;
import java.util.Calendar;

public class Path implements Serializable{
	public final static String WALK_SERVICE_NAME = "0";

	public final static int ROUTE_TYPE_BUS = 3;
	public final static int ROUTE_TYPE_TRAM = 0;
	public final static int ROUTE_TYPE_TROLLEYBUS = 800;
	public final static int ROUTE_TYPE_WALK = 1;

	private String serviceName;
	private Calendar departureTime;
	private Calendar arrivalTime;
	private Stop departureStop;

	private int directionId;
	private int routeType;

	public Path(String serviceName, Calendar departureTime, Calendar arrivalTime, Stop departureStop, int routeType) {
		this.serviceName = serviceName;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
		this.departureStop = departureStop;
		this.directionId = 0;
		this.routeType = routeType;
	}

	public String getServiceName() {
		return serviceName;
	}

	public Calendar getDepartureTime() {
		return departureTime;
	}

	public Calendar getArrivalTime() {
		return arrivalTime;
	}

	public Stop getDepartureStop() {
		return departureStop;
	}

	public int getDirectionId() { return directionId; }

	public void setDirectionId(int directionId) { this.directionId = directionId; }

	public int getRouteType() {
		return routeType;
	}

	public String toString() {
		return this.serviceName + " ";
	}
}
