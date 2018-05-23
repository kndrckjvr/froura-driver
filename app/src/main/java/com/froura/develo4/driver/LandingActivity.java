package com.froura.develo4.driver;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
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
import android.widget.DatePicker;
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
import com.froura.develo4.driver.adapter.HistoryAdapter;
import com.froura.develo4.driver.adapter.ReservationListAdapter;
import com.froura.develo4.driver.config.TaskConfig;
import com.froura.develo4.driver.history.HistorySingleActivity;
import com.froura.develo4.driver.job.JobAcceptActivity;
import com.froura.develo4.driver.object.HistoryObject;
import com.froura.develo4.driver.utils.DialogCreator;
import com.froura.develo4.driver.object.BookingObject;
import com.froura.develo4.driver.object.ReservationObject;
import com.froura.develo4.driver.utils.SnackBarCreator;
import com.froura.develo4.driver.utils.SuperTask;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LandingActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DialogCreator.DialogActionListener,
        SuperTask.TaskListener,
        BookingServicesAdapter.BookingServicesInterface,
        HistoryAdapter.HistoryAdapterListener,
        ReservationListAdapter.ReservationListAdapterListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private DrawerLayout drawer;
    private ViewFlipper viewFlipper;
    private Toolbar toolbar;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    final int LOCATION_REQUEST_CODE = 1;

    private ImageView nav_view_profile_pic;
    private TextView nav_view_name;
    private TextView nav_view_email;
    private Switch working;
    private String uid;

    private boolean isWorking = false;
    private boolean isNearShown = false;
    private DatabaseReference bookingRef;

    private String user_name;
    private String user_mobnum;
    private String user_email;
    private String user_pic;
    private String user_trusted_id;
    private String user_trusted_name;

    private String start_date;
    private String end_date;
    private String reason;
    private ArrayList<BookingObject> bookingList = new ArrayList<>();
    private String[] monthNames = {"January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    private HistoryAdapter historyAdapter;
    private BookingServicesAdapter bookingAdapter;
    private ReservationListAdapter resAdapter;

    private RecyclerView res_list_rec_vw;
    private RelativeLayout res_list_loading_view;
    private RelativeLayout res_list_blank_view;

    private RecyclerView booking_rec_vw;
    private RelativeLayout booking_loading_view;
    private RelativeLayout booking_blank_view;
    private TextView booking_blank_txt_vw;

    private Button send_leave_btn;
    private int start_month;
    private int start_day;
    private int start_year;
    private int end_month;
    private int end_day;
    private int end_year;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        uid = FirebaseAuth.getInstance().getUid();
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Booking List");
        setSupportActionBar(toolbar);
        working = findViewById(R.id.workStatus);
        viewFlipper = findViewById(R.id.app_bar_landing).findViewById(R.id.vf);
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);
        View v = navigationView.getHeaderView(0);
        nav_view_profile_pic = v.findViewById(R.id.nav_view_profile_pic);
        nav_view_name = v.findViewById(R.id.nav_view_name);
        nav_view_email = v.findViewById(R.id.nav_view_email);

        setDetails();

        booking_blank_view = findViewById(R.id.booking_blank_view);
        booking_blank_txt_vw = findViewById(R.id.booking_blank_txt_vw);
        booking_loading_view = findViewById(R.id.booking_loading_view);

        isWorking = getIntent().getBooleanExtra("fromJob", false);
        working.setChecked(isWorking);
        working.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateStatus();
                isWorking = !isWorking;
                working.setText(isWorking ? "On-Duty" : "Off-Duty");
                if(isWorking) {
                    getBookingList();
                } else {
                    booking_rec_vw.setVisibility(View.GONE);
                    booking_loading_view.setVisibility(View.GONE);
                    booking_blank_view.setVisibility(View.VISIBLE);
                    booking_blank_txt_vw.setText("You're Off-Duty");
                }
            }
        });
        bookingAdapter = new BookingServicesAdapter(LandingActivity.this,
                LandingActivity.this, bookingList);
        booking_rec_vw = findViewById(R.id.booking_rec_vw);
        booking_rec_vw.setAdapter(bookingAdapter);
        booking_rec_vw.setHasFixedSize(true);
        booking_rec_vw.setLayoutManager(new LinearLayoutManager(this));

        if(isWorking) {
            getBookingList();
        } else {
            booking_blank_view.setVisibility(View.VISIBLE);
            booking_blank_txt_vw.setText("You're Off-Duty");
        }

        buildGoogleApiClient();
    }

    private void getBookingList() {
        booking_blank_view.setVisibility(View.GONE);
        booking_loading_view.setVisibility(View.VISIBLE);

        bookingRef = FirebaseDatabase.getInstance().getReference("services");
        bookingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                bookingList.clear();
                if(dataSnapshot.exists()) {
                    for(DataSnapshot booking : dataSnapshot.getChildren()) {
                        int count = 0;
                        for (DataSnapshot pssngrid : booking.getChildren()) {
                            boolean skip = false;
                            String pass_id = pssngrid.getKey();
                            LatLng dropoffLoc = new LatLng();
                            String dropoffName = "";
                            LatLng pickupLoc = new LatLng();
                            String pickupName = "";
                            String fare = "";
                            for(DataSnapshot bookingDetails : pssngrid.getChildren())
                                switch (bookingDetails.getKey()) {
                                    case "dropoff":
                                        for(DataSnapshot dropoffDetails : bookingDetails.getChildren())
                                            switch (dropoffDetails.getKey()) {
                                                case "lat":
                                                    dropoffLoc.setLatitude(
                                                            Double.parseDouble(dropoffDetails.getValue().toString()));
                                                    break;
                                                case "lng":
                                                    dropoffLoc.setLongitude(
                                                            Double.parseDouble(dropoffDetails.getValue().toString()));
                                                    break;
                                                case "name":
                                                    dropoffName = dropoffDetails.getValue().toString();
                                                    break;
                                            }
                                        break;
                                    case "pickup":
                                        for(DataSnapshot pickupDetails : bookingDetails.getChildren())
                                            switch (pickupDetails.getKey()) {
                                                case "lat":
                                                    pickupLoc.setLatitude(
                                                            Double.parseDouble(pickupDetails.getValue().toString()));
                                                    break;
                                                case "lng":
                                                    pickupLoc.setLongitude(
                                                            Double.parseDouble(pickupDetails.getValue().toString()));
                                                    break;
                                                case "name":
                                                    pickupName = pickupDetails.getValue().toString();
                                                    break;
                                            }
                                        break;
                                    case "fare":
                                        fare = bookingDetails.getValue().toString();
                                        break;
                                    case "accepted_by":
                                        if(!uid.equals(bookingDetails.getValue().toString()))
                                            skip = true;
                                        break;
                                    case "nearest_driver":
                                        if(!isNearShown) {
                                            isNearShown = true;
                                            jobNearShow(count);
                                        }
                                        break;
                                }
                            if(!skip)
                                bookingList.add(new BookingObject(pass_id,
                                        pickupName, dropoffName, fare, pickupLoc, dropoffLoc));
                            bookingAdapter.notifyDataSetChanged();
                        }
                        count++;
                    }
                } else bookingAdapter.notifyDataSetChanged();

                if(bookingList.size() == 0) {
                    booking_loading_view.setVisibility(View.GONE);
                    booking_rec_vw.setVisibility(View.GONE);
                    booking_blank_view.setVisibility(View.VISIBLE);
                    booking_blank_txt_vw.setText("No Bookings Found.");
                } else {
                    booking_blank_view.setVisibility(View.GONE);
                    booking_loading_view.setVisibility(View.GONE);
                    booking_rec_vw.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void jobNearShow(final int pos) {
        final BookingObject booking = bookingList.get(pos);
        View mView = getLayoutInflater().inflate(R.layout.dialog_job_near, null);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(LandingActivity.this);
        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        TextView pickup = mView.findViewById(R.id.pickup);
        TextView dropoff = mView.findViewById(R.id.dropoff);
        TextView fare = mView.findViewById(R.id.fare);
        final ProgressBar cntdwntimer = mView.findViewById(R.id.cntdwntimer);
        Button acceptBtn = mView.findViewById(R.id.acceptBtn);
        Button declineBtn = mView.findViewById(R.id.declineBtn);

        final CountDownTimer timer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long l) {
                cntdwntimer.setProgress(Integer.parseInt(l / 1000+""));
            }

            @Override
            public void onFinish() {
                if(dialog.isShowing())
                    dialog.dismiss();
            }
        };
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptJob(pos);
                if(dialog.isShowing())
                    dialog.dismiss();
                timer.cancel();
            }
        });

        declineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeNearest(booking.getUid());

                if(dialog.isShowing())
                    dialog.dismiss();
                timer.cancel();
            }
        });
        pickup.setText(booking.getPickup());
        dropoff.setText(booking.getDropoff());
        fare.setText(booking.getFare());
        dialog.setCancelable(false);
        if(!this.isFinishing())
            dialog.show();
        timer.start();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                DialogCreator.create(this, "locationPermission")
                        .setMessage("We need to access your location and device state to continue using FROURÁ.")
                        .setPositiveButton("OK")
                        .show();
                return;
            }
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void updateStatus() {
        DatabaseReference driversAvailable = FirebaseDatabase.getInstance().getReference("available_drivers");
        DatabaseReference driversWorking = FirebaseDatabase.getInstance().getReference("working_drivers");
        GeoFire available = new GeoFire(driversAvailable);
        GeoFire working = new GeoFire(driversWorking);
        if(isWorking) {
            available.removeLocation(uid);
            working.setLocation(uid, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
        } else {
            working.removeLocation(uid);
            available.setLocation(uid, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mLastLocation = location;
            updateStatus();
        }
    }

    private void setDetails() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String JSON_DETAILS_KEY = "userDetails";
        String userDetails = sharedPref.getString(JSON_DETAILS_KEY, "{ \"name\" : NULL }");
        try {
            JSONObject jsonObject = new JSONObject(userDetails);
            if(!jsonObject.getString("profile_pic").equals("default")) {
                user_pic = jsonObject.getString("profile_pic");
                Glide.with(this)
                        .load(user_pic)
                        .apply(RequestOptions.circleCropTransform())
                        .into(nav_view_profile_pic);
            }
            user_name = jsonObject.getString("name");
            user_email = jsonObject.getString("email").equals("null") ?
                    "None" : jsonObject.getString("email");
            user_mobnum = jsonObject.getString("mobnum").equals("null") ?
                    "None" : jsonObject.getString("mobnum");
            nav_view_name.setText(user_name);
            nav_view_email.setText(user_email);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void removeNearest(String passId) {
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("services/booking/" +
                passId + "/nearest_driver");
        dbref.removeValue();
    }

    private void acceptJob(int pos) {
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("services/booking/" +
                bookingList.get(pos).getUid());
        dbref.child("accepted_by").setValue(uid);
        removeNearest(bookingList.get(pos).getUid());
        saveBookingDetails(pos);
    }

    private void saveBookingDetails(int pos) {
        BookingObject booking = bookingList.get(pos);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        String JSON_DETAILS_KEY = "bookingDetails";
        String jsonDetails = "{ \"passenger_id\" : \"" + booking.getUid() + "\", " +
                "\"startTime\" : \"" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\", " +
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

    @Override
    public void onBookingClick(ArrayList<BookingObject> mResultList, int position) {

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
        final Calendar cal = Calendar.getInstance();
        final int currentYear = cal.get(Calendar.YEAR);
        final int currentMonth =  cal.get(Calendar.MONTH);
        final int currentDay =  cal.get(Calendar.DAY_OF_MONTH);
        start_month = end_month = currentMonth;
        start_day = end_day = currentDay + 1;
        start_year = end_year = currentYear;
        final TextView start_date_txt_vw = findViewById(R.id.start_date_txt_vw);
        final TextView end_date_txt_vw = findViewById(R.id.end_date_txt_vw);
        final TextInputEditText reason_et = findViewById(R.id.reason_et);
        Button edit_start_date_btn = findViewById(R.id.edit_start_date_btn);
        Button edit_end_date_btn = findViewById(R.id.edit_end_date_btn);
        send_leave_btn = findViewById(R.id.send_leave_btn);

        start_date_txt_vw.setText(String.format(Locale.ENGLISH, "%s %02d, %04d",
                monthNames[currentMonth], (currentDay + 1), currentYear));
        end_date_txt_vw.setText(String.format(Locale.ENGLISH, "%s %02d, %04d",
                monthNames[currentMonth], (currentDay + 1), currentYear));
        start_date = String.format(Locale.ENGLISH, "%04d-%02d-%02d 00:00:00",
                currentYear, currentMonth, (currentDay + 1));
        end_date = String.format(Locale.ENGLISH, "%04d-%02d-%02d 00:00:00",
                currentYear, currentMonth, (currentDay + 1));
        edit_start_date_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(LandingActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                                int temp_day = day, temp_month = month, temp_year = year;
                                if(year < currentYear || month < currentMonth || day <= currentDay) {
                                    SnackBarCreator.set("Leaves must be a day after the current date.");
                                    SnackBarCreator.show(send_leave_btn);
                                } else if(end_day < temp_day || end_month < temp_month || end_year < temp_year) {
                                    SnackBarCreator.set("Start of Leave Date must be earlier or has the same date than End of Leave Date.");
                                    SnackBarCreator.show(send_leave_btn, true);
                                } else {
                                    start_day = day;
                                    start_month = month;
                                    start_year = year;
                                    start_date_txt_vw.setText(String.format(Locale.ENGLISH, "%s %02d, %04d",
                                            monthNames[month], day, year));
                                    month += 1;
                                    start_date = String.format(Locale.ENGLISH, "%04d-%02d-%02d 00:00:00",
                                            year, month, day);
                                }
                            }
                        }, start_year, start_month, (currentDay != start_day ? start_day : currentDay + 1));
                dialog.show();
            }
        });

        edit_end_date_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(LandingActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                                int temp_day = day, temp_month = month, temp_year = year;
                                if(year < currentYear || month < currentMonth || day <= currentDay) {
                                    SnackBarCreator.set("Leaves must be a day after the current date.");
                                    SnackBarCreator.show(send_leave_btn);
                                } else if(temp_day < start_day || temp_month < start_month || temp_year < start_year) {
                                    SnackBarCreator.set("Start of Leave Date must be earlier or has the same date than End of Leave Date.");
                                    SnackBarCreator.show(send_leave_btn, true);
                                } else {
                                    end_day = day;
                                    end_month = month;
                                    end_year = year;
                                    end_date_txt_vw.setText(String.format(Locale.ENGLISH, "%s %02d, %04d",
                                            monthNames[month], day, year));
                                    month += 1;
                                    end_date = String.format(Locale.ENGLISH, "%04d-%02d-%02d 00:00:00",
                                            year, month, day);
                                }
                            }
                        }, end_year, end_month, (currentDay != end_day ? end_day : currentDay + 1));
                dialog.show();
            }
        });

        send_leave_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reason = reason_et.getText().toString().isEmpty() ? "" : reason_et.getText().toString();
                if(reason.isEmpty()) {
                    SnackBarCreator.set("Please enter a valid reason.");
                    SnackBarCreator.show(view);
                } else {
                    SuperTask.execute(LandingActivity.this,
                            TaskConfig.SEND_LEAVE_URL,
                            "send_file",
                            "Sending Leave...");

                    start_date = String.format(Locale.ENGLISH, "%04d-%02d-%02d 00:00:00",
                            cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_MONTH), (cal.get(Calendar.DAY_OF_MONTH) + 1));
                    end_date = String.format(Locale.ENGLISH, "%04d-%02d-%02d 00:00:00",
                            cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_MONTH), (cal.get(Calendar.DAY_OF_MONTH) + 1));
                    reason_et.setText("");
                }
            }
        });
    }

    @Override
    public void onTaskRespond(String json, String id) {
        switch (id) {
            case "send_file":
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    if(jsonObject.getBoolean("success")) {
                        SnackBarCreator.set(jsonObject.getString("message"));
                        SnackBarCreator.show(send_leave_btn);
                    } else {

                    }
                } catch (Exception e) { }
                break;
            case "get_reservations":
                try {
                    mReservationList.clear();
                    JSONObject jsonObject = new JSONObject(json);
                    if(jsonObject.getBoolean("success")) {
                        JSONArray jsonArray = jsonObject.getJSONArray("reservations");
                        for(int i = 0; i < jsonArray.length(); i++) {
                            JSONObject reservation = jsonArray.getJSONObject(i);
                            mReservationList.add(new ReservationObject(reservation.getInt("id"),
                                    reservation.getString("driver_id"),
                                    reservation.getString("start_destination"),
                                    reservation.getString("end_destination"),
                                    new com.google.android.gms.maps.model.LatLng(
                                            reservation.getDouble("start_lat"),
                                            reservation.getDouble("start_lng")),
                                    new com.google.android.gms.maps.model.LatLng(
                                            reservation.getDouble("end_lat"),
                                            reservation.getDouble("end_lng")),
                                    reservation.getString("start_id"),
                                    reservation.getString("end_id"),
                                    reservation.getString("reservation_date"),
                                    reservation.getString("price"),
                                    reservation.getString("notes"),
                                    reservation.getInt("status")));
                        }
                        res_list_loading_view.setVisibility(View.GONE);
                        res_list_rec_vw.setVisibility(View.VISIBLE);
                        resAdapter.notifyDataSetChanged();
                    } else  {
                        res_list_loading_view.setVisibility(View.GONE);
                        res_list_blank_view.setVisibility(View.VISIBLE);
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
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date();
                contentValues.put("date_application", dateFormat.format(date));
                contentValues.put("reason", reason);
                contentValues.put("start_date", start_date);
                contentValues.put("end_date", end_date);
                contentValues.put("driver_id", FirebaseAuth.getInstance().getCurrentUser().getUid());
            case "get_reservations":
                contentValues.put("id", FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
        return contentValues;
    }

    private void setHistoryList() {
        viewFlipper.setDisplayedChild(2);
        final RecyclerView history_rec_vw = findViewById(R.id.history_rec_vw);
        final RelativeLayout history_loading_view = findViewById(R.id.history_loading_view);
        final RelativeLayout history_blank_view = findViewById(R.id.history_blank_view);
        history_blank_view.setVisibility(View.VISIBLE);
        historyAdapter = new HistoryAdapter(this, this);
        history_rec_vw.setAdapter(historyAdapter);
        history_rec_vw.setHasFixedSize(true);
        history_rec_vw.setLayoutManager(new LinearLayoutManager(this));
        history_rec_vw.setVisibility(View.GONE);
        history_loading_view.setVisibility(View.VISIBLE);
        final DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("history/" + uid);
        historyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                historyAdapter.clearHistory();
                if (dataSnapshot != null) {
                    for (DataSnapshot historyIds : dataSnapshot.getChildren()) {
                        String history_id = historyIds.getKey();
                        String driver_id = "";
                        String pickup_name = "";
                        com.google.android.gms.maps.model.LatLng pickup_location =
                                new com.google.android.gms.maps.model.LatLng(0, 0);
                        String dropoff_name = "";
                        com.google.android.gms.maps.model.LatLng dropoff_location =
                                new com.google.android.gms.maps.model.LatLng(0, 0);
                        int driver_rating = 0;
                        String date = "";
                        String time = "";
                        String service = "";
                        String fare = "";
                        double lat = 0;
                        double lng = 0;
                        for (DataSnapshot userHistory : historyIds.getChildren()) {
                            switch (userHistory.getKey()) {
                                case "driver":
                                    for (DataSnapshot driverDetails : userHistory.getChildren()) {
                                        switch (driverDetails.getKey()) {
                                            case "id":
                                                driver_id = driverDetails.getValue().toString();
                                                break;
                                            case "rating":
                                                driver_rating = Integer.parseInt(driverDetails.getValue().toString());
                                                break;
                                        }
                                    }
                                    driver_id = userHistory.getValue().toString();
                                    break;
                                case "dropoff":
                                    for (DataSnapshot dropoffDetails : userHistory.getChildren()) {
                                        switch (dropoffDetails.getKey()) {
                                            case "name":
                                                dropoff_name = dropoffDetails.getValue().toString();
                                                break;
                                            case "lat":
                                                lat = Double.parseDouble(dropoffDetails.getValue().toString());
                                                break;
                                            case "lng":
                                                lng = Double.parseDouble(dropoffDetails.getValue().toString());
                                                break;
                                        }
                                    }
                                    dropoff_location = new com.google.android.gms.maps.model.LatLng(lat, lng);
                                    break;
                                case "pickup":
                                    for (DataSnapshot pickupDetails : userHistory.getChildren()) {
                                        switch (pickupDetails.getKey()) {
                                            case "name":
                                                pickup_name = pickupDetails.getValue().toString();
                                                break;
                                            case "lat":
                                                lat = Double.parseDouble(pickupDetails.getValue().toString());
                                                break;
                                            case "lng":
                                                lng = Double.parseDouble(pickupDetails.getValue().toString());
                                                break;
                                        }
                                    }
                                    pickup_location = new com.google.android.gms.maps.model.LatLng(lat, lng);
                                    break;
                                case "date":
                                    date = userHistory.getValue().toString();
                                    break;
                                case "time":
                                    time = userHistory.getValue().toString();
                                    break;
                                case "service":
                                    service = userHistory.getValue().toString();
                                    break;
                                case "price":
                                    fare = userHistory.getValue().toString();
                                    break;
                            }
                        }
                        HistoryAdapter.historyList.add(new HistoryObject(history_id,
                                driver_id, dropoff_name, pickup_name,
                                pickup_location, dropoff_location, date, time, driver_rating, service, fare));
                        historyAdapter.notifyDataSetChanged();
                        history_blank_view.setVisibility(View.GONE);
                    }
                    history_loading_view.setVisibility(View.GONE);
                    history_rec_vw.setVisibility(View.VISIBLE);
                    historyRef.removeEventListener(this);
                } else {
                    history_loading_view.setVisibility(View.GONE);
                    history_blank_view.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    @Override
    public void onHistoryClick(ArrayList<HistoryObject> resultList, int position) {
        Intent intent = new Intent(LandingActivity.this, HistorySingleActivity.class);
        intent.putExtra("pickupName", resultList.get(position).getPickupName());
        intent.putExtra("pickupLat", resultList.get(position).getPickupLoc().latitude);
        intent.putExtra("pickupLng", resultList.get(position).getPickupLoc().longitude);
        intent.putExtra("dropoffName", resultList.get(position).getDropoffName());
        intent.putExtra("dropoffLat", resultList.get(position).getDropoffLoc().latitude);
        intent.putExtra("dropoffLng", resultList.get(position).getDropoffLoc().longitude);
        intent.putExtra("price", resultList.get(position).getFare());
        intent.putExtra("service", resultList.get(position).getService());
        intent.putExtra("datetime", resultList.get(position).getDate() + ", "
                + resultList.get(position).getTime());
        intent.putExtra("database_id", resultList.get(position).getHistory_id());
        startActivity(intent);
    }

    private ArrayList<ReservationObject> mReservationList = new ArrayList<>();

    private void showReservationList() {
        res_list_rec_vw = findViewById(R.id.res_list_rec_vw);
        res_list_loading_view = findViewById(R.id.res_list_loading_view);
        res_list_blank_view = findViewById(R.id.res_list_blank_view);

        resAdapter = new ReservationListAdapter(this, this, mReservationList);
        res_list_rec_vw.setAdapter(resAdapter);
        res_list_rec_vw.setHasFixedSize(true);
        res_list_rec_vw.setLayoutManager(new LinearLayoutManager(this));
        res_list_loading_view.setVisibility(View.VISIBLE);
        SuperTask.execute(this,
                TaskConfig.RESERVATION_LIST_URL,
                "get_reservations");
    }

    @Override
    public void onReservationListClick(ArrayList<ReservationObject> resultList, int position) { }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.booking:
                toolbar.setTitle("Booking List");
                viewFlipper.setDisplayedChild(0);
                break;
            case R.id.reservation:
                viewFlipper.setDisplayedChild(1);
                toolbar.setTitle("Reservation List");
                showReservationList();
                break;
            case R.id.history:
                viewFlipper.setDisplayedChild(2);
                toolbar.setTitle("History");
                setHistoryList();
                break;
            case R.id.leave:
                viewFlipper.setDisplayedChild(3);
                toolbar.setTitle("Leave / Absences");
                setFileTranscript();
                break;
            case R.id.profile:
                viewFlipper.setDisplayedChild(4);
                toolbar.setTitle("Profile");
                setProfile();
                break;
            case R.id.settings:
                simulateMultipleBookings();
                //viewFlipper.setDisplayedChild(5);
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

    private void setProfile() {
        ImageView profile_img_vw = findViewById(R.id.profile_img_vw);
        TextView profile_name_txt_vw = findViewById(R.id.profile_name_txt_vw);
        TextView profile_email_txt_vw = findViewById(R.id.profile_email_txt_vw);
        TextView profile_mobnum_txt_vw = findViewById(R.id.profile_mobnum_txt_vw);
        TextView profile_plate_txt_vw = findViewById(R.id.profile_plate_txt_vw);

        Glide.with(this)
                .load((user_pic.equals("default")) ? getImage("placeholder") : user_pic)
                .apply(RequestOptions.circleCropTransform())
                .into(profile_img_vw);

        profile_name_txt_vw.setText(user_name);
        profile_email_txt_vw.setText(user_email);
        profile_mobnum_txt_vw.setText(user_mobnum);
        profile_plate_txt_vw.setText("");
    }

    public int getImage(String imageName) {
        int drawableResourceId = this.getResources()
                .getIdentifier(imageName, "drawable", this.getPackageName());
        return drawableResourceId;
    }

    private void simulateMultipleBookings() {
        String[] passenger_id = {"1qgsXEDlaze1Nh7xlIJq5SMYANh2", "9k3C1juMLDP0EokhigwxSV4do1q2",
                "CBSMPIf8QUat599EMGDjJPozz3d2", "CszQtAMzxDZnegIssEXtFo3jj6b2"},
        dropoff_name = {"SM City Novaliches", "SM North Edsa", "NAIA Terminal 1", "NAIA Terminal 2"},
        pickup_name = {"NAIA Terminal 2", "NAIA Terminal 1", "SM North Edsa", "SM City Novaliches"},
        fare = {"128.00", "130.00", "140.00", "1200.00"};
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("services/booking");
        for(int i = 0; i < passenger_id.length; i++) {
            dbRef.child(passenger_id[i]).child("dropoff").child("name").setValue(dropoff_name[i]);
            dbRef.child(passenger_id[i]).child("pickup").child("name").setValue(pickup_name[i]);
            dbRef.child(passenger_id[i]).child("fare").setValue(fare[i]);
        }
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
    protected void onStop() {
        super.onStop();
        DatabaseReference driversAvailable = FirebaseDatabase.getInstance().getReference("available_drivers");
        GeoFire available = new GeoFire(driversAvailable);
        available.removeLocation(uid);
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
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }
}
