package com.example.smarthall.model;

public class Notice {
    private String id;
    private String title;
    private String message;
    private String datePosted;

    public Notice(String id, String title, String message, String datePosted) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.datePosted = datePosted;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDatePosted() { return datePosted; }
    public void setDatePosted(String datePosted) { this.datePosted = datePosted; }
}
