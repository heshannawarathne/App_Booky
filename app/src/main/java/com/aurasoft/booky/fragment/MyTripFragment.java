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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.adpter.TripAdapter;
import com.aurasoft.booky.model.TripModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MyTripFragment extends Fragment {

    private RecyclerView scheduleRecyclerView;
    private ImageView btnBack;
    private TripAdapter adapter;
    private List<TripModel> tripList;
    private FirebaseFirestore db;

    // Custom Loading Dialog
    private AlertDialog loadingDialog;

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

        // 1. Initialize UI Elements
        btnBack = view.findViewById(R.id.btnBack);
        scheduleRecyclerView = view.findViewById(R.id.scheduleRecyclerView);
        db = FirebaseFirestore.getInstance();

        // 2. Setup Custom Loading Dialog
        setupLoadingDialog();



        // 4. Setup RecyclerView
        setupRecyclerView();
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

    private void setupRecyclerView() {
        tripList = new ArrayList<>();
        adapter = new TripAdapter(tripList);

        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        scheduleRecyclerView.setHasFixedSize(true);
        scheduleRecyclerView.setAdapter(adapter);

        loadTripsFromFirestore();
    }

    private void loadTripsFromFirestore() {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId == null) {
            Toast.makeText(getContext(), "Please login again to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        if (loadingDialog != null) loadingDialog.show();

        db.collection("Bookings")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING) // අලුත්ම බුකින් එක උඩට එනවා
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Dismiss loading
                    if (loadingDialog != null) loadingDialog.dismiss();

                    tripList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            TripModel trip = document.toObject(TripModel.class);
                            if (trip != null) {
                                tripList.add(trip);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "No trips found in your history.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Dismiss loading
                    if (loadingDialog != null) loadingDialog.dismiss();
                    Log.e("FIRESTORE_ERROR", e.getMessage());
                    Toast.makeText(getContext(), "Failed to load travel data!", Toast.LENGTH_SHORT).show();
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