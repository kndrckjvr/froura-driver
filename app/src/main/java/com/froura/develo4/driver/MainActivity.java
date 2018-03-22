package com.froura.develo4.driver;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.froura.develo4.driver.config.TaskConfig;
import com.froura.develo4.driver.utils.DialogCreator;
import com.froura.develo4.driver.utils.SuperTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements DialogCreator.DialogActionListener {

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
                    //Check network connection
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                } else {
                    Intent intent = new Intent(MainActivity.this, LandingActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
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
    public void onClickNegativeButton(String actionId) {

    }

    @Override
    public void onClickNeutralButton(String actionId) {

    }

    @Override
    public void onClickMultiChoiceItem(String actionId, int which, boolean isChecked) {

    }

    @Override
    public void onCreateDialogView(String actionId, View view) {

    }
}
