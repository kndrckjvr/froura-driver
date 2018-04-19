package com.froura.develo4.driver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.froura.develo4.driver.config.TaskConfig;
import com.froura.develo4.driver.registration.LoginActivity;
import com.froura.develo4.driver.utils.DialogCreator;
import com.froura.develo4.driver.utils.SuperTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements
        DialogCreator.DialogActionListener,
        SuperTask.TaskListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

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

    @Override
    public void onTaskRespond(String json, String id) {
        switch (id) {
            case "check_connection":
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    if(jsonObject.getString("status").equals("success")) {
                        //updated
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
        }
    }

    @Override
    public ContentValues setRequestValues(ContentValues contentValues, String id) {
        contentValues.put("android", 1);
        return contentValues;
    }

    public int getImage(String imageName) {
        int drawableResourceId = this.getResources()
                .getIdentifier(imageName, "drawable", this.getPackageName());

        return drawableResourceId;
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
