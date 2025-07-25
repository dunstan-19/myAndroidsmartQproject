package com.example.hello;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {
    private RecyclerView reportRecyclerView;
    private ReportAdapter reportAdapter;
    private DatabaseReference reportsRef;
    private String department;
    private TextView noReportsTextView, departmentReportTitle;
    private Button clearReportsButton, downloadReportsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_report);

        // Get the department from the intent
        department = getIntent().getStringExtra("department");
        if (department == null || department.isEmpty()) {
            Toast.makeText(this, "Department not found", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if the department is missing
            return;
        }

        // Initialize views
        reportRecyclerView = findViewById(R.id.reportRecyclerView);
        noReportsTextView = findViewById(R.id.noReportsTextView);
        departmentReportTitle = findViewById(R.id.departmentReportTitle);
        clearReportsButton = findViewById(R.id.clearReportsButton);
        downloadReportsButton = findViewById(R.id.downloadReportsButton);

        // Set the department name in the title
        departmentReportTitle.setText(department + " Report");

        // Initialize RecyclerView
        reportRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Firebase
        reportsRef = FirebaseDatabase.getInstance().getReference("Reports");

        // Fetch and display reports
        fetchReports();

        // Handle "Clear All" button click
        clearReportsButton.setOnClickListener(v -> showClearReportsConfirmationDialog());

        // Handle "Download Reports" button click
        downloadReportsButton.setOnClickListener(v -> downloadReports());
    }

    private void fetchReports() {
        // Query reports for the specific department
        reportsRef.orderByChild("department").equalTo(department).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Report> reportList = new ArrayList<>();
                for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                    Report report = reportSnapshot.getValue(Report.class);
                    if (report != null) {
                        reportList.add(report);
                    }
                }

                // Update UI based on report list size
                if (reportList.isEmpty()) {
                    noReportsTextView.setVisibility(View.VISIBLE);
                    reportRecyclerView.setVisibility(View.GONE);
                } else {
                    noReportsTextView.setVisibility(View.GONE);
                    reportRecyclerView.setVisibility(View.VISIBLE);
                }

                // Set up the adapter
                reportAdapter = new ReportAdapter(reportList);
                reportRecyclerView.setAdapter(reportAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FetchReports", "Error loading reports: " + error.getMessage());
                Toast.makeText(ReportActivity.this, "Error loading reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows a confirmation dialog to clear all reports.
     */
    private void showClearReportsConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Reports")
                .setMessage("Are you sure you want to clear all reports? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> clearAllReports())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Clears all reports for the current department.
     */
    private void clearAllReports() {
        reportsRef.orderByChild("department").equalTo(department).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                    reportSnapshot.getRef().removeValue(); // Remove each report
                }
                Toast.makeText(ReportActivity.this, "All reports cleared.", Toast.LENGTH_SHORT).show();
                fetchReports(); // Refresh the report list
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ClearReports", "Error clearing reports: " + error.getMessage());
                Toast.makeText(ReportActivity.this, "Failed to clear reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Downloads the reports as a CSV file.
     */
    private void downloadReports() {
        reportsRef.orderByChild("department").equalTo(department).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Report> reportList = new ArrayList<>();
                for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                    Report report = reportSnapshot.getValue(Report.class);
                    if (report != null) {
                        reportList.add(report);
                    }
                }

                if (reportList.isEmpty()) {
                    Toast.makeText(ReportActivity.this, "No reports to download.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Generate CSV file
                File csvFile = generateCsvFile(reportList);
                if (csvFile != null) {
                    // Trigger the download
                    triggerDownload(csvFile);
                } else {
                    Toast.makeText(ReportActivity.this, "Failed to generate report file.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DownloadReports", "Error fetching reports: " + error.getMessage());
                Toast.makeText(ReportActivity.this, "Error fetching reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Generates a CSV file from the report list.
     */
    private File generateCsvFile(List<Report> reportList) {
        File file = null;
        FileWriter writer = null;
        try {
            // Create a file in the Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = department + "_Report_" + System.currentTimeMillis() + ".csv";
            file = new File(downloadsDir, fileName);

            writer = new FileWriter(file);

            // Write the heading (Department Name + Report)
            writer.append(department).append(" Report\n\n");

            // Write CSV header
            writer.append("User ID,Name,Ticket Number,Action,Timestamp\n");

            // Write report data
            for (Report report : reportList) {
                writer.append(report.getUserId()).append(",");
                writer.append(report.getName()).append(",");
                writer.append(report.getTicketNumber()).append(",");
                writer.append(report.getAction()).append(",");
                writer.append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(report.getTimestamp()))).append("\n");
            }

            writer.flush();
            Toast.makeText(this, "Report saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("GenerateCsvFile", "Error generating CSV file: " + e.getMessage());
            Toast.makeText(this, "Error generating CSV file", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                Log.e("GenerateCsvFile", "Error closing writer: " + e.getMessage());
            }
        }
        return file;
    }

    /**
     * Triggers the download of the file using Android's DownloadManager.
     */
    private void triggerDownload(File file) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri fileUri = Uri.fromFile(file);

        DownloadManager.Request request = new DownloadManager.Request(fileUri)
                .setTitle(department + " Report")
                .setDescription("Downloading report file")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.getName());

        downloadManager.enqueue(request);
    }
}