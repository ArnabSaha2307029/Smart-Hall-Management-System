package com.example.smarthall.model;

public class ChatMessage {

    private String messageId;
    private String senderRole;
    private String message;
    private long timestamp;
    private boolean read;

    public ChatMessage() {
    }

    public ChatMessage(String messageId, String senderRole, String message, long timestamp) {
        this.messageId = messageId;
        this.senderRole = senderRole;
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
    }

    public ChatMessage(String messageId, String senderRole, String message, long timestamp, boolean read) {
        this.messageId = messageId;
        this.senderRole = senderRole;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
