package com.aurasoft.booky.adpter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.model.ScheduleModel;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<ScheduleModel> scheduleList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ScheduleModel model);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ScheduleAdapter(List<ScheduleModel> scheduleList, Context context) {
        // මෙතනදී අපි පරණ schedules අයින් කරලා අලුත් ලිස්ට් එකක් හදාගන්නවා
        this.scheduleList = filterUpcomingSchedules(scheduleList);
        this.context = context;
    }

    // --- පරණ Schedules අයින් කරන Logic එක ---
    private List<ScheduleModel> filterUpcomingSchedules(List<ScheduleModel> fullList) {
        List<ScheduleModel> filteredList = new ArrayList<>();
        Timestamp currentTime = Timestamp.now(); // දැනට තියෙන වෙලාව

        for (ScheduleModel model : fullList) {
            if (model.getDeparture_time() != null) {
                // වෙලාව දැනට වඩා වැඩි නම් විතරක් ලිස්ට් එකට එකතු කරනවා
                if (model.getDeparture_time().compareTo(currentTime) > 0) {
                    filteredList.add(model);
                }
            }
        }
        return filteredList;
    }

    // දත්ත අලුතින් අප්ඩේට් කරනවා නම් මේ මෙතඩ් එක පාවිච්චි කරන්න පුළුවන්
    public void updateList(List<ScheduleModel> newList) {
        this.scheduleList = filterUpcomingSchedules(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_item, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        ScheduleModel model = scheduleList.get(position);

        holder.busNumber.setText("no - " + model.getBus_no());
        holder.busRoute.setText(model.getFrom() + " - " + model.getTo());

        if (model.getDeparture_time() != null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.busTime.setText(timeFormat.format(model.getDeparture_time().toDate()));

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.busDate.setText(dateFormat.format(model.getDeparture_time().toDate()));
        } else {
            holder.busTime.setText("N/A");
            holder.busDate.setText("N/A");
        }

        holder.callBtn.setOnClickListener(v -> {
            String phoneNumber = model.getPhone_number();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    context.startActivity(intent);
                } else {
                    if (context instanceof FragmentActivity) {
                        ActivityCompat.requestPermissions((FragmentActivity) context,
                                new String[]{Manifest.permission.CALL_PHONE}, 101);
                    }
                    Toast.makeText(context, "Please grant permission to make calls", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(model);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView busNumber, busRoute, busTime, busDate;
        View callBtn;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            busNumber = itemView.findViewById(R.id.busNumber);
            busRoute = itemView.findViewById(R.id.busRoute);
            busTime = itemView.findViewById(R.id.busTime);
            busDate = itemView.findViewById(R.id.busDate);
            callBtn = itemView.findViewById(R.id.callBtnBg);
        }
    }
}