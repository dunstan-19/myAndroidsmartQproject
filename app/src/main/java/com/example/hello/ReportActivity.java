package com.example.hello;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 1001;
    private static final int MANAGE_STORAGE_PERMISSION_CODE = 1002;

    private RecyclerView reportRecyclerView;
    private ReportAdapter reportAdapter;
    private DatabaseReference reportsRef;
    private String department;
    private TextView noReportsTextView, departmentReportTitle;
    private Button clearReportsButton, downloadReportsButton;
    private List<Report> currentReportList = new ArrayList<>();

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
            finish();
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
        downloadReportsButton.setOnClickListener(v -> checkAndRequestPermissions());
    }

    private void fetchReports() {
        reportsRef.orderByChild("department").equalTo(department).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentReportList.clear();
                for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                    Report report = reportSnapshot.getValue(Report.class);
                    if (report != null) {
                        currentReportList.add(report);
                    }
                }

                if (currentReportList.isEmpty()) {
                    noReportsTextView.setVisibility(View.VISIBLE);
                    reportRecyclerView.setVisibility(View.GONE);
                } else {
                    noReportsTextView.setVisibility(View.GONE);
                    reportRecyclerView.setVisibility(View.VISIBLE);
                }

                reportAdapter = new ReportAdapter(currentReportList);
                reportRecyclerView.setAdapter(reportAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FetchReports", "Error loading reports: " + error.getMessage());
                Toast.makeText(ReportActivity.this, "Error loading reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showClearReportsConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Reports")
                .setMessage("Are you sure you want to clear all reports? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> clearAllReports())
                .setNegativeButton("No", null)
                .show();
    }

    private void clearAllReports() {
        reportsRef.orderByChild("department").equalTo(department).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                    reportSnapshot.getRef().removeValue();
                }
                Toast.makeText(ReportActivity.this, "All reports cleared.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ClearReports", "Error clearing reports: " + error.getMessage());
                Toast.makeText(ReportActivity.this, "Failed to clear reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires MANAGE_EXTERNAL_STORAGE permission
            if (Environment.isExternalStorageManager()) {
                downloadReports();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_CODE);
            }
        } else {
            // For older versions, request WRITE_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                downloadReports();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadReports();
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot download reports.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                downloadReports();
            } else {
                Toast.makeText(this, "Storage access denied. Cannot download reports.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadReports() {
        if (currentReportList.isEmpty()) {
            Toast.makeText(this, "No reports to download.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File csvFile = generateCsvFile(currentReportList);
            if (csvFile != null && csvFile.exists()) {
                triggerDownload(csvFile);
            } else {
                Toast.makeText(this, "Failed to generate report file.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("DownloadReports", "Error downloading reports: " + e.getMessage());
            Toast.makeText(this, "Error downloading reports: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File generateCsvFile(List<Report> reportList) throws IOException {
        File file = null;
        OutputStreamWriter writer = null;

        try {
            // Use getExternalFilesDir for better compatibility
            File directory = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = department + "_Report_" + System.currentTimeMillis() + ".csv";
            file = new File(directory, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            writer = new OutputStreamWriter(fos);

            // Write CSV header
            writer.append("User ID,Name,Ticket Number,Action,Timestamp,Department\n");

            // Write report data
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            for (Report report : reportList) {
                writer.append(escapeCsv(report.getUserId())).append(",");
                writer.append(escapeCsv(report.getName())).append(",");
                writer.append(escapeCsv(report.getTicketNumber())).append(",");
                writer.append(escapeCsv(report.getAction())).append(",");
                writer.append(escapeCsv(sdf.format(new Date(report.getTimestamp())))).append(",");
                writer.append(escapeCsv(report.getDepartment())).append("\n");
            }

            writer.flush();
            Log.d("GenerateCsvFile", "Report saved to: " + file.getAbsolutePath());

        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.e("GenerateCsvFile", "Error closing writer: " + e.getMessage());
                }
            }
        }
        return file;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Escape commas and quotes in CSV
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void triggerDownload(File file) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri fileUri = Uri.fromFile(file);

            DownloadManager.Request request = new DownloadManager.Request(fileUri)
                    .setTitle(department + " Report")
                    .setDescription("Downloading report file")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setMimeType("text/csv")
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.getName());

            downloadManager.enqueue(request);
            Toast.makeText(this, "Download started. Check notifications for status.", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("TriggerDownload", "Error triggering download: " + e.getMessage());
            Toast.makeText(this, "Error starting download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}