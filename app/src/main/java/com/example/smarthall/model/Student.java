package com.example.smarthall.model;

public class Student {
    private String id;
    private String name;
    private String rollNumber;
    private String department;
    private String phone;
    private String email;
    private String allocatedRoom;
    private String allocatedSeat;
    private String feeStatus;
    private String password;

    public Student(String id, String name, String rollNumber, String department,
                   String phone, String email, String allocatedRoom,
                   String allocatedSeat, String feeStatus, String password) {
        this.id = id;
        this.name = name;
        this.rollNumber = rollNumber;
        this.department = department;
        this.phone = phone;
        this.email = email;
        this.allocatedRoom = allocatedRoom;
        this.allocatedSeat = allocatedSeat;
        this.feeStatus = feeStatus;
        this.password = password;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAllocatedRoom() { return allocatedRoom; }
    public void setAllocatedRoom(String allocatedRoom) { this.allocatedRoom = allocatedRoom; }

    public String getAllocatedSeat() { return allocatedSeat; }
    public void setAllocatedSeat(String allocatedSeat) { this.allocatedSeat = allocatedSeat; }

    public String getFeeStatus() { return feeStatus; }
    public void setFeeStatus(String feeStatus) { this.feeStatus = feeStatus; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isAssigned() {
        return allocatedRoom != null && !allocatedRoom.isEmpty()
                && !allocatedRoom.equalsIgnoreCase("Unassigned")
                && allocatedSeat != null && !allocatedSeat.isEmpty()
                && !allocatedSeat.equalsIgnoreCase("Unassigned");
    }

    @Override
    public String toString() {
        return name + " (" + rollNumber + ")";
    }
}
