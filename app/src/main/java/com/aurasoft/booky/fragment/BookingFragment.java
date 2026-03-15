package com.aurasoft.booky.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.adpter.BusAdapter;
import com.aurasoft.booky.model.ScheduleModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingFragment extends Fragment {

    private Spinner fromSpinner, toSpinner;
    private FirebaseFirestore db;
    private List<String> cityList;
    private ArrayAdapter<String> cityAdapter; // Spinner adapter එක
    private TextView dateValue;
    private RelativeLayout dateBox;

    // RecyclerView සඳහා variables
    private RecyclerView recyclerView;
    private com.aurasoft.booky.adpter.BusAdapter busAdapter;
    private List<ScheduleModel> busList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking, container, false);


        fromSpinner = view.findViewById(R.id.fromSpinner);
        toSpinner = view.findViewById(R.id.toSpinner);
        dateBox = view.findViewById(R.id.dateBox);
        dateValue = view.findViewById(R.id.dateValue);
        recyclerView = view.findViewById(R.id.busRecyclerView);

        db = FirebaseFirestore.getInstance();
        cityList = new ArrayList<>();
        busList = new ArrayList<>();

        cityList.add("Select");
        cityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, cityList);
        fromSpinner.setAdapter(cityAdapter);
        toSpinner.setAdapter(cityAdapter);


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        busAdapter = new BusAdapter(busList);
        recyclerView.setAdapter(busAdapter);

        loadCitiesFromFirestore();
        loadTodayAllBuses();
        setupDatePicker();

        return view;
    }

    private void loadCitiesFromFirestore() {
        db.collection("Locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cityList.clear();
                        cityList.add("Select");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            cityList.add(document.getId());
                        }
                        cityAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Error loading cities", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTodayAllBuses() {


        java.util.Calendar calStart = java.util.Calendar.getInstance();
        calStart.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calStart.set(java.util.Calendar.MINUTE, 0);
        calStart.set(java.util.Calendar.SECOND, 0);
        com.google.firebase.Timestamp startTimestamp = new com.google.firebase.Timestamp(calStart.getTime());


        java.util.Calendar calEnd = java.util.Calendar.getInstance();
        calEnd.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calEnd.set(java.util.Calendar.MINUTE, 59);
        calEnd.set(java.util.Calendar.SECOND, 59);
        com.google.firebase.Timestamp endTimestamp = new com.google.firebase.Timestamp(calEnd.getTime());


        db.collection("Schedules")
                .whereGreaterThanOrEqualTo("departure_time", startTimestamp)
                .whereLessThanOrEqualTo("departure_time", endTimestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    busList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            ScheduleModel bus = document.toObject(ScheduleModel.class);
                            busList.add(bus);
                        }
                        busAdapter.notifyDataSetChanged();
                    } else {

                        Toast.makeText(getContext(), "not found buses", Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(getContext(), "Buses found: " + busList.size(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDatePicker() {
        dateBox.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.show(getChildFragmentManager(), "DATE_PICKER");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                dateValue.setText(sdf.format(new Date(selection)));

              
            });
        });
    }
}