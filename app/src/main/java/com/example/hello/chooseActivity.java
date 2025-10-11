package com.example.hello;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class chooseActivity extends BaseActivity {
    private LinearLayout cardCreateAccount, cardDepositCash, cardWithdrawCash, cardCheckBalance, cardHelpdesk, cardOther;
    private Button btnDone;
    private DatabaseReference usersRef, queueRef, notificationsRef;
    private FirebaseAuth mAuth;
    private String selectedService = null;
    private LinearLayout lastSelectedCard = null;
    private ImageView profileImage;
    private TextView profileInitial;
    private BottomNavigationView bottomNavigationView;
    private String fcmToken = "";
    private static final String TAG = "chooseActivity";
    private TextView notificationBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose);

        // Handle edge-to-edge insets
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initializeViews();
        setupFirebase();
        setupClickListeners();
        setupBottomNavigation();
        setupNotificationBadge();

        loadProfileImage();
        initializeFCMToken();
        checkForNotifications();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profileImage);
        profileInitial = findViewById(R.id.profileInitial);
        makeImageViewCircular(profileImage);

        cardCreateAccount = findViewById(R.id.cardCreateAccount);
        cardDepositCash = findViewById(R.id.cardDepositCash);
        cardWithdrawCash = findViewById(R.id.cardWithdrawCash);
        cardCheckBalance = findViewById(R.id.cardCheckBalance);
        cardHelpdesk = findViewById(R.id.cardHelpdesk);
        cardOther = findViewById(R.id.cardOther);

        btnDone = findViewById(R.id.btnDone);

        setCardClickListener(cardCreateAccount, "Create Bank Account");
        setCardClickListener(cardDepositCash, "Deposit Cash");
        setCardClickListener(cardWithdrawCash, "Withdraw Cash");
        setCardClickListener(cardCheckBalance, "Check Balance");
        setCardClickListener(cardHelpdesk, "Helpdesk Services");
        setCardClickListener(cardOther, "Other");
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getCurrentUser().getUid());
        queueRef = FirebaseDatabase.getInstance().getReference("ServiceQueues");
        notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications");
    }

    private void setupClickListeners() {
        profileImage.setOnClickListener(v -> startActivity(new Intent(chooseActivity.this, ProfileActivity.class)));
        profileInitial.setOnClickListener(v -> startActivity(new Intent(chooseActivity.this, ProfileActivity.class)));

        btnDone.setOnClickListener(v -> {
            if (selectedService != null) {
                if (fcmToken.isEmpty()) {
                    Log.w(TAG, "FCM token is empty, retrying to get token");
                    getFCMToken(new FCMTokenCallback() {
                        @Override
                        public void onTokenReceived(String token) {
                            if (!token.isEmpty()) {
                                saveToDatabase(selectedService);
                            } else {
                                Toast.makeText(chooseActivity.this, "Please wait, initializing notifications...", Toast.LENGTH_SHORT).show();
                                saveToDatabase(selectedService);
                            }
                        }
                    });
                } else {
                    saveToDatabase(selectedService);
                }
            } else {
                Toast.makeText(chooseActivity.this, "Please select a service", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Setup notification badge
    private void setupNotificationBadge() {
        // Find the notification menu item and add badge
        bottomNavigationView.post(() -> {
            View notificationView = bottomNavigationView.findViewById(R.id.notification);
            if (notificationView != null) {
                View badgeView = notificationView.findViewById(R.id.notification_badge);
                if (badgeView instanceof TextView) {
                    notificationBadge = (TextView) badgeView;
                    updateNotificationBadge();
                }
            }
        });
    }

    // Update notification badge count
    private void updateNotificationBadge() {
        String userId = mAuth.getCurrentUser().getUid();
        notificationsRef.child(userId).orderByChild("read").equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int unreadCount = (int) snapshot.getChildrenCount();

                        runOnUiThread(() -> {
                            if (bottomNavigationView != null) {
                                View notificationItem = bottomNavigationView.findViewById(R.id.notification);
                                if (notificationItem != null) {
                                    // Remove any existing dot
                                    View existingDot = notificationItem.findViewWithTag("red_dot");
                                    if (existingDot != null) {
                                        ((ViewGroup) notificationItem).removeView(existingDot);
                                    }

                                    if (unreadCount > 0) {
                                        View redDot = new View(chooseActivity.this);
                                        redDot.setTag("red_dot");
                                        redDot.setBackgroundResource(R.drawable.red_dot);
                                        int size = (int) getResources().getDimension(R.dimen.notification_dot_size);
                                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                                        params.setMargins(90, 17, 0, 0); // Adjust position (top-right)
                                        redDot.setLayoutParams(params);
                                        ((ViewGroup) notificationItem).addView(redDot);
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("NotificationBadge", "Error loading notification count: " + error.getMessage());
                    }
                });
    }


    // 🔥 IMPROVED FCM TOKEN MANAGEMENT
    private interface FCMTokenCallback {
        void onTokenReceived(String token);
    }

    private void initializeFCMToken() {
        usersRef.child("fcmToken").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    fcmToken = snapshot.getValue(String.class);
                    Log.d(TAG, "Loaded existing FCM token from DB: " + fcmToken);
                    getFCMToken(null);
                } else {
                    Log.d(TAG, "No existing FCM token found, generating new one");
                    getFCMToken(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to load FCM token from DB: " + error.getMessage());
                getFCMToken(null);
            }
        });
    }

    private void getFCMToken(FCMTokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        Toast.makeText(chooseActivity.this, "Failed to get FCM token", Toast.LENGTH_SHORT).show();
                        retryFCMTokenGeneration();
                        if (callback != null) {
                            callback.onTokenReceived("");
                        }
                        return;
                    }

                    String newToken = task.getResult();
                    Log.d(TAG, "FCM Token retrieved: " + newToken);

                    if (!newToken.equals(fcmToken)) {
                        fcmToken = newToken;
                        saveFCMTokenToDatabase(fcmToken);
                    } else {
                        Log.d(TAG, "FCM token unchanged, skipping DB update");
                    }

                    if (callback != null) {
                        callback.onTokenReceived(fcmToken);
                    }
                });
    }

    private void saveFCMTokenToDatabase(String token) {
        if (token != null && !token.isEmpty()) {
            usersRef.child("fcmToken").setValue(token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "FCM Token saved successfully to database");
                        usersRef.child("QueueChoices").child("fcmToken").setValue(token)
                                .addOnSuccessListener(aVoid1 ->
                                        Log.d(TAG, "FCM Token updated in QueueChoices"))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Failed to update FCM token in QueueChoices", e));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save FCM token to database", e);
                        Toast.makeText(chooseActivity.this, "Failed to save notification token", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.w(TAG, "Attempted to save empty FCM token");
        }
    }

    private void retryFCMTokenGeneration() {
        new Handler().postDelayed(() -> {
            Log.d(TAG, "Retrying FCM token generation...");
            getFCMToken(null);
        }, 5000);
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                return true;
            } else if (itemId == R.id.help) {
                startActivity(new Intent(chooseActivity.this, helpActivity.class));
                return true;
            } else if (itemId == R.id.settings) {
                startActivity(new Intent(chooseActivity.this, Settings.class));
                return true;
            } else if (itemId == R.id.wallet) {
                startActivity(new Intent(chooseActivity.this, QpointsActivity.class));
                return true;
            } else if (itemId == R.id.notification) {
                startActivity(new Intent(chooseActivity.this, NotificationsActivity.class));
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.home);
    }

    private void makeImageViewCircular(ImageView imageView) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(0xFFFFFFFF);
        imageView.setBackground(shape);
        imageView.setClipToOutline(true);
    }

    private void loadProfileImage() {
        usersRef.child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String encodedImage = snapshot.getValue(String.class);
                    if (encodedImage != null && !encodedImage.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            profileImage.setImageBitmap(decodedBitmap);
                            profileInitial.setVisibility(View.GONE);
                        } catch (Exception e) {
                            Log.e(TAG, "Error decoding profile image", e);
                            showDefaultProfile();
                        }
                    } else {
                        showDefaultProfile();
                    }
                } else {
                    showDefaultProfile();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showDefaultProfile();
            }
        });
    }

    private void showDefaultProfile() {
        profileImage.setVisibility(View.GONE);
        profileInitial.setVisibility(View.VISIBLE);
        profileInitial.setText("🤭");
    }

    private void setCardClickListener(LinearLayout card, String serviceName) {
        card.setOnClickListener(v -> {
            if (selectedService != null && selectedService.equals(serviceName)) {
                selectedService = null;
                card.setAlpha(1.0f);
                lastSelectedCard = null;
            } else {
                if (lastSelectedCard != null) {
                    lastSelectedCard.setAlpha(1.0f);
                }
                selectedService = serviceName;
                card.setAlpha(0.5f);
                lastSelectedCard = card;
            }
        });
    }

    private void saveToDatabase(String selectedService) {
        String userId = mAuth.getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> userChoices = new HashMap<>();
        userChoices.put("selectedService", selectedService);
        userChoices.put("timestamp", timestamp);
        userChoices.put("fcmToken", fcmToken);

        usersRef.child("QueueChoices").setValue(userChoices)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String serviceKey = selectedService.replace(" ", "_");
                        queueRef.child(serviceKey).child(userId).setValue(timestamp)
                                .addOnCompleteListener(queueTask -> {
                                    if (queueTask.isSuccessful()) {
                                        getQueuePositionAndNotify(selectedService, serviceKey, userId);
                                    } else {
                                        Toast.makeText(chooseActivity.this, "Failed to join queue", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(chooseActivity.this, "Failed to save service", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getQueuePositionAndNotify(String selectedService, String serviceKey, String userId) {
        queueRef.child(serviceKey).orderByValue().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int position = 1;
                boolean userFound = false;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    if (userSnapshot.getKey().equals(userId)) {
                        userFound = true;
                        break;
                    }
                    position++;
                }

                if (userFound) {
                    showConfirmationDialog(selectedService, position);
                    createQueueNotification(userId, selectedService, position, "joined");

                    // Create priority notification for positions 1-3
                    if (position <= 3) {
                        createPriorityQueueNotification(userId, selectedService, position);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showConfirmationDialog(selectedService, -1);
            }
        });
    }

    // Create priority notification for positions 1-3
    private void createPriorityQueueNotification(String userId, String service, int position) {
        String notificationId = notificationsRef.child(userId).push().getKey();
        String title = "";
        String message = "";

        switch (position) {
            case 1:
                title = "🚀 You're Next!";
                message = "You are position 1 in " + service + " queue. Get ready!";
                break;
            case 2:
                title = "⏰ Almost There!";
                message = "You are position 2 in " + service + " queue. Almost your turn!";
                break;
            case 3:
                title = "📋 Getting Close!";
                message = "You are position 3 in " + service + " queue. Your turn is coming up!";
                break;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("service", service);
        notification.put("position", position);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("type", "queue_priority");
        notification.put("priority", "high");

        notificationsRef.child(userId).child(notificationId).setValue(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Priority queue notification created for position " + position);
                    updateNotificationBadge(); // Update badge when new notification is created
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create priority notification", e);
                });
    }

    private void showConfirmationDialog(final String selectedService, int position) {
        String message = "You have selected:\n• " + selectedService +
                "\n\nYour position in queue: " + position +
                "\n\nDo you want to proceed?";

        new AlertDialog.Builder(this)
                .setTitle("Confirm Selection")
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> proceedToNextActivity())
                .setNegativeButton("No", null)
                .show();
    }

    private void proceedToNextActivity() {
        Intent intent = new Intent(chooseActivity.this, QueueActivity.class);
        intent.putExtra("selectedService", selectedService);
        startActivity(intent);
    }

    private void createQueueNotification(String userId, String service, int position, String type) {
        String notificationId = notificationsRef.child(userId).push().getKey();
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "Queue Update");
        notification.put("message", "You are position " + position + " in " + service + " queue");
        notification.put("service", service);
        notification.put("position", position);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("type", type);

        notificationsRef.child(userId).child(notificationId).setValue(notification)
                .addOnSuccessListener(aVoid -> {
                    updateNotificationBadge(); // Update badge when new notification is created
                });
    }

    private void checkForNotifications() {
        String userId = mAuth.getCurrentUser().getUid();
        notificationsRef.child(userId).orderByChild("read").equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                                try {
                                    String message = notificationSnapshot.child("message").getValue(String.class);
                                    if (message != null) {
                                        showNotificationToast(message);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing notification", e);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("Notifications", "Error loading notifications: " + error.getMessage());
                    }
                });
    }

    private void showNotificationToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.home);
        }
        loadProfileImage();
        checkForNotifications();
        updateNotificationBadge(); // Update badge when activity resumes

        if (fcmToken.isEmpty()) {
            Log.d(TAG, "FCM token empty on resume, reinitializing...");
            initializeFCMToken();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any handlers or listeners if needed
    }
}