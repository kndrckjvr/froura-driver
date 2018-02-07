package com.froura.develo4.driver;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.froura.develo4.driver.adapter.BookingServicesAdapter;
import com.froura.develo4.driver.libraries.DialogCreator;
import com.froura.develo4.driver.objects.BookingObject;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DialogCreator.DialogActionListener,
        BookingServicesAdapter.BookingServicesInterface {

    private DrawerLayout drawer;
    private RecyclerView bookingList;
    private BookingServicesAdapter mAdapter;
    private ArrayList<BookingObject> mResultList;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    final int LOCATION_REQUEST_CODE = 1;

    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        uid = FirebaseAuth.getInstance().getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
             this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        bookingList = findViewById(R.id.bookList);
        DatabaseReference bookRef = FirebaseDatabase.getInstance().getReference("services");

        bookRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                BookingServicesAdapter.mResultList.clear();
                for(DataSnapshot uniqueKeySnapshot : dataSnapshot.getChildren()){
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

        mAdapter = new BookingServicesAdapter(this, this);
        bookingList.setAdapter(mAdapter);
        bookingList.setHasFixedSize(true);
        bookingList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onBookingClick(ArrayList<BookingObject> mResultList, int position) {
        DatabaseReference bookref = FirebaseDatabase.getInstance().getReference("services/booking");
        bookref.child(mResultList.get(position).getUid())
                .child("accepted").setValue(uid);
        Intent intent = new Intent(HomeActivity.this, NavigationActivity.class);
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
    }
}
