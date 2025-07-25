package com.example.hello;

import java.util.List;

public class QueueUser {
    private String userId;
    private String name;
    private List<String> departments;
    private long joinTime;
    private String ticketNumber;
    private String status;

    public QueueUser(String userId, String name, List<String> departments, long joinTime, String ticketNumber, String status) {
        this.userId = userId;
        this.name = name;
        this.departments = departments;
        this.joinTime = joinTime;
        this.ticketNumber = ticketNumber;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public List<String> getDepartments() {
        return departments;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}