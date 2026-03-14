package com.aurasoft.booky.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.aurasoft.booky.R;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingFragment extends Fragment {

    private Spinner fromSpinner, toSpinner;
    private DatabaseReference mDatabase;
    private List<String> cityList;
    private ArrayAdapter<String> adapter;
    private TextView dateValue;
    private RelativeLayout dateBox;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking, container, false);

        fromSpinner = view.findViewById(R.id.fromSpinner);
        toSpinner = view.findViewById(R.id.toSpinner);
        dateBox = view.findViewById(R.id.dateBox);
        dateValue = view.findViewById(R.id.dateValue);

        // 1. Firebase Reference එක ලබා ගැනීම
        mDatabase = FirebaseDatabase.getInstance("https://bookyapp-bef0e-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("cities");
        cityList = new ArrayList<>();

        // 2. Spinner එක සඳහා Adapter එකක් සැකසීම (මුලින්ම නිකන් ලැයිස්තුවක් දෙනවා)
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, cityList);
        fromSpinner.setAdapter(adapter);
        toSpinner.setAdapter(adapter);

        // 3. Firebase වලින් නගර ලැයිස්තුව කියවීම
        loadCitiesFromFirebase();

        setupDatePicker();

        return view;
    }

    private void loadCitiesFromFirebase() {


        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cityList.clear(); // පරණ data අයින් කරනවා
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String cityName = dataSnapshot.getValue(String.class);
                    cityList.add(cityName);
                }
                // Data වෙනස් වුණා කියලා Spinner එකට දන්වනවා
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // මෙතනදී Error එකක් ආවොත් Log එකක් දාන්න පුළුවන්
            }
        });
    }

    private void setupDatePicker() {


        dateBox.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .build();

            datePicker.show(getChildFragmentManager(), "DATE_PICKER");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                dateValue.setText(sdf.format(new Date(selection)));
            });
        });
    }
}