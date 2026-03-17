package com.aurasoft.booky;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        makeFullScreen();
        setContentView(R.layout.activity_seat_selection);

        db = FirebaseFirestore.getInstance();

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
                Intent intent = new Intent(SeatSelectionActivity.this, MapActivity.class);
                intent.putExtra("SCHEDULE_ID", currentScheduleId);
                intent.putExtra("TOTAL_PRICE", totalAmount);

                ArrayList<String> selectedSeats = new ArrayList<>();
                ArrayList<String> selectedGenders = new ArrayList<>();

                for (SeatModel seat : seatList) {
                    if (seat.getStatus() == 1) {
                        selectedSeats.add(seat.getSeatName());
                        selectedGenders.add(seat.getSelectedGender());
                    }
                }

                intent.putStringArrayListExtra("SELECTED_SEATS", selectedSeats);
                intent.putStringArrayListExtra("SELECTED_GENDERS", selectedGenders);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please select at least one seat!", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView backBtn = findViewById(R.id.imageView);
        backBtn.setOnClickListener(v -> finish());
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
        tvTotalAmount.setText("Total: LKR " + String.valueOf(totalAmount));
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
            Toast.makeText(this, "Already Booked!", Toast.LENGTH_SHORT).show();
        }
    }

    // මෙතන තමයි අලුත් BottomSheetDialog එක තියෙන්නේ
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
        seat.setStatus(1); // Selected
        seat.setSelectedGender(gender);
        adapter.notifyItemChanged(position);
        updateTotalPrice();
    }

    private void loadBookedSeats(String scheduleId) {
        db.collection("Schedules").document(scheduleId).collection("BookedSeats")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String seatNo = doc.getId();
                        String gender = doc.getString("gender");

                        for (SeatModel seat : seatList) {
                            if (seat.getSeatName().equals(seatNo)) {
                                if ("male".equals(gender)) seat.setStatus(2);
                                else if ("female".equals(gender)) seat.setStatus(3);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error: " + e.getMessage()));
    }
}