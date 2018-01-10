package com.froura.develo4.driver;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
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
    private boolean phoneReg;
    private CountDownTimer requestCodeTimer;

    private String TAG = "PhoneAuth";
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
        progressDialog.setMessage("Logging in...");
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
                    Intent intent = new Intent(PhoneAuthentication.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
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

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            userExist(mAuth.getCurrentUser().getUid());
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
                        Log.w(TAG, "onVerificationFailed", e);
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

    private void userExist(String user_id) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("users").child("passenger").child(user_id);
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    registerUser();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void registerUser() {
        String user_id = mAuth.getCurrentUser().getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("users").child("passenger").child(user_id);
        dbRef.child("name").setValue(name);
        dbRef.child("email").setValue(email);
        dbRef.child("mobnum").setValue(mobNum);
        new CheckUserTasks(PhoneAuthentication.this).execute();
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
    public void parseCheckUserJSONString(String jsonString) {

    }

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
