package com.aurasoft.booky;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
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
// මෙන්න මේ wildcard import එක නිසා දැන් ඔක්කොම classes වැඩ
import com.mapbox.search.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView tvSelectedLocation;
    private ImageView imgCenterMarker;
    private Point selectedPoint;

    // පරණ Activity එකෙන් එන දත්ත
    private String scheduleId;
    private int totalPrice;
    private ArrayList<String> selectedSeats;
    private ArrayList<String> selectedGenders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Mapbox Token එක සෙට් කිරීම
        com.mapbox.common.MapboxOptions.setAccessToken(getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_map);

        // UI Initialize
        mapView = findViewById(R.id.mapView);
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        imgCenterMarker = findViewById(R.id.img_center_marker);
        FloatingActionButton fabCurrentLocation = findViewById(R.id.fabCurrentLocation);
        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);
        MaterialCardView searchCard = findViewById(R.id.searchCard);
        MaterialCardView btnBack = findViewById(R.id.btnBack);

        // Intent Data ලබා ගැනීම
        scheduleId = getIntent().getStringExtra("SCHEDULE_ID");
        totalPrice = getIntent().getIntExtra("TOTAL_PRICE", 0);
        selectedSeats = getIntent().getStringArrayListExtra("SELECTED_SEATS");
        selectedGenders = getIntent().getStringArrayListExtra("SELECTED_GENDERS");

        // 2. Map එක Load කිරීම
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            Log.i("Map", "Success load map");

            // Default Camera (ලංකාවට)
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                    .center(Point.fromLngLat(79.8612, 6.9271))
                    .zoom(14.0)
                    .build());

            setupMapListeners();
        });

        // 3. Search Logic
//        searchCard.setOnClickListener(v -> {
//            try {
//                // Search UI එකට අදාළ Builder එක
//                SearchPlaceActivity.IntentBuilder intentBuilder = new SearchPlaceActivity.IntentBuilder();
//                startActivityForResult(intentBuilder.build(this), 101);
//            } catch (Exception e) {
//                Log.e("SearchError", e.getMessage());
//            }
//        });

        // 4. Back Button Logic
        btnBack.setOnClickListener(v -> onBackPressed());

        // 5. Current Location Button
        fabCurrentLocation.setOnClickListener(v -> focusLocation());

        // 6. Confirm & Go to Summary
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
                Toast.makeText(this, "Please wait until location is selected", Toast.LENGTH_SHORT).show();
            }
        });

        requestLocationPermission();
    }

    private void setupMapListeners() {
        GesturesUtils.getGestures(mapView).addOnMoveListener(new OnMoveListener() {
            @Override
            public void onMoveBegin(@NonNull com.mapbox.android.gestures.MoveGestureDetector detector) {
                // Pin එක උඩට යනවා (Jump)
                imgCenterMarker.animate().translationY(-60f).setDuration(250).start();
            }

            @Override
            public boolean onMove(@NonNull com.mapbox.android.gestures.MoveGestureDetector detector) {
                return false;
            }

            @Override
            public void onMoveEnd(@NonNull com.mapbox.android.gestures.MoveGestureDetector detector) {


                selectedPoint = mapView.getMapboxMap().getCameraState().getCenter();
                updateAddressText(selectedPoint);
            }
        });
    }

    private void updateAddressText(Point point) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(point.latitude(), point.longitude(), 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        String addressStr = addresses.get(0).getAddressLine(0);
                        tvSelectedLocation.setText(addressStr);
                    } else {
                        tvSelectedLocation.setText("Location not found");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> tvSelectedLocation.setText("Finding address..."));
            }
        }).start();
    }

//    // Search කරලා ආපහු එනකොට වැඩ කරන කෑල්ල
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == 101 && resultCode == RESULT_OK) {
//            // මෙතන typo එක හරිගැස්සුවා
//            SearchPlaceActivity.Result result = SearchPlaceActivity.getResult(data);
//            if (result != null) {
//                Point searchPoint = result.getCoordinate();
//                mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
//                        .center(searchPoint)
//                        .zoom(16.0)
//                        .build());
//
//                tvSelectedLocation.setText(result.getName());
//                selectedPoint = searchPoint;
//            }
//        }
//    }

    private void focusLocation() {
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
                mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build());
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
    protected void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override
    protected void onStop() { super.onStop(); if (mapView != null) mapView.onStop(); }
    @Override
    protected void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDestroy(); }
}