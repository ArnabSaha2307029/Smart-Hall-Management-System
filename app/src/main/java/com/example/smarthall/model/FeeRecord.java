package com.example.smarthall.model;

public class FeeRecord {
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_UNPAID = "UNPAID";

    private String id;
    private String studentId;
    private String month;
    private double amount;
    private String dueDate;
    private String status;

    public FeeRecord(String id, String studentId, String month, double amount,
                     String dueDate, String status) {
        this.id = id;
        this.studentId = studentId;
        this.month = month;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPaid() {
        return STATUS_PAID.equals(status);
    }
}
