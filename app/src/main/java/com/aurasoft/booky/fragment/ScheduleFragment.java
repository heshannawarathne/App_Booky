package com.aurasoft.booky.fragment;

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

        // 1. RecyclerView එක හොයාගන්න (ඔයාගේ XML එකේ ID එක recyclerView කියලා හිතමු)
        recyclerView = view.findViewById(R.id.scheduleRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. List එක සහ Adapter එක Initialize කරන්න
        scheduleList = new ArrayList<>();
        adapter = new ScheduleAdapter(scheduleList, getContext());
        recyclerView.setAdapter(adapter);

        // 3. Firestore instance එක ගන්න
        db = FirebaseFirestore.getInstance();

        // 4. දත්ත load කරන්න function එක call කරන්න
        loadSchedules();

        ImageView backBtn = view.findViewById(R.id.btnBack);

        // 2. Click Listener එකක් දාන්න
        backBtn.setOnClickListener(v -> {
            BookingFragment bookingFragment = new BookingFragment();

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, bookingFragment) // මෙතන R.id.fragment_container කියන්නේ ඔයා Fragment පෙන්වන XML එකේ තියෙන FrameLayout එකේ ID එක
                    .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE) // පොඩි animation එකක්
                    .commit();
        });
        adapter.setOnItemClickListener(model -> {
            // ඊළඟ Fragment එකට (SeatSelectionFragment) යන කෝඩ් එක මෙතනට එනවා
            // උදාහරණයක් විදිහට:
    /*
    Bundle bundle = new Bundle();
    bundle.putString("scheduleId", model.getSchedule_id());
    SeatSelectionFragment seatFragment = new SeatSelectionFragment();
    seatFragment.setArguments(bundle);

    getParentFragmentManager().beginTransaction()
        .replace(R.id.fragment_container, seatFragment)
        .addToBackStack(null)
        .commit();
    */
            Toast.makeText(getContext(), "Selected Bus: " + model.getBus_no(), Toast.LENGTH_SHORT).show();
        });
        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permission Granted! දැන් නැවත කෝල් බොත්තම ඔබන්න.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadSchedules() {
        db.collection("Schedules")
                .orderBy("departure_time", Query.Direction.ASCENDING) // වෙලාව අනුව පිළිවෙළට ගන්න
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