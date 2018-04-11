package com.froura.develo4.driver.history;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.froura.develo4.driver.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback {

    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private List<Polyline> polylines;

    private TextView fare_txt_vw;
    private TextView date_txt_vw;
    private TextView pickup_txt_vw;
    private TextView dropoff_txt_vw;
    private TextView service_type_txt_vw;
    private LinearLayout history_details_layout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        polylines = new ArrayList<>();
        history_details_layout = findViewById(R.id.history_details_layout);
        fare_txt_vw = findViewById(R.id.fare_txt_vw);
        date_txt_vw = findViewById(R.id.date_txt_vw);
        pickup_txt_vw = findViewById(R.id.pickup_txt_vw);
        dropoff_txt_vw = findViewById(R.id.dropoff_txt_vw);
        service_type_txt_vw = findViewById(R.id.service_type_txt_vw);
        fare_txt_vw.setText("Php " + getIntent().getStringExtra("price"));
        date_txt_vw.setText(getIntent().getStringExtra("datetime"));
        pickup_txt_vw.setText(getIntent().getStringExtra("pickupName"));
        pickup_txt_vw.setSelected(true);
        dropoff_txt_vw.setText(getIntent().getStringExtra("dropoffName"));
        dropoff_txt_vw.setSelected(true);
        service_type_txt_vw.setText(getIntent().getStringExtra("service"));

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.mapstyle));
        } catch (Resources.NotFoundException e) { }
        mMap = googleMap;
        mMap.setPadding(0, 0,
                0, history_details_layout.getLayoutParams().height);

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(getIntent().getDoubleExtra("pickupLat", 0.0),
                        getIntent().getDoubleExtra("pickupLng", 0.0)))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.yellow_marker)));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(getIntent().getDoubleExtra("dropoffLat", 0.0),
                        getIntent().getDoubleExtra("dropoffLng", 0.0)))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.black_marker)));


        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.DRIVING)
                .withListener(new RoutingListener() {
                    @Override
                    public void onRoutingFailure(RouteException e) { }

                    @Override
                    public void onRoutingStart() { }

                    @Override
                    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(new LatLng(getIntent().getDoubleExtra("pickupLat", 0.0),
                                getIntent().getDoubleExtra("pickupLng", 0.0)));
                        builder.include(new LatLng(getIntent().getDoubleExtra("dropoffLat", 0.0),
                                getIntent().getDoubleExtra("dropoffLng", 0.0)));
                        LatLngBounds bounds = builder.build();

                        int padding = (int) (getResources().getDisplayMetrics().widthPixels * 0.2);

                        CameraUpdate cameraUpdate = CameraUpdateFactory
                                .newLatLngBounds(bounds, padding);

                        mMap.animateCamera(cameraUpdate);

                        if(polylines.size()>0) {
                            for (Polyline poly : polylines) {
                                poly.remove();
                            }
                        }

                        polylines = new ArrayList<>();
                        for (int i = 0; i <route.size(); i++) {

                            PolylineOptions polyOptions = new PolylineOptions();
                            polyOptions.color(Color.BLACK);
                            polyOptions.width(10 + i * 3);
                            polyOptions.addAll(route.get(i).getPoints());
                            Polyline polyline = mMap.addPolyline(polyOptions);
                            polylines.add(polyline);
                        }
                    }

                    @Override
                    public void onRoutingCancelled() { }
                })
                .waypoints(new LatLng(getIntent().getDoubleExtra("pickupLat", 0.0),
                                getIntent().getDoubleExtra("pickupLng", 0.0)),
                        new LatLng(getIntent().getDoubleExtra("dropoffLat", 0.0),
                                getIntent().getDoubleExtra("dropoffLng", 0.0)))
                .build();
        routing.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
