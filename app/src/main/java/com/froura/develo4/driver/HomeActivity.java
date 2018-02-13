package com.froura.develo4.driver;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
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
                int count = 0;
                if(dataSnapshot != null && isWorking) {
                    for(DataSnapshot uniqueKeySnapshot : dataSnapshot.getChildren()){
                        String passId;
                        String pickup = "";
                        String dropoff = "";
                        String fare = "";
                        LatLng pickupLocation = new LatLng(0, 0);
                        LatLng dropoffLocation = new LatLng(0, 0);
                        for(DataSnapshot passengerSnapshot : uniqueKeySnapshot.getChildren()){
                            boolean nearJobFound = false;
                            passId = passengerSnapshot.getKey();
                            for(DataSnapshot bookingDetailsSnapshot: passengerSnapshot.getChildren()) {
                                if(bookingDetailsSnapshot.getKey().equals("pickupName")) {
                                    pickup = bookingDetailsSnapshot.getValue().toString();
                                }

                                if(bookingDetailsSnapshot.getKey().equals("dropoffName")) {
                                    dropoff = bookingDetailsSnapshot.getValue().toString();
                                }

                                if(bookingDetailsSnapshot.getKey().equals("fare")) {
                                    fare = bookingDetailsSnapshot.getValue().toString();
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
                                    }
                                }

                                if(bookingDetailsSnapshot.getKey().equals("nearest_driver")) {
                                    if(bookingDetailsSnapshot.getValue().equals(uid)) {
                                        nearJobFound = true;
                                    }
                                }
                            }
                            BookingServicesAdapter.mResultList.add(new BookingObject(passId, pickup, dropoff, fare, pickupLocation, dropoffLocation));
                            if(nearJobFound) setJob(count, true);
                            mAdapter.notifyDataSetChanged();
                            count++;
                        }
                    }
                } else if(dataSnapshot == null) {
                    Toast.makeText(HomeActivity.this, "data == null", Toast.LENGTH_SHORT).show();
                    BookingServicesAdapter.mResultList.clear();
                    mAdapter.notifyDataSetChanged();
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

    }

    private void refreshBookings() {
        bookRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                BookingServicesAdapter.mResultList.clear();
                if (dataSnapshot != null && isWorking) {
                    int count = 0;
                    for (DataSnapshot uniqueKeySnapshot : dataSnapshot.getChildren()) {
                        String passId;
                        String pickup = "";
                        String dropoff = "";
                        String fare = "";
                        LatLng pickupLocation = new LatLng(0, 0);
                        LatLng dropoffLocation = new LatLng(0, 0);
                        for(DataSnapshot passengerSnapshot : uniqueKeySnapshot.getChildren()){
                            boolean nearJobFound = false;
                            passId = passengerSnapshot.getKey();
                            for(DataSnapshot bookingDetailsSnapshot: passengerSnapshot.getChildren()) {
                                if(bookingDetailsSnapshot.getKey().equals("pickupName")) {
                                    pickup = bookingDetailsSnapshot.getValue().toString();
                                }

                                if(bookingDetailsSnapshot.getKey().equals("dropoffName")) {
                                    dropoff = bookingDetailsSnapshot.getValue().toString();
                                }

                                if(bookingDetailsSnapshot.getKey().equals("fare")) {
                                    fare = bookingDetailsSnapshot.getValue().toString();
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
                                    }
                                }

                                if(bookingDetailsSnapshot.getKey().equals("nearest_driver")) {
                                    if(bookingDetailsSnapshot.getValue().equals(uid)) {
                                        nearJobFound = true;
                                    }
                                }
                            }
                            BookingServicesAdapter.mResultList.add(new BookingObject(passId, pickup, dropoff, fare, pickupLocation, dropoffLocation));
                            if(nearJobFound) setJob(count, true);
                            mAdapter.notifyDataSetChanged();
                            count++;
                        }
                    }
                } else if(dataSnapshot == null) {
                    Toast.makeText(HomeActivity.this, "data == null", Toast.LENGTH_SHORT).show();
                    BookingServicesAdapter.mResultList.clear();
                    mAdapter.notifyDataSetChanged();
                    SnackBarCreator.set("No Bookings Found.");
                    SnackBarCreator.show(bookingList);
                    refreshLayout.setRefreshing(false);
                } else {
                    Toast.makeText(HomeActivity.this, "working == false", Toast.LENGTH_SHORT).show();
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

    private void removeNearest(String passId) {
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("services/booking/" + passId);
        dbref.child("nearest_driver").setValue(null);
    }

    private void acceptJob(String passId) {
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("services/booking/" + passId);
        dbref.child("accepted_by").setValue(uid);
        removeNearest(passId);
    }

    private void setJob(int pos, final boolean auto) {
        final BookingObject bookingdetails = BookingServicesAdapter.mResultList.get(pos);

        View mView = getLayoutInflater().inflate(R.layout.nearjob_dialog, null);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(HomeActivity.this);
        mBuilder.setView(mView);

        final AlertDialog dialog = mBuilder.create();
        TextView pickup = mView.findViewById(R.id.pickup);
        TextView dropoff = mView.findViewById(R.id.dropoff);
        TextView fare = mView.findViewById(R.id.fare);
        final ProgressBar cntdwntimer = mView.findViewById(R.id.cntdwntimer);
        Button acceptBtn = mView.findViewById(R.id.acceptBtn);
        Button declineBtn = mView.findViewById(R.id.declineBtn);
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                acceptJob(bookingdetails.getUid());
            }
        });

        declineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(auto)
                    removeNearest(bookingdetails.getUid());
                dialog.dismiss();
            }
        });
        pickup.setText(bookingdetails.getPickup());
        dropoff.setText(bookingdetails.getDropoff());
        fare.setText(bookingdetails.getFare());
        dialog.setCancelable(false);
        dialog.show();

        CountDownTimer timer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long l) {
                cntdwntimer.setProgress(Integer.parseInt(l / 1000+""));
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
            }
        };
        timer.start();
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
        setJob(position, false);
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
