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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.adpter.SeatAdapter;
import com.aurasoft.booky.fragment.BookingFragment;
import com.aurasoft.booky.model.SeatModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // 1. Views Initialize කිරීම (මේක උඩටම ගන්න)
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnProceed = findViewById(R.id.btnProceed);
        recyclerView = findViewById(R.id.recyclerViewSeats);

        // 2. Intent Data ලබාගැනීම
        currentScheduleId = getIntent().getStringExtra("SCHEDULE_ID");

        ticketPrice = getIntent().getIntExtra("TICKET_PRICE", 0);
        Log.d("DEBUG_PRICE", "Ticket Price is: " + ticketPrice); // මේක දාලා Logcat එකේ බලන්න

        // 3. Layout එක සෙට් කිරීම
        setupSeatLayout();

        // 4. Firestore එකෙන් දැනට බුක් කරපු දත්ත ලෝඩ් කිරීම
        if (currentScheduleId != null) {
            loadBookedSeats(currentScheduleId);
        }
        btnProceed.setOnClickListener(v -> {
            if (totalAmount > 0) {
                // මීළඟට යන Activity එකේ නම මෙතන දෙන්න (උදා: MapActivity)
                Intent intent = new Intent(SeatSelectionActivity.this, MapActivity.class);

                // මූලික දත්ත ටික යවනවා
                intent.putExtra("SCHEDULE_ID", currentScheduleId);
                intent.putExtra("TOTAL_PRICE", totalAmount);

                // තෝරගත්ත සීට් සහ ඒවායේ ජෙන්ඩර් එක List විදිහට එකතු කරගමු
                ArrayList<String> selectedSeats = new ArrayList<>();
                ArrayList<String> selectedGenders = new ArrayList<>();

                for (SeatModel seat : seatList) {
                    if (seat.getStatus() == 1) { // Selected seats
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

        backBtn.setOnClickListener(v -> {

            finish();
        });
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

        // String.valueOf පාවිච්චි කිරීමෙන් අනිවාර්යයෙන්ම Text එක වැටෙනවා
        tvTotalAmount.setText("Total: LKR " + String.valueOf(totalAmount));

        // Debug කරලා බලන්න ඇත්තටම මෙතනට එනවද කියලා
        Log.d("DEBUG_UI", "Price Updated: " + totalAmount);
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

        if (clickedSeat.getStatus() == 0) { // Available නම් Dialog එක පෙන්වන්න
            showGenderSelectionDialog(position);
        } else if (clickedSeat.getStatus() == 1) { // Unselect කරනවා නම්
            clickedSeat.setStatus(0);
            clickedSeat.setSelectedGender("");
            adapter.notifyItemChanged(position);
            updateTotalPrice(); // status එක මාරු කළාට පස්සේ මිල update කරන්න
        } else if (clickedSeat.getStatus() == 2 || clickedSeat.getStatus() == 3) {
            Toast.makeText(this, "Already Booked!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showGenderSelectionDialog(int position) {
        String[] genders = {"Male", "Female"};
        new AlertDialog.Builder(this)
                .setTitle("Select Gender")
                .setItems(genders, (dialog, which) -> {
                    SeatModel seat = seatList.get(position);
                    seat.setStatus(1); // Selected
                    seat.setSelectedGender(which == 0 ? "male" : "female");
                    adapter.notifyItemChanged(position);
                    updateTotalPrice(); // මිල update කරන්න
                }).show();
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