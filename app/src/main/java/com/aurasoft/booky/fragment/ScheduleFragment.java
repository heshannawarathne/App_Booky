package com.aurasoft.booky.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
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
import java.util.List;

public class ScheduleFragment extends Fragment {

    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private List<ScheduleModel> scheduleList;
    private FirebaseFirestore db;
    private AlertDialog loadingDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI Initialization
        recyclerView = view.findViewById(R.id.scheduleRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        setupLoadingDialog();

        scheduleList = new ArrayList<>();
        // මෙතනදී filter වුණු ලිස්ට් එකක් ඇඩැප්ටර් එකට යනවා
        adapter = new ScheduleAdapter(scheduleList, getContext());
        recyclerView.setAdapter(adapter);

        // Load Data
        loadSchedules();


        // Click listener for seat selection
        adapter.setOnItemClickListener(model -> {
            Intent intent = new Intent(getContext(), SeatSelectionActivity.class);
            intent.putExtra("SCHEDULE_ID", model.getSchedule_id());

            int priceValue = 0;
            try {
                // Number එකක් String කරලා ආයේ Int කරනවාට වඩා direct casting එක හොඳයි
                priceValue = Integer.parseInt(String.valueOf(model.getPrice()));
            } catch (Exception e) {
                priceValue = 0;
            }
            intent.putExtra("TICKET_PRICE", priceValue);
            startActivity(intent);
        });
//        ImageView backBtn = view.findViewById(R.id.btnBack);
//        backBtn.setOnClickListener(v -> {
//            if (getParentFragmentManager() != null) {
//                // මේකෙන් වෙන්නේ stack එකේ කලින් හිටපු fragment එකට පස්සට යන එක
//                getParentFragmentManager().popBackStack();
//            }
//        });

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

        // දැනට තියෙන වෙලාව ගමු
        Timestamp now = Timestamp.now();

        // --- මෙතන තමයි වැදගත්ම වෙනස ---
        // අපි Query එකෙන්ම කියනවා Departure Time එක දැනට වඩා වැඩි ඒව විතරක් දෙන්න කියලා.
        db.collection("Schedules")
                .whereGreaterThan("departure_time", now)
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

                    // ඇඩැප්ටර් එකට අලුත් ඩේටා ටික දැනුම් දෙනවා
                    adapter.updateList(scheduleList);

                    if (scheduleList.isEmpty()) {
                        Toast.makeText(getContext(), "No upcoming schedules found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadingDialog != null) loadingDialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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