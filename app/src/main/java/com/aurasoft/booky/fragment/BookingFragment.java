package com.aurasoft.booky.fragment;

import android.app.AlertDialog;
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

    private AutoCompleteTextView fromSpinner, toSpinner;
    private FirebaseFirestore db;
    private List<String> cityList;
    private ArrayAdapter<String> cityAdapter;
    private TextView dateValue;
    private RelativeLayout dateBox;

    private RecyclerView recyclerView;
    private BusAdapter busAdapter;
    private List<ScheduleModel> busList;
    private Button button;

    // Custom Loading Dialog
    private AlertDialog loadingDialog;

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

        // Setup Custom Loading Dialog
        setupLoadingDialog();

        cityAdapter = new ArrayAdapter<>(getContext(), R.layout.list_item_dropdown, cityList);
        fromSpinner.setAdapter(cityAdapter);
        toSpinner.setAdapter(cityAdapter);

        fromSpinner.setOnClickListener(v -> fromSpinner.showDropDown());
        toSpinner.setOnClickListener(v -> toSpinner.showDropDown());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        busAdapter = new BusAdapter(busList);
        recyclerView.setAdapter(busAdapter);

        loadCitiesFromFirestore();
        loadTodayAllBuses();
        setupDatePicker();

        // Search Button Logic
        button.setOnClickListener(v -> {
            String from = fromSpinner.getText().toString();
            String to = toSpinner.getText().toString();
            String selectedDateStr = dateValue.getText().toString();

            if (from.equals("Select") || to.equals("Select")) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireActivity().getApplicationContext(),
                                "Please select locations", Toast.LENGTH_LONG).show();
                    });
                }
                return;
            }

            performSearch(from, to, selectedDateStr);
        });

        // User Greeting Section
        setupUserGreeting(view);

        view.findViewById(R.id.seeAll).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ScheduleFragment())
                    .addToBackStack(null)
                    .commit();
        });

        ImageView bellIcon = view.findViewById(R.id.imgbell);
        bellIcon.setOnClickListener(v -> {
            Fragment notificationFragment = new NotificationFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, notificationFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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

    private void loadCitiesFromFirestore() {
        if (loadingDialog != null) loadingDialog.show();

        db.collection("Locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (loadingDialog != null) loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        cityList.clear();
                        cityList.add("Select");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            cityList.add(document.getId());
                        }
                        cityAdapter.notifyDataSetChanged();

                        fromSpinner.setText(cityList.get(0), false);
                        toSpinner.setText(cityList.get(0), false);
                    }
                });
    }

    private void performSearch(String from, String to, String selectedDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        try {
            Date date = sdf.parse(selectedDateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            Date dayStart = calendar.getTime();
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            Date dayEnd = calendar.getTime();

            if (loadingDialog != null) loadingDialog.show();

            db.collection("Schedules")
                    .whereEqualTo("from", from)
                    .whereEqualTo("to", to)
                    .whereGreaterThanOrEqualTo("departure_time", dayStart)
                    .whereLessThanOrEqualTo("departure_time", dayEnd)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Bundle bundle = new Bundle();
                            bundle.putString("FROM", from);
                            bundle.putString("TO", to);
                            bundle.putString("DATE_STR", selectedDateStr);

                            SearchResultsFragment resultFragment = new SearchResultsFragment();
                            resultFragment.setArguments(bundle);
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, resultFragment)
                                    .addToBackStack(null).commit();
                        } else {
                            Toast.makeText(getContext(), "No buses found for this route", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        Toast.makeText(getContext(), "Search failed. Please try again.", Toast.LENGTH_SHORT).show();
                    });
        } catch (ParseException e) { e.printStackTrace(); }
    }

    private void setupUserGreeting(View view) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        TextView userNameTv = view.findViewById(R.id.userName);

        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            if (name != null && !name.isEmpty()) {
                userNameTv.setText("Hello, " + name.split(" ")[0] + "!");
            } else {
                db.collection("Users").document(currentUser.getUid()).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String dbName = doc.getString("name");
                                if (dbName != null) userNameTv.setText("Hello, " + dbName.split(" ")[0] + "!");
                            }
                        });
            }
        }
    }

    private void loadTodayAllBuses() {
        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        com.google.firebase.Timestamp startTimestamp = new com.google.firebase.Timestamp(calStart.getTime());

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        com.google.firebase.Timestamp endTimestamp = new com.google.firebase.Timestamp(calEnd.getTime());

        db.collection("Schedules")
                .whereGreaterThanOrEqualTo("departure_time", startTimestamp)
                .whereLessThanOrEqualTo("departure_time", endTimestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    busList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ScheduleModel bus = document.toObject(ScheduleModel.class);
                        busList.add(bus);
                    }
                    busAdapter.notifyDataSetChanged();
                });
    }

    private void setupDatePicker() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        dateValue.setText(sdf.format(new Date()));
        dateBox.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build())
                    .build();
            datePicker.show(getChildFragmentManager(), "DATE_PICKER");
            datePicker.addOnPositiveButtonClickListener(selection -> dateValue.setText(sdf.format(new Date(selection))));
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