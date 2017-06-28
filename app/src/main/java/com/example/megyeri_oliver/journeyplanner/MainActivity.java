package com.example.megyeri_oliver.journeyplanner;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity
        extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerDragListener {

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private Marker mOriginMarker;
    private Marker mDestinationMarker;
    private Polyline mPolyline;

    private ArrayList<Marker> mPath;

    private Calendar mDate;
    private TimePickerDialog mTimePickerDialog;

    private ProgressBar mProgressBar;

    private int REQUEST_ACCESS_FINE_LOCATION = 1;   //not used

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        Db.init(this);

        this.mDate = Calendar.getInstance();

        this.mTimePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                mDate.set(Calendar.HOUR, hour);
                mDate.set(Calendar.MINUTE, minute);
            }
        }, this.mDate.get(Calendar.HOUR), this.mDate.get(Calendar.MINUTE), true);

        this.mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        ((Button)findViewById(R.id.button2)).setEnabled(false);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                onMapLongClick(place.getLatLng());
            }

            @Override
            public void onError(Status status) {
                System.err.println("Auto complete error: " + status);
            }
        });
        autocompleteFragment.setHint(getResources().getString(R.string.autoCompleteHint));

        if( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    protected void onStart() {
        this.mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        this.mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void onConnectionSuspended(int cause) {
        //handle this...
    }

    public void onConnectionFailed(ConnectionResult result) {
        //handle this...
    }

    public void onConnected(Bundle connectionHint) throws SecurityException {   //handle exception...
        Location location = LocationServices.FusedLocationApi.getLastLocation(this.mGoogleApiClient);

        LatLng point;
        if( location != null && new Stop(Stop.DEFAULT_ID, Stop.ORIGIN_NAME, location.getLatitude(), location.getLongitude()).isInSzeged() ) {
            point = new LatLng(location.getLatitude(), location.getLongitude());
        }
        else {
            Context context = getApplicationContext();
            CharSequence text = "Location error!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

            point = new LatLng(Stop.SZEGED[0], Stop.SZEGED[1]);
        }
        this.mOriginMarker = this.mGoogleMap.addMarker(new MarkerOptions().position(point)
                .title("Origin").draggable(true));
    }

    public void onMapReady(GoogleMap googleMap) {
        this.mGoogleMap = googleMap;
        this.mGoogleMap.setOnMapLongClickListener(this);
        this.mGoogleMap.setOnMarkerDragListener(this);
    }

    public void onMapLongClick (LatLng point) {
        if(this.mDestinationMarker != null) {
            this.mDestinationMarker.remove();
        }
        else {
            findViewById(R.id.button2).setEnabled(true);
        }

        if( !(new Stop(Stop.DEFAULT_ID, "", point.latitude, point.longitude).isInSzeged()) ) {
            Context context = getApplicationContext();
            CharSequence text = "Location error!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
        else {
            this.mDestinationMarker = this.mGoogleMap.addMarker(new MarkerOptions().position(point)
                    .title(Stop.DESTINATION_NAME).draggable(true));
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {}

    @Override
    public void onMarkerDrag(Marker marker) {}

    @Override
    public void onMarkerDragEnd(Marker marker) {
        if( !(new Stop(Stop.DEFAULT_ID, "", marker.getPosition().latitude, marker.getPosition().longitude).isInSzeged()) ) {
            Context context = getApplicationContext();
            CharSequence text = "Location error!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

            marker.remove();
            if( marker.getTitle().equals(Stop.ORIGIN_NAME) ) {
                mOriginMarker = mGoogleMap.addMarker(new MarkerOptions().position( new LatLng(Stop.SZEGED[0], Stop.SZEGED[1]) )
                        .title(Stop.ORIGIN_NAME).draggable(true));
            }
        }
    }

    public void onClickStartButton(View view) {
        if(this.mOriginMarker !=null && this.mDestinationMarker !=null) {
            if (this.mPath != null) {
                for (Marker m : this.mPath) {
                    m.remove();
                }
            }
            this.mPath = new ArrayList<Marker>();

            Stop originStop =
                    new Stop(Stop.DEFAULT_ID, Stop.ORIGIN_NAME, mOriginMarker.getPosition().latitude, mOriginMarker.getPosition().longitude);
            Stop destinationStop =
                    new Stop(1, Stop.DESTINATION_NAME, mDestinationMarker.getPosition().latitude, mDestinationMarker.getPosition().longitude);

            originStop.setDate(this.mDate);
            originStop.setDestinationAndDestinationStops(destinationStop);

            this.mProgressBar.setVisibility(View.VISIBLE);
            findViewById(R.id.button2).setEnabled(false);
            findViewById(R.id.imageButton2).setEnabled(false);
            new SearchPathTask().execute(originStop);
        }
    }

    public void showPath(Stop result) {
        ArrayList<Stop> stops = new ArrayList<Stop>();

        ArrayList<Stop> stopsTemp = new ArrayList<Stop>();
        //Visszafelé haladunk!
        while(result != null) {
            stopsTemp.add(result);

            System.out.println(result);
            System.out.println(result.getPath());

            result = result.getPrev();
        }
        //Megfordítás
        for(int i = stopsTemp.size()-1; i >= 0; i--) {
            stops.add( stopsTemp.get(i) );
        }

        if(this.mPolyline != null) this.mPolyline.remove();
        PolylineOptions polylineOpts = new PolylineOptions();

        for(int i=0; i<stops.size(); i++) {
            System.out.println(stops.get(i));

            LatLng point = new LatLng(stops.get(i).getLatitude(), stops.get(i).getLongitude());

            String titleText = "";
            if( i > 0 &&  i < stops.size()-1 ) {
                titleText = stops.get(i).getName()
                        + " / " + stops.get(i).getDate().get(Calendar.HOUR) + ":" + stops.get(i).getDate().get(Calendar.MINUTE)
                        + " - " + stops.get(i+1).getPath().getDepartureTime().get(Calendar.HOUR) + ":" + stops.get(i+1).getPath().getArrivalTime().get(Calendar.MINUTE)
                        + " / " + stops.get(i).getPath().getServiceName()
                        + " - " + stops.get(i+1).getPath().getServiceName();
            }
            else if( i == stops.size()-1 ) {
                titleText = stops.get(i).getName()
                        + " / " + stops.get(i).getDate().get(Calendar.HOUR) + ":" + stops.get(i).getDate().get(Calendar.MINUTE)
                        + " / " + stops.get(i).getPath().getServiceName();
            }
            mPath.add( this.mGoogleMap.addMarker(new MarkerOptions().position(point).title( titleText )) );

            polylineOpts.add(point);
        }

        polylineOpts.add(this.mDestinationMarker.getPosition());
        this.mPolyline = this.mGoogleMap.addPolyline(polylineOpts);
    }

    public void onClickClockButton(View view) {
        mTimePickerDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main_menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                Intent intent = new Intent(this, InfoActivity.class);
                startActivity(intent);
                return true;
            default:
                return true;
        }
    }

    private class SearchPathTask extends AsyncTask<Stop, Void, Stop> {

        @Override
        protected Stop doInBackground(Stop ... stops) {
            return Algorithm.algorithm(stops[0]);
        }

        protected void onPostExecute(Stop result) {
            showPath(result);
            mProgressBar.setVisibility(View.INVISIBLE);
            findViewById(R.id.button2).setEnabled(true);
            findViewById(R.id.imageButton2).setEnabled(true);

            Intent intent = new Intent(MainActivity.this, DirectionListActivity.class);
            intent.putExtra("Stop", result);
            startActivity(intent);
        }
    }
}
