package com.froura.develo4.driver;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.froura.develo4.driver.libraries.RequestPostString;
import com.froura.develo4.driver.libraries.SnackBarCreator;
import com.froura.develo4.driver.tasks.CheckUserTasks;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.text.WordUtils;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

public class PhoneAuthentication extends AppCompatActivity implements CheckUserTasks.OnLoginDriverTasksListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private TextView requestCode;
    private TextView mob_num;
    private Button verify;
    private EditText verifCode;
    private ProgressDialog progressDialog;

    private String mobNum;
    private String email;
    private String name;
    private String profpic = "default";
    private String auth = "mobile";
    private boolean phoneReg;
    private CountDownTimer requestCodeTimer;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_authentication);

        requestCode = findViewById(R.id.txtVw_request_code);
        verify = findViewById(R.id.btn_verify);
        verifCode = findViewById(R.id.et_verif_code);
        mob_num = findViewById(R.id.txtVw_mob_num);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Login");
        progressDialog.setMessage("Logging in with Mobile...");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);

        mobNum = getIntent().getStringExtra("mobNum");
        email = getIntent().getStringExtra("email");
        name = getIntent().getStringExtra("name");
        phoneReg = getIntent().getBooleanExtra("phoneReg", false);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(mAuth.getCurrentUser() != null) {
                    registerUser();
                    return;
                }
            }
        };

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mCode = verifCode.getText().toString();
                if(!mCode.isEmpty()) {
                    signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(mVerificationId, mCode));
                    verifCode.setError(null);
                    verifCode.setCompoundDrawablesWithIntrinsicBounds(0,0, 0,0);
                } else {
                    verifCode.setError("Code is required.");
                    verifCode.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_warning_red_24dp,0);
                }

            }
        });

        requestCodeTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long l) {
                requestCode.setText("Request a new code in 00:"+ l/1000);
                requestCode.setTextColor(getResources().getColor(R.color.textViewColor));
            }

            @Override
            public void onFinish() {
                requestCode.setText(Html.fromHtml("<u>Request a new code.</u>"));
                requestCode.setTextColor(getResources().getColor(R.color.textLinkColor));

                requestCode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestCode();
                    }
                });
            }
        };

        if(mobNum.matches("^(09)\\d{9}$")) {
            mob_num.setText("+63 " + mobNum.substring(1));
        } else if(mobNum.matches("^(\\+639)\\d{9}$")) {
            mob_num.setText("+63 " + mobNum.substring(3));
        }

        if(phoneReg) {
            requestCode();
            requestCode.setOnClickListener(null);
            requestCodeTimer.start();
            phoneReg = false;
        }
    }

    private void saveUserDetails() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        String JSON_DETAILS_KEY = "userDetails";
        String jsonDetails = "{ \"name\" : \"" + WordUtils.capitalize(name.toLowerCase()) + "\", \"email\" : \"" + email + "\", \"mobnum\" : \"" + mobNum + "\", \"profile_pic\" : \"" + profpic + "\", \"auth\" : \"" + auth + "\"}";
        editor.putString(JSON_DETAILS_KEY, jsonDetails);
        editor.apply();
        progressDialog.dismiss();
        Intent intent = new Intent(PhoneAuthentication.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()) {
                            Log.e("firebaseMobile", task.getException().toString());
                        }
                    }
                });
    }

    private void requestCode() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mobNum,
                60,
                TimeUnit.SECONDS,
                PhoneAuthentication.this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        progressDialog.show();
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            //Phone number is incorrect
                            Toast.makeText(PhoneAuthentication.this, "Phone number is incorrect!", Toast.LENGTH_SHORT).show();
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            //Quota has been reached
                            Toast.makeText(PhoneAuthentication.this, "Server Overload! A request has been sent.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        mVerificationId = s;
                    }
                });
    }

    private void registerUser() {
        final String user_id = mAuth.getCurrentUser().getUid();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean userFound = false;
                for(DataSnapshot users: dataSnapshot.getChildren()) {
                    if(users.getKey().equals("passenger")) {
                        for(DataSnapshot passenger : users.getChildren()) {
                            if(user_id.equals(passenger.getKey())) {
                                Log.d("hello", passenger.getKey() + " " + passenger.getValue() + " " + userFound);
                                userFound = true;
                                progressDialog.dismiss();
                                mAuth.signOut();
                                Intent intent = new Intent(PhoneAuthentication.this, LandingActivity.class);
                                intent.putExtra("loginError", 1);
                                startActivity(intent);
                                finish();
                                break;
                            }
                        }
                    } else if(users.getKey().equals("driver")){
                        for(DataSnapshot driver : users.getChildren()) {
                            if(user_id.equals(driver.getKey())) {
                                userFound = true;
                                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(user_id);
                                dbRef.child("name").setValue(WordUtils.capitalize(name.toLowerCase()));
                                dbRef.child("email").setValue(email);
                                dbRef.child("mobnum").setValue(mobNum);
                                dbRef.child("auth").setValue(auth);
                                dbRef.child("profile_pic").setValue(profpic);
                                saveUserDetails();
                                new CheckUserTasks(PhoneAuthentication.this).execute();
                                break;
                            }
                        }
                    }
                    if(userFound) break;
                }
                if(!userFound) {
                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(user_id);
                    dbRef.child("name").setValue(WordUtils.capitalize(name.toLowerCase()));
                    dbRef.child("email").setValue(email);
                    dbRef.child("mobnum").setValue(mobNum);
                    dbRef.child("auth").setValue(auth);
                    dbRef.child("profile_pic").setValue(profpic);
                    saveUserDetails();
                    new CheckUserTasks(PhoneAuthentication.this).execute();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
    public void parseCheckUserJSONString(String jsonString) { }

    @Override
    public String createCheckUserPostString(ContentValues contentValues) throws UnsupportedEncodingException {
        contentValues.put("android", 1);
        contentValues.put("name", name);
        contentValues.put("email", email);
        contentValues.put("mobile", mobNum);
        contentValues.put("firebase_id", mAuth.getCurrentUser().getUid());
        return RequestPostString.create(contentValues);
    }
}
