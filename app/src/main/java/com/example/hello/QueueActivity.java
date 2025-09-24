package com.example.hello;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
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
    private String currentUserId;
    private Button JoinQueueButton;
    private Button LeaveQueueButton;
    private Button StepAwayButton;
    private Button ReturnButton;
    private TextView GreetingTextView;
    private TextView EmptyQueueTextView;
    private TextView currentQueueTextView;
    private TextView ticketNumberTextView;
    private TextView WaitingTimeTextView;
    private boolean isUserInQueue = false;
    private boolean isSteppedAway = false;
    private String selectedService;
    private List<String> queueUserIds = new ArrayList<>();
    private CountDownTimer countDownTimer;
    private long estimatedWaitTimeMillis = 0;
    private BottomNavigationView bottomNavigationView;
    private ImageView profileImage;
    private TextView profileInitial;
    private Handler holdTimerHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

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
        GreetingTextView = findViewById(R.id.GreetingTextView);
        EmptyQueueTextView = findViewById(R.id.EmptyQueueTextView);
        currentQueueTextView = findViewById(R.id.TextView);
        ticketNumberTextView = findViewById(R.id.ticketNumberTextView);
        WaitingTimeTextView = findViewById(R.id.WaitingTimeTextView);

        // Initialize Firebase references
        queueRef = FirebaseDatabase.getInstance().getReference("Queue");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        queueStatusRef = FirebaseDatabase.getInstance().getReference("QueueStatus");

        // Set click listeners
        profileImage.setOnClickListener(v -> {
            startActivity(new Intent(QueueActivity.this, ProfileActivity.class));
        });

        selectedService = getIntent().getStringExtra("selectedService");

        // Setup bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            fetchUserData();
            checkIfUserInQueue();
        }

        listenForQueueUpdates();

        JoinQueueButton.setOnClickListener(v -> joinQueue());
        LeaveQueueButton.setOnClickListener(v -> confirmLeaveQueue());
        StepAwayButton.setOnClickListener(v -> confirmStepAway());
        ReturnButton.setOnClickListener(v -> returnFromStepAway());
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
                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        profileImage.setImageBitmap(decodedBitmap);
                        profileInitial.setVisibility(View.GONE);
                    } else {
                        profileImage.setVisibility(View.GONE);
                        profileInitial.setVisibility(View.VISIBLE);
                        profileInitial.setText("U"); // Set initial if no image is available
                    }
                } else {
                    profileImage.setVisibility(View.GONE);
                    profileInitial.setVisibility(View.VISIBLE);
                    profileInitial.setText("U"); // Set initial if no image is available
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
        GreetingTextView.setText("Hello, " + username + "! You are in the " + selectedService + " queue. Your position: " + positionText);

        // Calculate and display waiting time
        if (position > 0) {
            estimatedWaitTimeMillis = (position - 1) * 10 * 60 * 1000; // 10 minutes per user in milliseconds
            startCountdownTimer(estimatedWaitTimeMillis);
        } else {
            stopCountdownTimer();
            WaitingTimeTextView.setText("");
        }
    }

    private void startCountdownTimer(long millisInFuture) {
        stopCountdownTimer(); // Stop any existing timer

        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update the countdown every second
                updateCountdownText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                // When the countdown finishes
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

        // Always display hours with leading zeros
        String timeText = String.format("⏱️ Estimated Wait Time: \n%02d:%02d:%02d", hours, minutes, seconds);

        // Apply Roboto font from res/font folder
        Typeface robotoRegular = ResourcesCompat.getFont(this, R.font.roboto);
        Typeface robotoBold = ResourcesCompat.getFont(this, R.font.roboto);

        WaitingTimeTextView.setTypeface(robotoBold);
        WaitingTimeTextView.setText(timeText);

        // Increased font sizes and dynamic coloring
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

        // Enhanced shadow
        WaitingTimeTextView.setShadowLayer(4, 2, 2, Color.parseColor("#60000000"));
    }

    private void checkIfUserInQueue() {
        queueRef.child(selectedService).child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                isUserInQueue = dataSnapshot.exists();
                if (isUserInQueue) {
                    String ticketNumber = dataSnapshot.child("ticketNumber").getValue(String.class);
                    if (ticketNumber != null) {
                        ticketNumberTextView.setText("Ticket Number " + ticketNumber);
                    }
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

                // Schedule expiration check
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
        queueRef.child(selectedService).orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                queueUserIds.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    queueUserIds.add(snapshot.getKey());
                }
                displayQueue();
                fetchUserData();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(QueueActivity.this, "Failed to load queue", Toast.LENGTH_SHORT).show();
            }
        });

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

                    // Create a parent layout to hold the user initial and status text
                    LinearLayout parentLayout = new LinearLayout(QueueActivity.this);
                    parentLayout.setOrientation(LinearLayout.VERTICAL);
                    parentLayout.setGravity(Gravity.CENTER);

                    // Create a layout to hold the circular background for the user initial
                    LinearLayout userLayout = new LinearLayout(QueueActivity.this);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(150, 150);
                    layoutParams.setMargins(10, 10, 10, 10);
                    userLayout.setLayoutParams(layoutParams);
                    userLayout.setGravity(Gravity.CENTER);
                    userLayout.setBackgroundResource(userId.equals(currentUserId) ?
                            R.drawable.circle_current_user : R.drawable.circle_other_user);

                    // Create the TextView for the user's initial
                    TextView userView = new TextView(QueueActivity.this);
                    userView.setText(firstLetter);
                    userView.setTextSize(32);
                    userView.setTextColor(Color.BLUE);
                    userView.setTypeface(null, Typeface.BOLD);
                    userView.setGravity(Gravity.CENTER);

                    // Add the TextView to the layout
                    userLayout.addView(userView);

                    // Add the user layout to the parent layout
                    parentLayout.addView(userLayout);

                    // Add status text based on the user's position in the queue
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

                    // Add the status text to the parent layout
                    parentLayout.addView(statusTextView);

                    // Add the parent layout to the queue container
                    QueueContainer.addView(parentLayout);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(QueueActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
            }
        });
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

                            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    String fcmToken = task.getResult();

                                    Map<String, Object> userQueueData = new HashMap<>();
                                    userQueueData.put("selectedService", selectedService);
                                    userQueueData.put("timestamp", timestamp);
                                    userQueueData.put("ticketNumber", ticketNumber);
                                    userQueueData.put("fcmToken", fcmToken);
                                    userQueueData.put("isServing", false); // Default to not being served

                                    usersRef.child(currentUserId).child("QueueChoices").setValue(userQueueData)
                                            .addOnCompleteListener(userTask -> {
                                                if (userTask.isSuccessful()) {
                                                    Map<String, Object> queueData = new HashMap<>();
                                                    queueData.put("timestamp", timestamp);
                                                    queueData.put("ticketNumber", ticketNumber);
                                                    queueData.put("fcmToken", fcmToken);
                                                    queueData.put("isServing", false); // Default to not being served

                                                    queueRef.child(selectedService).child(currentUserId).setValue(queueData)
                                                            .addOnCompleteListener(queueTask -> {
                                                                if (queueTask.isSuccessful()) {
                                                                    isUserInQueue = true;
                                                                    updateButtons();
                                                                    ticketNumberTextView.setText("Your Ticket Number is " + ticketNumber);
                                                                    Toast.makeText(QueueActivity.this, "You have joined the queue", Toast.LENGTH_SHORT).show();
                                                                } else {
                                                                    Toast.makeText(QueueActivity.this, "Failed to join the queue", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                } else {
                                                    Toast.makeText(QueueActivity.this, "Failed to save queue choice", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    Toast.makeText(QueueActivity.this, "Failed to get FCM token", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.actionlogout) {
            showLogoutConfirmationDialog();
            return true;
        } else if (item.getItemId() == R.id.actionhelp) {
            startActivity(new Intent(this, helpActivity.class));
            return true;
        } else if (item.getItemId() == R.id.actionsettings) {
            startActivity(new Intent(this, Settings.class));
            return true;
        } else if (item.getItemId() == R.id.actionprofile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void leaveQueue() {
        queueRef.child(selectedService).child(currentUserId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isUserInQueue = false;
                updateButtons();
                ticketNumberTextView.setText("");
                stopCountdownTimer();
                WaitingTimeTextView.setText("");
                Toast.makeText(QueueActivity.this, "You have left the queue", Toast.LENGTH_SHORT).show();
            }
        });
    }
        @Override
        protected void onDestroy() {
            super.onDestroy();
            holdTimerHandler.removeCallbacksAndMessages(null);
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
        }
    }
