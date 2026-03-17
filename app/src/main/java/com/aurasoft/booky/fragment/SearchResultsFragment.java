package com.aurasoft.booky.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.adpter.SearchResultsAdapter;
import com.aurasoft.booky.model.ScheduleModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchResultsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SearchResultsAdapter adapter;
    private List<ScheduleModel> busList;
    private TextView tvCount;

    public SearchResultsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_results, container, false);

        // UI Initialization
        recyclerView = view.findViewById(R.id.rvBusList);
        tvCount = view.findViewById(R.id.tvCount);
        TextView tvFrom = view.findViewById(R.id.fromCity);
        TextView tvTo = view.findViewById(R.id.tocity);
        TextView tvDate = view.findViewById(R.id.dateBus);
        ImageView backBtn = view.findViewById(R.id.btnBack);

        // RecyclerView Setup
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        busList = new ArrayList<>();
        adapter = new SearchResultsAdapter(busList);
        recyclerView.setAdapter(adapter);

        // Get Search Data from Arguments
        if (getArguments() != null) {
            String fromLocation = getArguments().getString("FROM");
            String toLocation = getArguments().getString("TO");
            String busDateStr = getArguments().getString("DATE_STR");

            tvFrom.setText(fromLocation);
            tvTo.setText(toLocation);
            tvDate.setText(busDateStr);

            // බස් දත්ත Load කිරීම
            loadBusData(fromLocation, toLocation, busDateStr);
        }

        // Back Button Action
        backBtn.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Permission check for calling (අවශ්‍ය නම් පමණක්)
        if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, 101);
        }

        return view;
    }

    private void loadBusData(String from, String to, String dateStr) {
        try {
            // 1. String දිනය Date object එකකට හරවනවා
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            Date selectedDate = sdf.parse(dateStr);

            if (selectedDate == null) return;

            // 2. දවසේ ආරම්භය (00:00:00)
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Timestamp start = new Timestamp(calendar.getTime());

            // 3. දවසේ අවසානය (23:59:59)
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Timestamp end = new Timestamp(calendar.getTime());

            // 4. Firestore Query
            FirebaseFirestore.getInstance().collection("Schedules")
                    .whereEqualTo("from", from)
                    .whereEqualTo("to", to)
                    .whereGreaterThanOrEqualTo("departure_time", start)
                    .whereLessThanOrEqualTo("departure_time", end)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        busList.clear();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            ScheduleModel model = doc.toObject(ScheduleModel.class);
                            model.setSchedule_id(doc.getId());
                            busList.add(model);
                        }

                        // UI එක Update කිරීම
                        if (tvCount != null) {
                            tvCount.setText(busList.size() > 0 ? busList.size() + " Available Buses" : "No Buses Available");
                        }
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FIRESTORE_ERROR", e.getMessage());
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}