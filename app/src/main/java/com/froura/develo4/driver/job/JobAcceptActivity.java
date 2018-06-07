package com.froura.develo4.driver.job;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.froura.develo4.driver.LandingActivity;
import com.froura.develo4.driver.R;
import com.froura.develo4.driver.config.TaskConfig;
import com.froura.develo4.driver.utils.DialogCreator;
import com.froura.develo4.driver.object.BookingObject;
import com.froura.develo4.driver.utils.NavigationLauncher;
import com.froura.develo4.driver.utils.SuperTask;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class JobAcceptActivity extends AppCompatActivity
        implements DialogCreator.DialogActionListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        SuperTask.TaskListener {

    private String passenger_id;
    private BookingObject booking;

    private TextView pickup_txt_vw;
    private TextView dropoff_txt_vw;
    private TextView fare_txt_vw;

    private Button pickup_passenger_btn;
    private Button start_transit_btn;
    private Button finish_booking_btn;
    private Button cancel_booking_btn;

    private LatLng pickupLatLng = new LatLng();
    private LatLng dropoffLatLng = new LatLng();

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location originLocation;
    private LatLng originCoord;
    private LatLng destinationCoord;
    private Point originPosition;
    private Point destinationPosition;
    private String start_time;
    private String pickup_name;
    private String dropoff_name;
    private String amount = "";

    private String uid;
    private SharedPreferences sharedPref;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_accept);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading Data...");
        progressDialog.show();

        buildGoogleApiClient();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        uid = FirebaseAuth.getInstance().getUid();
        pickup_passenger_btn = findViewById(R.id.pickup_passenger_btn);
        start_transit_btn = findViewById(R.id.start_transit_btn);
        finish_booking_btn = findViewById(R.id.finish_booking_btn);
        cancel_booking_btn = findViewById(R.id.cancel_booking_btn);
        pickup_txt_vw = findViewById(R.id.pickup_txt_vw);
        dropoff_txt_vw = findViewById(R.id.dropoff_txt_vw);
        fare_txt_vw = findViewById(R.id.fare_txt_vw);
        pickup_txt_vw.setSelected(true);
        dropoff_txt_vw.setSelected(true);
        setDetails();
        checkPickup();
        pickup_passenger_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("pickupClicked", true);
                editor.apply();
                navigate(pickupLatLng.getLatitude(), pickupLatLng.getLongitude());
                finish();
            }
        });

        start_transit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                        .getReference("services/booking/"+passenger_id+"/in_transit");
                bookingRef.setValue(true);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("transitClicked", true);
                editor.apply();
                navigate(dropoffLatLng.getLatitude(), dropoffLatLng.getLongitude());
                finish();
            }
        });

        finish_booking_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogCreator.create(JobAcceptActivity.this, "amount")
                        .setTitle("Input amount(Meter Fare):")
                        .setView(R.layout.dialog_amount)
                        .setPositiveButton("Save")
                        .setNegativeButton("Cancel")
                        .show();
            }
        });

        cancel_booking_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogCreator.create(JobAcceptActivity.this, "cancel_booking")
                        .setMessage("Are you sure to cancel? This may have deductions on your incentives.")
                        .setPositiveButton("YES")
                        .setNegativeButton("NO")
                        .setCancelable(false)
                        .show();
            }
        });
    }

    private void checkPickup() {
        boolean pickupClicked = sharedPref.getBoolean("pickupClicked", false);
        boolean pickupPass = sharedPref.getBoolean("pickupPass", false);
        boolean transitClicked = sharedPref.getBoolean("transitClicked", false);
        boolean transitPass = sharedPref.getBoolean("transitPass", false);
        if(pickupClicked) {
            if(!pickupPass && getIntent().getStringExtra("navAc") != null) {
                DialogCreator.create(this, "pickupPass")
                        .setCancelable(false)
                        .setPositiveButton("Yes")
                        .setNegativeButton("No")
                        .setMessage("Did you pick up the passenger?")
                        .show();
            }
        }
        if(transitClicked) {
            if(!transitPass && getIntent().getStringExtra("navAc") != null) {
                DialogCreator.create(this, "intransit")
                        .setCancelable(false)
                        .setPositiveButton("Yes")
                        .setNegativeButton("No")
                        .setMessage("Did you reach the dropoff?")
                        .show();
            }
        }
    }

    private void navigate(double destLat, double destLng) {
        Mapbox.getInstance(this, getResources().getString(R.string.mapbox_token));
        originCoord = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());

        destinationCoord = new LatLng(destLat, destLng);

        destinationPosition = Point.fromLngLat(destinationCoord.getLongitude(), destinationCoord.getLatitude());
        originPosition = Point.fromLngLat(originCoord.getLongitude(), originCoord.getLatitude());
        Point origin = originPosition;
        Point destination = destinationPosition;

        MapboxNavigationOptions navOptions = MapboxNavigationOptions.builder()
                .manuallyEndNavigationUponCompletion(true)
                .unitType(NavigationUnitType.TYPE_METRIC)
                .build();

        NavigationViewOptions options = NavigationViewOptions.builder()
                .origin(origin)
                .navigationOptions(navOptions)
                .destination(destination)
                .navigationListener(new NavigationListener() {
                    @Override
                    public void onCancelNavigation() {

                    }

                    @Override
                    public void onNavigationFinished() {
                        finish();
                    }

                    @Override
                    public void onNavigationRunning() {

                    }
                })
                .shouldSimulateRoute(false)
                .awsPoolId(null)
                .build();

        NavigationLauncher.startNavigation(JobAcceptActivity.this, options);
    }

    private void setDetails() {
        String JSON_DETAILS_KEY = "bookingDetails";
        String bookingDetails = sharedPref.getString(JSON_DETAILS_KEY, "{ \"passenger_id\" : NULL }");
        Log.d("bookingDetails", bookingDetails+"");
        try {
            JSONObject jsonObject = new JSONObject(bookingDetails);
            start_time = jsonObject.getString("startTime");
            passenger_id = jsonObject.getString("passenger_id");
            pickup_txt_vw.setText(pickup_name = jsonObject.getString("pickupName"));
            dropoff_txt_vw.setText(dropoff_name = jsonObject.getString("dropoffName"));
            fare_txt_vw.setText("₱ " + jsonObject.getString("fare"));
            pickupLatLng.setLatitude(jsonObject.getDouble("pickupLat"));
            pickupLatLng.setLongitude(jsonObject.getDouble("pickupLng"));
            dropoffLatLng.setLatitude(jsonObject.getDouble("dropoffLat"));
            dropoffLatLng.setLongitude(jsonObject.getDouble("dropoffLng"));
            Log.d("bookingDetails", bookingDetails+"");
        } catch (Exception e) { }
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

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }

    @Override
    public void onLocationChanged(Location location) {
        DatabaseReference driversAvailable = FirebaseDatabase.getInstance().getReference("working_drivers");
        GeoFire geoFire = new GeoFire(driversAvailable);
        if (location != null) {
            progressDialog.dismiss();
            originLocation = location;
            geoFire.setLocation(uid, new GeoLocation(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onTaskRespond(String json, String id) {
        try {
            Log.d("JOBACCEPT_AC", json+"");
            JSONObject jsonObject = new JSONObject(json);
            if(jsonObject.getBoolean("status")) {
                Intent intent = new Intent(JobAcceptActivity.this, LandingActivity.class);
                intent.putExtra("fromJob", true);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) { }
    }

    @Override
    public ContentValues setRequestValues(ContentValues contentValues, String id) {
        contentValues.put("android", 1);
        switch (id) {
            case "amount":
                contentValues.put("start_time", start_time);
                contentValues.put("end_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                contentValues.put("passenger_uid", passenger_id);
                contentValues.put("start_destination", pickup_name);
                contentValues.put("end_destination", dropoff_name);
                contentValues.put("driver_uid", FirebaseAuth.getInstance().getUid());
                contentValues.put("amount", amount);
                break;
        }
        Log.d("JOBACCEPT_AC", contentValues+"");
        return contentValues;
    }

    @Override
    public void onClickPositiveButton(String actionId) {
        SharedPreferences.Editor editor = sharedPref.edit();
        String userDetails = sharedPref.getString("userDetails", "");
        switch (actionId) {
            case "pickupPass":
                editor.remove("pickupClicked");
                editor.putBoolean("pickupPass", true);
                editor.apply();
                pickup_passenger_btn.setEnabled(false);
                pickup_passenger_btn.setBackground(getResources().getDrawable(R.drawable.background_border_sharp_disabled));
                start_transit_btn.setEnabled(true);
                start_transit_btn.setBackground(getResources().getDrawable(R.drawable.background_border_sharp));
                break;
            case "intransit":
                editor.remove("transitClicked");
                editor.putBoolean("transitPass", true);
                editor.apply();
                finish_booking_btn.setEnabled(true);
                finish_booking_btn.setBackground(getResources().getDrawable(R.drawable.background_border_sharp));
                start_transit_btn.setEnabled(false);
                start_transit_btn.setBackground(getResources().getDrawable(R.drawable.background_border_sharp_disabled));
                pickup_passenger_btn.setEnabled(false);
                pickup_passenger_btn.setBackground(getResources().getDrawable(R.drawable.background_border_sharp_disabled));
                cancel_booking_btn.setEnabled(false);
                cancel_booking_btn.setBackground(getResources().getDrawable(R.drawable.background_border_sharp_disabled));
                break;
            case "cancel_booking":
                editor.clear();
                editor.putString("userDetails", userDetails);
                editor.apply();
                DatabaseReference dbRef = FirebaseDatabase.getInstance()
                        .getReference("services/booking/" + passenger_id +"/cancelled_by");
                dbRef.child("driver").setValue(true);
                Intent intent = new Intent(JobAcceptActivity.this, LandingActivity.class);
                intent.putExtra("fromJob", true);
                startActivity(intent);
                finish();
                break;
            case "amount":
                if(amount.isEmpty()) {
                    DialogCreator.create(JobAcceptActivity.this, "amount")
                            .setTitle("Input amount(Meter Fare):")
                            .setView(R.layout.dialog_amount)
                            .setPositiveButton("Save")
                            .setNegativeButton("Cancel")
                            .show();
                } else {
                    editor.clear();
                    editor.putString("userDetails", userDetails);
                    editor.apply();
                    SuperTask.execute(JobAcceptActivity.this,
                            TaskConfig.SAVE_BOOKING_DETAILS,
                            "amount");
                    end_trip();
                    save_history();
                }
                break;
        }
    }

    private void save_history() {
        double price = Double.parseDouble(amount);
        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("history/"+passenger_id);

        String push_id = historyRef.push().getKey();
        historyRef = FirebaseDatabase.getInstance()
                .getReference("history/"+passenger_id+"/"+push_id);

        historyRef.child("date").setValue(new SimpleDateFormat("MMMM dd, yyyy").format(new Date()));
        historyRef.child("driver").child("id").setValue(uid);
        historyRef.child("driver").child("rating").setValue(0);
        historyRef.child("dropoff").child("name").setValue(dropoff_name);
        historyRef.child("dropoff").child("lat").setValue(dropoffLatLng.getLatitude());
        historyRef.child("dropoff").child("lng").setValue(dropoffLatLng.getLongitude());
        historyRef.child("pickup").child("name").setValue(pickup_name);
        historyRef.child("pickup").child("lat").setValue(pickupLatLng.getLatitude());
        historyRef.child("pickup").child("lng").setValue(pickupLatLng.getLongitude());
        historyRef.child("time").setValue(new SimpleDateFormat("h:mm a").format(new Date()));
        historyRef.child("price").setValue("Php " + String.format("%.2f",price));
        historyRef.child("service").setValue("Booking");

        historyRef = FirebaseDatabase.getInstance()
                .getReference("history/"+FirebaseAuth.getInstance().getUid());

        push_id = historyRef.push().getKey();
        historyRef = FirebaseDatabase.getInstance()
                .getReference("history/"+FirebaseAuth.getInstance().getUid()+"/"+push_id);
        historyRef.child("date").setValue(new SimpleDateFormat("MMMM dd, yyyy").format(new Date()));
        historyRef.child("passenger").child("id").setValue(passenger_id);
        historyRef.child("dropoff").child("name").setValue(dropoff_name);
        historyRef.child("dropoff").child("lat").setValue(dropoffLatLng.getLatitude());
        historyRef.child("dropoff").child("lng").setValue(dropoffLatLng.getLongitude());
        historyRef.child("pickup").child("name").setValue(pickup_name);
        historyRef.child("pickup").child("lat").setValue(pickupLatLng.getLatitude());
        historyRef.child("pickup").child("lng").setValue(pickupLatLng.getLongitude());
        historyRef.child("time").setValue(new SimpleDateFormat("h:mm a").format(new Date()));
        historyRef.child("price").setValue("Php " + String.format("%.2f",price));
        historyRef.child("service").setValue("Booking");
        DatabaseReference bookRef = FirebaseDatabase.getInstance().getReference("services/booking/" + passenger_id);
        bookRef.removeValue();
    }

    private void end_trip() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("services/booking/"+ passenger_id +"/end");
        dbRef.setValue(true);
    }

    @Override
    public void onClickNegativeButton(String actionId) { }

    @Override
    public void onClickNeutralButton(String actionId) { }

    @Override
    public void onClickMultiChoiceItem(String actionId, int which, boolean isChecked) { }

    @Override
    public void onCreateDialogView(String actionId, View view) {
        switch (actionId) {
            case "amount":
                final TextInputEditText amount_et = view.findViewById(R.id.amount_et);
                amount_et.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        amount = amount_et.getText().toString();
                    }

                    @Override
                    public void afterTextChanged(Editable editable) { }
                });
                break;
        }
    }
}
