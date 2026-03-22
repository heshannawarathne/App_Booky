package com.aurasoft.booky.adpter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.SeatSelectionActivity;
import com.aurasoft.booky.model.ScheduleModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private List<ScheduleModel> busList;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public SearchResultsAdapter(List<ScheduleModel> busList) {
        this.busList = busList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_bus_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleModel bus = busList.get(position);
        String scheduleId = bus.getSchedule_id();

        // --- පරණ Basic Data (Price, Time) ---
        holder.tvPrice.setText("Rs. " + bus.getPrice());

        if (bus.getDeparture_time() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(bus.getDeparture_time().toDate()));
        }

        // --- Firebase Seat Status Check Logic ---
        // BookedSeats sub-collection එකේ ලේඛන ගණන ගන්නවා
        db.collection("Schedules")
                .document(scheduleId)
                .collection("BookedSeats")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int bookedCount = task.getResult().size();
                        int totalSeats = 49; // ඔයා සඳහන් කළ මුළු සීට් ගණන

                        if (bookedCount >= totalSeats) {
                            // බස් එකේ සියලුම සීට් බුක් වී ඇත්නම්
                            holder.sheatBookStatus.setText("Fully Booked");
                            holder.sheatBookStatus.setTextColor(Color.RED);

                            // Button එක Disable කර පෙනුම වෙනස් කිරීම
                            holder.btnBookNow.setEnabled(false);
                            holder.btnBookNow.setText("FULL");

                            // මුළු Item එකම click වෙන එක නතර කිරීම
                            holder.itemView.setOnClickListener(null);
                        } else {
                            // සීට් ඉතිරිව ඇත්නම්
                            int available = totalSeats - bookedCount;
                            holder.sheatBookStatus.setText("Available: " + available + " Seats");
                            holder.sheatBookStatus.setTextColor(
                                    androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.white_02)
                            );
                            holder.btnBookNow.setEnabled(true);
                            holder.btnBookNow.setText("Book Now");
                            holder.btnBookNow.setAlpha(1.0f);

                            // සාමාන්‍ය Click Listeners
                            holder.itemView.setOnClickListener(v -> {
                                Toast.makeText(v.getContext(), "Opening seat selection for " + bus.getSchedule_id(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });

        // --- Call Button Logic ---
        holder.btnCall.setOnClickListener(v -> {
            String number = bus.getPhone_number();
            if (v.getContext().checkSelfPermission(android.Manifest.permission.CALL_PHONE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + number));
                v.getContext().startActivity(callIntent);
            } else {
                Toast.makeText(v.getContext(), "Please Enable Call Permission", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Book Now Button Logic ---
        holder.btnBookNow.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, SeatSelectionActivity.class);

            int priceValue = 0;
            try {
                priceValue = Integer.parseInt(String.valueOf(bus.getPrice()));
            } catch (Exception e) {
                priceValue = 0;
            }

            intent.putExtra("SCHEDULE_ID", bus.getSchedule_id());
            intent.putExtra("TICKET_PRICE", priceValue);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return busList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPrice, tvTime, sheatBookStatus;
        ImageButton btnCall;
        AppCompatButton btnBookNow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvTime = itemView.findViewById(R.id.tvTime);
            sheatBookStatus = itemView.findViewById(R.id.sheatBookStatus);
            btnCall = itemView.findViewById(R.id.btncall);
            btnBookNow = itemView.findViewById(R.id.btnBookNow);
        }
    }
}