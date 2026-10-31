package com.example.smarthall.model;

import java.io.Serializable;

public class PaymentRecord implements Serializable {

    private String trxId;
    private String roll;
    private double amount;
    private String method;
    private String accountNumber;
    private String feeRecordId;
    private long paidAt;
    private String status;

    public PaymentRecord() {
    }

    public PaymentRecord(String trxId, String roll, double amount, String method,
                         String accountNumber, String feeRecordId, long paidAt, String status) {
        this.trxId = trxId;
        this.roll = roll;
        this.amount = amount;
        this.method = method;
        this.accountNumber = accountNumber;
        this.feeRecordId = feeRecordId;
        this.paidAt = paidAt;
        this.status = status;
    }

    public String getTrxId() {
        return trxId;
    }

    public void setTrxId(String trxId) {
        this.trxId = trxId;
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getFeeRecordId() {
        return feeRecordId;
    }

    public void setFeeRecordId(String feeRecordId) {
        this.feeRecordId = feeRecordId;
    }

    public long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(long paidAt) {
        this.paidAt = paidAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
