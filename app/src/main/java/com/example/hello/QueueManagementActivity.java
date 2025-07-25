package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QueueManagementActivity extends AppCompatActivity {
    private TextView queueStatusTextView, departmentNameTextView, noUsersTextView,
            userCountTextView, peakHoursTextView;
    private Button openQueueButton, closeQueueButton;
    private DatabaseReference queueRef, usersRef, queueStatusRef, peakHoursRef;
    private String department;
    private ImageView reporticon, closeSearchIcon,click;
    private boolean isQueueOpen;

    private RecyclerView usersRecyclerView;
    private QueueUserAdapter queueUserAdapter;
    private List<QueueUser> queueUsers;
    private List<QueueUser> filteredQueueUsers;
    private EditText searchInput;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_queue_management);

        // Initialize views
        initializeViews();

        department = getIntent().getStringExtra("department");
        if (department == null || department.isEmpty()) {
            Toast.makeText(this, "Department not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        departmentNameTextView.setText(department + " Department");

        // Initialize Firebase references
        initializeFirebaseReferences();

        // Initialize RecyclerView
        initializeRecyclerView();

        // Set up listeners
        setupListeners();

        // Load initial data
        checkQueueStatus();
        fetchUsersInQueue();
        loadPeakHours();
    }

    private void initializeViews() {
        queueStatusTextView = findViewById(R.id.queueStatusTextView);
        departmentNameTextView = findViewById(R.id.departmentNameTextView);
        userCountTextView = findViewById(R.id.userCountTextView);
        peakHoursTextView = findViewById(R.id.peakHoursTextView);
        openQueueButton = findViewById(R.id.openQueueButton);
        closeQueueButton = findViewById(R.id.closeQueueButton);
        reporticon = findViewById(R.id.reporticon);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        noUsersTextView = findViewById(R.id.noUsersTextView);
        searchInput = findViewById(R.id.searchInput);
        click = findViewById(R.id.click);
        closeSearchIcon = findViewById(R.id.closeSearchIcon);
    }

    private void initializeFirebaseReferences() {
        queueRef = FirebaseDatabase.getInstance().getReference("Queue").child(department);
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        queueStatusRef = FirebaseDatabase.getInstance().getReference("QueueStatus").child(department);
        peakHoursRef = FirebaseDatabase.getInstance().getReference("PeakHours").child(department);
    }

    private void initializeRecyclerView() {
        queueUsers = new ArrayList<>();
        filteredQueueUsers = new ArrayList<>();
        queueUserAdapter = new QueueUserAdapter(queueUsers, department);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(queueUserAdapter);
    }

    private void setupListeners() {
        openQueueButton.setOnClickListener(v -> updateQueueStatus(true));
        closeQueueButton.setOnClickListener(v -> updateQueueStatus(false));

        reporticon.setOnClickListener(v -> {
            Intent intent = new Intent(QueueManagementActivity.this, ReportActivity.class);
            intent.putExtra("department", department);
            startActivity(intent);
        });

        click.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                searchUserInQueue(query);
                closeSearchIcon.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Please enter a ticket number or username", Toast.LENGTH_SHORT).show();
            }
        });

        closeSearchIcon.setOnClickListener(v -> closeSearch());
    }

    private void checkQueueStatus() {
        queueStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isQueueOpen = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("QueueStatus", "Error loading queue status: " + error.getMessage());
                Toast.makeText(QueueManagementActivity.this, "Error loading queue status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateQueueStatus(boolean status) {
        queueStatusRef.setValue(status).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isQueueOpen = status;
                updateUI();
                Toast.makeText(this, "Queue " + (status ? "Opened" : "Closed"), Toast.LENGTH_SHORT).show();
                if (!status) removeUsersFromQueue();
            } else {
                Log.e("QueueStatus", "Failed to update queue status: " + task.getException().getMessage());
                Toast.makeText(this, "Failed to update queue status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeUsersFromQueue() {
        queueRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("RemoveUsers", "All users removed from the queue");
                Toast.makeText(this, "All users removed from the queue", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("RemoveUsers", "Error removing users: " + task.getException().getMessage());
            }
        });
    }

    private void loadPeakHours() {
        peakHoursRef.orderByChild("count").limitToLast(3).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder peakHoursInfo = new StringBuilder("Peak Hours:\n");

                if (!snapshot.exists()) {
                    peakHoursInfo.append("No data available yet");
                    peakHoursTextView.setText(peakHoursInfo);
                    return;
                }

                List<Map.Entry<String, Long>> hoursList = new ArrayList<>();
                for (DataSnapshot hourSnapshot : snapshot.getChildren()) {
                    String hour = hourSnapshot.getKey();
                    Long count = hourSnapshot.child("count").getValue(Long.class);
                    if (hour != null && count != null) {
                        hoursList.add(new HashMap.SimpleEntry<>(hour, count));
                    }
                }

                // Sort by count descending
                hoursList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

                if (hoursList.isEmpty()) {
                    peakHoursInfo.append("No data available yet");
                } else {
                    for (int i = 0; i < Math.min(hoursList.size(), 3); i++) {
                        Map.Entry<String, Long> entry = hoursList.get(i);
                        peakHoursInfo.append(String.format(Locale.getDefault(),
                                "%s: %d users\n", entry.getKey(), entry.getValue()));
                    }
                }

                peakHoursTextView.setText(peakHoursInfo.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PeakHours", "Error loading peak hours: " + error.getMessage());
                peakHoursTextView.setText("Error loading peak hours data");
            }
        });
    }

    private void recordQueueEntry(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String hourKey = String.format(Locale.getDefault(), "%02d:00-%02d:00", hour, hour + 1);

        peakHoursRef.child(hourKey).child("count").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.exists() ? snapshot.getValue(Long.class) : 0;
                peakHoursRef.child(hourKey).child("count").setValue(count + 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PeakHours", "Failed to record queue entry: " + error.getMessage());
            }
        });
    }

    private void fetchUsersInQueue() {
        queueRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<QueueUser> tempQueueUsers = new ArrayList<>();
                if (!snapshot.exists()) {
                    queueUsers.clear();
                    queueUserAdapter.notifyDataSetChanged();
                    noUsersTextView.setVisibility(View.VISIBLE);
                    usersRecyclerView.setVisibility(View.GONE);
                    userCountTextView.setText("0 users in queue");
                    return;
                }

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    Long timestamp = userSnapshot.child("timestamp").getValue(Long.class);
                    String ticketNumber = userSnapshot.child("ticketNumber").getValue(String.class);
                    String status = userSnapshot.child("status").getValue(String.class);

                    if (userId != null && timestamp != null && ticketNumber != null) {
                        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userDataSnapshot) {
                                String name = userDataSnapshot.child("username").getValue(String.class);
                                if (name != null) {
                                    tempQueueUsers.add(new QueueUser(
                                            userId,
                                            name,
                                            Collections.singletonList(department),
                                            timestamp,
                                            ticketNumber,
                                            status != null ? status : "waiting"
                                    ));

                                    Collections.sort(tempQueueUsers, Comparator.comparingLong(QueueUser::getJoinTime));

                                    queueUsers.clear();
                                    queueUsers.addAll(tempQueueUsers);
                                    queueUserAdapter.notifyDataSetChanged();
                                    updateQueueUI();

                                    int userCount = queueUsers.size();
                                    userCountTextView.setText(userCount + (userCount == 1 ? " user in queue" : " users in queue"));

                                    // Record queue entry for peak hour analysis
                                    recordQueueEntry(timestamp);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("FetchUsers", "Error fetching user: " + error.getMessage());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FetchUsers", "Error loading queue: " + error.getMessage());
                Toast.makeText(QueueManagementActivity.this, "Error loading queue", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchUserInQueue(String query) {
        filteredQueueUsers.clear();
        for (QueueUser user : queueUsers) {
            if (user.getName().toLowerCase().contains(query.toLowerCase()) ||
                    user.getTicketNumber().equalsIgnoreCase(query)) {
                filteredQueueUsers.add(user);
            }
        }

        if (filteredQueueUsers.isEmpty()) {
            Toast.makeText(this, "No matching users found", Toast.LENGTH_SHORT).show();
        } else {
            queueUserAdapter = new QueueUserAdapter(filteredQueueUsers, department);
            usersRecyclerView.setAdapter(queueUserAdapter);
            userCountTextView.setText(filteredQueueUsers.size() + (filteredQueueUsers.size() == 1 ? " matching user" : " matching users"));
        }
    }

    private void closeSearch() {
        searchInput.setText("");
        closeSearchIcon.setVisibility(View.GONE);
        queueUserAdapter = new QueueUserAdapter(queueUsers, department);
        usersRecyclerView.setAdapter(queueUserAdapter);
        userCountTextView.setText(queueUsers.size() + (queueUsers.size() == 1 ? " user in queue" : " users in queue"));
    }

    private void updateQueueUI() {
        noUsersTextView.setVisibility(queueUsers.isEmpty() ? View.VISIBLE : View.GONE);
        usersRecyclerView.setVisibility(queueUsers.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateUI() {
        queueStatusTextView.setText(isQueueOpen ? "Queue is Open" : "Queue is Closed");
        openQueueButton.setVisibility(isQueueOpen ? View.GONE : View.VISIBLE);
        closeQueueButton.setVisibility(isQueueOpen ? View.VISIBLE : View.GONE);
    }
}