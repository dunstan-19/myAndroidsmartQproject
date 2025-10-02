package com.example.hello;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_choose);

        // Initialize profile image and initial
        profileImage = findViewById(R.id.profileImage);
        profileInitial = findViewById(R.id.profileInitial);
        makeImageViewCircular(profileImage);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getCurrentUser().getUid());
        queueRef = FirebaseDatabase.getInstance().getReference("ServiceQueues");
        notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications");

        loadProfileImage();

        // Set click listener for profile image to open ProfileActivity
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(chooseActivity.this, ProfileActivity.class));
            }
        });

        // Also set click listener for profile initial in case image is not available
        profileInitial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(chooseActivity.this, ProfileActivity.class));
            }
        });

        // Initialize card views
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

        btnDone.setOnClickListener(v -> {
            if (selectedService != null) {
                saveToDatabase(selectedService);
            } else {
                Toast.makeText(chooseActivity.this, "Please select a service", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        // Check for new notifications
        checkForNotifications();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                // Already on home, do nothing
                return true;
            } else if (itemId == R.id.help) {
                startActivity(new Intent(chooseActivity.this, helpActivity.class));
                return true;
            } else if (itemId == R.id.settings) {
                startActivity(new Intent(chooseActivity.this, Settings.class));
                return true;
            } else if (itemId == R.id.logout) {
                showLogoutConfirmationDialog();
                return true;
            } else if (itemId == R.id.notification) {
                startActivity(new Intent(chooseActivity.this, NotificationsActivity.class));
                return true;
            }
            return false;
        });

        // Set the default selected item
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
                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        profileImage.setImageBitmap(decodedBitmap);
                        profileInitial.setVisibility(View.GONE);
                    } else {
                        profileImage.setVisibility(View.GONE);
                        profileInitial.setVisibility(View.VISIBLE);
                        profileInitial.setText("🤭"); // Set initial if no image is available
                    }
                } else {
                    profileImage.setVisibility(View.GONE);
                    profileInitial.setVisibility(View.VISIBLE);
                    profileInitial.setText("🤭"); // Set initial if no image is available
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
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

        usersRef.child("QueueChoices").setValue(userChoices)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String serviceKey = selectedService.replace(" ", "_");
                        queueRef.child(serviceKey).child(userId).setValue(timestamp)
                                .addOnCompleteListener(queueTask -> {
                                    if (queueTask.isSuccessful()) {
                                        // Get user position in queue and create notification
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
                    // Create initial notification for the user
                    createQueueNotification(userId, selectedService, position, "joined");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showConfirmationDialog(selectedService, -1);
            }
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

        notificationsRef.child(userId).child(notificationId).setValue(notification);
    }

    private void checkForNotifications() {
        String userId = mAuth.getCurrentUser().getUid();
        notificationsRef.child(userId).orderByChild("read").equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                                Notification notification = notificationSnapshot.getValue(Notification.class);
                                if (notification != null && !notification.isRead()) {
                                    showNotificationToast(notification);
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

    private void showNotificationToast(Notification notification) {
        String message = notification.getMessage();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure the correct item is selected when returning to this activity
        bottomNavigationView.setSelectedItemId(R.id.home);
        // Refresh profile image when returning to activity
        loadProfileImage();
        // Check for new notifications
        checkForNotifications();
    }
}