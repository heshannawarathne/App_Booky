package com.aurasoft.booky.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.SeatSelectionActivity;
import com.aurasoft.booky.adpter.ScheduleAdapter;
import com.aurasoft.booky.model.ScheduleModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ScheduleFragment extends Fragment {

    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private List<ScheduleModel> scheduleList;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        // UI Initialization
        recyclerView = view.findViewById(R.id.scheduleRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        scheduleList = new ArrayList<>();
        adapter = new ScheduleAdapter(scheduleList, getContext());
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadSchedules();

        ImageView backBtn = view.findViewById(R.id.btnBack);

        // Back button action
        backBtn.setOnClickListener(v -> {
            BookingFragment bookingFragment = new BookingFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, bookingFragment)
                    .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        });

        // Click listener for seat selection
        adapter.setOnItemClickListener(model -> {
            Intent intent = new Intent(getContext(), SeatSelectionActivity.class);
            intent.putExtra("SCHEDULE_ID", model.getSchedule_id());

            int priceValue = 0;
            try {
                // Price එක string එකක් නම් integer එකකට හරවනවා
                priceValue = Integer.parseInt(String.valueOf(model.getPrice()));
            } catch (Exception e) {
                priceValue = 0;
            }
            intent.putExtra("TICKET_PRICE", priceValue);

            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permission Granted! Click Again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadSchedules() {
        db.collection("Schedules")
                .orderBy("departure_time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    scheduleList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ScheduleModel model = doc.toObject(ScheduleModel.class);
                        if (model != null) {
                            model.setSchedule_id(doc.getId());
                            scheduleList.add(model);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}