package com.example.hello;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QueueActivity extends BaseActivity {
    private LinearLayout QueueContainer;
    private DatabaseReference queueRef;
    private DatabaseReference usersRef;
    private DatabaseReference queueStatusRef;
    private DatabaseReference notificationsRef;
    private DatabaseReference fcmTokensRef;
    private String currentUserId;
    private Button JoinQueueButton;
    private Button LeaveQueueButton;
    private Button StepAwayButton;
    private Button ReturnButton;
    private Button PriorityJoinButton;
    private TextView GreetingTextView;
    private TextView EmptyQueueTextView;
    private TextView currentQueueTextView;
    private TextView ticketNumberTextView;
    private TextView WaitingTimeTextView;
    private boolean isUserInQueue = false;
    private boolean isSteppedAway = false;
    private boolean isEmergencyUser = false;
    private String selectedService;
    private List<String> queueUserIds = new ArrayList<>();
    private CountDownTimer countDownTimer;
    private long estimatedWaitTimeMillis = 0;
    private BottomNavigationView bottomNavigationView;
    private ImageView profileImage;
    private TextView profileInitial;
    private Handler holdTimerHandler = new Handler();
    private String fcmToken = "";

    // Notification constants
    private static final String CHANNEL_ID = "queue_updates";
    private static final int NOTIFICATION_ID_POSITION = 1000;
    private static final int NOTIFICATION_ID_STEP_AWAY = 1001;
    private static final int NOTIFICATION_ID_HOLD_EXPIRED = 1002;

    // Listener references for cleanup
    private ValueEventListener queueListener;
    private ValueEventListener userQueueStatusListener;
    private ValueEventListener notificationListener;
    private ValueEventListener queuePositionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setupBottomNavigation();
        // Initialize views
        profileImage = findViewById(R.id.profileImage);
        profileInitial = findViewById(R.id.profileInitial);
        makeImageViewCircular(profileImage);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        QueueContainer = findViewById(R.id.QueueContainer);
        JoinQueueButton = findViewById(R.id.JoinQueueButton);
        LeaveQueueButton = findViewById(R.id.LeaveQueueButton);
        StepAwayButton = findViewById(R.id.StepAwayButton);
        ReturnButton = findViewById(R.id.ReturnButton);
        PriorityJoinButton = findViewById(R.id.PriorityJoinButton);
        GreetingTextView = findViewById(R.id.GreetingTextView);
        EmptyQueueTextView = findViewById(R.id.EmptyQueueTextView);
        currentQueueTextView = findViewById(R.id.TextView);
        ticketNumberTextView = findViewById(R.id.ticketNumberTextView);
        WaitingTimeTextView = findViewById(R.id.WaitingTimeTextView);

        // Initialize Firebase references
        queueRef = FirebaseDatabase.getInstance().getReference("Queue");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        queueStatusRef = FirebaseDatabase.getInstance().getReference("QueueStatus");
        notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications");
        fcmTokensRef = FirebaseDatabase.getInstance().getReference("FCMTokens");

        // Set click listeners
        profileImage.setOnClickListener(v -> {
            startActivity(new Intent(QueueActivity.this, ProfileActivity.class));
        });

        selectedService = getIntent().getStringExtra("selectedService");

        // Setup bottom navigation

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            initializeFCMToken();
            fetchUserData();
            checkIfUserInQueue();
        }

        setupRealTimeListeners();
        listenForQueueUpdates();
        setupQueuePositionListener();

        JoinQueueButton.setOnClickListener(v -> joinQueue());
        LeaveQueueButton.setOnClickListener(v -> confirmLeaveQueue());
        StepAwayButton.setOnClickListener(v -> confirmStepAway());
        ReturnButton.setOnClickListener(v -> returnFromStepAway());
        PriorityJoinButton.setOnClickListener(v -> showEmergencyIdDialog());
    }

    // 🔥 NEW: Initialize FCM Token for potential future use
    private void initializeFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        fcmToken = task.getResult();
                        Log.d("FCM", "FCM Token: " + fcmToken);

                        // Save token to Firebase for potential admin use
                        if (fcmToken != null && !fcmToken.isEmpty()) {
                            saveFCMTokenToDatabase(fcmToken);
                        }
                    }
                });
    }

    // 🔥 NEW: Save FCM token to database (for future use)
    private void saveFCMTokenToDatabase(String token) {
        if (currentUserId != null && token != null && !token.isEmpty()) {
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("token", token);
            tokenData.put("userId", currentUserId);
            tokenData.put("timestamp", System.currentTimeMillis());
            tokenData.put("platform", "android");

            fcmTokensRef.child(currentUserId).setValue(tokenData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("FCM", "FCM token saved successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FCM", "Failed to save FCM token", e);
                    });
        }
    }

    // 🔥 NEW: Show local notification for queue position
    private void showLocalNotificationForPosition(int position) {
        String title = "";
        String message = "";

        switch (position) {
            case 1:
                title = "🚀 You're Next in Line!";
                message = "You are position 1 in " + selectedService + " queue. Get ready immediately!";
                break;
            case 2:
                title = "⏰ Almost Your Turn!";
                message = "You are position 2 in " + selectedService + " queue. Your turn is coming up soon!";
                break;
            case 3:
                title = "📋 Getting Close!";
                message = "You are position 3 in " + selectedService + " queue. Be prepared for your turn!";
                break;
        }

        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.letter)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        try {
            notificationManager.notify(NOTIFICATION_ID_POSITION + position, builder.build());
            Log.d("LocalNotification", "Local notification shown for position " + position);
        } catch (SecurityException e) {
            Log.e("LocalNotification", "Notification permission issue: " + e.getMessage());
        }
    }

    // 🔥 NEW: Show local notification for step away reminder
    private void showStepAwayLocalNotification() {
        String title = "⏰ Step Away Reminder";
        String message = "Your position in " + selectedService + " queue is held for 15 minutes. Please return soon!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.letter)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        try {
            notificationManager.notify(NOTIFICATION_ID_STEP_AWAY, builder.build());
            Log.d("LocalNotification", "Step away local notification shown");
        } catch (SecurityException e) {
            Log.e("LocalNotification", "Notification permission issue: " + e.getMessage());
        }
    }

    // 🔥 NEW: Show local notification for hold expired
    private void showHoldExpiredLocalNotification() {
        String title = "❌ Queue Position Expired";
        String message = "Your 15-minute hold time has expired. You've been removed from " + selectedService + " queue.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.letter)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        try {
            notificationManager.notify(NOTIFICATION_ID_HOLD_EXPIRED, builder.build());
            Log.d("LocalNotification", "Hold expired local notification shown");
        } catch (SecurityException e) {
            Log.e("LocalNotification", "Notification permission issue: " + e.getMessage());
        }
    }

    // 🔥 NEW: Show local notification when served
    private void showServedLocalNotification() {
        String title = "✅ You've Been Served!";
        String message = "You have been served in " + selectedService + " queue. Thank you for using SmartQ!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.letter)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        try {
            notificationManager.notify(NOTIFICATION_ID_POSITION + 999, builder.build());
            Log.d("LocalNotification", "Served local notification shown");
        } catch (SecurityException e) {
            Log.e("LocalNotification", "Notification permission issue: " + e.getMessage());
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                startActivity(new Intent(QueueActivity.this, chooseActivity.class));
                return true;
            } else if (itemId == R.id.help) {
                startActivity(new Intent(QueueActivity.this, helpActivity.class));
                return true;
            } else if (itemId == R.id.settings) {
                startActivity(new Intent(QueueActivity.this, Settings.class));
                return true;
            } else if (itemId == R.id.wallet) {
                startActivity(new Intent(QueueActivity.this, QpointsActivity.class));
                return true;
            } else if (itemId == R.id.notification) {
                startActivity(new Intent(QueueActivity.this, NotificationsActivity.class));
                return true;
            }
            return false;
        });
    }

    // NEW: Show dialog for emergency ID input
    private void showEmergencyIdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Priority Join");
        builder.setMessage("Enter your Emergency ID to join as priority user:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Emergency ID");
        builder.setView(input);

        builder.setPositiveButton("Verify & Join", (dialog, which) -> {
            String emergencyId = input.getText().toString().trim();
            if (!emergencyId.isEmpty()) {
                verifyEmergencyId(emergencyId);
            } else {
                Toast.makeText(QueueActivity.this, "Please enter your Emergency ID", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // NEW: Verify emergency ID and join queue as priority
    private void verifyEmergencyId(String enteredEmergencyId) {
        usersRef.child(currentUserId).child("emergencyId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String storedEmergencyId = snapshot.getValue(String.class);
                    if (storedEmergencyId != null && storedEmergencyId.equals(enteredEmergencyId)) {
                        // Emergency ID verified, join as priority
                        isEmergencyUser = true;
                        joinQueueAsPriority();
                    } else {
                        Toast.makeText(QueueActivity.this, "Invalid Emergency ID", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(QueueActivity.this, "No Emergency ID found for your account", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QueueActivity.this, "Error verifying Emergency ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // NEW: Join queue as priority user
    private void joinQueueAsPriority() {
        if (!isUserInQueue) {
            queueStatusRef.child(selectedService).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Boolean isQueueOpen = snapshot.getValue(Boolean.class);
                        if (isQueueOpen != null && isQueueOpen) {
                            long timestamp = System.currentTimeMillis();
                            String ticketNumber = generateTicketNumber();

                            Map<String, Object> userQueueData = new HashMap<>();
                            userQueueData.put("selectedService", selectedService);
                            userQueueData.put("timestamp", timestamp);
                            userQueueData.put("ticketNumber", ticketNumber);
                            userQueueData.put("isServing", false);
                            userQueueData.put("isEmergency", true);
                            userQueueData.put("joinType", "priority");
                            userQueueData.put("joinTime", System.currentTimeMillis());
                            userQueueData.put("priorityVerified", true);

                            usersRef.child(currentUserId).child("QueueChoices").setValue(userQueueData)
                                    .addOnCompleteListener(userTask -> {
                                        if (userTask.isSuccessful()) {
                                            Map<String, Object> queueData = new HashMap<>();
                                            queueData.put("timestamp", timestamp);
                                            queueData.put("ticketNumber", ticketNumber);
                                            queueData.put("isServing", false);
                                            queueData.put("isEmergency", true);
                                            queueData.put("joinType", "priority");
                                            queueData.put("joinTime", System.currentTimeMillis());
                                            queueData.put("emergencyJoinTime", System.currentTimeMillis());
                                            queueData.put("priorityVerified", true);
                                            queueData.put("userId", currentUserId);

                                            placeUserInPriorityPosition(queueData, ticketNumber);
                                        } else {
                                            Toast.makeText(QueueActivity.this, "Failed to save queue choice", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(QueueActivity.this, "Queue is closed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(QueueActivity.this, "Queue is closed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("QueueStatus", "Error checking queue status: " + error.getMessage());
                    Toast.makeText(QueueActivity.this, "Error checking queue status", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // NEW: Place user in priority position
    private void placeUserInPriorityPosition(Map<String, Object> queueData, String ticketNumber) {
        queueRef.child(selectedService).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> emergencyUserIds = new ArrayList<>();
                List<String> regularUserIds = new ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Boolean isEmergency = userSnapshot.child("isEmergency").getValue(Boolean.class);
                    if (isEmergency != null && isEmergency) {
                        emergencyUserIds.add(userSnapshot.getKey());
                    } else {
                        regularUserIds.add(userSnapshot.getKey());
                    }
                }

                queueRef.child(selectedService).child(currentUserId).setValue(queueData)
                        .addOnCompleteListener(queueTask -> {
                            if (queueTask.isSuccessful()) {
                                isUserInQueue = true;
                                isEmergencyUser = true;
                                updateButtons();
                                ticketNumberTextView.setText("Your Ticket Number is " + ticketNumber);
                                Toast.makeText(QueueActivity.this, "You have joined the queue as PRIORITY user", Toast.LENGTH_LONG).show();

                                QpointsActivity.addPointsToUser(currentUserId, 10, "Emergency join to " + selectedService + " queue");
                                logQueueJoinEvent("priority", selectedService, ticketNumber);
                            } else {
                                Toast.makeText(QueueActivity.this, "Failed to join the queue", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QueueActivity.this, "Error finding priority position", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // NEW: Log queue join event for admin tracking
    private void logQueueJoinEvent(String joinType, String service, String ticketNumber) {
        String joinEventId = usersRef.child(currentUserId).child("QueueHistory").push().getKey();
        Map<String, Object> joinEvent = new HashMap<>();
        joinEvent.put("joinType", joinType);
        joinEvent.put("service", service);
        joinEvent.put("ticketNumber", ticketNumber);
        joinEvent.put("timestamp", System.currentTimeMillis());
        joinEvent.put("userId", currentUserId);

        if (joinType.equals("priority")) {
            joinEvent.put("emergencyVerified", true);
        }

        usersRef.child(currentUserId).child("QueueHistory").child(joinEventId).setValue(joinEvent)
                .addOnSuccessListener(aVoid -> {
                    Log.d("QueueActivity", "Queue join event logged: " + joinType + " for " + service);
                })
                .addOnFailureListener(e -> {
                    Log.e("QueueActivity", "Failed to log queue join event", e);
                });
    }

    // 🔥 UPDATED: Listen for queue position changes with local notifications
    private void setupQueuePositionListener() {
        queuePositionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isUserInQueue) {
                    int currentPosition = getCurrentUserPosition();
                    checkAndCreatePositionNotification(currentPosition);

                    // 🔥 NEW: Show local notification for positions 1-3
                    if (currentPosition <= 3 && currentPosition > 0) {
                        showLocalNotificationForPosition(currentPosition);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("QueuePosition", "Error listening to queue position: " + error.getMessage());
            }
        };

        queueRef.child(selectedService).addValueEventListener(queuePositionListener);
    }

    // 🔥 NEW: Get current user position
    private int getCurrentUserPosition() {
        return queueUserIds.indexOf(currentUserId) + 1;
    }

    // 🔥 NEW: Check and create position notification
    private void checkAndCreatePositionNotification(int currentPosition) {
        if (currentPosition <= 3 && currentPosition > 0) {
            createPriorityQueueNotification(currentPosition);
        }
    }

    // 🔥 NEW: Create priority notification for positions 1-3
    private void createPriorityQueueNotification(int position) {
        notificationsRef.child(currentUserId).orderByChild("position").equalTo(position)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            String notificationId = notificationsRef.child(currentUserId).push().getKey();
                            String title = "";
                            String message = "";

                            switch (position) {
                                case 1:
                                    title = "🚀 You're Next!";
                                    message = "You are position 1 in " + selectedService + " queue. Get ready!";
                                    break;
                                case 2:
                                    title = "⏰ Almost There!";
                                    message = "You are position 2 in " + selectedService + " queue. Almost your turn!";
                                    break;
                                case 3:
                                    title = "📋 Getting Close!";
                                    message = "You are position 3 in " + selectedService + " queue. Your turn is coming up!";
                                    break;
                            }

                            Map<String, Object> notification = new HashMap<>();
                            notification.put("title", title);
                            notification.put("message", message);
                            notification.put("service", selectedService);
                            notification.put("position", position);
                            notification.put("timestamp", System.currentTimeMillis());
                            notification.put("read", false);
                            notification.put("type", "queue_priority");
                            notification.put("priority", "high");

                            notificationsRef.child(currentUserId).child(notificationId).setValue(notification);

                            if (position == 1) {
                                Toast.makeText(QueueActivity.this, "You're next in line! Get ready!", Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("QueueNotification", "Error checking existing notifications: " + error.getMessage());
                    }
                });
    }

    // Setup real-time listeners for queue status
    private void setupRealTimeListeners() {
        userQueueStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    if (isUserInQueue) {
                        checkIfAdminRemoval();
                    }
                } else {
                    String ticketNumber = snapshot.child("ticketNumber").getValue(String.class);
                    Boolean isEmergency = snapshot.child("isEmergency").getValue(Boolean.class);
                    String joinType = snapshot.child("joinType").getValue(String.class);
                    if (ticketNumber != null) {
                        ticketNumberTextView.setText("Ticket Number " + ticketNumber);
                    }
                    isEmergencyUser = isEmergency != null && isEmergency;

                    if (joinType != null) {
                        isEmergencyUser = "priority".equals(joinType);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("QueueActivity", "Error listening to user queue status: " + error.getMessage());
            }
        };

        queueRef.child(selectedService).child(currentUserId).addValueEventListener(userQueueStatusListener);

        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                    String type = notificationSnapshot.child("type").getValue(String.class);
                    Boolean read = notificationSnapshot.child("read").getValue(Boolean.class);

                    if ("removed_by_admin".equals(type) && (read == null || !read)) {
                        showRemovalNotification();
                        notificationSnapshot.getRef().child("read").setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("QueueActivity", "Error listening to notifications: " + error.getMessage());
            }
        };

        notificationsRef.child(currentUserId).orderByChild("type").equalTo("removed_by_admin")
                .addValueEventListener(notificationListener);
    }

    // Check if removal was by admin or voluntary
    private void checkIfAdminRemoval() {
        notificationsRef.child(currentUserId).orderByChild("type").equalTo("removed_by_admin")
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            handleAdminRemoval();
                        } else {
                            handleVoluntaryLeave();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handleVoluntaryLeave();
                    }
                });
    }

    // Handle voluntary leave
    private void handleVoluntaryLeave() {
        isUserInQueue = false;
        isSteppedAway = false;
        isEmergencyUser = false;

        usersRef.child(currentUserId).child("QueueChoices").removeValue();
        updateButtons();
        ticketNumberTextView.setText("");
        stopCountdownTimer();
        WaitingTimeTextView.setText("");

        Toast.makeText(this, "You have left the " + selectedService + " queue", Toast.LENGTH_SHORT).show();
    }

    // Handle admin removal from queue
    private void handleAdminRemoval() {
        isUserInQueue = false;
        isSteppedAway = false;
        isEmergencyUser = false;

        usersRef.child(currentUserId).child("QueueChoices").removeValue();
        updateButtons();
        ticketNumberTextView.setText("");
        stopCountdownTimer();
        WaitingTimeTextView.setText("");

        showRemovalNotification();
        Toast.makeText(this, "You have been removed from the queue by admin", Toast.LENGTH_LONG).show();
    }

    // Show removal notification
    private void showRemovalNotification() {
        new AlertDialog.Builder(this)
                .setTitle("Queue Update")
                .setMessage("You have been removed from the " + selectedService + " queue by administrator.")
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show();
    }

    private void makeImageViewCircular(ImageView imageView) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(0xFFFFFFFF);
        imageView.setBackground(shape);
        imageView.setClipToOutline(true);
    }

    private void loadProfileImage() {
        usersRef.child(currentUserId).child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String encodedImage = snapshot.getValue(String.class);
                    if (encodedImage != null && !encodedImage.isEmpty()) {
                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        profileImage.setImageBitmap(decodedBitmap);
                        profileInitial.setVisibility(View.GONE);
                    } else {
                        profileImage.setVisibility(View.GONE);
                        profileInitial.setVisibility(View.VISIBLE);
                        profileInitial.setText("U");
                    }
                } else {
                    profileImage.setVisibility(View.GONE);
                    profileInitial.setVisibility(View.VISIBLE);
                    profileInitial.setText("U");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    private void fetchUserData() {
        usersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("username").getValue(String.class);
                    if (name == null || name.isEmpty()) {
                        name = "User";
                    }
                    updateGreeting(name);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(QueueActivity.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateGreeting(String username) {
        int position = queueUserIds.indexOf(currentUserId) + 1;
        String positionText = (position > 0) ? String.valueOf(position) : "Not in queue";
        String emergencyText = isEmergencyUser ? " (PRIORITY)" : "";
        GreetingTextView.setText("Hello, " + username + "! You are in the " + selectedService + " queue. Your position: " + positionText + emergencyText);

        if (position > 0) {
            estimatedWaitTimeMillis = (position - 1) * 10 * 60 * 1000;
            startCountdownTimer(estimatedWaitTimeMillis);
        } else {
            stopCountdownTimer();
            WaitingTimeTextView.setText("");
        }
    }

    private void startCountdownTimer(long millisInFuture) {
        stopCountdownTimer();

        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateCountdownText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                WaitingTimeTextView.setText("Your turn is next!");
            }
        }.start();
    }

    private void stopCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void updateCountdownText(long millisUntilFinished) {
        long hours = millisUntilFinished / 1000 / 3600;
        long minutes = (millisUntilFinished / 1000 % 3600) / 60;
        long seconds = (millisUntilFinished / 1000) % 60;

        String timeText = String.format("⏱️ Estimated Wait Time: \n%02d:%02d:%02d", hours, minutes, seconds);

        Typeface robotoBold = ResourcesCompat.getFont(this, R.font.roboto);
        WaitingTimeTextView.setTypeface(robotoBold);
        WaitingTimeTextView.setText(timeText);

        if (millisUntilFinished < 5 * 60 * 1000) {
            WaitingTimeTextView.setTextColor(Color.RED);
            WaitingTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        } else if (millisUntilFinished < 15 * 60 * 1000) {
            WaitingTimeTextView.setTextColor(Color.parseColor("#FFA500"));
            WaitingTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        } else if (millisUntilFinished < 60 * 60 * 1000) {
            WaitingTimeTextView.setTextColor(Color.parseColor("#2196F3"));
            WaitingTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        } else {
            WaitingTimeTextView.setTextColor(Color.parseColor("#228B22"));
            WaitingTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }

        WaitingTimeTextView.setShadowLayer(4, 2, 2, Color.parseColor("#60000000"));
    }

    private void checkIfUserInQueue() {
        queueRef.child(selectedService).child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                isUserInQueue = dataSnapshot.exists();
                if (isUserInQueue) {
                    String ticketNumber = dataSnapshot.child("ticketNumber").getValue(String.class);
                    Boolean isEmergency = dataSnapshot.child("isEmergency").getValue(Boolean.class);
                    String joinType = dataSnapshot.child("joinType").getValue(String.class);
                    if (ticketNumber != null) {
                        ticketNumberTextView.setText("Ticket Number " + ticketNumber);
                    }
                    isEmergencyUser = (isEmergency != null && isEmergency) || "priority".equals(joinType);
                }
                updateButtons();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(QueueActivity.this, "Error checking queue status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateButtons() {
        JoinQueueButton.setVisibility(isUserInQueue ? View.GONE : View.VISIBLE);
        PriorityJoinButton.setVisibility(isUserInQueue ? View.GONE : View.VISIBLE);
        LeaveQueueButton.setVisibility(isUserInQueue && !isSteppedAway ? View.VISIBLE : View.GONE);
        StepAwayButton.setVisibility(isUserInQueue && !isSteppedAway ? View.VISIBLE : View.GONE);
        ReturnButton.setVisibility(isSteppedAway ? View.VISIBLE : View.GONE);
    }

    private void confirmStepAway() {
        new AlertDialog.Builder(this)
                .setTitle("Step Away")
                .setMessage("You can leave temporarily (max 15 mins). Your position will be held.")
                .setPositiveButton("Confirm", (d, w) -> stepAway())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void stepAway() {
        DatabaseReference userQueueRef = queueRef.child(selectedService).child(currentUserId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "stepped_away");
        updates.put("stepAwayTime", System.currentTimeMillis());

        userQueueRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isSteppedAway = true;
                updateButtons();
                Toast.makeText(this, "Your position is held for 15 mins", Toast.LENGTH_LONG).show();

                // 🔥 NEW: Show step away local notification
                showStepAwayLocalNotification();

                holdTimerHandler.postDelayed(this::checkHoldExpired, 15 * 60 * 1000);
            }
        });
    }

    private void returnFromStepAway() {
        DatabaseReference userQueueRef = queueRef.child(selectedService).child(currentUserId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "waiting");
        updates.put("stepAwayTime", 0L);

        userQueueRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isSteppedAway = false;
                updateButtons();
                holdTimerHandler.removeCallbacksAndMessages(null);
                Toast.makeText(this, "Welcome back! Your position is restored", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkHoldExpired() {
        queueRef.child(selectedService).child(currentUserId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && "stepped_away".equals(snapshot.child("status").getValue(String.class))) {
                            // 🔥 NEW: Show hold expired local notification before leaving queue
                            showHoldExpiredLocalNotification();

                            leaveQueue();
                            Toast.makeText(QueueActivity.this,
                                    "Your hold time expired. You've been removed from queue",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                }
        );
    }

    private void listenForQueueUpdates() {
        queueListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<QueueUser> emergencyUsers = new ArrayList<>();
                List<QueueUser> regularUsers = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userId = snapshot.getKey();
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                    Boolean isEmergency = snapshot.child("isEmergency").getValue(Boolean.class);
                    String joinType = snapshot.child("joinType").getValue(String.class);

                    if (timestamp != null) {
                        boolean isPriorityUser = (isEmergency != null && isEmergency) || "priority".equals(joinType);
                        QueueUser queueUser = new QueueUser(userId, timestamp, isPriorityUser);
                        if (queueUser.isEmergency) {
                            emergencyUsers.add(queueUser);
                        } else {
                            regularUsers.add(queueUser);
                        }
                    }
                }

                emergencyUsers.sort((u1, u2) -> Long.compare(u1.timestamp, u2.timestamp));
                regularUsers.sort((u1, u2) -> Long.compare(u1.timestamp, u2.timestamp));

                queueUserIds.clear();
                for (QueueUser user : emergencyUsers) {
                    queueUserIds.add(user.userId);
                }
                for (QueueUser user : regularUsers) {
                    queueUserIds.add(user.userId);
                }

                displayQueue();
                fetchUserData();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(QueueActivity.this, "Failed to load queue", Toast.LENGTH_SHORT).show();
            }
        };

        queueRef.child(selectedService).addValueEventListener(queueListener);

        queueStatusRef.child(selectedService).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isQueueOpen = snapshot.getValue(Boolean.class);
                    if (isQueueOpen != null) {
                        String currentQueueText = isQueueOpen ? "Current Queue (is open)" : "Current Queue (is closed)";
                        currentQueueTextView.setText(currentQueueText);
                    }
                } else {
                    currentQueueTextView.setText("Current Queue (is closed)");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("QueueStatus", "Error checking queue status: " + error.getMessage());
                currentQueueTextView.setText("Current Queue (Error)");
            }
        });
    }

    // Helper class for sorting queue users
    private static class QueueUser {
        String userId;
        long timestamp;
        boolean isEmergency;

        QueueUser(String userId, long timestamp, boolean isEmergency) {
            this.userId = userId;
            this.timestamp = timestamp;
            this.isEmergency = isEmergency;
        }
    }

    private void displayQueue() {
        QueueContainer.removeAllViews();
        if (queueUserIds.isEmpty()) {
            EmptyQueueTextView.setVisibility(View.VISIBLE);
        } else {
            EmptyQueueTextView.setVisibility(View.GONE);
            for (int i = 0; i < queueUserIds.size(); i++) {
                String userId = queueUserIds.get(i);
                fetchAndDisplayUser(userId, i + 1);
            }
        }
    }

    private void fetchAndDisplayUser(String userId, int position) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("username").getValue(String.class);
                    if (name == null || name.isEmpty()) {
                        name = "?";
                    }
                    String firstLetter = String.valueOf(name.charAt(0)).toUpperCase();

                    String finalName = name;
                    queueRef.child(selectedService).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userQueueSnapshot) {
                            Boolean isEmergency = userQueueSnapshot.child("isEmergency").getValue(Boolean.class);
                            String joinType = userQueueSnapshot.child("joinType").getValue(String.class);

                            boolean isEmergencyUser = (isEmergency != null && isEmergency) || "priority".equals(joinType);

                            LinearLayout parentLayout = new LinearLayout(QueueActivity.this);
                            parentLayout.setOrientation(LinearLayout.VERTICAL);
                            parentLayout.setGravity(Gravity.CENTER);

                            LinearLayout userLayout = new LinearLayout(QueueActivity.this);
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(150, 150);
                            layoutParams.setMargins(10, 10, 10, 10);
                            userLayout.setLayoutParams(layoutParams);
                            userLayout.setGravity(Gravity.CENTER);

                            if (isEmergencyUser) {
                                userLayout.setBackgroundResource(userId.equals(currentUserId) ?
                                        R.drawable.circle_emergency_current_user : R.drawable.circle_emergency_user);
                            } else {
                                userLayout.setBackgroundResource(userId.equals(currentUserId) ?
                                        R.drawable.circle_current_user : R.drawable.circle_other_user);
                            }

                            TextView userView = new TextView(QueueActivity.this);
                            userView.setText(firstLetter);
                            userView.setTextSize(32);
                            userView.setTextColor(isEmergencyUser ? Color.WHITE : Color.BLUE);
                            userView.setTypeface(null, Typeface.BOLD);
                            userView.setGravity(Gravity.CENTER);

                            userLayout.addView(userView);
                            parentLayout.addView(userLayout);

                            TextView statusTextView = new TextView(QueueActivity.this);
                            statusTextView.setTextSize(14);
                            statusTextView.setTypeface(null, Typeface.BOLD);
                            statusTextView.setGravity(Gravity.CENTER);

                            if (position == 1) {
                                statusTextView.setText("Being\nserved");
                                statusTextView.setTextColor(Color.RED);
                            } else if (position == 2) {
                                statusTextView.setText("Next");
                                statusTextView.setTextColor(Color.GREEN);
                            } else {
                                statusTextView.setText("Waiting");
                                statusTextView.setTextColor(Color.GRAY);
                            }

                            if (isEmergencyUser) {
                                TextView emergencyTextView = new TextView(QueueActivity.this);
                                emergencyTextView.setText("🚨 PRIORITY");
                                emergencyTextView.setTextSize(10);
                                emergencyTextView.setTextColor(Color.RED);
                                emergencyTextView.setTypeface(null, Typeface.BOLD);
                                emergencyTextView.setGravity(Gravity.CENTER);
                                parentLayout.addView(emergencyTextView);
                            }

                            parentLayout.addView(statusTextView);
                            QueueContainer.addView(parentLayout);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            displayRegularUser(userId, position, firstLetter, finalName);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(QueueActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fallback method to display regular user
    private void displayRegularUser(String userId, int position, String firstLetter, String name) {
        LinearLayout parentLayout = new LinearLayout(QueueActivity.this);
        parentLayout.setOrientation(LinearLayout.VERTICAL);
        parentLayout.setGravity(Gravity.CENTER);

        LinearLayout userLayout = new LinearLayout(QueueActivity.this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(150, 150);
        layoutParams.setMargins(10, 10, 10, 10);
        userLayout.setLayoutParams(layoutParams);
        userLayout.setGravity(Gravity.CENTER);
        userLayout.setBackgroundResource(userId.equals(currentUserId) ?
                R.drawable.circle_current_user : R.drawable.circle_other_user);

        TextView userView = new TextView(QueueActivity.this);
        userView.setText(firstLetter);
        userView.setTextSize(32);
        userView.setTextColor(Color.BLUE);
        userView.setTypeface(null, Typeface.BOLD);
        userView.setGravity(Gravity.CENTER);

        userLayout.addView(userView);
        parentLayout.addView(userLayout);

        TextView statusTextView = new TextView(QueueActivity.this);
        statusTextView.setTextSize(14);
        statusTextView.setTypeface(null, Typeface.BOLD);
        statusTextView.setGravity(Gravity.CENTER);

        if (position == 1) {
            statusTextView.setText("Being\nserved");
            statusTextView.setTextColor(Color.RED);
        } else if (position == 2) {
            statusTextView.setText("Next");
            statusTextView.setTextColor(Color.GREEN);
        } else {
            statusTextView.setText("Waiting");
            statusTextView.setTextColor(Color.GRAY);
        }

        parentLayout.addView(statusTextView);
        QueueContainer.addView(parentLayout);
    }

    private void joinQueue() {
        if (!isUserInQueue) {
            queueStatusRef.child(selectedService).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Boolean isQueueOpen = snapshot.getValue(Boolean.class);
                        if (isQueueOpen != null && isQueueOpen) {
                            long timestamp = System.currentTimeMillis();
                            String ticketNumber = generateTicketNumber();

                            Map<String, Object> userQueueData = new HashMap<>();
                            userQueueData.put("selectedService", selectedService);
                            userQueueData.put("timestamp", timestamp);
                            userQueueData.put("ticketNumber", ticketNumber);
                            userQueueData.put("isServing", false);
                            userQueueData.put("isEmergency", false);
                            userQueueData.put("joinType", "normal");
                            userQueueData.put("joinTime", System.currentTimeMillis());

                            usersRef.child(currentUserId).child("QueueChoices").setValue(userQueueData)
                                    .addOnCompleteListener(userTask -> {
                                        if (userTask.isSuccessful()) {
                                            Map<String, Object> queueData = new HashMap<>();
                                            queueData.put("timestamp", timestamp);
                                            queueData.put("ticketNumber", ticketNumber);
                                            queueData.put("isServing", false);
                                            queueData.put("isEmergency", false);
                                            queueData.put("joinType", "normal");
                                            queueData.put("joinTime", System.currentTimeMillis());
                                            queueData.put("userId", currentUserId);

                                            queueRef.child(selectedService).child(currentUserId).setValue(queueData)
                                                    .addOnCompleteListener(queueTask -> {
                                                        if (queueTask.isSuccessful()) {
                                                            isUserInQueue = true;
                                                            isEmergencyUser = false;
                                                            updateButtons();
                                                            ticketNumberTextView.setText("Your Ticket Number is " + ticketNumber);
                                                            Toast.makeText(QueueActivity.this, "You have joined the queue", Toast.LENGTH_SHORT).show();

                                                            QpointsActivity.addPointsToUser(currentUserId, 5, "Joined " + selectedService + " queue");
                                                            logQueueJoinEvent("normal", selectedService, ticketNumber);
                                                        } else {
                                                            Toast.makeText(QueueActivity.this, "Failed to join the queue", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(QueueActivity.this, "Failed to save queue choice", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(QueueActivity.this, "Queue is closed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(QueueActivity.this, "Queue is closed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("QueueStatus", "Error checking queue status: " + error.getMessage());
                    Toast.makeText(QueueActivity.this, "Error checking queue status", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String generateTicketNumber() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void confirmLeaveQueue() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Queue")
                .setMessage("Are you sure you want to leave the queue?")
                .setPositiveButton("Yes", (dialog, which) -> leaveQueue())
                .setNegativeButton("No", null)
                .show();
    }

    private void leaveQueue() {
        final boolean isVoluntaryLeave = true;

        queueRef.child(selectedService).child(currentUserId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                usersRef.child(currentUserId).child("QueueChoices").removeValue()
                        .addOnCompleteListener(userTask -> {
                            isUserInQueue = false;
                            isSteppedAway = false;
                            isEmergencyUser = false;
                            updateButtons();
                            ticketNumberTextView.setText("");
                            stopCountdownTimer();
                            WaitingTimeTextView.setText("");

                            Toast.makeText(QueueActivity.this, "You have left the " + selectedService + " queue", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();

        if (fcmToken.isEmpty()) {
            initializeFCMToken();
        }
    }

    private void updateNotificationBadge() {
        if (notificationsRef != null && currentUserId != null) {
            notificationsRef.child(currentUserId).orderByChild("read").equalTo(false)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            int unreadCount = (int) snapshot.getChildrenCount();

                            runOnUiThread(() -> {
                                if (bottomNavigationView != null) {
                                    View notificationItem = bottomNavigationView.findViewById(R.id.notification);
                                    if (notificationItem != null) {
                                        View existingDot = notificationItem.findViewWithTag("red_dot");
                                        if (existingDot != null) {
                                            ((ViewGroup) notificationItem).removeView(existingDot);
                                        }

                                        if (unreadCount > 0) {
                                            View redDot = new View(QueueActivity.this);
                                            redDot.setTag("red_dot");
                                            redDot.setBackgroundResource(R.drawable.red_dot);
                                            int size = (int) getResources().getDimension(R.dimen.notification_dot_size);
                                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                                            params.setMargins(90, 17, 0, 0);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queueListener != null) {
            queueRef.child(selectedService).removeEventListener(queueListener);
        }
        if (userQueueStatusListener != null) {
            queueRef.child(selectedService).child(currentUserId).removeEventListener(userQueueStatusListener);
        }
        if (notificationListener != null) {
            notificationsRef.child(currentUserId).removeEventListener(notificationListener);
        }
        if (queuePositionListener != null) {
            queueRef.child(selectedService).removeEventListener(queuePositionListener);
        }
        holdTimerHandler.removeCallbacksAndMessages(null);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}