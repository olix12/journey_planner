package com.example.megyeri_oliver.journeyplanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

public class Algorithm {
	final static int CHANGE = 3;
	final static boolean IS_FAST_FORWARD_ON = true;

	public static Stop algorithm(Stop origin) {
		PriorityQueue<Stop> routes = new PriorityQueue<>();
		HashMap<Long, Stop> recordings = new HashMap<>();

		routes.add(origin);
		ArrayList<Stop> destinations = origin.getDestinationStops();

		//it's maybe dead code, consider deleting...
		ArrayList<Stop> destinationTrips = new ArrayList<>();
		for(Stop s: destinations) {
			for(Stop sNext: s.getNexts()) {
				boolean ok = true;
				for(Stop t: destinationTrips) {
					if( sNext.getPath().getServiceName().equals( t.getPath().getServiceName() )
							&& sNext.getPath().getDirectionId() == t.getPath().getDirectionId() ) {
						ok = false;
						break;
					}
				}
				if(ok) {
					destinationTrips.add(sNext);
				}
			}
		}

		while( atDestination(recordings, destinations) == null ) {
			Stop head = routes.poll();

			//System.out.println(head.toString());

      if(head.isFastForwardPossible()) {
        System.out.println("Fast forwarding point!");
        if(IS_FAST_FORWARD_ON) {
          return head.fastForward();
        }
      }

			for(Stop nextStop: head.getNexts()) {
			    /*if( ! head.isFirst() ) {
                    for(Stop s : destinationTrips) {
                        if( head.getPath().getServiceName().equals(s.getPath().getServiceName())
                                && head.getPath().getDirectionId() == s.getPath().getDirectionId()
                                && head.getSequence() < s.getSequence() ) {
                            return goAlongDestinationTrip(head, destinations, recordings, destinationTrips);
                        }
                    }
                }*/

				if(nextStop.getChange() <= CHANGE) {
					if( recordings.get(nextStop.getID()) == null ) {
						recordings.put(nextStop.getID(), nextStop);
						routes.add(nextStop);
					}
					else if( recordings.get( nextStop.getID() ).getChange() >= nextStop.getChange()
										|| recordings.get( nextStop.getID() ).getSumWalkTime() > nextStop.getSumWalkTime()
										|| recordings.get( nextStop.getID() ).getStopCount() > nextStop.getStopCount() ) {
						routes.remove( recordings.get(nextStop.getID()) );
						recordings.remove(nextStop.getID());
						recordings.put(nextStop.getID(), nextStop);
						routes.add(nextStop);
					}
				}
			}

		}

		return atDestination(recordings, destinations);
	}

	private static Stop atDestination(HashMap<Long, Stop> recordings, ArrayList<Stop> destinations) {
		for(Stop s: destinations) {
			if(recordings.get(s.getID()) != null) {
				return recordings.get(s.getID());
			}
		}
		return null;
	}

	private static Stop goAlongDestinationTrip(Stop stop, ArrayList<Stop> destinations, HashMap<Long, Stop> recordings, ArrayList<Stop> destinationTrips) {
		final int GO_ALONG_LIMIT = 10;
		int count = 0;
		while( atDestination(recordings, destinations) == null ) {
			for(Stop nextStop: stop.getNexts()) {
				for(Stop s: destinationTrips) {
					if( nextStop.getPath().getServiceName().equals( s.getPath().getServiceName() )
							&& nextStop.getPath().getDirectionId() == s.getPath().getDirectionId() ) {
						stop = nextStop;
						recordings.put(nextStop.getID(), nextStop);
						break;
					}
				}
			}
			count++;
			if(count < GO_ALONG_LIMIT) return null;
		}
		return stop;
	}

}
