package com.aurasoft.booky;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private AlertDialog loadingDialog;

    // Google Sign-In Variables
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initLoadingDialog();
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        setupVerificationCallbacks();

        findViewById(R.id.login_btn_login).setOnClickListener(v -> {
            loadingDialog.show();
            sendOtp();
        });

        findViewById(R.id.googleSignIn).setOnClickListener(v -> {
            signInWithGoogle();
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String userName = account.getDisplayName();
                    String userEmail = account.getEmail();

                    Log.d("GoogleData", "Name: " + userName);

                    firebaseAuthWithGoogle(account.getIdToken(), userName);
                }
            } catch (ApiException e) {
                Log.e("GoogleAuth", "Sign in failed: " + e.getStatusCode());
                Toast.makeText(this, "Sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken, String name) {
        loadingDialog.show();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        String email = mAuth.getCurrentUser().getEmail();

                        com.google.firebase.firestore.FirebaseFirestore db =
                                com.google.firebase.firestore.FirebaseFirestore.getInstance();

                        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
                        userMap.put("name", name);
                        userMap.put("email", email);
                        userMap.put("uid", uid);
                        userMap.put("method", "google");

                        db.collection("Users").document(uid)
                                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    loadingDialog.dismiss();
                                    Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    loadingDialog.dismiss();
                                    Log.e("FirestoreError", e.getMessage());
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                });

                    } else {
                        loadingDialog.dismiss();
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
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

        // mobile validation
        if (mobileNo.length() != 9) {
            loadingDialog.dismiss();
            etMobile.setError("Enter 9 digits");
            return;
        }

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
        builder.setCancelable(false);
        loadingDialog = builder.create();
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}