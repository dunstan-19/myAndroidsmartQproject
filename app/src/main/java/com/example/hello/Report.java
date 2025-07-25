package com.example.hello;

public class Report {
    private String userId;
    private String name;
    private String ticketNumber;
    private long timestamp;
    private String department;
    private String action;

    // Default constructor required for Firebase
    public Report() {}

    public Report(String userId, String name, String ticketNumber, long timestamp, String department, String action) {
        this.userId = userId;
        this.name = name;
        this.ticketNumber = ticketNumber;
        this.timestamp = timestamp;
        this.department = department;
        this.action = action;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getTicketNumber() { return ticketNumber; }
    public long getTimestamp() { return timestamp; }
    public String getDepartment() { return department; }
    public String getAction() { return action; }
}