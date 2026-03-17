package com.aurasoft.booky;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Fullscreen Logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_splash);

        ImageView im = findViewById(R.id.logoImg);
        Glide.with(this).asBitmap().load(R.drawable.img_6).override(300).into(im);

        // Progress Bar එක තත්පර 1කට පස්සේ පෙන්වීම
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (findViewById(R.id.progressBar) != null) {
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            }
        }, 1000);

        // තත්පර 3.5කට පසු Network සහ User Status චෙක් කිරීම ආරම්භ කිරීම
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserStatus();
        }, 3500);
    }

    private void checkUserStatus() {
        // 1. මුලින්ම ඉන්ටර්නෙට් තියෙනවද බලනවා
        if (isNetworkAvailable()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            Intent intent;
            if (user != null) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, GetstartedActivity.class);
            }
            startActivity(intent);
            finish();
        } else {
            // 2. ඉන්ටර්නෙට් නැත්නම් Toast එකක් පෙන්වා නැවත තත්පර 3කින් චෙක් කරනවා
            Toast.makeText(this, "No internet connection. Please connect and wait...", Toast.LENGTH_SHORT).show();

            // ලූප් එකක් වගේ ආයෙත් මේ මෙතඩ් එකම කෝල් කරනවා
            new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, 10000);
        }
    }

    // Network Connection එක චෙක් කරන මෙතඩ් එක
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}