package com.aurasoft.booky;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.view.LayoutInflater;
import android.view.View;

public class BookingSummaryActivity extends AppCompatActivity {

    private TextView tvPickup, tvSeatsList, tvUnitPrice, tvTotalAmount;
    private MaterialButton btnPayNow;

    private String scheduleId, pickupAddress;
    private int totalPrice;
    private ArrayList<String> selectedSeats, selectedGenders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_summary);

        // UI Initialize
        tvPickup = findViewById(R.id.tvSummaryPickup);
        tvSeatsList = findViewById(R.id.tvSummarySeatsList);
        tvUnitPrice = findViewById(R.id.tvUnitPrice);
        tvTotalAmount = findViewById(R.id.tvSummaryTotal);
        btnPayNow = findViewById(R.id.btnConfirmPayment);

        // Data from Intent (Flow එකේ එන ටික)
        scheduleId = getIntent().getStringExtra("SCHEDULE_ID");
        totalPrice = getIntent().getIntExtra("TOTAL_PRICE", 0);
        selectedSeats = getIntent().getStringArrayListExtra("SELECTED_SEATS");
        selectedGenders = getIntent().getStringArrayListExtra("SELECTED_GENDERS");
        pickupAddress = getIntent().getStringExtra("PICKUP_ADDRESS");

        setupUI();

        btnPayNow.setOnClickListener(v -> startPayHerePayment());
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

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            saveBookingToFirestore(user.getEmail());
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore.getInstance().collection("Users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("email")) {
                        saveBookingToFirestore(documentSnapshot.getString("email"));
                    } else {
                        showEmailInputDialog();
                    }
                })
                .addOnFailureListener(e -> showEmailInputDialog());
    }

    private void showEmailInputDialog() {
        // 1. BottomSheetDialog එකක් ක්‍රියාත්මක කරමු
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // 2. ඒකට අදාළ UI එක dynamic විදිහට හදාගමු
        // (මේකට වෙනම XML එකක් හදන්නත් පුළුවන්, නැත්නම් කලින් වගේම code එකෙන් හදමු)
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_email_input, null);

        // --- Layout එක ඇතුලේ තියෙන components (XML එකේ තියෙන විදිහට) ---
        EditText input = bottomSheetView.findViewById(R.id.etEmailInput);
        MaterialButton btnConfirm = bottomSheetView.findViewById(R.id.btnConfirmEmail);
        TextView tvTitle = bottomSheetView.findViewById(R.id.tvSheetTitle);

        tvTitle.setText("E-Ticket එක සඳහා Email ලිපිනය");
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

                bottomSheetDialog.dismiss(); // Dialog එක වහනවා
                saveBookingToFirestore(email);

            } else {
                Toast.makeText(this, "කරුණාකර නිවැරදි Email එකක් ඇතුළත් කරන්න", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.setCancelable(false); // පිටතින් click කළොත් වැහෙන්නේ නැති වෙන්න
        bottomSheetDialog.show();
    }

    private void saveBookingToFirestore(String userEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();

        // ✅ Firestore එකෙන් Schedule විස්තර ඇදලා ගන්නා කොටස
        db.collection("Schedules").document(scheduleId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // Firestore එකේ තියෙන field names මේවාම නේද බලන්න (busNo, date, time)
                String busNo = doc.getString("bus_no");

                String dateStr = "N/A";
                String timeStr = "N/A";

                try {
                    // 1. මුලින්ම Timestamp එකක්ද කියලා බලනවා
                    Object timeObject = doc.get("departure_time");

                    if (timeObject instanceof com.google.firebase.Timestamp) {
                        com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) timeObject;
                        java.util.Date dateObj = ts.toDate();
                        dateStr = android.text.format.DateFormat.format("dd MMM yyyy", dateObj).toString();
                        timeStr = android.text.format.DateFormat.format("hh:mm a", dateObj).toString();
                    }
                    // 2. Timestamp එකක් නෙවෙයි නම් String එකක්ද කියලා බලනවා
                    else if (timeObject instanceof String) {
                        String fullDateTime = (String) timeObject;
                        // String එකේ තියෙන්නේ "17 March 2026 at..." වගේ නම් ඒකම පෙන්වනවා
                        dateStr = fullDateTime;
                        timeStr = "";
                    }
                } catch (Exception e) {
                    Log.e("DEBUG_BOOKY", "Error parsing date: " + e.getMessage());
                }

                final String finalBusNo = (busNo != null) ? busNo : "N/A";
                final String finalDate = dateStr;
                final String finalTime = timeStr;


                Map<String, Object> booking = new HashMap<>();
                booking.put("userId", uid);
                booking.put("email", userEmail);
                booking.put("scheduleId", scheduleId);
                booking.put("busNo", busNo);
                booking.put("date", finalDate);
                booking.put("time", finalTime);
                booking.put("seats", selectedSeats);
                booking.put("genders", selectedGenders);
                booking.put("totalPrice", totalPrice);
                booking.put("pickup", pickupAddress);
                booking.put("status", "Confirmed");
                booking.put("timestamp", FieldValue.serverTimestamp());

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
                        if (task.isSuccessful()) {
                            // ✅ ලබාගත්තු විස්තර සමඟ Email එක යවනවා
                            sendTicketEmail(userEmail, busNo, finalDate,finalTime);
                            Toast.makeText(this, "Booking Successful! Ticket Sent 🎉", Toast.LENGTH_LONG).show();
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
}