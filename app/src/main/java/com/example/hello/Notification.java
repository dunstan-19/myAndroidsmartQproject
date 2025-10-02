package com.example.hello;

public class Notification {
    private String title;
    private String message;
    private String service;
    private int position;
    private long timestamp;
    private boolean read;
    private String type;

    public Notification() {
        // Default constructor required for Firebase
    }

    public Notification(String title, String message, String service, int position, long timestamp, boolean read, String type) {
        this.title = title;
        this.message = message;
        this.service = service;
        this.position = position;
        this.timestamp = timestamp;
        this.read = read;
        this.type = type;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}