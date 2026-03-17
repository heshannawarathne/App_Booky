package com.aurasoft.booky.adpter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.model.SeatModel;

import java.util.List;

public class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatViewHolder> {

    private List<SeatModel> seatList;
    private Context context;
    private OnSeatClickListener listener; // Click Listener එක

    // Interface එක අනිවාර්යයෙන්ම මෙතන තියෙන්න ඕනේ
    public interface OnSeatClickListener {
        void onSeatClick(int position);
    }

    // Constructor එක Arguments 3ක් ගන්න විදිහට හැදුවා
    public SeatAdapter(List<SeatModel> seatList, Context context, OnSeatClickListener listener) {
        this.seatList = seatList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_seat, parent, false);
        return new SeatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        SeatModel model = seatList.get(position);

        if (model.getStatus() == 4) { // Aisle
            holder.tvSeat.setVisibility(View.INVISIBLE);
        } else {
            holder.tvSeat.setVisibility(View.VISIBLE);
            holder.tvSeat.setText(model.getSeatName());

            // Status අනුව UI එක Update කිරීම
            updateSeatUI(holder.tvSeat, model.getStatus());

            // Click Event එක Listener එකට යැවීම
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSeatClick(position);
                }
            });
        }
    }

    private void updateSeatUI(TextView tv, int status) {
        switch (status) {
            case 0: tv.setBackgroundResource(R.drawable.bg_seat_available); break;
            case 1: tv.setBackgroundResource(R.drawable.bg_seat_selected); break;
            case 2: tv.setBackgroundResource(R.drawable.bg_seat_male); break;
            case 3: tv.setBackgroundResource(R.drawable.bg_seat_female); break;
        }
    }

    @Override
    public int getItemCount() {
        return seatList != null ? seatList.size() : 0;
    }

    public static class SeatViewHolder extends RecyclerView.ViewHolder {
        TextView tvSeat;
        public SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSeat = itemView.findViewById(R.id.tvSeat);
        }
    }
}