package com.example.hello;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class QpointsActivity extends AppCompatActivity {

    private TextView pointsBalanceTextView;
    private TextView coffeeRewardPointsTextView;
    private TextView parkingRewardPointsTextView;
    private Button redeemCoffeeButton;
    private Button redeemParkingButton;
    private CardView coffeeRewardCard;
    private CardView parkingRewardCard;
    private TextView transactionHistoryTextView;
    private ImageView backButton;

    private DatabaseReference usersRef;
    private DatabaseReference transactionsRef;
    private DatabaseReference notificationsRef;
    private String currentUserId;
    private int userPoints = 0;

    // Reward requirements
    private static final int COFFEE_REWARD_POINTS = 50;
    private static final int PARKING_REWARD_POINTS = 100;

    // Reward IDs
    private static final String COFFEE_REWARD_ID = "COFFEE";
    private static final String PARKING_REWARD_ID = "PARKING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qpoints);

        initializeViews();
        setupFirebase();
        setupClickListeners();
        loadUserPoints();
        loadTransactionHistory();
    }

    private void initializeViews() {
        pointsBalanceTextView = findViewById(R.id.pointsBalanceTextView);
        coffeeRewardPointsTextView = findViewById(R.id.coffeeRewardPointsTextView);
        parkingRewardPointsTextView = findViewById(R.id.parkingRewardPointsTextView);
        redeemCoffeeButton = findViewById(R.id.redeemCoffeeButton);
        redeemParkingButton = findViewById(R.id.redeemParkingButton);
        coffeeRewardCard = findViewById(R.id.coffeeRewardCard);
        parkingRewardCard = findViewById(R.id.parkingRewardCard);
        transactionHistoryTextView = findViewById(R.id.transactionHistoryTextView);
        backButton = findViewById(R.id.backButton);

        // Set reward point requirements
        coffeeRewardPointsTextView.setText(String.format("Redeem for %d points", COFFEE_REWARD_POINTS));
        parkingRewardPointsTextView.setText(String.format("Redeem for %d points", PARKING_REWARD_POINTS));
    }

    private void setupFirebase() {
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
        transactionsRef = FirebaseDatabase.getInstance().getReference("Transactions").child(currentUserId);
        notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUserId);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        redeemCoffeeButton.setOnClickListener(v -> redeemCoffeeReward());

        redeemParkingButton.setOnClickListener(v -> redeemParkingReward());
    }

    private void loadUserPoints() {
        usersRef.child("qpoints").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userPoints = snapshot.getValue(Integer.class);
                } else {
                    userPoints = 0;
                    // Initialize points if they don't exist
                    usersRef.child("qpoints").setValue(0);
                }
                updatePointsDisplay();
                updateRewardButtons();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QpointsActivity.this, "Failed to load points", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePointsDisplay() {
        pointsBalanceTextView.setText(String.valueOf(userPoints));
    }

    private void updateRewardButtons() {
        // Update coffee reward button
        if (userPoints >= COFFEE_REWARD_POINTS) {
            redeemCoffeeButton.setEnabled(true);
            redeemCoffeeButton.setAlpha(1.0f);
            redeemCoffeeButton.setText("Redeem Coffee");
        } else {
            redeemCoffeeButton.setEnabled(false);
            redeemCoffeeButton.setAlpha(0.5f);
            redeemCoffeeButton.setText(String.format("Need %d more points", COFFEE_REWARD_POINTS - userPoints));
        }

        // Update parking reward button
        if (userPoints >= PARKING_REWARD_POINTS) {
            redeemParkingButton.setEnabled(true);
            redeemParkingButton.setAlpha(1.0f);
            redeemParkingButton.setText("Redeem Parking");
        } else {
            redeemParkingButton.setEnabled(false);
            redeemParkingButton.setAlpha(0.5f);
            redeemParkingButton.setText(String.format("Need %d more points", PARKING_REWARD_POINTS - userPoints));
        }
    }

    private void redeemCoffeeReward() {
        if (userPoints < COFFEE_REWARD_POINTS) {
            Toast.makeText(this, "Not enough points for coffee reward", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Redeem Coffee Reward")
                .setMessage("Do you want to redeem " + COFFEE_REWARD_POINTS + " points for a free coffee?")
                .setPositiveButton("Yes, Redeem", (dialog, which) -> processCoffeeRedemption())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void redeemParkingReward() {
        if (userPoints < PARKING_REWARD_POINTS) {
            Toast.makeText(this, "Not enough points for parking reward", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Redeem Parking Reward")
                .setMessage("Do you want to redeem " + PARKING_REWARD_POINTS + " points for free parking?")
                .setPositiveButton("Yes, Redeem", (dialog, which) -> processParkingRedemption())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processCoffeeRedemption() {
        int newPointsBalance = userPoints - COFFEE_REWARD_POINTS;

        // Generate unique coffee ID
        String coffeeId = "COFFEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Update points balance
        usersRef.child("qpoints").setValue(newPointsBalance)
                .addOnSuccessListener(aVoid -> {
                    // Record transaction
                    recordTransaction("Coffee Reward Redemption", -COFFEE_REWARD_POINTS, coffeeId);

                    // 🔥 NEW: Create Qpoints notification
                    createQpointsNotification("Coffee Reward Redeemed!",
                            "You successfully redeemed " + COFFEE_REWARD_POINTS + " points for a free coffee. Your Coffee ID: " + coffeeId,
                            "points_redeemed");

                    // Show success message with coffee ID
                    showRedemptionSuccessDialog("Coffee", coffeeId, COFFEE_REWARD_POINTS);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to redeem coffee reward", Toast.LENGTH_SHORT).show();
                });
    }

    private void processParkingRedemption() {
        int newPointsBalance = userPoints - PARKING_REWARD_POINTS;

        // Generate unique parking ID
        String parkingId = "PARKING-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Update points balance
        usersRef.child("qpoints").setValue(newPointsBalance)
                .addOnSuccessListener(aVoid -> {
                    // Record transaction
                    recordTransaction("Parking Reward Redemption", -PARKING_REWARD_POINTS, parkingId);

                    // 🔥 NEW: Create Qpoints notification
                    createQpointsNotification("Parking Reward Redeemed!",
                            "You successfully redeemed " + PARKING_REWARD_POINTS + " points for free parking. Your Parking ID: " + parkingId,
                            "points_redeemed");

                    // Show success message with parking ID
                    showRedemptionSuccessDialog("Parking", parkingId, PARKING_REWARD_POINTS);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to redeem parking reward", Toast.LENGTH_SHORT).show();
                });
    }

    // 🔥 NEW: Create Qpoints notification
    private void createQpointsNotification(String title, String message, String type) {
        String notificationId = notificationsRef.push().getKey();
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("type", type);
        notification.put("priority", "medium");

        notificationsRef.child(notificationId).setValue(notification);
    }

    private void showRedemptionSuccessDialog(String rewardType, String rewardId, int pointsUsed) {
        new AlertDialog.Builder(this)
                .setTitle("Reward Redeemed Successfully!")
                .setMessage(String.format(
                        "You have successfully redeemed %d points for %s.\n\n" +
                                "Your %s ID: %s\n\n" +
                                "Show this ID to claim your reward.",
                        pointsUsed, rewardType.toLowerCase(), rewardType, rewardId
                ))
                .setPositiveButton("OK", null)
                .show();
    }

    private void recordTransaction(String description, int points, String rewardId) {
        String transactionId = transactionsRef.push().getKey();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("description", description);
        transaction.put("points", points);
        transaction.put("timestamp", timestamp);
        transaction.put("rewardId", rewardId);
        transaction.put("type", points > 0 ? "earned" : "redeemed");

        transactionsRef.child(transactionId).setValue(transaction);
    }

    private void loadTransactionHistory() {
        transactionsRef.orderByChild("timestamp").limitToLast(10).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder historyBuilder = new StringBuilder();
                historyBuilder.append("Recent Transactions:\n\n");

                if (!snapshot.exists()) {
                    historyBuilder.append("No transactions yet.\nEarn points by using queue services!");
                } else {
                    for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                        String description = transactionSnapshot.child("description").getValue(String.class);
                        Integer points = transactionSnapshot.child("points").getValue(Integer.class);
                        String timestamp = transactionSnapshot.child("timestamp").getValue(String.class);

                        if (description != null && points != null && timestamp != null) {
                            String sign = points > 0 ? "+" : "";
                            historyBuilder.append(String.format("• %s: %s%d points\n",
                                    timestamp, sign, points));
                            historyBuilder.append(String.format("  %s\n\n", description));
                        }
                    }
                }

                transactionHistoryTextView.setText(historyBuilder.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                transactionHistoryTextView.setText("Failed to load transaction history");
            }
        });
    }

    // Method to add points (can be called from other activities when user completes queue services)
    public static void addPointsToUser(String userId, int points, String reason) {
        if (userId == null || points <= 0) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications").child(userId);

        // Get current points and add new points
        userRef.child("qpoints").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int currentPoints = 0;
                if (snapshot.exists()) {
                    currentPoints = snapshot.getValue(Integer.class);
                }

                int newPoints = currentPoints + points;

                // Update points
                userRef.child("qpoints").setValue(newPoints);

                // Record transaction
                recordPointsTransaction(userId, reason, points);

                // 🔥 NEW: Create points earned notification
                String notificationId = notificationsRef.push().getKey();
                Map<String, Object> notification = new HashMap<>();
                notification.put("title", "🎉 Points Earned!");
                notification.put("message", "You earned " + points + " points for: " + reason);
                notification.put("timestamp", System.currentTimeMillis());
                notification.put("read", false);
                notification.put("type", "points_earned");
                notification.put("priority", "medium");

                notificationsRef.child(notificationId).setValue(notification);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Points update failed, but we don't show error to user
            }
        });
    }

    private static void recordPointsTransaction(String userId, String description, int points) {
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference("Transactions").child(userId);
        String transactionId = transactionsRef.push().getKey();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("description", description);
        transaction.put("points", points);
        transaction.put("timestamp", timestamp);
        transaction.put("type", "earned");

        transactionsRef.child(transactionId).setValue(transaction);
    }

    // Method to check if user has enough points for a specific reward
    public static void checkUserPoints(String userId, PointsCheckCallback callback) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.child("qpoints").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int points = 0;
                if (snapshot.exists()) {
                    points = snapshot.getValue(Integer.class);
                }
                callback.onPointsChecked(points);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onPointsChecked(0);
            }
        });
    }

    public interface PointsCheckCallback {
        void onPointsChecked(int points);
    }

    // Getters for reward requirements (useful for other activities)
    public static int getCoffeeRewardPoints() {
        return COFFEE_REWARD_POINTS;
    }

    public static int getParkingRewardPoints() {
        return PARKING_REWARD_POINTS;
    }
    // Add this method to your QpointsActivity class, before the onDestroy() method if it exists
    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
    }

    // Also add this helper method to QpointsActivity
    private void updateNotificationBadge() {
        if (notificationsRef != null && currentUserId != null) {
            notificationsRef.child(currentUserId).orderByChild("read").equalTo(false)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            int unreadCount = (int) snapshot.getChildrenCount();
                            // You can update a badge here if you implement it in QpointsActivity
                            // For now, we'll just ensure the count is maintained
                            Log.d("QpointsActivity", "Unread notifications: " + unreadCount);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("QpointsActivity", "Error loading notification count: " + error.getMessage());
                        }
                    });
        }
    }
}