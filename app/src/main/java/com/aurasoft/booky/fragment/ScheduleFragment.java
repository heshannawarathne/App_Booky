package com.aurasoft.booky.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.SeatSelectionActivity;
import com.aurasoft.booky.adpter.ScheduleAdapter;
import com.aurasoft.booky.model.ScheduleModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ScheduleFragment extends Fragment {

    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private List<ScheduleModel> scheduleList;
    private FirebaseFirestore db;
    private AlertDialog loadingDialog;
    private long lastClickTime = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.scheduleRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        setupLoadingDialog();

        scheduleList = new ArrayList<>();
        adapter = new ScheduleAdapter(scheduleList, getContext());
        recyclerView.setAdapter(adapter);

        loadSchedules();

        adapter.setOnItemClickListener(model -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 1000) {
                return;
            }
            lastClickTime = currentTime;

            Intent intent = new Intent(getContext(), SeatSelectionActivity.class);
            intent.putExtra("SCHEDULE_ID", model.getSchedule_id());

            int priceValue = 0;
            try {
                priceValue = Integer.parseInt(String.valueOf(model.getPrice()));
            } catch (Exception e) {
                priceValue = 0;
            }
            intent.putExtra("TICKET_PRICE", priceValue);
            startActivity(intent);
        });
    }

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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

    private void loadSchedules() {
        if (loadingDialog != null) loadingDialog.show();

        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.HOUR_OF_DAY, -2);

        Timestamp thresholdTime = new Timestamp(cal.getTime());

        db.collection("Schedules")
                .whereGreaterThan("departure_time", thresholdTime)
                .orderBy("departure_time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (loadingDialog != null) loadingDialog.dismiss();

                    scheduleList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ScheduleModel model = doc.toObject(ScheduleModel.class);
                        if (model != null) {
                            model.setSchedule_id(doc.getId());
                            scheduleList.add(model);
                        }
                    }

                    adapter.updateList(scheduleList);

                    if (scheduleList.isEmpty()) {
                        Toast.makeText(getContext(), "No schedules found within the last 2 hours.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadingDialog != null) loadingDialog.dismiss();
                    Log.e("FIRESTORE_ERROR", e.getMessage());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}