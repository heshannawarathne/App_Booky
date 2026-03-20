package com.aurasoft.booky;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class BookingSummaryActivity extends AppCompatActivity {

    private TextView tvPickup, tvSeatsList, tvUnitPrice, tvTotalAmount;
    private MaterialButton btnPayNow;

    private String scheduleId, pickupAddress;
    private int totalPrice;
    private ArrayList<String> selectedSeats, selectedGenders;

    private AlertDialog loadingDialog;
    private AlertDialog noInternetDialog;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_summary);

        setupLoadingDialog();

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        startNetworkMonitoring();

        tvPickup = findViewById(R.id.tvSummaryPickup);
        tvSeatsList = findViewById(R.id.tvSummarySeatsList);
        tvUnitPrice = findViewById(R.id.tvUnitPrice);
        tvTotalAmount = findViewById(R.id.tvSummaryTotal);
        btnPayNow = findViewById(R.id.btnConfirmPayment);

        scheduleId = getIntent().getStringExtra("SCHEDULE_ID");
        totalPrice = getIntent().getIntExtra("TOTAL_PRICE", 0);
        selectedSeats = getIntent().getStringArrayListExtra("SELECTED_SEATS");
        selectedGenders = getIntent().getStringArrayListExtra("SELECTED_GENDERS");
        pickupAddress = getIntent().getStringExtra("PICKUP_ADDRESS");

        setupUI();

        btnPayNow.setOnClickListener(v -> {
            if (isVpnConnection(this)) {
                showVpnDialog();
            } else if (!isNetworkAvailable()) {
                showNoInternetDialog();
            } else {
                startPayHerePayment();
            }
        });
    }

    private void startNetworkMonitoring() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (noInternetDialog != null && noInternetDialog.isShowing()) {
                        noInternetDialog.dismiss();
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> showNoInternetDialog());
            }
        };

        connectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                networkCallback
        );
    }

    private boolean isVpnConnection(Context context) {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        }
        return false;
    }

    private boolean isNetworkAvailable() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }

    private void showVpnDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layoutView = getLayoutInflater().inflate(R.layout.dialog_vpn_warning, null);
        builder.setView(layoutView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        layoutView.findViewById(R.id.btnOk).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layoutView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);
        builder.setView(layoutView);
        builder.setCancelable(false);

        noInternetDialog = builder.create();
        if (noInternetDialog.getWindow() != null) noInternetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        layoutView.findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            if (isNetworkAvailable()) noInternetDialog.dismiss();
            else Toast.makeText(this, "Still no connection...", Toast.LENGTH_SHORT).show();
        });
        noInternetDialog.show();
    }

    private void setupUI() {
        if (pickupAddress != null) tvPickup.setText(pickupAddress);

        if (selectedSeats != null && selectedGenders != null && !selectedSeats.isEmpty()) {
            StringBuilder seatsDisplay = new StringBuilder();
            for (int i = 0; i < selectedSeats.size(); i++) {
                String gender = selectedGenders.get(i);
                String genderShort = gender.substring(0, 1).toUpperCase() + gender.substring(1);
                seatsDisplay.append(selectedSeats.get(i)).append(" (").append(genderShort).append(")");
                if (i < selectedSeats.size() - 1) seatsDisplay.append(", ");
            }
            tvSeatsList.setText(seatsDisplay.toString());
            tvUnitPrice.setText("LKR " + (totalPrice / selectedSeats.size()) + ".00 x " + selectedSeats.size());
        }
        tvTotalAmount.setText("LKR " + totalPrice + ".00");
    }

    private void startPayHerePayment() {
        InitRequest req = new InitRequest();
        req.setMerchantId("1226955");
        req.setMerchantSecret("MzQwMjI4MTE2NDc3MzQwMzgzODIwMjczMDA5ODA0MjM0NTQ2MTI5");
        req.setCurrency("LKR");
        req.setAmount(Double.valueOf(totalPrice));
        req.setOrderId("BOOKY_" + System.currentTimeMillis());
        req.setItemsDescription("Bus Booking - " + scheduleId);
        req.setSandBox(true);

        req.getCustomer().setFirstName("Customer");
        req.getCustomer().setLastName("User");
        req.getCustomer().setEmail("test@email.com");
        req.getCustomer().setPhone("+94775796448");
        req.getCustomer().getAddress().setAddress("No 1, Colombo");
        req.getCustomer().getAddress().setCity("Colombo");
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        Intent intent = new Intent(this, PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        payhereLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> payhereLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);
                        if (response != null && response.isSuccess()) {
                            checkUserEmailAndProceed();
                        } else {
                            Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    private void checkUserEmailAndProceed() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        if (loadingDialog != null) loadingDialog.show();

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            saveBookingToFirestore(user.getEmail());
        } else {
            FirebaseFirestore.getInstance().collection("Users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("email")) {
                            saveBookingToFirestore(documentSnapshot.getString("email"));
                        } else {
                            if (loadingDialog != null) loadingDialog.dismiss();
                            showEmailInputDialog();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        showEmailInputDialog();
                    });
        }
    }

    private void showEmailInputDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_email_input, null);
        EditText input = bottomSheetView.findViewById(R.id.etEmailInput);
        MaterialButton btnConfirm = bottomSheetView.findViewById(R.id.btnConfirmEmail);
        TextView tvTitle = bottomSheetView.findViewById(R.id.tvSheetTitle);
        tvTitle.setText("Email address for the E-Ticket");
        input.setHint("example@gmail.com");

        btnConfirm.setOnClickListener(v -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                String userId = FirebaseAuth.getInstance().getUid();
                if (userId != null) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", email);
                    FirebaseFirestore.getInstance().collection("Users").document(userId)
                            .set(userData, SetOptions.merge());
                }
                bottomSheetDialog.dismiss();
                if (loadingDialog != null) loadingDialog.show();
                saveBookingToFirestore(email);
            } else {
                Toast.makeText(this, "Please Enter valid email", Toast.LENGTH_SHORT).show();
            }
        });
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.show();
    }

    private void saveBookingToFirestore(String userEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();

        db.collection("Schedules").document(scheduleId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String busNo = doc.getString("bus_no");
                String dateStr = "N/A";
                String timeStr = "N/A";
                Object departureTimeObject = doc.get("departure_time");

                try {
                    if (departureTimeObject instanceof com.google.firebase.Timestamp) {
                        com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) departureTimeObject;
                        java.util.Date dateObj = ts.toDate();
                        dateStr = android.text.format.DateFormat.format("dd MMM yyyy", dateObj).toString();
                        timeStr = android.text.format.DateFormat.format("hh:mm a", dateObj).toString();
                    }
                } catch (Exception e) { Log.e("DEBUG_BOOKY", "Error parsing date: " + e.getMessage()); }

                final String finalBusNo = (busNo != null) ? busNo : "N/A";
                final String finalDate = dateStr;
                final String finalTime = timeStr;

                Map<String, Object> booking = new HashMap<>();
                booking.put("userId", uid);
                booking.put("email", userEmail);
                booking.put("scheduleId", scheduleId);
                booking.put("busNo", finalBusNo);
                booking.put("date", finalDate);
                booking.put("time", finalTime);
                booking.put("seats", selectedSeats);
                booking.put("genders", selectedGenders);
                booking.put("totalPrice", totalPrice);
                booking.put("pickup", pickupAddress);
                booking.put("status", "Confirmed");
                booking.put("fromLocation", doc.getString("from"));
                booking.put("toLocation", doc.getString("to"));
                booking.put("timestamp", departureTimeObject);
                booking.put("booking_date", FieldValue.serverTimestamp());

                db.collection("Bookings").add(booking).addOnSuccessListener(documentReference -> {
                    WriteBatch batch = db.batch();
                    for (int i = 0; i < selectedSeats.size(); i++) {
                        String seat = selectedSeats.get(i);
                        Map<String, Object> seatData = new HashMap<>();
                        seatData.put("status", "Booked");
                        seatData.put("userId", uid);
                        seatData.put("gender", selectedGenders.get(i));
                        seatData.put("pickupLocation", pickupAddress);
                        batch.set(db.collection("Schedules").document(scheduleId).collection("BookedSeats").document(seat), seatData);
                    }

                    batch.commit().addOnCompleteListener(task -> {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        if (task.isSuccessful()) {
                            sendTicketEmail(userEmail, finalBusNo, finalDate, finalTime);
                            Toast.makeText(this, "Booking Successful! Ticket Sent 🎉", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    });
                });
            }
        });
    }

    private void sendTicketEmail(String recipientEmail, String busNo, String date, String time) {
        String subject = "Booking Confirmed! - Your Booky E-Ticket";
        String htmlBody = "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<div style='width: 100%; max-width: 600px; margin: auto; border: 1px solid #ddd; border-radius: 10px; overflow: hidden;'>" +
                "<div style='background-color: #3A7B73; color: white; padding: 20px; text-align: center;'>" +
                "<h2>Booky E-Ticket</h2>" +
                "<p>Your journey starts here!</p>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<h3>Booking Confirmation</h3>" +
                "<table style='width: 100%; border-collapse: collapse;'>" +
                "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><b>Bus No:</b></td><td style='padding: 8px; border-bottom: 1px solid #eee;'>" + busNo + "</td></tr>" +
                "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><b>Date & Time:</b></td><td style='padding: 8px; border-bottom: 1px solid #eee;'>" + date + " at " + time + "</td></tr>" +
                "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><b>Seats:</b></td><td style='padding: 8px; border-bottom: 1px solid #eee; color: #E91E63;'><b>" + selectedSeats.toString().replace("[", "").replace("]", "") + "</b></td></tr>" +
                "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><b>Pickup Location:</b></td><td style='padding: 8px; border-bottom: 1px solid #eee;'>" + pickupAddress + "</td></tr>" +
                "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><b>Total Paid:</b></td><td style='padding: 8px; border-bottom: 1px solid #eee;'>LKR " + totalPrice + ".00</td></tr>" +
                "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><b>Status:</b></td><td style='padding: 8px; border-bottom: 1px solid #eee;'><span style='background-color: #4CAF50; color: white; padding: 3px 8px; border-radius: 5px;'>CONFIRMED</span></td></tr>" +
                "</table>" +
                "<br><p style='color: #777; font-size: 12px; text-align: center;'>Please show this email to the conductor when boarding the bus.</p>" +
                "</div>" +
                "<div style='background-color: #f9f9f9; padding: 10px; text-align: center; font-size: 12px; color: #999;'>" +
                "© 2026 Booky App. Safe Travels!" +
                "</div>" +
                "</div>" +
                "</body></html>";

        new JavaMailAPI(recipientEmail, subject, htmlBody).execute();
    }

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
        if (loadingDialog.getWindow() != null) {
            Window window = loadingDialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.y = 80;
            window.setAttributes(params);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}