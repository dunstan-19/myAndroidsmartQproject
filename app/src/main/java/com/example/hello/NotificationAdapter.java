package com.example.hello;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.titleTextView.setText(notification.getTitle());
        holder.messageTextView.setText(notification.getMessage());

        // Format timestamp
        String time = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date(notification.getTimestamp()));
        holder.timeTextView.setText(time);

        // Show service and position if available
        if (notification.getService() != null && notification.getPosition() > 0) {
            holder.serviceTextView.setText(notification.getService() + " • Position: " + notification.getPosition());
            holder.serviceTextView.setVisibility(View.VISIBLE);
        } else {
            holder.serviceTextView.setVisibility(View.GONE);
        }

        // Visual indicator for unread notifications
        if (!notification.isRead()) {
            holder.itemView.setAlpha(1.0f);
            holder.unreadIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setAlpha(0.7f);
            holder.unreadIndicator.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, messageTextView, timeTextView, serviceTextView;
        View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.notificationTitle);
            messageTextView = itemView.findViewById(R.id.notificationMessage);
            timeTextView = itemView.findViewById(R.id.notificationTime);
            serviceTextView = itemView.findViewById(R.id.notificationService);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
}