package com.aurasoft.booky.adpter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.model.ScheduleModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private List<ScheduleModel> busList;

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

        // මිල සහ වේලාව පෙන්වීම
        holder.tvPrice.setText("Rs. " + bus.getPrice());

        if (bus.getDeparture_time() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(bus.getDeparture_time().toDate()));
        }

        holder.btnCall.setOnClickListener(v -> {
            String number = bus.getPhone_number();

            // පර්මිෂන් තියෙනවා නම් විතරක් ACTION_CALL පාවිච්චි කරන්න
            if (v.getContext().checkSelfPermission(android.Manifest.permission.CALL_PHONE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + number));
                v.getContext().startActivity(callIntent);

            } else {

                Toast.makeText(v.getContext(), "please Enable Call Permission", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Book Now Logic (සීට් තෝරන පේජ් එකට යන්න) ---
        holder.itemView.setOnClickListener(v -> {
            // මෙතනදී අපි පස්සේ SeatBooking එකට යන logic එක ලියමු
            Toast.makeText(v.getContext(), "Opening seat selection for " + bus.getSchedule_id(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return busList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPrice, tvTime;
        ImageButton btnCall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnCall = itemView.findViewById(R.id.btncall); // XML එකේ ID එක හරියටම btncall නේද කියලා බලන්න
        }
    }
}