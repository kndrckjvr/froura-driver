package com.froura.develo4.driver;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.froura.develo4.driver.libraries.DialogCreator;
import com.froura.develo4.driver.objects.BookingObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONObject;

public class JobAcceptActivity extends AppCompatActivity implements DialogCreator.DialogActionListener {

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_accept);
        passenger_id = getIntent().getStringExtra("passenger_id");

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

        pickup_passenger_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(JobAcceptActivity.this, MapNavigationActivity.class);
                intent.putExtra("destinationLat", pickupLatLng.getLatitude());
                intent.putExtra("destinationLng", pickupLatLng.getLongitude());
                startActivity(intent);
                finish();
            }
        });

        start_transit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference bookingRef = FirebaseDatabase.getInstance().getReference("services/booking/"+booking.getUid());
                bookingRef.removeValue();
                DatabaseReference intransitRef = FirebaseDatabase.getInstance().getReference("services/in_transit/"+booking.getUid());
                intransitRef.child("pickup").child("name").setValue(pickup_txt_vw.getText());
                intransitRef.child("pickup").child("lat").setValue(pickupLatLng.getLatitude());
                intransitRef.child("pickup").child("lng").setValue(pickupLatLng.getLongitude());
                intransitRef.child("dropoff").child("name").setValue(dropoff_txt_vw.getText());
                intransitRef.child("dropoff").child("lat").setValue(dropoffLatLng.getLatitude());
                intransitRef.child("dropoff").child("lng").setValue(dropoffLatLng.getLongitude());
                intransitRef.child("driver").setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
                Intent intent = new Intent(JobAcceptActivity.this, MapNavigationActivity.class);
                intent.putExtra("dropoffLat", dropoffLatLng.getLatitude());
                intent.putExtra("dropoffLng", dropoffLatLng.getLongitude());
                finish();
            }
        });

        finish_booking_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //End-service module
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

    private void setDetails() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String JSON_DETAILS_KEY = "bookingDetails";
        String bookingDetails = sharedPref.getString(JSON_DETAILS_KEY, "{ \"passenger_id\" : NULL }");
        Log.d("bookingDetails", bookingDetails+"");
        try {
            JSONObject jsonObject = new JSONObject(bookingDetails);
            pickup_txt_vw.setText(jsonObject.getString("pickupName"));
            dropoff_txt_vw.setText(jsonObject.getString("dropoffName"));
            fare_txt_vw.setText("₱ " + jsonObject.getString("fare"));
            pickupLatLng.setLatitude(jsonObject.getDouble("pickupLat"));
            pickupLatLng.setLongitude(jsonObject.getDouble("pickupLng"));
            dropoffLatLng.setLatitude(jsonObject.getDouble("dropoffLat"));
            dropoffLatLng.setLongitude(jsonObject.getDouble("dropoffLng"));
            Log.d("bookingDetails", bookingDetails+"");
        } catch (Exception e) { }
    }

    @Override
    public void onClickPositiveButton(String actionId) { }

    @Override
    public void onClickNegativeButton(String actionId) { }

    @Override
    public void onClickNeutralButton(String actionId) { }

    @Override
    public void onClickMultiChoiceItem(String actionId, int which, boolean isChecked) { }

    @Override
    public void onCreateDialogView(String actionId, View view) { }
}
