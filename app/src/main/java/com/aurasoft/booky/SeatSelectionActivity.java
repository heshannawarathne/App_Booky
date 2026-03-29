package com.aurasoft.booky;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.adpter.SeatAdapter;
import com.aurasoft.booky.model.SeatModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class SeatSelectionActivity extends AppCompatActivity implements SeatAdapter.OnSeatClickListener {

    private RecyclerView recyclerView;
    private SeatAdapter adapter;
    private List<SeatModel> seatList;
    private Button btnProceed;
    private String currentScheduleId;
    private FirebaseFirestore db;
    private int ticketPrice;

    private TextView tvTotalAmount;
    private int totalAmount = 0;
    private ListenerRegistration seatListener;

    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        makeFullScreen();
        setContentView(R.layout.activity_seat_selection);

        db = FirebaseFirestore.getInstance();
        setupLoadingDialog();

        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnProceed = findViewById(R.id.btnProceed);
        recyclerView = findViewById(R.id.recyclerViewSeats);

        currentScheduleId = getIntent().getStringExtra("SCHEDULE_ID");
        ticketPrice = getIntent().getIntExtra("TICKET_PRICE", 0);

        setupSeatLayout();

        if (currentScheduleId != null) {
            loadBookedSeats(currentScheduleId);
        }

        btnProceed.setOnClickListener(v -> {
            if (totalAmount > 0) {
                if (isVpnConnection(this)) {
                    showVpnDialog();
                } else {
                    goToMapActivity();
                }
            } else {
                Toast.makeText(this, "Please select at least one seat!", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView backBtn = findViewById(R.id.imageView);
        backBtn.setOnClickListener(v -> finish());
    }

    private boolean isVpnConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            }
        }
        return false;
    }

    private void showVpnDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layoutView = getLayoutInflater().inflate(R.layout.dialog_vpn_warning, null);
        builder.setView(layoutView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        layoutView.findViewById(R.id.btnOk).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
            Toast.makeText(this, "Booking cancelled due to VPN usage.", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void goToMapActivity() {
        ArrayList<String> selectedSeats = new ArrayList<>();
        ArrayList<String> selectedGenders = new ArrayList<>();

        for (SeatModel seat : seatList) {
            if (seat.getStatus() == 1) {
                selectedSeats.add(seat.getSeatName());
                selectedGenders.add(seat.getSelectedGender());
            }
        }


        db.collection("Schedules").document(currentScheduleId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        double fromLat = documentSnapshot.getDouble("from_lat") != null ? documentSnapshot.getDouble("from_lat") : 0.0;
                        double fromLng = documentSnapshot.getDouble("from_lng") != null ? documentSnapshot.getDouble("from_lng") : 0.0;
                        double toLat = documentSnapshot.getDouble("to_lat") != null ? documentSnapshot.getDouble("to_lat") : 0.0;
                        double toLng = documentSnapshot.getDouble("to_lng") != null ? documentSnapshot.getDouble("to_lng") : 0.0;
                        String fromCity = documentSnapshot.getString("from");
                        String toCity = documentSnapshot.getString("to");

                        Intent intent = new Intent(SeatSelectionActivity.this, MapActivity.class);

                        intent.putExtra("SCHEDULE_ID", currentScheduleId);
                        intent.putExtra("TOTAL_PRICE", totalAmount);
                        intent.putStringArrayListExtra("SELECTED_SEATS", selectedSeats);
                        intent.putStringArrayListExtra("SELECTED_GENDERS", selectedGenders);

                        intent.putExtra("FROM_LAT", fromLat);
                        intent.putExtra("FROM_LNG", fromLng);
                        intent.putExtra("TO_LAT", toLat);
                        intent.putExtra("TO_LNG", toLng);
                        intent.putExtra("FROM_CITY", fromCity);
                        intent.putExtra("TO_CITY", toCity);

                        startActivity(intent);

                    } else {
                        Toast.makeText(this, "Schedule coordinates not found in Firestore!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching map data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);
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

    private void updateTotalPrice() {
        if (tvTotalAmount == null) return;
        int selectedCount = 0;
        for (SeatModel seat : seatList) {
            if (seat.getStatus() == 1) {
                selectedCount++;
            }
        }
        totalAmount = selectedCount * ticketPrice;
        tvTotalAmount.setText("Total: LKR " + totalAmount + ".00");
    }

    private void makeFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    private void setupSeatLayout() {
        seatList = new ArrayList<>();
        int seatCounter = 1;
        for (int i = 1; i <= 60; i++) {
            if (i % 5 == 3 && i < 58) {
                seatList.add(new SeatModel("", 4)); // Aisle
            } else {
                if (seatCounter <= 49) {
                    seatList.add(new SeatModel(String.valueOf(seatCounter), 0));
                    seatCounter++;
                }
            }
        }
        recyclerView.setLayoutManager(new GridLayoutManager(this, 5));
        adapter = new SeatAdapter(seatList, this, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onSeatClick(int position) {
        SeatModel clickedSeat = seatList.get(position);
        if (clickedSeat.getStatus() == 0) {
            showGenderSelectionDialog(position);
        } else if (clickedSeat.getStatus() == 1) {
            clickedSeat.setStatus(0);
            clickedSeat.setSelectedGender("");
            adapter.notifyItemChanged(position);
            updateTotalPrice();
        } else if (clickedSeat.getStatus() == 2 || clickedSeat.getStatus() == 3) {
            Toast.makeText(this, "Seat already booked!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showGenderSelectionDialog(int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_gender_selection, null);
        bottomSheetDialog.setContentView(sheetView);

        LinearLayout btnMale = sheetView.findViewById(R.id.btnMale);
        LinearLayout btnFemale = sheetView.findViewById(R.id.btnFemale);

        btnMale.setOnClickListener(v -> {
            applyGenderSelection(position, "male");
            bottomSheetDialog.dismiss();
        });

        btnFemale.setOnClickListener(v -> {
            applyGenderSelection(position, "female");
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void applyGenderSelection(int position, String gender) {
        SeatModel seat = seatList.get(position);
        seat.setStatus(1);
        seat.setSelectedGender(gender);
        adapter.notifyItemChanged(position);
        updateTotalPrice();
    }

    private void loadBookedSeats(String scheduleId) {
        if (loadingDialog != null) loadingDialog.show();
        seatListener = db.collection("Schedules").document(scheduleId).collection("BookedSeats")
                .addSnapshotListener((value, error) -> {
                    if (loadingDialog != null) loadingDialog.dismiss();
                    if (error != null) return;
                    if (value != null) {
                        for (SeatModel seat : seatList) {
                            if (seat.getStatus() == 2 || seat.getStatus() == 3) seat.setStatus(0);
                        }
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String seatNo = doc.getId();
                            String gender = doc.getString("gender");
                            for (SeatModel seat : seatList) {
                                if (seat.getSeatName().trim().equals(seatNo.trim())) {
                                    seat.setStatus("female".equalsIgnoreCase(gender) ? 3 : 2);
                                    break;
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (seatListener != null) seatListener.remove();
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }
}