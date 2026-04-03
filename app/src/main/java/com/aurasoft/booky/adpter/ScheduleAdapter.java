package com.aurasoft.booky.adpter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.model.ScheduleModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<ScheduleModel> scheduleList;
    private Context context;
    private OnItemClickListener listener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    public void updateList(List<ScheduleModel> newList) {
        this.scheduleList = newList;
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
        String scheduleId = model.getSchedule_id();

        holder.busNumber.setText("no - " + model.getBus_no());
        holder.busRoute.setText(model.getFrom() + " - " + model.getTo());

        if (model.getDeparture_time() != null) {
            long departureMillis = model.getDeparture_time().toDate().getTime();
            long currentTime = System.currentTimeMillis();

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.busTime.setText(timeFormat.format(model.getDeparture_time().toDate()));
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.busDate.setText(dateFormat.format(model.getDeparture_time().toDate()));

            if (currentTime > departureMillis) {
                holder.statusText.setText("IN PROGRESS");
                holder.statusText.setTextColor(Color.parseColor("#FF9800"));
            } else {
                holder.statusText.setText("UPCOMING");
                holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.ap_title));
            }
        } else {
            holder.busTime.setText("N/A");
            holder.busDate.setText("N/A");
        }

        db.collection("Schedules")
                .document(scheduleId)
                .collection("BookedSeats")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int bookedCount = task.getResult().size();
                        int totalSeats = 49;
                        int availableSeats = totalSeats - bookedCount;

                        if (bookedCount >= totalSeats) {
                            holder.statusText.setText("SOLD OUT");
                            holder.statusText.setTextColor(Color.RED);
                            holder.itemView.setAlpha(0.6f);
                            holder.itemView.setOnClickListener(null);
                        } else {
                            holder.statusText.setText(String.format(Locale.getDefault(), "%02d Seats Available", availableSeats));

                            long departureMillis = (model.getDeparture_time() != null) ? model.getDeparture_time().toDate().getTime() : 0;
                            if (System.currentTimeMillis() > departureMillis && departureMillis != 0) {
                                holder.statusText.setTextColor(Color.parseColor("#FF9800"));
                            } else {
                                holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.ap_title));
                            }

                            holder.itemView.setAlpha(1.0f);
                            holder.itemView.setOnClickListener(v -> {
                                if (listener != null) {
                                    listener.onItemClick(model);
                                }
                            });
                        }
                        holder.statusText.setVisibility(View.VISIBLE);
                    }
                });

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
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView busNumber, busRoute, busTime, busDate, statusText;
        View callBtn;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            busNumber = itemView.findViewById(R.id.busNumber);
            busRoute = itemView.findViewById(R.id.busRoute);
            busTime = itemView.findViewById(R.id.busTime);
            busDate = itemView.findViewById(R.id.busDate);
            statusText = itemView.findViewById(R.id.statusText);
            callBtn = itemView.findViewById(R.id.callBtnBg);
        }
    }
}