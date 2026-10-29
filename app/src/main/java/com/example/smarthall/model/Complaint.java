package com.example.smarthall.model;

public class Complaint {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESOLVED = "RESOLVED";

    private String id;
    private String studentId;
    private String studentName;
    private String roomNo;
    private String title;
    private String description;
    private String date;
    private String status;

    public Complaint(String id, String studentId, String studentName, String roomNo,
                     String title, String description, String date, String status) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.roomNo = roomNo;
        this.title = title;
        this.description = description;
        this.date = date;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isResolved() {
        return STATUS_RESOLVED.equals(status);
    }
}
