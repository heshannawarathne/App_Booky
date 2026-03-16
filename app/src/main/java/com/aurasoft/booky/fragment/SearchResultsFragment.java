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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        busList = new ArrayList<>();
        adapter = new SearchResultsAdapter(busList);
        recyclerView.setAdapter(adapter);

        if (getArguments() != null) {
            String fromLocation = getArguments().getString("FROM");
            String toLocation = getArguments().getString("TO");
            String busDate = getArguments().getString("DATE_STR");

            tvFrom.setText(fromLocation);
            tvTo.setText(toLocation);
            tvDate.setText(busDate);

            loadBusData(fromLocation, toLocation);
        }

        ImageView backBtn = view.findViewById(R.id.btnBack);

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

    private void loadBusData(String from, String to) {
        FirebaseFirestore.getInstance().collection("Schedules")
                .whereEqualTo("from", from)
                .whereEqualTo("to", to)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    busList.clear();

                    int count = queryDocumentSnapshots.size();

                    // Available Buses count එක set කිරීම
                    if (tvCount != null) {
                        if (count > 0) {
                            tvCount.setText(count + " Available Buses");
                        } else {
                            tvCount.setText("No Buses Available");
                        }
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ScheduleModel model = doc.toObject(ScheduleModel.class);
                        model.setSchedule_id(doc.getId());
                        busList.add(model);
                    }

                    adapter.notifyDataSetChanged();

                    if (busList.isEmpty()) {
                        Toast.makeText(getContext(), "No buses found for this route", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_ERROR", e.getMessage());
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}