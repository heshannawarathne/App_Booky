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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<ScheduleModel> scheduleList;
    private Context context;
    private OnItemClickListener listener;

    // Interface for item click (to go to Seat Selection)
    public interface OnItemClickListener {
        void onItemClick(ScheduleModel model);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ScheduleAdapter(List<ScheduleModel> scheduleList, Context context) {
        this.scheduleList = scheduleList;
        this.context = context;
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

        // Formatting Date and Time
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

                // පර්මිෂන් තියෙනවද කියලා චෙක් කරනවා
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    // කෙලින්ම call එක යනවා
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    context.startActivity(intent);
                } else {
                    // පර්මිෂන් නැත්නම් පර්මිෂන් ඉල්ලනවා
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

        // Full Item Click (Go to Seat Selection)
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