package com.example.smarthall.model;

import com.google.firebase.database.Exclude;

public class Complaint {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_RESOLVED = "resolved";

    private String complaintId;
    private String title;
    private String description;
    private String studentRoll;
    private String studentPhone;
    private String status = STATUS_PENDING;
    private long timestamp;

    private String studentName;
    private String roomNo;
    private String date;

    public Complaint() {
        this.status = STATUS_PENDING;
    }

    public Complaint(String complaintId, String title, String description, String studentRoll, String studentPhone, String status, long timestamp) {
        this.complaintId = complaintId;
        this.title = title;
        this.description = description;
        this.studentRoll = studentRoll;
        this.studentPhone = studentPhone;
        this.status = status != null ? status : STATUS_PENDING;
        this.timestamp = timestamp;
    }

    public Complaint(String id, String studentId, String studentName, String roomNo,
                     String title, String description, String date, String status) {
        this.complaintId = id;
        this.studentRoll = studentId;
        this.studentName = studentName;
        this.roomNo = roomNo;
        this.title = title;
        this.description = description;
        this.date = date;
        this.status = status != null ? status.toLowerCase() : STATUS_PENDING;
        this.timestamp = System.currentTimeMillis();
    }

    public String getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(String complaintId) {
        this.complaintId = complaintId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStudentRoll() {
        return studentRoll;
    }

    public void setStudentRoll(String studentRoll) {
        this.studentRoll = studentRoll;
    }

    public String getStudentPhone() {
        return studentPhone;
    }

    public void setStudentPhone(String studentPhone) {
        this.studentPhone = studentPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Exclude
    public String getId() {
        return complaintId;
    }

    @Exclude
    public void setId(String id) {
        this.complaintId = id;
    }

    @Exclude
    public String getStudentId() {
        return studentRoll != null ? studentRoll : "";
    }

    @Exclude
    public void setStudentId(String studentId) {
        this.studentRoll = studentId;
    }

    @Exclude
    public String getStudentName() {
        return studentName != null ? studentName : (studentRoll != null ? "Student " + studentRoll : "");
    }

    @Exclude
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    @Exclude
    public String getRoomNo() {
        return roomNo != null ? roomNo : "";
    }

    @Exclude
    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    @Exclude
    public String getDate() {
        return date != null ? date : "";
    }

    @Exclude
    public void setDate(String date) {
        this.date = date;
    }

    @Exclude
    public boolean isPending() {
        return STATUS_PENDING.equalsIgnoreCase(status);
    }

    @Exclude
    public boolean isResolved() {
        return STATUS_RESOLVED.equalsIgnoreCase(status);
    }
}
