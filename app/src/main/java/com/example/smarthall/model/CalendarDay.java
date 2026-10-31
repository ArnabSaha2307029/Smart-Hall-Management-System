package com.example.smarthall.model;

public class CalendarDay {
    private int dayNumber;
    private String dateString;
    private String status;
    private boolean isCurrentMonth;

    public CalendarDay() {
    }

    public CalendarDay(int dayNumber, String dateString, String status, boolean isCurrentMonth) {
        this.dayNumber = dayNumber;
        this.dateString = dateString;
        this.status = status;
        this.isCurrentMonth = isCurrentMonth;
    }

    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }

    public String getDateString() { return dateString; }
    public void setDateString(String dateString) { this.dateString = dateString; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isCurrentMonth() { return isCurrentMonth; }
    public void setCurrentMonth(boolean currentMonth) { isCurrentMonth = currentMonth; }
}
