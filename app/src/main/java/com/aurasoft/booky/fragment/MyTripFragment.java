package com.aurasoft.booky.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.adpter.*; // Adapter එක තියෙන තැන බලන්න
import com.aurasoft.booky.model.TripModel;    // Model එක තියෙන තැන බලන්න
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyTripFragment extends Fragment {

    private RecyclerView scheduleRecyclerView;
    private ImageView btnBack;

    // Firestore සහ Adapter සඳහා Variables
    private TripAdapter adapter;
    private List<TripModel> tripList;
    private FirebaseFirestore db;

    public MyTripFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_trip, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. UI Elements හඳුනා ගැනීම
        btnBack = view.findViewById(R.id.btnBack);
        scheduleRecyclerView = view.findViewById(R.id.scheduleRecyclerView);

        // 2. Firestore Initialize කිරීම
        db = FirebaseFirestore.getInstance();

        // 3. Back Button එකට Click Listener එක දීම
        btnBack.setOnClickListener(v -> {
            BookingFragment bookingFragment = new BookingFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, bookingFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        });

        // 4. RecyclerView සහ Firestore Data Setup කිරීම
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        tripList = new ArrayList<>();
        adapter = new TripAdapter(tripList);

        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        scheduleRecyclerView.setAdapter(adapter);

        // Firestore එකෙන් Data Load කිරීම ආරම්භ කරන්න
        loadTripsFromFirestore();
    }

    private void loadTripsFromFirestore() {
        // Firestore එකේ "Bookings" කියන collection එකටයි මම මෙතන access කරන්නේ
        db.collection("Bookings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        tripList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            TripModel trip = document.toObject(TripModel.class);
                            tripList.add(trip);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        // Data නැතිනම් පෙන්වන්න ඕන දේ මෙතනට දාන්න (Toast එකක් වගේ)
                        Toast.makeText(getContext(), "No bookings found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}