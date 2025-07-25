package com.example.hello;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
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
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class chooseActivity extends AppCompatActivity {
    private LinearLayout cardCreateAccount, cardDepositCash, cardWithdrawCash, cardCheckBalance, cardHelpdesk, cardOther;
    private Button btnDone;
    private DatabaseReference usersRef, queueRef;
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
            } else if (itemId == R.id.logout) {
                showLogoutConfirmationDialog();
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
                                        showConfirmationDialog(selectedService);
                                    } else {
                                        Toast.makeText(chooseActivity.this, "Failed to join queue", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(chooseActivity.this, "Failed to save service", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showConfirmationDialog(final String selectedService) {
        String message = "You have selected:\n• " + selectedService + "\n\nDo you want to proceed?";

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

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure the correct item is selected when returning to this activity
        bottomNavigationView.setSelectedItemId(R.id.home);
        // Refresh profile image when returning to activity
        loadProfileImage();
    }
}