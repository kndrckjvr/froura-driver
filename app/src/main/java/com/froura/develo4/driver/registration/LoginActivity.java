package com.froura.develo4.driver.registration;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.froura.develo4.driver.R;
import com.froura.develo4.driver.utils.SnackBarCreator;

public class LoginActivity extends AppCompatActivity {

    private Button mobLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mobLogin = findViewById(R.id.mobLogin);

        mobLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, PhoneRegistration.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        if(getIntent().getIntExtra("loginError", -1) == 1) {
            SnackBarCreator.set("Sorry! You're not a Driver.");
            SnackBarCreator.show(mobLogin);
        }
    }
}