package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminActivity extends AppCompatActivity {
    private RecyclerView departmentRecyclerView;
    private DepartmentAdapter departmentAdapter;
    private List<String> departmentsList;
    private DatabaseReference usersRef;
    private ProgressBar progressBar;
    private TextView emptyQueueText;
    private ImageView logouticon;

    private static final List<String> DEFAULT_DEPARTMENTS = List.of(
             "Deposit Cash", "Withdraw Cash", "Check Balance", "Other"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_admin);

        // Initialize views
        logouticon = findViewById(R.id.logouticon);
        departmentRecyclerView = findViewById(R.id.departmentRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyQueueText = findViewById(R.id.emptyQueueText);

        // Set up RecyclerView
        departmentRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter and list
        departmentsList = new ArrayList<>();
        departmentAdapter = new DepartmentAdapter(departmentsList, this);
        departmentRecyclerView.setAdapter(departmentAdapter);

        // Initialize Firebase references
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadDepartments();
        logouticon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadDepartments() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> uniqueDepartments = new HashSet<>(DEFAULT_DEPARTMENTS);

                for (DataSnapshot user : snapshot.getChildren()) {
                    if (user.hasChild("QueueChoices") && user.child("QueueChoices").hasChild("selectedService")) {
                        String serviceName = user.child("QueueChoices").child("selectedService").getValue(String.class);
                        if (serviceName != null) {
                            uniqueDepartments.add(serviceName);
                        }
                    }
                }

                departmentsList.clear();
                departmentsList.addAll(uniqueDepartments);
                departmentAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Error loading departments", Toast.LENGTH_SHORT).show();
            }
        });
    }
}