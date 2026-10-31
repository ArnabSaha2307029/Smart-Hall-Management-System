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
    private String boarderNo;
    private String dining;
    private String profileImageBase64;

    public Student() {
    }

    public Student(String id, String name, String rollNumber, String department,
                   String phone, String email, String allocatedRoom,
                   String allocatedSeat, String feeStatus, String password) {
        this(id, name, rollNumber, department, phone, email, allocatedRoom, allocatedSeat, feeStatus, password, "", "");
    }

    public Student(String id, String name, String rollNumber, String department,
                   String phone, String email, String allocatedRoom,
                   String allocatedSeat, String feeStatus, String password,
                   String boarderNo, String dining) {
        this(id, name, rollNumber, department, phone, email, allocatedRoom, allocatedSeat, feeStatus, password, boarderNo, dining, "");
    }

    public Student(String id, String name, String rollNumber, String department,
                   String phone, String email, String allocatedRoom,
                   String allocatedSeat, String feeStatus, String password,
                   String boarderNo, String dining, String profileImageBase64) {
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
        this.boarderNo = boarderNo;
        this.dining = dining;
        this.profileImageBase64 = profileImageBase64;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getRoll() { return rollNumber != null ? rollNumber : ""; }
    public void setRoll(String roll) { this.rollNumber = roll; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAllocatedRoom() { return allocatedRoom; }
    public void setAllocatedRoom(String allocatedRoom) { this.allocatedRoom = allocatedRoom; }

    public String getRoomNo() { return allocatedRoom != null ? allocatedRoom : ""; }
    public void setRoomNo(String roomNo) { this.allocatedRoom = roomNo; }

    public String getAllocatedSeat() { return allocatedSeat; }
    public void setAllocatedSeat(String allocatedSeat) { this.allocatedSeat = allocatedSeat; }

    public String getFeeStatus() { return feeStatus; }
    public void setFeeStatus(String feeStatus) { this.feeStatus = feeStatus; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBoarderNo() { return boarderNo; }
    public void setBoarderNo(String boarderNo) { this.boarderNo = boarderNo; }

    public String getDining() { return dining; }
    public void setDining(String dining) { this.dining = dining; }

    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }

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
