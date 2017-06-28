package com.example.megyeri_oliver.journeyplanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by megyeri_oliver on 2017. 06. 13..
 */

public class StopAdapter extends ArrayAdapter<Stop> {
    private final Context context;
    private final Stop[] values;

    public StopAdapter(Context context, Stop[] values) {
        super(context, R.layout.item_direction_list, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View listElementView = inflater.inflate(R.layout.item_direction_list, parent, false);

        TextView textView1 = (TextView)listElementView.findViewById(R.id.textView2);
        TextView textView2 = (TextView)listElementView.findViewById(R.id.textView3);
        TextView textView3 = (TextView)listElementView.findViewById(R.id.textView4);
        ImageView imageView = (ImageView)listElementView.findViewById(R.id.imageView2);
        textView1.setText( values[position].getName() );
        if(position < values.length-2) {
            Calendar date = values[position+1].getPath().getDepartureTime();
            textView2.setText( "" + date.get(Calendar.HOUR) + ":" + date.get(Calendar.MINUTE) );
            if( values[position+1].getPath().getServiceName().equals("0") ) {
                textView3.setText("séta");
            } else {
                textView3.setText( values[position+1].getPath().getServiceName() );
            }

            switch ( values[position+1].getPath().getRouteType() ) {
                case Path.ROUTE_TYPE_BUS: imageView.setImageResource(R.drawable.bus_black_24dp);
                    break;
                case Path.ROUTE_TYPE_TRAM: imageView.setImageResource(R.drawable.ic_tram_black_24dp);
                    break;
                case Path.ROUTE_TYPE_TROLLEYBUS: imageView.setImageResource(R.drawable.trolleybus_black_24dp);
                    break;
            }
        }

        return listElementView;
    }
}
