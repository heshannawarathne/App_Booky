
package com.aurasoft.booky;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initLoadingDialog();

        mAuth = FirebaseAuth.getInstance();

        setupVerificationCallbacks();


        findViewById(R.id.login_btn_login).setOnClickListener(v -> {
            loadingDialog.show();
            sendOtp();
        });


//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });


        //otp


    }

    private void setupVerificationCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("Test", e.getMessage());
                if (loadingDialog != null) {
                    loadingDialog.dismiss();
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                mVerificationId = verificationId;

                Intent intent = new Intent(LoginActivity.this, OtpActivity.class);
                intent.putExtra("verificationId", verificationId);
                startActivity(intent);

                if (loadingDialog != null) {
                    loadingDialog.dismiss();
                }
            }
        };
    }

    private void sendOtp() {
        EditText etMobile = findViewById(R.id.login_tf_mobile);
        String mobileNo = etMobile.getText().toString().trim();

        if (mobileNo.isEmpty()) {
            loadingDialog.dismiss();
            etMobile.setError("Please enter your mobile number");
            etMobile.requestFocus();
            return;

        }

        if (!(mobileNo.startsWith("70") || mobileNo.startsWith("71") ||
                mobileNo.startsWith("72") || mobileNo.startsWith("74") ||
                mobileNo.startsWith("75") || mobileNo.startsWith("76") ||
                mobileNo.startsWith("77") || mobileNo.startsWith("78"))) {
            loadingDialog.dismiss();

            etMobile.setError("Use a valid Sri Lankan mobile number");
            etMobile.requestFocus();
            return;
        }

        if (!mobileNo.matches("[0-9]+")) {
            loadingDialog.dismiss();
            etMobile.setError("Please enter digits only");
            etMobile.requestFocus();
            return;
        }
        if (mobileNo.length() != 9) {
            loadingDialog.dismiss();
            etMobile.setError("Enter the remaining 9 digits of your number");
            etMobile.requestFocus();
            return;
        }
        if (mobileNo.startsWith("0")) {
            loadingDialog.dismiss();
            etMobile.setError("Do not include the leading 0");
            etMobile.requestFocus();
            return;
        }
        if (loadingDialog != null) loadingDialog.show();

        String phoneNumber = "+94" + mobileNo;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void initLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(view);

        builder.setCancelable(false); //


        loadingDialog = builder.create();

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
