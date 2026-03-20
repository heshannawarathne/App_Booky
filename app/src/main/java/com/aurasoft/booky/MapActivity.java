package com.aurasoft.booky;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView tvSelectedLocation;
    private ImageView imgCenterMarker;
    private Point selectedPoint;

    private String scheduleId;
    private int totalPrice;
    private ArrayList<String> selectedSeats;
    private ArrayList<String> selectedGenders;

    private AlertDialog loadingDialog;
    private AlertDialog noInternetDialog;
    private final Handler connectionHandler = new Handler(Looper.getMainLooper());

    // නිතරම කනෙක්ෂන් එක බලන්න අවශ්‍ය Variables
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isStyleLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.mapbox.common.MapboxOptions.setAccessToken(getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_map);

        setupLoadingDialog();

        mapView = findViewById(R.id.mapView);
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        imgCenterMarker = findViewById(R.id.img_center_marker);
        FloatingActionButton fabCurrentLocation = findViewById(R.id.fabCurrentLocation);
        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);
        MaterialCardView btnBack = findViewById(R.id.btnBack);

        scheduleId = getIntent().getStringExtra("SCHEDULE_ID");
        totalPrice = getIntent().getIntExtra("TOTAL_PRICE", 0);
        selectedSeats = getIntent().getStringArrayListExtra("SELECTED_SEATS");
        selectedGenders = getIntent().getStringArrayListExtra("SELECTED_GENDERS");

        // 1. Connectivity Manager එක පටන් ගන්නවා
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        startNetworkMonitoring();

        // 2. මුලින්ම කනෙක්ෂන් චෙක් කරලා මැප් එක ලෝඩ් කරනවා
        checkInitialConnection();

        btnBack.setOnClickListener(v -> onBackPressed());
        fabCurrentLocation.setOnClickListener(v -> focusLocation());

        btnConfirm.setOnClickListener(v -> {
            if (selectedPoint != null) {
                Intent summaryIntent = new Intent(MapActivity.this, BookingSummaryActivity.class);
                summaryIntent.putExtra("SCHEDULE_ID", scheduleId);
                summaryIntent.putExtra("TOTAL_PRICE", totalPrice);
                summaryIntent.putStringArrayListExtra("SELECTED_SEATS", selectedSeats);
                summaryIntent.putStringArrayListExtra("SELECTED_GENDERS", selectedGenders);
                summaryIntent.putExtra("PICKUP_ADDRESS", tvSelectedLocation.getText().toString());
                summaryIntent.putExtra("PICKUP_LAT", selectedPoint.latitude());
                summaryIntent.putExtra("PICKUP_LNG", selectedPoint.longitude());
                startActivity(summaryIntent);
            } else {
                Toast.makeText(this, "Please select a pickup location first", Toast.LENGTH_SHORT).show();
            }
        });

        requestLocationPermission();
    }

    // --- Real-time Network Monitoring ---
    private void startNetworkMonitoring() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // ඉන්ටර්නෙට් ආපු ගමන් dialog එක වහනවා
                runOnUiThread(() -> {
                    if (noInternetDialog != null && noInternetDialog.isShowing()) {
                        noInternetDialog.dismiss();
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                // ඉන්ටර්නෙට් ඕනෑම වෙලාවක නැතිවුණොත් Dialog එක පෙන්වනවා
                runOnUiThread(() -> showNoInternetDialog());
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    private void checkInitialConnection() {
        if (loadingDialog != null) loadingDialog.show();

        // තත්පර 5ක ටයිමර් එක
        connectionHandler.postDelayed(() -> {
            if (!isStyleLoaded) {
                if (loadingDialog != null) loadingDialog.dismiss();
                showNoInternetDialog();
            }
        }, 5000);

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            isStyleLoaded = true;
            connectionHandler.removeCallbacksAndMessages(null);
            if (loadingDialog != null) loadingDialog.dismiss();

            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                    .center(Point.fromLngLat(79.8612, 6.9271))
                    .zoom(14.0)
                    .build());

            setupMapListeners();
        });
    }

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layoutView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);
        builder.setView(layoutView);
        builder.setCancelable(false);

        noInternetDialog = builder.create();
        if (noInternetDialog.getWindow() != null) {
            noInternetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        layoutView.findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                noInternetDialog.dismiss();
                if (!isStyleLoaded) checkInitialConnection();
            } else {
                Toast.makeText(this, "Still no connection. Please check again.", Toast.LENGTH_SHORT).show();
            }
        });

        noInternetDialog.show();
    }

    private boolean isNetworkAvailable() {
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // --- Helper UI Methods ---

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = loadingDialog.getWindow().getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.y = 80;
            loadingDialog.getWindow().setAttributes(params);
        }
    }

    private void setupMapListeners() {
        GesturesUtils.getGestures(mapView).addOnMoveListener(new OnMoveListener() {
            @Override
            public void onMoveBegin(@NonNull com.mapbox.android.gestures.MoveGestureDetector detector) {
                imgCenterMarker.animate().translationY(-60f).setDuration(250).start();
            }

            @Override
            public boolean onMove(@NonNull com.mapbox.android.gestures.MoveGestureDetector detector) {
                return false;
            }

            @Override
            public void onMoveEnd(@NonNull com.mapbox.android.gestures.MoveGestureDetector detector) {
                imgCenterMarker.animate().translationY(0f).setDuration(250).start();
                selectedPoint = mapView.getMapboxMap().getCameraState().getCenter();
                updateAddressText(selectedPoint);
            }
        });
    }

    private void updateAddressText(Point point) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        runOnUiThread(() -> tvSelectedLocation.setText("Finding your location..."));
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(point.latitude(), point.longitude(), 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        tvSelectedLocation.setText(addresses.get(0).getAddressLine(0));
                    } else {
                        tvSelectedLocation.setText("Location details not found");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> tvSelectedLocation.setText("Network error, try moving the map"));
            }
        }).start();
    }

    private void focusLocation() {
        if (!isNetworkAvailable()) {
            showNoInternetDialog();
            return;
        }
        if (loadingDialog != null) loadingDialog.show();
        LocationComponentPlugin locationPlugin = LocationComponentUtils.getLocationComponent(mapView);
        locationPlugin.setEnabled(true);
        locationPlugin.updateSettings(settings -> {
            settings.setEnabled(true);
            settings.setPulsingEnabled(true);
            return null;
        });
        locationPlugin.addOnIndicatorPositionChangedListener(new OnIndicatorPositionChangedListener() {
            @Override
            public void onIndicatorPositionChanged(@NonNull Point point) {
                if (loadingDialog != null) loadingDialog.dismiss();
                mapView.getMapboxMap().setCamera(new CameraOptions.Builder().center(point).zoom(15.0).build());
                locationPlugin.removeOnIndicatorPositionChangedListener(this);
            }
        });
    }

    private void requestLocationPermission() {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Memory leak නොවෙන්න callback එක අයින් කරනවා
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        connectionHandler.removeCallbacksAndMessages(null);
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
        if (mapView != null) mapView.onDestroy();
    }
}