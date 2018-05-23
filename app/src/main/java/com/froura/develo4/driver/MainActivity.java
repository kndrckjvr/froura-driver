package com.froura.develo4.driver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.froura.develo4.driver.config.TaskConfig;
import com.froura.develo4.driver.registration.LoginActivity;
import com.froura.develo4.driver.registration.PhoneAuthentication;
import com.froura.develo4.driver.utils.DialogCreator;
import com.froura.develo4.driver.utils.SuperTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements
        DialogCreator.DialogActionListener,
        SuperTask.TaskListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private String name;
    private String email;
    private String profpic;
    private String mobnum;
    private String auth;
    private String database_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = findViewById(R.id.loader);

        Glide.with(this).load(getImage("loader")).into(imageView);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user == null) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                } else {
                    if(SuperTask.isNetworkAvailable(MainActivity.this)) {
                        SuperTask.execute(MainActivity.this,
                                TaskConfig.CHECK_CONNECTION_URL,
                                "check_connection");
                    } else {
                        DialogCreator.create(MainActivity.this, "connectionError")
                                .setCancelable(false)
                                .setTitle("No Internet Connection")
                                .setMessage("This application needs an Internet Connection.")
                                .setPositiveButton("Exit")
                                .show();
                    }
                }
            }
        };
    }

    private void saveUserDetails() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        String JSON_DETAILS_KEY = "userDetails";
        String jsonDetails = "{\"name\" : \"" + WordUtils.capitalize(name.toLowerCase()) + "\", " +
                "\"email\" : \"" + email + "\", " +
                "\"mobnum\" : \"" + mobnum + "\", " +
                "\"profile_pic\" : \"" + profpic + "\", " +
                "\"auth\" : \"" + auth + "\", " +
                "\"database_id\": \""+ database_id +"\"}";
        editor.putString(JSON_DETAILS_KEY, jsonDetails);
        editor.apply();
        Intent intent = new Intent(MainActivity.this, LandingActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onTaskRespond(String json, String id) {
        switch (id) {
            case "check_connection":
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    if(jsonObject.getString("status").equals("success")) {
                        SuperTask.execute(MainActivity.this, TaskConfig.GET_DRIVER_DATA_URL,
                                "update_details");
                    } else {
                        DialogCreator.create(MainActivity.this, "connectionError")
                                .setCancelable(false)
                                .setTitle("No Server Connection!")
                                .setMessage("Server may be having some errors.")
                                .setPositiveButton("Exit")
                                .show();
                    }
                } catch (NullPointerException e) {
                    DialogCreator.create(MainActivity.this, "connectionError")
                            .setCancelable(false)
                            .setTitle("No Internet Connection")
                            .setMessage("This application needs an Internet Connection.")
                            .setPositiveButton("Exit")
                            .show();
                } catch (Exception e) { }
                break;
            case "update_details":
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    String status = jsonObject.getString("status");
                    if(status.equals("success")) {
                        String plate = jsonObject.getString("plate");
                        database_id = jsonObject.getString("database_id");
                        email = jsonObject.getString("email");
                        name = jsonObject.getString("name");
                        mobnum = jsonObject.getString("contact");
                        auth = "mobile";
                        profpic = jsonObject.getString("img_path");

                        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                                .getReference("users/driver/"+FirebaseAuth.getInstance().getUid());
                        dbRef.child("name").setValue(WordUtils.capitalize(name.toLowerCase()));
                        dbRef.child("email").setValue(email);
                        dbRef.child("mobnum").setValue(mobnum);
                        dbRef.child("auth").setValue(auth);
                        dbRef.child("profile_pic").setValue(profpic);
                        dbRef.child("plate").setValue(plate);
                        saveUserDetails();
                    }
                } catch (Exception e) {}
                break;
        }
    }

    @Override
    public ContentValues setRequestValues(ContentValues contentValues, String id) {
        contentValues.put("android", 1);
        contentValues.put("firebase_uid", FirebaseAuth.getInstance().getUid());
        return contentValues;
    }

    public int getImage(String imageName) {
        return this.getResources()
                .getIdentifier(imageName, "drawable", this.getPackageName());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    public void onClickPositiveButton(String actionId) {
        switch (actionId) {
            case "internetDisabled":
                finish();
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
