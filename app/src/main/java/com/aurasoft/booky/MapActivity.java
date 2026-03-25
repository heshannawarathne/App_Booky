package com.aurasoft.booky;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private TextView tvSelectedLocation;
    private ImageView imgCenterMarker;
    private LatLng selectedLatLng;

    private String scheduleId;
    private double fromLat, fromLng, toLat, toLng;
    private int totalPrice;
    private ArrayList<String> selectedSeats, selectedGenders;

    private AlertDialog loadingDialog, noInternetDialog;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isStyleLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_map);

        setupLoadingDialog();
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        imgCenterMarker = findViewById(R.id.img_center_marker);
        FloatingActionButton fabCurrentLocation = findViewById(R.id.fabCurrentLocation);
        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);
        MaterialCardView btnBack = findViewById(R.id.btnBack);

        // Intent Data
        scheduleId = getIntent().getStringExtra("SCHEDULE_ID");
        totalPrice = getIntent().getIntExtra("TOTAL_PRICE", 0);
        selectedSeats = getIntent().getStringArrayListExtra("SELECTED_SEATS");
        selectedGenders = getIntent().getStringArrayListExtra("SELECTED_GENDERS");
        fromLat = getIntent().getDoubleExtra("FROM_LAT", 6.9271);
        fromLng = getIntent().getDoubleExtra("FROM_LNG", 79.8612);
        toLat = getIntent().getDoubleExtra("TO_LAT", 7.2906);
        toLng = getIntent().getDoubleExtra("TO_LNG", 80.6337);

        // --- Network Monitoring ---
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        startNetworkMonitoring();

        mapView.getMapAsync(mapboxMap -> {
            this.mapboxMap = mapboxMap;
            mapboxMap.getUiSettings().setZoomGesturesEnabled(true);
            mapboxMap.setMinZoomPreference(10);
            mapboxMap.setMaxZoomPreference(18);

            mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
                isStyleLoaded = true;
                initRouteAndMarkers(style);
                setupCameraListener();
            });
        });

        btnBack.setOnClickListener(v -> onBackPressed());
        fabCurrentLocation.setOnClickListener(v -> focusLocation());

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                showConfirmDialog(); // Custom Popup එක පෙන්වනවා
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Custom Confirmation Dialog ---
    private void showConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_location, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvAddress = dialogView.findViewById(R.id.tvConfirmAddress);
        tvAddress.setText(tvSelectedLocation.getText().toString());

        dialogView.findViewById(R.id.btnYes).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, BookingSummaryActivity.class);
            intent.putExtra("SCHEDULE_ID", scheduleId);
            intent.putExtra("TOTAL_PRICE", totalPrice);
            intent.putStringArrayListExtra("SELECTED_SEATS", selectedSeats);
            intent.putStringArrayListExtra("SELECTED_GENDERS", selectedGenders);
            intent.putExtra("PICKUP_ADDRESS", tvSelectedLocation.getText().toString());
            intent.putExtra("PICKUP_LAT", selectedLatLng.getLatitude());
            intent.putExtra("PICKUP_LNG", selectedLatLng.getLongitude());
            startActivity(intent);
        });

        dialogView.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- Real-time Network Monitoring (v11 logic) ---
    private void startNetworkMonitoring() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> { if (noInternetDialog != null) noInternetDialog.dismiss(); });
            }
            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> showNoInternetDialog());
            }
        };
        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), networkCallback);
    }

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) return;
        View layout = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);
        noInternetDialog = new AlertDialog.Builder(this).setView(layout).setCancelable(false).create();
        noInternetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        layout.findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            if (isNetworkAvailable()) noInternetDialog.dismiss();
        });
        noInternetDialog.show();
    }

    private boolean isNetworkAvailable() {
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void drawRoute(Style style, DirectionsRoute route) {
        LineString lineString = LineString.fromPolyline(route.geometry(), PRECISION_6);
        style.addSource(new GeoJsonSource("route-source", lineString));
        style.addLayer(new LineLayer("route-layer", "route-source")
                .withProperties(lineColor(Color.BLACK), lineWidth(4f)));

        // 1. Route එකේ Bounds (මුලින්ම Zoom වෙන්න)
        LatLngBounds routeBounds = new LatLngBounds.Builder()
                .include(new LatLng(fromLat, fromLng))
                .include(new LatLng(toLat, toLng))
                .build();

        // මැප් එක ලෝඩ් වෙද්දී මුළු පාරම පේන්න Zoom වෙනවා
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(routeBounds, 150));

        // 2. ශ්‍රී ලංකාවට අදාළ දළ සීමාවන් (Sri Lanka Bounds)
        // මේ Coordinates වලින් මුළු ලංකාවම ආවරණය වෙනවා
        LatLngBounds sriLankaBounds = new LatLngBounds.Builder()
                .include(new LatLng(5.9, 79.5))  // දකුණ/බටහිර කෙළවර
                .include(new LatLng(9.9, 81.9))  // උතුර/නැගෙනහිර කෙළවර
                .build();

        // 3. මැප් එක ශ්‍රී ලංකාවට විතරක් Lock කරනවා
        // දැන් User ට ලංකාව ඇතුළේ ඕනෑම විදිහකට Zoom Out/Move කරන්න පුළුවන්
        mapboxMap.setLatLngBoundsForCameraTarget(sriLankaBounds);

        // 4. Zoom සීමාවන් (පොඩි Zoom එකකදී මුළු ලංකාවම පේනවා)
        mapboxMap.setMinZoomPreference(6.5); // ලංකාවට වඩා ඈතට Zoom out වීම වැළැක්වීමට
    }

    private void initRouteAndMarkers(Style style) {
        Point origin = Point.fromLngLat(fromLng, fromLat);
        Point destination = Point.fromLngLat(toLng, toLat);
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.dot);
        style.addImage("marker-icon", b);
        style.addSource(new GeoJsonSource("marker-source", FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(origin), Feature.fromGeometry(destination)})));
        style.addLayer(new SymbolLayer("marker-layer", "marker-source").withProperties(iconImage("marker-icon"), iconAllowOverlap(true)));
        getRoute(style, origin, destination);
    }

    private void getRoute(Style style, Point origin, Point destination) {
        MapboxDirections.builder().origin(origin).destination(destination).accessToken(getString(R.string.mapbox_access_token)).profile(DirectionsCriteria.PROFILE_DRIVING).build()
                .enqueueCall(new Callback<DirectionsResponse>() {
                    @Override public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() != null && !response.body().routes().isEmpty()) drawRoute(style, response.body().routes().get(0));
                    }
                    @Override public void onFailure(Call<DirectionsResponse> call, Throwable t) {}
                });
    }

    private void setupCameraListener() {
        mapboxMap.addOnCameraMoveStartedListener(reason -> imgCenterMarker.animate().translationY(-50f).start());
        mapboxMap.addOnCameraIdleListener(() -> {
            imgCenterMarker.animate().translationY(0f).start();
            selectedLatLng = mapboxMap.getCameraPosition().target;
            updateAddressText(selectedLatLng);
        });
    }

    private void updateAddressText(LatLng latLng) {
        new Thread(() -> {
            try {
                List<Address> addresses = new Geocoder(this, Locale.getDefault()).getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 1);
                if (!addresses.isEmpty()) runOnUiThread(() -> tvSelectedLocation.setText(addresses.get(0).getAddressLine(0)));
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void focusLocation() {
        if (mapboxMap == null || mapboxMap.getStyle() == null) return;
        LocationComponent lc = mapboxMap.getLocationComponent();
        lc.activateLocationComponent(LocationComponentActivationOptions.builder(this, mapboxMap.getStyle()).build());
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            lc.setLocationComponentEnabled(true);
            lc.setCameraMode(CameraMode.TRACKING);
            lc.setRenderMode(RenderMode.COMPASS);
        } else { requestLocationPermission(); }
    }

    private void requestLocationPermission() {
        androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    private void setupLoadingDialog() {
        loadingDialog = new AlertDialog.Builder(this).setView(getLayoutInflater().inflate(R.layout.dialog_loading, null)).create();
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) connectivityManager.unregisterNetworkCallback(networkCallback);
        mapView.onDestroy();
    }
}