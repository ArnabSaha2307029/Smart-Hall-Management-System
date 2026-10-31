package com.example.smarthall.model;

public class FeeRecord {
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_UNPAID = "UNPAID";

    private String id;
    private String studentId;
    private String roll;
    private String month;
    private double diningFee;
    private double hallFee;
    private double totalAmount;
    private double amount;
    private String dueDate;
    private String status;
    private long paidAt;
    private String trxId;

    public FeeRecord() {
    }

    public FeeRecord(String id, String studentId, String month, double amount,
                     String dueDate, String status) {
        this.id = id;
        this.studentId = studentId;
        this.roll = studentId;
        this.month = month;
        this.amount = amount;
        this.totalAmount = amount;
        this.dueDate = dueDate;
        this.status = status;
        this.diningFee = amount * 0.67;
        this.hallFee = amount - this.diningFee;
    }

    public FeeRecord(String id, String roll, String month, double diningFee,
                     double hallFee, double totalAmount, String dueDate,
                     String status, long paidAt, String trxId) {
        this.id = id;
        this.studentId = roll;
        this.roll = roll;
        this.month = month;
        this.diningFee = diningFee;
        this.hallFee = hallFee;
        this.totalAmount = totalAmount;
        this.amount = totalAmount;
        this.dueDate = dueDate;
        this.status = status;
        this.paidAt = paidAt;
        this.trxId = trxId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return roll != null && !roll.isEmpty() ? roll : studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
        if (this.roll == null || this.roll.isEmpty()) {
            this.roll = studentId;
        }
    }

    public String getRoll() {
        return roll != null && !roll.isEmpty() ? roll : studentId;
    }

    public void setRoll(String roll) {
        this.roll = roll;
        if (this.studentId == null || this.studentId.isEmpty()) {
            this.studentId = roll;
        }
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public double getDiningFee() {
        return diningFee;
    }

    public void setDiningFee(double diningFee) {
        this.diningFee = diningFee;
    }

    public double getHallFee() {
        return hallFee;
    }

    public void setHallFee(double hallFee) {
        this.hallFee = hallFee;
    }

    public double getTotalAmount() {
        return totalAmount > 0 ? totalAmount : amount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
        this.amount = totalAmount;
    }

    public double getAmount() {
        return totalAmount > 0 ? totalAmount : amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
        if (this.totalAmount == 0) {
            this.totalAmount = amount;
        }
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(long paidAt) {
        this.paidAt = paidAt;
    }

    public String getTrxId() {
        return trxId;
    }

    public void setTrxId(String trxId) {
        this.trxId = trxId;
    }

    public boolean isPaid() {
        return STATUS_PAID.equalsIgnoreCase(status);
    }
}
