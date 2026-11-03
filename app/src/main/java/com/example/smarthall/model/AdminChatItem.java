package com.example.smarthall.model;

public class AdminChatItem {

    private String studentRoll;
    private String lastMessage;
    private long timestamp;
    private int unreadCount;

    public AdminChatItem() {
    }

    public AdminChatItem(String studentRoll, String lastMessage, long timestamp) {
        this.studentRoll = studentRoll;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public AdminChatItem(String studentRoll, String lastMessage, long timestamp, int unreadCount) {
        this.studentRoll = studentRoll;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
    }

    public String getStudentRoll() {
        return studentRoll;
    }

    public void setStudentRoll(String studentRoll) {
        this.studentRoll = studentRoll;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
