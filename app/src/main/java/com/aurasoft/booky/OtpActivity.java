package com.aurasoft.booky;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class OtpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText otp1 = findViewById(R.id.otp1);
        EditText otp2 = findViewById(R.id.otp2);
        EditText otp3 = findViewById(R.id.otp3);
        EditText otp4 = findViewById(R.id.otp4);
        EditText otp5 = findViewById(R.id.otp5);
        EditText otp6 = findViewById(R.id.otp6);


        otp1.addTextChangedListener(new OtpTextWatcher(otp1, null, otp2));
        otp2.addTextChangedListener(new OtpTextWatcher(otp2, otp1, otp3));
        otp3.addTextChangedListener(new OtpTextWatcher(otp3, otp2, otp4));
        otp4.addTextChangedListener(new OtpTextWatcher(otp4, otp3, otp5));
        otp5.addTextChangedListener(new OtpTextWatcher(otp5, otp4, otp6));
        otp6.addTextChangedListener(new OtpTextWatcher(otp6, otp5, null));

        View.OnKeyListener backspaceListener = (v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL
                    && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {

                EditText current = (EditText) v;

                if (current.getText().toString().isEmpty()) {
                    if (v.getId() == R.id.otp2) otp1.requestFocus();
                    else if (v.getId() == R.id.otp3) otp2.requestFocus();
                    else if (v.getId() == R.id.otp4) otp3.requestFocus();
                    else if (v.getId() == R.id.otp5) otp4.requestFocus();
                    else if (v.getId() == R.id.otp6) otp5.requestFocus();
                }
            }
            return false;
        };
        otp2.setOnKeyListener(backspaceListener);
        otp3.setOnKeyListener(backspaceListener);
        otp4.setOnKeyListener(backspaceListener);
        otp5.setOnKeyListener(backspaceListener);
        otp6.setOnKeyListener(backspaceListener);


        String mVerificationId = getIntent().getStringExtra("verificationId");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        Button btn = findViewById(R.id.btnVerify);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String otpCode = otp1.getText().toString() +
                        otp2.getText().toString() +
                        otp3.getText().toString() +
                        otp4.getText().toString() +
                        otp5.getText().toString() +
                        otp6.getText().toString();

                if (otpCode.length() == 6) {
                    // 2. Firebase එකට කෝඩ් එක යවනවා
                    verifyOtp(otpCode, mVerificationId, mAuth);
                } else {
                    Toast.makeText(OtpActivity.this, "please Entaer valid Otp", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    public class OtpTextWatcher implements TextWatcher {
        private View currentView;
        private View previousView;
        private View nextView;

        public OtpTextWatcher(View currentView, View previousView, View nextView) {
            this.currentView = currentView;
            this.previousView = previousView;
            this.nextView = nextView;
        }

        @Override
        public void afterTextChanged(Editable s) {
            String text = s.toString();


            if (text.length() == 1 && nextView != null) {
                nextView.requestFocus();
            } else if (text.length() == 0 && previousView != null) {
                previousView.requestFocus();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }


    //firebase verification for code

    private void verifyOtp(String code, String verificationId, FirebaseAuth auth) {

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Firebase හරහා මෙය නව පරිශීලකයෙක්දැයි පරීක්ෂා කිරීම
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                        Intent intent;
                        if (isNewUser) {
                            // පළමු වතාවට එන කෙනෙක් නම් Name Entry Screen එකට
                            intent = new Intent(OtpActivity.this, NameEntryActivity.class);
                        } else {
                            // දැනටමත් සිටින පරිශීලකයෙක් නම් Home Screen එකට
                            intent = new Intent(OtpActivity.this, MainActivity.class);
                        }

                        // *** වැදගත්: Flags සෙට් කළ යුත්තේ startActivity කිරීමට පෙරයි ***
                        // මෙය මගින් Login සහ OTP Screens සියල්ලම Stack එකෙන් ඉවත් කරයි
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        startActivity(intent);

                        // වර්තමාන OtpActivity එක වසා දැමීම
                        finish();

                    } else {
                        // OTP එක වැරදියි හෝ වෙනත් දෝෂයක්
                        Toast.makeText(OtpActivity.this, "Wrong OTP, please try again", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}