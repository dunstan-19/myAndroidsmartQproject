package com.example.hello;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class queueAdapter extends RecyclerView.Adapter<queueAdapter.QueueViewHolder> {

    private List<QueueUser> queueList;

    public queueAdapter(List<QueueUser> queueList) {
        this.queueList = queueList;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.queue_item, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        QueueUser queueUser = queueList.get(position);
        holder.userNameTextView.setText("Name: " + queueUser.getName());
        holder.userIdTextView.setText("User ID: " + queueUser.getUserId());
        holder.joinTimeTextView.setText("Join Time: " + queueUser.getJoinTime());

    }

    @Override
    public int getItemCount() {
        return queueList.size();
    }

    public static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView;
        TextView userIdTextView;
        TextView joinTimeTextView;
        TextView userServicesTextView;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            userIdTextView = itemView.findViewById(R.id.userIdTextView);
            joinTimeTextView = itemView.findViewById(R.id.joinTimeTextView);
            userServicesTextView = itemView.findViewById(R.id.userServicesTextView);
        }
    }
}
