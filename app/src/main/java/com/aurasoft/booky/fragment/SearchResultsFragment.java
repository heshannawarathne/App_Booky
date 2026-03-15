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
    private TextView tvCount; // Global variable එකක් විදිහට ගත්තා crash එක නැති කරන්න

    public SearchResultsFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_results, container, false);

        // UI Elements Initialize කිරීම
        recyclerView = view.findViewById(R.id.rvBusList);
        tvCount = view.findViewById(R.id.tvCount); // onCreateView එකේදීම initialize කළා
        TextView tvFrom = view.findViewById(R.id.fromCity);
        TextView tvTo = view.findViewById(R.id.tocity);
        TextView tvDate = view.findViewById(R.id.dateBus);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        busList = new ArrayList<>();
        adapter = new SearchResultsAdapter(busList);
        recyclerView.setAdapter(adapter);

        // Bundle එකෙන් දත්ත කියවීම සහ Header එක Set කිරීම
        if (getArguments() != null) {
            String fromLocation = getArguments().getString("FROM");
            String toLocation = getArguments().getString("TO");
            String busDate = getArguments().getString("DATE_STR");

            tvFrom.setText(fromLocation);
            tvTo.setText(toLocation);
            tvDate.setText(busDate);

            // Data load කරන්න මෙතඩ් එකට දත්ත යවනවා
            loadBusData(fromLocation, toLocation);
        }

        ImageView backBtn = view.findViewById(R.id.btnBack);

        // 2. Click Listener එකක් දාන්න
        backBtn.setOnClickListener(v -> {
            // මේකෙන් වෙන්නේ කලින් හිටපු Fragment එකට (BookingFragment එකට) ආපහු යන එක
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // පර්මිෂන් එක දැනටමත් දීලා නැත්නම් විතරක් ඉල්ලන්න
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

                    // බස් ලිස්ට් එක ලෝඩ් කිරීම
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ScheduleModel model = doc.toObject(ScheduleModel.class);
                        // Document ID එක අනිවාර්යයෙන්ම ඕනේ වෙනවා සීට් බුක් කරන්න
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