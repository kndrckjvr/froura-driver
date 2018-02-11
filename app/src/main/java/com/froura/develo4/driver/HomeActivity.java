package com.froura.develo4.driver;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.froura.develo4.driver.adapter.BookingServicesAdapter;
import com.froura.develo4.driver.adapter.SimpleDividerItemDecoration;
import com.froura.develo4.driver.libraries.DialogCreator;
import com.froura.develo4.driver.libraries.SnackBarCreator;
import com.froura.develo4.driver.objects.BookingObject;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DialogCreator.DialogActionListener,
        BookingServicesAdapter.BookingServicesInterface,
        LocationEngineListener, PermissionsListener {

    private DrawerLayout drawer;
    private RecyclerView bookingList;
    private BookingServicesAdapter mAdapter;
    private ArrayList<BookingObject> mResultList;
    private DatabaseReference bookRef;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    final int LOCATION_REQUEST_CODE = 1;

    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private Location originLocation;

    private Switch working;
    private String uid;

    private boolean isWorking = false;

    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        uid = FirebaseAuth.getInstance().getUid();
        refreshLayout = findViewById(R.id.swiperefresh);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
             this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        working = findViewById(R.id.workStatus);
        working.setChecked(isWorking);
        working.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isWorking = !isWorking;
                working.setText(isWorking ? "On-Duty" : "Off-Duty");
                if(!isWorking) {
                    BookingServicesAdapter.mResultList.clear();
                    mAdapter.notifyDataSetChanged();
                } else {
                    refreshBookings();
                }
            }
        });
        bookingList = findViewById(R.id.bookList);
        bookRef = FirebaseDatabase.getInstance().getReference("services");

        bookRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                BookingServicesAdapter.mResultList.clear();
                if(dataSnapshot != null && isWorking)
                    for(DataSnapshot uniqueKeySnapshot : dataSnapshot.getChildren()){
                        Log.d("bookref", uniqueKeySnapshot.getValue()+"");
                        String uid;
                        String pickup = "";
                        String dropoff = "";
                        String fare = "";
                        LatLng pickupLocation = new LatLng(0, 0);
                        LatLng dropoffLocation = new LatLng(0, 0);
                        for(DataSnapshot passengerSnapshot : uniqueKeySnapshot.getChildren()){
                            uid = passengerSnapshot.getKey();
                            Log.d("parse", passengerSnapshot.getKey()+"");;
                            for(DataSnapshot bookingDetailsSnapshot: passengerSnapshot.getChildren()) {
                                if(bookingDetailsSnapshot.getKey().equals("pickupName")) {
                                    pickup = bookingDetailsSnapshot.getValue().toString();
                                    Log.d("parse", bookingDetailsSnapshot.getValue()+"");
                                }

                                if(bookingDetailsSnapshot.getKey().equals("dropoffName")) {
                                    dropoff = bookingDetailsSnapshot.getValue().toString();
                                    Log.d("parse", bookingDetailsSnapshot.getValue()+"");
                                }

                                if(bookingDetailsSnapshot.getKey().equals("fare")) {
                                    fare = bookingDetailsSnapshot.getValue().toString();
                                    Log.d("parse", bookingDetailsSnapshot.getValue()+"");
                                }

                                if(bookingDetailsSnapshot.getKey().equals("pickupLocation")) {
                                    double lat = 0.0;
                                    double lng = 0.0;
                                    for(DataSnapshot locationDetails: bookingDetailsSnapshot.getChildren()) {
                                        if(locationDetails.getKey().equals("0")) {
                                            lat = Double.parseDouble(locationDetails.getValue().toString());
                                        } else {
                                            lng = Double.parseDouble(locationDetails.getValue().toString());
                                        }
                                        pickupLocation = new LatLng(lat, lng);
                                        Log.d("parse", locationDetails.getValue()+" "+ lat + " " + lng);
                                    }
                                }

                                if(bookingDetailsSnapshot.getKey().equals("dropoffLocation")) {
                                    double lat = 0.0;
                                    double lng = 0.0;
                                    for(DataSnapshot locationDetails: bookingDetailsSnapshot.getChildren()) {
                                        if(locationDetails.getKey().equals("0")) {
                                            lat = Double.parseDouble(locationDetails.getValue().toString());
                                        } else {
                                            lng = Double.parseDouble(locationDetails.getValue().toString());
                                        }
                                        dropoffLocation = new LatLng(lat, lng);
                                        Log.d("parse", locationDetails.getValue()+" "+ lat + " " + lng);
                                    }
                                }
                            }
                            BookingServicesAdapter.mResultList.add(new BookingObject(uid, pickup, dropoff, fare, pickupLocation, dropoffLocation));
                            mAdapter.notifyDataSetChanged();
                        }
                    }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshBookings();
            }
        });

        enableLocationPlugin();
        mAdapter = new BookingServicesAdapter(this, this);
        bookingList.setAdapter(mAdapter);
        bookingList.setHasFixedSize(true);
        bookingList.setLayoutManager(new LinearLayoutManager(this));
        bookingList.addItemDecoration(new SimpleDividerItemDecoration(this));

        DatabaseReference nearPass = FirebaseDatabase.getInstance().getReference();
        nearPass.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot != null) {
                    for(DataSnapshot drvrDetails : dataSnapshot.getChildren()) {
                        if(drvrDetails.getKey().equals("available_drivers")) {
                            for(DataSnapshot myDetails : drvrDetails.getChildren()) {
                                if(myDetails.getKey().equals(uid)) {
                                    for(DataSnapshot nearJob : myDetails.getChildren()) {
                                        if(nearJob.getKey().equals("nearest_passenger")) {
                                            int position = 0;
                                            for(BookingObject booking : BookingServicesAdapter.mResultList) {
                                                if(booking.getUid().equals(nearJob.getValue())) {
                                                    Log.d("bookref", "meron kang job");
                                                    setJob(position);
                                                } else {
                                                    position++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void refreshBookings() {
        bookRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                BookingServicesAdapter.mResultList.clear();
                if (dataSnapshot != null && isWorking) {
                    for (DataSnapshot uniqueKeySnapshot : dataSnapshot.getChildren()) {
                        Log.d("bookref", uniqueKeySnapshot.getValue() + "");
                        String uid;
                        String pickup = "";
                        String dropoff = "";
                        String fare = "";
                        LatLng pickupLocation = new LatLng(0, 0);
                        LatLng dropoffLocation = new LatLng(0, 0);
                        for (DataSnapshot passengerSnapshot : uniqueKeySnapshot.getChildren()) {
                            uid = passengerSnapshot.getKey();
                            Log.d("parse", passengerSnapshot.getKey() + "");
                            ;
                            for (DataSnapshot bookingDetailsSnapshot : passengerSnapshot.getChildren()) {
                                if (bookingDetailsSnapshot.getKey().equals("pickupName")) {
                                    pickup = bookingDetailsSnapshot.getValue().toString();
                                    Log.d("parse", bookingDetailsSnapshot.getValue() + "");
                                }

                                if (bookingDetailsSnapshot.getKey().equals("dropoffName")) {
                                    dropoff = bookingDetailsSnapshot.getValue().toString();
                                    Log.d("parse", bookingDetailsSnapshot.getValue() + "");
                                }

                                if (bookingDetailsSnapshot.getKey().equals("fare")) {
                                    fare = bookingDetailsSnapshot.getValue().toString();
                                    Log.d("parse", bookingDetailsSnapshot.getValue() + "");
                                }

                                if (bookingDetailsSnapshot.getKey().equals("pickupLocation")) {
                                    double lat = 0.0;
                                    double lng = 0.0;
                                    for (DataSnapshot locationDetails : bookingDetailsSnapshot.getChildren()) {
                                        if (locationDetails.getKey().equals("0")) {
                                            lat = Double.parseDouble(locationDetails.getValue().toString());
                                        } else {
                                            lng = Double.parseDouble(locationDetails.getValue().toString());
                                        }
                                        pickupLocation = new LatLng(lat, lng);
                                        Log.d("parse", locationDetails.getValue() + " " + lat + " " + lng);
                                    }
                                }

                                if (bookingDetailsSnapshot.getKey().equals("dropoffLocation")) {
                                    double lat = 0.0;
                                    double lng = 0.0;
                                    for (DataSnapshot locationDetails : bookingDetailsSnapshot.getChildren()) {
                                        if (locationDetails.getKey().equals("0")) {
                                            lat = Double.parseDouble(locationDetails.getValue().toString());
                                        } else {
                                            lng = Double.parseDouble(locationDetails.getValue().toString());
                                        }
                                        dropoffLocation = new LatLng(lat, lng);
                                        Log.d("parse", locationDetails.getValue() + " " + lat + " " + lng);
                                    }
                                }
                            }
                            BookingServicesAdapter.mResultList.add(new BookingObject(uid, pickup, dropoff, fare, pickupLocation, dropoffLocation));
                            mAdapter.notifyDataSetChanged();
                            refreshLayout.setRefreshing(false);
                        }
                    }
                } else {
                    SnackBarCreator.set("You're Off-Duty.");
                    SnackBarCreator.show(bookingList);
                    refreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setJob(int pos) {
        BookingObject bookingdetails = BookingServicesAdapter.mResultList.get(pos);
        DialogCreator.create(this, "nearestJob")
                .setTitle("Near Bookings")
                .setMessage("pickup: " + bookingdetails.getPickup() + " dropoff: " + bookingdetails.getDropoff() + " fare: " + bookingdetails.getFare())
                .setPositiveButton("Accept")
                .setNegativeButton("Decline")
                .show();
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            originLocation = location;

            DatabaseReference driversAvailable = FirebaseDatabase.getInstance().getReference("available_drivers");
            GeoFire geoFire = new GeoFire(driversAvailable);
            geoFire.setLocation(uid, new GeoLocation(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            finish();
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine() {
        locationEngine = new LostLocationEngine(HomeActivity.this);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationPlugin() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onBookingClick(ArrayList<BookingObject> mResultList, int position) {
        DatabaseReference bookref = FirebaseDatabase.getInstance().getReference("services/booking");
        bookref.child(mResultList.get(position).getUid())
                .child("accepted").setValue(uid);
        Intent intent = new Intent(HomeActivity.this, MapNavigationActivity.class);
        intent.putExtra("pickupLat", mResultList.get(position).getPickupLatLng().getLatitude());
        intent.putExtra("pickupLng", mResultList.get(position).getPickupLatLng().getLongitude());
        intent.putExtra("dropoffLat", mResultList.get(position).getDropoffLatLng().getLatitude());
        intent.putExtra("dropoffLng", mResultList.get(position).getDropoffLatLng().getLongitude());
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.commit();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClickPositiveButton(String actionId) {
        switch (actionId) {
            case "requestLocation":
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                break;
            case "locationPermission":
                ActivityCompat.requestPermissions(HomeActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);
                break;
        }
    }

    @Override
    public void onClickNegativeButton(String actionId) { }

    @Override
    public void onClickNeutralButton(String actionId) { }

    @Override
    public void onClickMultiChoiceItem(String actionId, int which, boolean isChecked) { }

    @Override
    public void onCreateDialogView(String actionId, View view) { }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    finish();
                }
                break;
        }
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
