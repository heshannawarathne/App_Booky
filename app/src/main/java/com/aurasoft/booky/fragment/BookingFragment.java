package com.aurasoft.booky.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.adpter.BusAdapter;
import com.aurasoft.booky.model.ScheduleModel;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingFragment extends Fragment {

    private Spinner fromSpinner, toSpinner;
    private FirebaseFirestore db;
    private List<String> cityList;
    private ArrayAdapter<String> cityAdapter; // Spinner adapter
    private TextView dateValue;
    private RelativeLayout dateBox;

    // RecyclerView variables
    private RecyclerView recyclerView;
    private com.aurasoft.booky.adpter.BusAdapter busAdapter;
    private List<ScheduleModel> busList;


    private Button button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking, container, false);


        fromSpinner = view.findViewById(R.id.fromSpinner);
        toSpinner = view.findViewById(R.id.toSpinner);
        dateBox = view.findViewById(R.id.dateBox);
        dateValue = view.findViewById(R.id.dateValue);
        recyclerView = view.findViewById(R.id.busRecyclerView);
        button = view.findViewById(R.id.btnSearch);

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

        button.setOnClickListener(v -> {
            String from = fromSpinner.getSelectedItem().toString();
            String to = toSpinner.getSelectedItem().toString();
            String selectedDateStr = dateValue.getText().toString();
            Log.d("DEBUG_DATE", "Data: '" + selectedDateStr + "'");

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

            try {
                Date date = sdf.parse(selectedDateStr);
                if (date == null) return;

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                Date dayStart = calendar.getTime();

                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                Date dayEnd = calendar.getTime();

                FirebaseFirestore.getInstance().collection("Schedules")
                        .whereEqualTo("from", from)
                        .whereEqualTo("to", to)
                        .whereGreaterThanOrEqualTo("departure_time", dayStart)
                        .whereLessThanOrEqualTo("departure_time", dayEnd)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                Bundle bundle = new Bundle();
                                bundle.putString("FROM", from);
                                bundle.putString("TO", to);
                                bundle.putString("DATE_STR", selectedDateStr);

                                SearchResultsFragment resultFragment = new SearchResultsFragment();
                                resultFragment.setArguments(bundle);

                                getParentFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, resultFragment)
                                        .addToBackStack(null)
                                        .commit();
                            } else {
                                Toast.makeText(getContext(), "bus is Not found", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FIRESTORE_ERROR", e.getMessage());
                            Toast.makeText(getContext(), "Error: Check Logcat for Index link", Toast.LENGTH_LONG).show();
                        });

            } catch (ParseException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "wrong Dtae", Toast.LENGTH_SHORT).show();
            }
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        TextView userNameTv = view.findViewById(R.id.userName);

        if (currentUser != null) {
            String googleName = currentUser.getDisplayName();

            if (googleName != null && !googleName.isEmpty()) {
                String firstName = googleName.split(" ")[0];
                userNameTv.setText("Hello, " + firstName + "!");
            } else {
                String uid = currentUser.getUid();

                db.collection("Users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nameFromDb = documentSnapshot.getString("name");
                        if (nameFromDb != null && !nameFromDb.isEmpty()) {
                            String firstName = nameFromDb.split(" ")[0];
                            userNameTv.setText("Hello, " + firstName + "!");
                        }
                    } else {
                        userNameTv.setText("Hello, User!");
                    }
                }).addOnFailureListener(e -> {
                    userNameTv.setText("Hello, User!");
                });
            }
        }

        TextView seeAllBtn = view.findViewById(R.id.seeAll);

        seeAllBtn.setOnClickListener(v -> {
            ScheduleFragment scheduleFragment = new ScheduleFragment();

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, scheduleFragment)
                    .addToBackStack(null)
                    .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        });

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
        SimpleDateFormat initialSdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        dateValue.setText(initialSdf.format(new Date()));

        dateBox.setOnClickListener(v -> {
            CalendarConstraints constraints = new CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.now())
                    .build();

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setCalendarConstraints(constraints)
                    .build();

            datePicker.show(getChildFragmentManager(), "DATE_PICKER");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                dateValue.setText(sdf.format(new Date(selection)));
            });
        });
    }
}