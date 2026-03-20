package com.aurasoft.booky.adpter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aurasoft.booky.R;
import com.aurasoft.booky.model.TripModel;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<TripModel> tripList;

    public TripAdapter(List<TripModel> tripList) {
        this.tripList = tripList;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_trip, parent, false);
        return new TripViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        TripModel trip = tripList.get(position);

        // 1. බස් එකේ අංකය
        holder.tvBusName.setText(trip.getBusNo() != null ? trip.getBusNo() : "N/A");

        // 2. Route එක (From ➝ To)
        String from = (trip.getFromLocation() != null) ? trip.getFromLocation() : "N/A";
        String to = (trip.getToLocation() != null) ? trip.getToLocation() : "N/A";
        holder.tvRoute.setText(from + " ➝ " + to);

        // 3. දිනය සහ වෙලාව
        holder.tvDate.setText(trip.getDate());
        holder.tvTime.setText(trip.getTime());

        // 4. සීට් ගණන සහ සීට් අංක (Seats List එක පාවිච්චි කරමින්)
        if (trip.getSeats() != null && !trip.getSeats().isEmpty()) {
            int count = trip.getSeats().size();

            // ලිස්ට් එක [A1, A2] වගේ තියෙන එක "A1, A2" විදිහට සකස් කරනවා
            String seatNumbers = trip.getSeats().toString()
                    .replace("[", "")
                    .replace("]", "");

            // අවසාන ප්‍රතිඵලය: "2 Seats (A1, A2)"
            holder.tvSeatCount.setText(count + " Seats (" + seatNumbers + ")");
        } else {
            holder.tvSeatCount.setText("0 Seats");
        }

        // 5. Status එක (කලින් වගේමයි)
        if (trip.getTimestamp() != null) {
            long currentTime = System.currentTimeMillis();
            long departureTime = trip.getTimestamp().toDate().getTime();

            if (departureTime < currentTime) {
                holder.tvStatus.setText("COMPLETED");
                holder.tvStatus.setTextColor(R.color.black);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_compleate);
                holder.itemView.setAlpha(0.6f);
            } else {
                holder.tvStatus.setText("UPCOMING");
                holder.tvStatus.setTextColor(R.color.black);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_upcoming);
                holder.itemView.setAlpha(1.0f);
            }
        }
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvBusName, tvRoute, tvDate, tvTime, tvSeatCount, tvStatus;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusName = itemView.findViewById(R.id.tvBusName);
            tvRoute = itemView.findViewById(R.id.tvRouteDisplay);
            tvDate = itemView.findViewById(R.id.tvTripDate);
            tvTime = itemView.findViewById(R.id.tvTripTime);
            tvSeatCount = itemView.findViewById(R.id.tvSeatCount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}