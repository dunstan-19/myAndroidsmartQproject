package com.example.hello;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueueNotificationService extends Service {
    private DatabaseReference queueRef, notificationsRef;
    private FirebaseAuth mAuth;
    private Map<String, Map<String, Long>> previousQueueStates = new HashMap<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
        queueRef = FirebaseDatabase.getInstance().getReference("ServiceQueues");
        notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications");

        startQueueMonitoring();
    }

    private void startQueueMonitoring() {
        queueRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    checkQueueChanges(snapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("QueueNotification", "Error monitoring queues: " + error.getMessage());
            }
        });
    }

    private void checkQueueChanges(DataSnapshot currentSnapshot) {
        for (DataSnapshot serviceSnapshot : currentSnapshot.getChildren()) {
            String service = serviceSnapshot.getKey();
            Map<String, Long> currentQueue = new HashMap<>();
            List<QueueUser> queueUsers = new ArrayList<>();

            // Get current queue state
            for (DataSnapshot userSnapshot : serviceSnapshot.getChildren()) {
                String userId = userSnapshot.getKey();
                Long timestamp = userSnapshot.getValue(Long.class);
                currentQueue.put(userId, timestamp);
                queueUsers.add(new QueueUser(userId, timestamp));
            }

            // Sort by timestamp (earliest first)
            Collections.sort(queueUsers, new Comparator<QueueUser>() {
                @Override
                public int compare(QueueUser u1, QueueUser u2) {
                    return Long.compare(u1.timestamp, u2.timestamp);
                }
            });

            // Check for changes and notify users at positions 2 and 3
            Map<String, Long> previousQueue = previousQueueStates.get(service);
            if (previousQueue != null && !previousQueue.equals(currentQueue)) {
                notifyQueuePositionUsers(queueUsers, service);
            }

            previousQueueStates.put(service, currentQueue);
        }
    }

    private void notifyQueuePositionUsers(List<QueueUser> queueUsers, String service) {
        for (int i = 0; i < Math.min(queueUsers.size(), 3); i++) {
            QueueUser user = queueUsers.get(i);
            int position = i + 1;

            // Only notify users at positions 2 and 3
            if (position == 2 || position == 3) {
                createPositionNotification(user.userId, service, position);
            }

            // Notify first position user when they're at the front
            if (position == 1) {
                createFrontOfQueueNotification(user.userId, service);
            }
        }
    }

    private void createPositionNotification(String userId, String service, int position) {
        String notificationId = notificationsRef.child(userId).push().getKey();
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "Queue Position Update");
        notification.put("message", "You are now position " + position + " in " + service + " queue");
        notification.put("service", service);
        notification.put("position", position);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("type", "position_update");

        notificationsRef.child(userId).child(notificationId).setValue(notification);

        Log.d("QueueNotification", "Created position notification for user: " + userId + " at position: " + position);
    }

    private void createFrontOfQueueNotification(String userId, String service) {
        String notificationId = notificationsRef.child(userId).push().getKey();
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "Queue Update");
        notification.put("message", "You are now at the front of " + service + " queue. Please proceed to the counter.");
        notification.put("service", service);
        notification.put("position", 1);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("type", "front_of_queue");

        notificationsRef.child(userId).child(notificationId).setValue(notification);

        Log.d("QueueNotification", "Created front-of-queue notification for user: " + userId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up listeners
    }

    private static class QueueUser {
        String userId;
        long timestamp;

        QueueUser(String userId, long timestamp) {
            this.userId = userId;
            this.timestamp = timestamp;
        }
    }
}