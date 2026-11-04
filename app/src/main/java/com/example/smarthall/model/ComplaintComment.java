package com.example.smarthall.model;

public class ComplaintComment {

    private String commentId;
    private String senderRoll;
    private String senderRole;
    private String text;
    private long timestamp;

    public ComplaintComment() {
    }

    public ComplaintComment(String commentId, String senderRoll, String senderRole, String text, long timestamp) {
        this.commentId = commentId;
        this.senderRoll = senderRoll;
        this.senderRole = senderRole;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getSenderRoll() {
        return senderRoll;
    }

    public void setSenderRoll(String senderRoll) {
        this.senderRoll = senderRoll;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}