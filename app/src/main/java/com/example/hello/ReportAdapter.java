package com.example.hello;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
    private List<Report> reportList;
    public ReportAdapter(List<Report> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.userNameTextView.setText("User: " + report.getName());
        holder.ticketNumberTextView.setText("Ticket Number: " + report.getTicketNumber());
        holder.actionTextView.setText("Action: " + report.getAction());

        // Format timestamp to a readable date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String dateTime = sdf.format(new Date(report.getTimestamp()));
        holder.timestampTextView.setText("Date: " + dateTime);
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView, ticketNumberTextView, actionTextView, timestampTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            ticketNumberTextView = itemView.findViewById(R.id.ticketNumberTextView);
            actionTextView = itemView.findViewById(R.id.actionTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}