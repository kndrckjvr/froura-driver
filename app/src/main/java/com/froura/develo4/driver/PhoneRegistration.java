package com.froura.develo4.driver;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class PhoneRegistration extends AppCompatActivity {

    private EditText mobET;
    private TextInputLayout mobTL;

    private String mobNum = "";
    private boolean mobET_error_empty = false;
    private boolean mobET_error_match = false;
    private boolean mobile_check = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_registration);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mobET = findViewById(R.id.mobET);
        mobTL = findViewById(R.id.mobTL);

        mobET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mobNum = mobET.getText().toString().isEmpty() ? "" : mobET.getText().toString();
                checkMobErrors();
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }

    private void checkMobErrors() {
        if(mobNum.isEmpty()) {
            mobET_error_empty = true;
        } else {
            mobET_error_empty = false;
        }

        if(mobNum.matches("^(09|\\+639)\\d{9}$")) {
            mobET_error_match = false;
        } else {
            mobET_error_match = true;
        }
        putMobErrors();
    }

    private void putMobErrors() {
        if(mobET_error_empty) {
            setError("Enter your mobile number.", mobTL, mobET);
            mobile_check = false;
            return;
        } else {
            clearError(mobTL, mobET);
        }

        if(mobET_error_match) {
            setError("Enter a valid Philippine mobile number.", mobTL, mobET);
            mobile_check = false;
            return;
        } else {
            clearError(mobTL, mobET);
        }

        mobile_check = true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.next:
                Intent intent = new Intent(PhoneRegistration.this, PhoneAuthentication.class);
                intent.putExtra("mobnum", mobNum);
                intent.putExtra("phoneReg", true);
                finish();
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(mobile_check) {
            menu.findItem(R.id.next).setEnabled(true);
        } else {
            menu.findItem(R.id.next).setEnabled(false);
        }
        return true ;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(PhoneRegistration.this, LoginActivity.class);
        finish();
        startActivity(intent);
        return;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.phone_reg, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setError(String message, TextInputLayout tl, EditText et) {
        tl.setError(message);
        et.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_warning_red_24dp,0);
        invalidateOptionsMenu();
    }

    private void clearError(TextInputLayout tl, EditText et) {
        tl.setError(null);
        tl.setErrorEnabled(false);
        et.setCompoundDrawablesWithIntrinsicBounds(0,0, 0,0);
        invalidateOptionsMenu();
    }
}