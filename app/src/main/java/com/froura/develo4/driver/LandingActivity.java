package com.froura.develo4.driver;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputEditText;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.froura.develo4.driver.adapter.BookingServicesAdapter;
import com.froura.develo4.driver.adapter.ReservationServicesAdapter;
import com.froura.develo4.driver.adapter.SimpleDividerItemDecoration;
import com.froura.develo4.driver.config.TaskConfig;
import com.froura.develo4.driver.libraries.DialogCreator;
import com.froura.develo4.driver.objects.BookingObject;
import com.froura.develo4.driver.objects.ReservationObject;
import com.froura.develo4.driver.tasks.SuperTask;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;

import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LandingActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DialogCreator.DialogActionListener,
        SuperTask.TaskListener,
        BookingServicesAdapter.BookingServicesInterface,
        ReservationServicesAdapter.ReservationServicesInterface,
        LocationEngineListener, PermissionsListener {

    private DrawerLayout drawer;
    private RelativeLayout blank_view;
    private TextView blank_text_view;
    private RecyclerView bookingList;
    private BookingServicesAdapter mAdapter;
    private DatabaseReference bookRef;
    private ViewFlipper viewFlipper;
    private Toolbar toolbar;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    final int LOCATION_REQUEST_CODE = 1;

    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private Location originLocation;

    private ImageView nav_view_profile_pic;
    private TextView nav_view_name;
    private TextView nav_view_email;
    private Switch working;
    private String uid;

    private boolean isWorking = false;

    private SwipeRefreshLayout refreshLayout;
    private String user_name;
    private String user_mobnum;
    private String user_email;
    private String user_pic;
    private String user_trusted_id;
    private String user_trusted_name;

    private String start_date;
    private String end_date;
    private String reason;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        uid = FirebaseAuth.getInstance().getUid();
        refreshLayout = findViewById(R.id.swiperefresh);
        blank_view = findViewById(R.id.blank_view);
        blank_text_view = findViewById(R.id.blank_view_txt);
        toolbar = findViewById(R.id.toolbar);
        viewFlipper = findViewById(R.id.app_bar_landing).findViewById(R.id.vf);
        setSupportActionBar(toolbar);
        enableLocationPlugin();
        setDetails();
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
             this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View v = navigationView.getHeaderView(0);
        nav_view_profile_pic = v.findViewById(R.id.nav_view_profile_pic);
        nav_view_name = v.findViewById(R.id.nav_view_name);
        nav_view_email = v.findViewById(R.id.nav_view_email);
        working = findViewById(R.id.workStatus);
        working.setChecked(isWorking);
        if(!isWorking) {
            blank_text_view.setText("You're Off-Duty.");
        }
        bookingList = findViewById(R.id.bookList);
        bookRef = FirebaseDatabase.getInstance().getReference("services");
        mAdapter = new BookingServicesAdapter(this, this);
        bookingList.setAdapter(mAdapter);
        bookingList.setHasFixedSize(true);
        bookingList.setLayoutManager(new LinearLayoutManager(this));
        bookingList.addItemDecoration(new SimpleDividerItemDecoration(this));
        working.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isWorking = !isWorking;
                working.setText(isWorking ? "On-Duty" : "Off-Duty");
                if(isWorking) {
                    nearJobListener();
                    if(BookingServicesAdapter.mResultList.size() <= 0) {
                        blank_text_view.setText("No Bookings Found.");
                    } else {
                        bookingList.setVisibility(View.VISIBLE);
                        blank_view.setVisibility(View.GONE);
                    }
                    mAdapter = new BookingServicesAdapter(LandingActivity.this, LandingActivity.this);
                    bookingList.setAdapter(mAdapter);
                } else {
                    blank_text_view.setText("You're Off-Duty.");
                    blank_view.setVisibility(View.VISIBLE);
                    bookingList.setVisibility(View.GONE);
                    bookingList.setAdapter(null);
                }
            }
        });
    }

    private void setDetails() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String JSON_DETAILS_KEY = "userDetails";
        String userDetails = sharedPref.getString(JSON_DETAILS_KEY, "{ \"name\" : NULL }");
        try {
            JSONObject jsonObject = new JSONObject(userDetails);
            if(!jsonObject.getString("name").equals("NULL")) {
                if(!jsonObject.getString("profile_pic").equals("default")) {
                    user_pic = jsonObject.getString("profile_pic");
                    Glide.with(this)
                            .load(user_pic)
                            .apply(RequestOptions.circleCropTransform())
                            .into(nav_view_profile_pic);
                }
                user_name = jsonObject.getString("name");
                user_email = jsonObject.getString("email").equals("null") ? "None" : jsonObject.getString("email");
                user_mobnum = jsonObject.getString("mobnum").equals("null") ? "None" : jsonObject.getString("mobnum");
                user_trusted_id = jsonObject.getString("trusted_id").equals("null") ? "None" : jsonObject.getString("trusted_id");
                nav_view_name.setText(jsonObject.getString("name"));
                nav_view_email.setText(user_email);
            }
        } catch (Exception e) { }
    }

    private void removeNearest(String passId) {
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("services/booking/" + passId + "/nearest_driver");
        dbref.removeValue();
    }

    private void acceptJob(int pos) {
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("services/booking/" + BookingServicesAdapter.mResultList.get(pos).getUid());
        dbref.child("accepted_by").setValue(uid);
        removeNearest(BookingServicesAdapter.mResultList.get(pos).getUid());
        saveBookingDetails(pos);
    }

    private void saveBookingDetails(int pos) {
        BookingObject booking = BookingServicesAdapter.mResultList.get(pos);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        String JSON_DETAILS_KEY = "bookingDetails";
        String jsonDetails = "{ \"passenger_id\" : \"" + booking.getUid() + "\", " +
                "\"pickupName\" : \"" + booking.getPickup() + "\", " +
                "\"pickupLat\" : \"" + booking.getPickupLatLng().getLatitude() + "\", " +
                "\"pickupLng\" : \"" + booking.getPickupLatLng().getLongitude() + "\", " +
                "\"dropoffName\" : \"" + booking.getDropoff() + "\", " +
                "\"dropoffLat\" : \"" + booking.getDropoffLatLng().getLatitude() + "\", " +
                "\"dropoffLng\": \""+ booking.getDropoffLatLng().getLongitude() +"\", " +
                "\"fare\" : " + booking.getFare() + "}";
        editor.putString(JSON_DETAILS_KEY, jsonDetails);
        editor.apply();
        Intent intent = new Intent(this, JobAcceptActivity.class);
        startActivity(intent);
        finish();
    }

    private void nearJobListener() {
        DatabaseReference bookingRef = FirebaseDatabase.getInstance().getReference("services");
        bookingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("nearjoblistener", "triggered");
                BookingServicesAdapter.mResultList.clear();
                for(DataSnapshot booking : dataSnapshot.getChildren()) {
                    boolean nearjob = false;
                    int count = 0;
                    for (DataSnapshot pssngrid : booking.getChildren()) {
                        boolean skip = false;
                        String pass_id = pssngrid.getKey().toString();
                        LatLng dropoffLoc = new LatLng();
                        String dropoffName = "";
                        LatLng pickupLoc = new LatLng();
                        String pickupName = "";
                        String fare = "";
                        for(DataSnapshot bookingDetails : pssngrid.getChildren()) {
                            switch (bookingDetails.getKey()) {
                                case "dropoff":
                                    for(DataSnapshot dropoffDetails : bookingDetails.getChildren()) {
                                        switch (dropoffDetails.getKey()) {
                                            case "lat":
                                                dropoffLoc.setLatitude(Double.parseDouble(dropoffDetails.getValue().toString()));
                                                break;
                                            case "lng":
                                                dropoffLoc.setLongitude(Double.parseDouble(dropoffDetails.getValue().toString()));
                                                break;
                                            case "name":
                                                dropoffName = dropoffDetails.getValue().toString();
                                                break;
                                        }
                                    }
                                    break;
                                case "pickup":
                                    for(DataSnapshot pickupDetails : bookingDetails.getChildren()) {
                                        switch (pickupDetails.getKey()) {
                                            case "lat":
                                                pickupLoc.setLatitude(Double.parseDouble(pickupDetails.getValue().toString()));
                                                break;
                                            case "lng":
                                                pickupLoc.setLongitude(Double.parseDouble(pickupDetails.getValue().toString()));
                                                break;
                                            case "name":
                                                pickupName = pickupDetails.getValue().toString();
                                                break;
                                        }
                                    }
                                    break;
                                case "fare":
                                    fare = bookingDetails.getValue().toString();
                                    break;
                                case "nearest_driver":
                                    if(uid.equals(bookingDetails.getValue().toString())) {
                                        Log.d("neardriver", "uid");
                                        nearjob = true;
                                    }
                                    break;
                                case "accepted_by":
                                    skip = true;
                                    break;
                            }
                        }
                        if(!skip) {
                            BookingServicesAdapter.mResultList.add(new BookingObject(pass_id, pickupName, dropoffName, fare, pickupLoc, dropoffLoc));
                            if(nearjob) {
                                job_near(count);
                            }
                            count++;
                        }
                    }
                }
                if(isWorking) {
                    if(BookingServicesAdapter.mResultList.size() <= 0 || BookingServicesAdapter.mResultList == null) {
                        blank_text_view.setText("No Bookings Found.");
                        bookingList.setVisibility(View.GONE);
                        blank_view.setVisibility(View.VISIBLE);
                    } else {
                        bookingList.setVisibility(View.VISIBLE);
                        blank_view.setVisibility(View.GONE);
                    }
                }
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void job_near(final int pos) {
        Log.d("neardriver", "job_near");
        final BookingObject bookingdetails = BookingServicesAdapter.mResultList.get(pos);

        View mView = getLayoutInflater().inflate(R.layout.job_near_dialog, null);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(LandingActivity.this);
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
                acceptJob(pos);
                dialog.dismiss();
            }
        });

        declineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

    private void job_accept(final int pos) {
        final BookingObject bookingdetails = BookingServicesAdapter.mResultList.get(pos);

        View mView = getLayoutInflater().inflate(R.layout.job_near_dialog, null);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(LandingActivity.this);
        mBuilder.setView(mView);

        final AlertDialog dialog = mBuilder.create();
        TextView pickup = mView.findViewById(R.id.pickup);
        TextView dropoff = mView.findViewById(R.id.dropoff);
        TextView fare = mView.findViewById(R.id.fare);
        Button acceptBtn = mView.findViewById(R.id.acceptBtn);
        Button declineBtn = mView.findViewById(R.id.declineBtn);
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptJob(pos);
                dialog.dismiss();
            }
        });

        declineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        pickup.setText(bookingdetails.getPickup());
        dropoff.setText(bookingdetails.getDropoff());
        fare.setText(bookingdetails.getFare());
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        DatabaseReference driversAvailable = FirebaseDatabase.getInstance().getReference("available_drivers");
        GeoFire geoFire = new GeoFire(driversAvailable);
        if (location != null) {
            originLocation = location;
            if(isWorking) {
                geoFire.setLocation(uid, new GeoLocation(location.getLatitude(), location.getLongitude()));
            }
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
        locationEngine = new LostLocationEngine(LandingActivity.this);
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
        job_accept(position);
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

    private void setFileTranscript() {
        final TextInputEditText start_date_et = findViewById(R.id.start_date_et);
        final TextInputEditText end_date_et = findViewById(R.id.end_date_et);
        final TextInputEditText reason_et = findViewById(R.id.reason_et);
        Button button = findViewById(R.id.send_leave_btn);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start_date = start_date_et.getText().toString();
                end_date = end_date_et.getText().toString();
                reason = reason_et.getText().toString();

                SuperTask.execute(LandingActivity.this,
                        "send_file",
                        TaskConfig.SEND_LEAVE_URL,
                        "Sending Leave...");
            }
        });
    }

    private void getReservations() {
        SuperTask.execute(this, "get_reservations", TaskConfig.GET_RESERVATIONS_URL, "Fetching Data...");
    }

    @Override
    public void onReservationClick(ArrayList<ReservationObject> mResultList, int position) {
        accept_reservation(position);
    }

    private void accept_reservation(int pos) {
        ReservationServicesAdapter.mResultList.get(pos);
    }

    @Override
    public void onTaskRespond(String json, String id) {
        switch (id) {
            case "send_file":
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    if(jsonObject.getString("status").equals("success"))
                    Toast.makeText(this, jsonObject.getString("message")+"", Toast.LENGTH_SHORT).show();
                } catch (Exception e) { }
                break;
            case "get_reservations":
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    if(jsonObject.getString("status").equals("success")) {
                        JSONArray jsonArray = jsonObject.getJSONArray("reservations");
                        for(int i = 0; jsonArray.length() > i; i++) {
                            jsonObject = jsonArray.getJSONObject(i);
                            ReservationServicesAdapter.mResultList.add(new ReservationObject(
                                    jsonObject.getString("id"),
                                    jsonObject.getString("passenger_id"),
                                    jsonObject.getString("start_destination"),
                                    jsonObject.getString("end_destination"),
                                    jsonObject.getString("start_id"),
                                    jsonObject.getString("end_id"),
                                    jsonObject.getString("start_lat"),
                                    jsonObject.getString("start_lng"),
                                    jsonObject.getString("end_lat"),
                                    jsonObject.getString("end_lng"),
                                    jsonObject.getString("duration"),
                                    jsonObject.getString("reservation_date"),
                                    jsonObject.getString("price"),
                                    jsonObject.getString("notes")
                            ));
                        }

                        RecyclerView res_rec_vw = findViewById(R.id.reservation_rec_vw);
                        ReservationServicesAdapter resAdapter = new ReservationServicesAdapter(this, this);
                        res_rec_vw.setAdapter(resAdapter);
                        res_rec_vw.setHasFixedSize(true);
                        res_rec_vw.setLayoutManager(new LinearLayoutManager(this));
                        res_rec_vw.addItemDecoration(new SimpleDividerItemDecoration(this));
                    }
                } catch (Exception e) { }
                break;
        }
    }

    @Override
    public ContentValues setRequestValues(ContentValues contentValues, String id) {
        contentValues.put("android", 1);
        switch (id) {
            case "send_file":
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                contentValues.put("date_application", dateFormat.format(date));
                contentValues.put("reason", reason);
                contentValues.put("start_date", start_date);
                contentValues.put("end_date", end_date);
                contentValues.put("driver_id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                return contentValues;
            case "get_reservations":
                contentValues.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                return contentValues;
        }
        return null;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.booking:
                viewFlipper.setDisplayedChild(0);
                toolbar.setTitle("Booking Lists");
                break;
            case R.id.reservation:
                viewFlipper.setDisplayedChild(1);
                toolbar.setTitle("Booking Lists");
                break;
            case R.id.history:
                viewFlipper.setDisplayedChild(2);
                toolbar.setTitle("History");
                break;
            case R.id.leave:
                viewFlipper.setDisplayedChild(3);
                toolbar.setTitle("Leave / Absences ");
                break;
            case R.id.profile:
                viewFlipper.setDisplayedChild(4);
                toolbar.setTitle("Profile");
                break;
            case R.id.settings:
                viewFlipper.setDisplayedChild(5);
                toolbar.setTitle("Settings");
                break;
            case R.id.logout:
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.commit();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(LandingActivity.this, MainActivity.class);
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
                ActivityCompat.requestPermissions(LandingActivity.this,
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
}
