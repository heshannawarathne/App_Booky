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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_results, container, false);

        recyclerView = view.findViewById(R.id.rvBusList);
        tvCount = view.findViewById(R.id.tvCount);
        TextView tvFrom = view.findViewById(R.id.fromCity);
        TextView tvTo = view.findViewById(R.id.tocity);
        TextView tvDate = view.findViewById(R.id.dateBus);
        ImageView backBtn = view.findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        busList = new ArrayList<>();
        adapter = new SearchResultsAdapter(busList);
        recyclerView.setAdapter(adapter);

        if (getArguments() != null) {
            String fromLocation = getArguments().getString("FROM");
            String toLocation = getArguments().getString("TO");
            String busDateStr = getArguments().getString("DATE_STR");

            tvFrom.setText(fromLocation);
            tvTo.setText(toLocation);
            tvDate.setText(busDateStr);

            loadBusData(fromLocation, toLocation, busDateStr);
        }

        backBtn.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, 101);
        }

        return view;
    }

    private void loadBusData(String from, String to, String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            Date selectedDate = sdf.parse(dateStr);
            if (selectedDate == null) return;

            Calendar calendar = Calendar.getInstance();
            Calendar nowCalendar = Calendar.getInstance();

            calendar.setTime(selectedDate);


            if (calendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR)) {
                calendar.setTime(new Date());
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
            }
            Timestamp start = new Timestamp(calendar.getTime());


            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(selectedDate);
            endCalendar.set(Calendar.HOUR_OF_DAY, 23);
            endCalendar.set(Calendar.MINUTE, 59);
            endCalendar.set(Calendar.SECOND, 59);
            Timestamp end = new Timestamp(endCalendar.getTime());

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
                            if (model != null) {
                                model.setSchedule_id(doc.getId());
                                busList.add(model);
                            }
                        }

                        if (tvCount != null) {
                            tvCount.setText(busList.size() > 0 ? busList.size() + " Available Buses" : "No Buses Available");
                        }
                        adapter.updateList(busList);
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