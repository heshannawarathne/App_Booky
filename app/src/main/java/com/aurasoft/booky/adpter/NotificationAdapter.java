package com.aurasoft.booky.adpter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.model.NotificationModel;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationModel> notificationList;

    public NotificationAdapter(List<NotificationModel> notificationList) {
        this.notificationList = notificationList != null ? notificationList : new ArrayList<>();
    }

    public void setNotifications(List<NotificationModel> newList) {
        this.notificationList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel model = notificationList.get(position);

        holder.txtTitle.setText(model.getTitle());
        holder.txtMessage.setText(model.getMessage());

        if (model.getTimestamp() > 0) {
            java.util.Date date = new java.util.Date(model.getTimestamp());
            String formattedTime = android.text.format.DateFormat.format("dd MMM, hh:mm a", date).toString();
            holder.txtTime.setText(formattedTime);
        } else {
            holder.txtTime.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtMessage, txtTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.notiTitle);
            txtMessage = itemView.findViewById(R.id.notiMessage);
            txtTime = itemView.findViewById(R.id.notiTime);
        }
    }
}