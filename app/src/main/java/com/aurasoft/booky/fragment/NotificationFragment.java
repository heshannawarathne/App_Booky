package com.aurasoft.booky.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.adpter.NotificationAdapter;
import com.aurasoft.booky.model.NotificationModel;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        recyclerView = view.findViewById(R.id.rvNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // නිකන් පෙන්වන්න Dummy Data ටිකක් දාමු
        notificationList = new ArrayList<>();
        notificationList.add(new NotificationModel("Booking Confirmed", "Your seat B12 is reserved for Colombo-Kandy.", "Just now"));
        notificationList.add(new NotificationModel("Special Offer", "Get 10% off on your next trip! Use code BUS10", "2 hours ago"));
        notificationList.add(new NotificationModel("Bus Update", "Bus ND-4562 is arriving at Maharagama in 5 mins.", "Yesterday"));

        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        // Clear All Button එකට
        TextView btnClearAll = view.findViewById(R.id.btnClearAll);
        btnClearAll.setOnClickListener(v -> {
            notificationList.clear();
            adapter.notifyDataSetChanged();
            // මෙතනදී පස්සේ DB එකත් clear කරන code එක දානවා
        });

        return view;
    }
}