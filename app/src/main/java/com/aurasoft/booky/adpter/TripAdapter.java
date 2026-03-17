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
        // my_trip.xml එක සම්බන්ධ කිරීම
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        TripModel trip = tripList.get(position);

        // 1. Bus Name පෙන්වීම
        holder.tvBusName.setText(trip.getBusName());

        // 2. දිනය පෙන්වීම
        holder.tvDate.setText(trip.getDate());

        // 3. From සහ To එකතු කර Route එක පෙන්වීම (Badulla → Colombo වගේ)
        String fullRoute = trip.getFrom() + " → " + trip.getTo();
        holder.tvRoute.setText(fullRoute);

        // 4. මිල පෙන්වීම
        holder.tvPrice.setText("LKR " + trip.getPrice());

        // 5. Status එක (Upcoming/Completed) පෙන්වීම
        holder.tvStatus.setText(trip.getStatus());

        // Status එක අනුව පාට වෙනස් කරනවා නම් මෙතනින් කරන්න පුළුවන්
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        // IDs ඔයාගේ my_trip.xml එකේ තියෙන ඒවාටම ගැලපෙන්න ඕනේ
        TextView tvBusName, tvDate, tvRoute, tvPrice, tvStatus;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvTripDate);
            tvRoute = itemView.findViewById(R.id.tvBusName); // XML එකේ route එකට තියෙන ID එක
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
