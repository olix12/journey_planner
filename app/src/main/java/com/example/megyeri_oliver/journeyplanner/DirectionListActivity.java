package com.example.megyeri_oliver.journeyplanner;

import android.content.Intent;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collections;

public class DirectionListActivity extends AppCompatActivity implements OnMapReadyCallback {
    ArrayList<Stop> mChangeStops;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction_list);

        Stop stop = (Stop)getIntent().getSerializableExtra("Stop");
        mChangeStops = new ArrayList<>();

        while(stop != null) {
            mChangeStops.add(stop);
            stop = stop.getPrev();
        }
        Collections.reverse(mChangeStops);
        mChangeStops.add( mChangeStops.get(0).getDestination() );

        ListView listView = (ListView)findViewById(R.id.listView);
        StopAdapter stopAdapter = new StopAdapter(this, mChangeStops.toArray(new Stop[mChangeStops.size()]));
        listView.setAdapter(stopAdapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        showPath();
    }

    public void showPath() {
        PolylineOptions polylineOpts = new PolylineOptions();
        for(Stop s: mChangeStops) {
            LatLng point = new LatLng(s.getLatitude(), s.getLongitude());
            mMap.addMarker(new MarkerOptions().position(point).title( s.getName() ));
            polylineOpts.add(point);
        }
        mMap.addPolyline(polylineOpts);
    }
}
