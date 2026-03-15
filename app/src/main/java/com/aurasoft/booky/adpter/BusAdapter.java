package com.aurasoft.booky.adpter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.model.ScheduleModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BusAdapter extends RecyclerView.Adapter<BusAdapter.BusViewHolder> {
    private List<ScheduleModel> busList;

    public BusAdapter(List<ScheduleModel> busList) {
        this.busList = busList;
    }

    @NonNull
    @Override
    public BusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.bus_item, parent, false);
        return new BusViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BusViewHolder holder, int position) {
        ScheduleModel bus = busList.get(position);

        holder.busRoute.setText(bus.getFrom() + " - " + bus.getTo());

        if (bus.getDeparture_time() != null) {
            Date date = bus.getDeparture_time().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.busTime.setText(sdf.format(date));
        } else {
            holder.busTime.setText("N/A");
        }
    }

    @Override
    public int getItemCount() {
        return busList.size();
    }

    public static class BusViewHolder extends RecyclerView.ViewHolder {
        TextView busRoute, busTime;

        public BusViewHolder(View itemView) {
            super(itemView);
            busRoute = itemView.findViewById(R.id.busRoute);
            busTime = itemView.findViewById(R.id.busTime);
        }
    }
}