package com.example.hello;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private DatabaseReference notificationsRef;
    private FirebaseAuth mAuth;
    private ImageView emptyStateImage;
    private TextView emptyStateText;
    private Button btnClearNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();
        notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications");

        // Initialize views
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        emptyStateImage = findViewById(R.id.emptyStateImage);
        emptyStateText = findViewById(R.id.emptyStateText);
        btnClearNotifications = findViewById(R.id.btnClearNotifications);

        // Setup RecyclerView
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notificationList);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(notificationAdapter);

        // Load notifications
        loadNotifications();

        // Clear notifications button
        btnClearNotifications.setOnClickListener(v -> clearAllNotifications());

        // Back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void loadNotifications() {
        String userId = mAuth.getCurrentUser().getUid();
        notificationsRef.child(userId).orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        notificationList.clear();

                        if (snapshot.exists()) {
                            for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                                Notification notification = notificationSnapshot.getValue(Notification.class);
                                if (notification != null) {
                                    notificationList.add(notification);
                                }
                            }

                            // Sort by timestamp (newest first)
                            Collections.sort(notificationList, (n1, n2) ->
                                    Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                            notificationAdapter.notifyDataSetChanged();
                            showEmptyState(false);
                        } else {
                            showEmptyState(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Notifications", "Error loading notifications: " + error.getMessage());
                        showEmptyState(true);
                    }
                });
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyStateImage.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.VISIBLE);
            notificationsRecyclerView.setVisibility(View.GONE);
            btnClearNotifications.setVisibility(View.GONE);
        } else {
            emptyStateImage.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.GONE);
            notificationsRecyclerView.setVisibility(View.VISIBLE);
            btnClearNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void clearAllNotifications() {
        String userId = mAuth.getCurrentUser().getUid();
        notificationsRef.child(userId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
                    notificationList.clear();
                    notificationAdapter.notifyDataSetChanged();
                    showEmptyState(true);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to clear notifications", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        markAllAsRead();
    }

    private void markAllAsRead() {
        String userId = mAuth.getCurrentUser().getUid();
        notificationsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                    notificationSnapshot.getRef().child("read").setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Notifications", "Error marking notifications as read: " + error.getMessage());
            }
        });
    }
}
