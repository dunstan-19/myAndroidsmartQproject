package com.example.hello;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class QueueUserAdapter extends RecyclerView.Adapter<QueueUserAdapter.ViewHolder> {
    private List<QueueUser> queueUsers;
    private DatabaseReference queueRef;
    private DatabaseReference reportsRef;
    private String department;

    public QueueUserAdapter(List<QueueUser> queueUsers, String department) {
        this.queueUsers = queueUsers;
        this.department = department;
        this.queueRef = FirebaseDatabase.getInstance().getReference("Queue").child(department);
        this.reportsRef = FirebaseDatabase.getInstance().getReference("Reports");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QueueUser user = queueUsers.get(position);
        holder.userName.setText(user.getName());
        holder.userPosition.setText("Position: " + (position + 1));
        holder.userTicketNumber.setText("Ticket Number: " + user.getTicketNumber());

        // Store context from holder
        Context context = holder.itemView.getContext();

        // Set up remove button
        holder.removeButton.setOnClickListener(v -> {
            saveReportAndRemove(user, "Removed", position, context);
        });

        // Check user status from database
        checkUserStatus(user.getUserId(), holder, position, context);
    }

    private void checkUserStatus(String userId, ViewHolder holder, int position, Context context) {
        queueRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    if (status != null && status.equals("stepped_away")) {
                        // User has stepped away
                        holder.servedButton.setVisibility(View.GONE);
                        holder.steppedAwayText.setVisibility(View.VISIBLE);
                        holder.steppedAwayText.setText("STEPPED AWAY");
                    } else {
                        // User is active
                        holder.servedButton.setVisibility(View.VISIBLE);
                        holder.steppedAwayText.setVisibility(View.GONE);
                        setupServeButton(holder, queueUsers.get(position), context);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("QueueUserAdapter", "Error checking user status: " + error.getMessage());
            }
        });
    }

    private void setupServeButton(ViewHolder holder, QueueUser user, Context context) {
        holder.servedButton.setText("Serve");
        holder.servedButton.setOnClickListener(v -> {
            if (holder.servedButton.getText().toString().equals("Serve")) {
                holder.servedButton.setText("Done");
                Toast.makeText(context, "Now serving " + user.getName(), Toast.LENGTH_SHORT).show();
            } else {
                saveReportAndRemove(user, "Served", holder.getAdapterPosition(), context);
            }
        });
    }

    private void saveReportAndRemove(QueueUser user, String action, int position, Context context) {
        Report report = new Report(
                user.getUserId(),
                user.getName(),
                user.getTicketNumber(),
                System.currentTimeMillis(),
                department,
                action
        );

        reportsRef.push().setValue(report)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        queueRef.child(user.getUserId()).removeValue()
                                .addOnCompleteListener(removeTask -> {
                                    if (removeTask.isSuccessful()) {
                                        queueUsers.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, queueUsers.size());
                                        Toast.makeText(context,
                                                user.getName() + " has been " + action.toLowerCase(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    @Override
    public int getItemCount() {
        return queueUsers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userPosition, userTicketNumber, steppedAwayText;
        Button servedButton, removeButton;
        public ViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userPosition = itemView.findViewById(R.id.userPosition);
            userTicketNumber = itemView.findViewById(R.id.userTicketNumber);
            servedButton = itemView.findViewById(R.id.servedButton);
            removeButton = itemView.findViewById(R.id.removeButton);
            steppedAwayText = itemView.findViewById(R.id.steppedAwayText);
        }
    }
}