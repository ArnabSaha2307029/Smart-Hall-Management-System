package com.example.smarthall.repository;

import com.example.smarthall.model.Complaint;
import com.example.smarthall.model.FeeRecord;
import com.example.smarthall.model.Notice;
import com.example.smarthall.model.Room;
import com.example.smarthall.model.Student;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HallRepository {

    private static volatile HallRepository instance;

    public static HallRepository getInstance() {
        if (instance == null) {
            synchronized (HallRepository.class) {
                if (instance == null) {
                    instance = new HallRepository();
                }
            }
        }
        return instance;
    }

    private final Map<String, Student> studentMap = new HashMap<>();
    private final Map<String, Room> roomMap = new HashMap<>();
    private final List<FeeRecord> feeRecords = new ArrayList<>();
    private final List<Complaint> complaints = new ArrayList<>();
    private final List<Notice> notices = new ArrayList<>();
    private int complaintCounter = 0;
    private int noticeCounter = 0;

    private static final String ADMIN_ID = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private HallRepository() {
        seedRooms();
        seedStudents();
        seedFeeRecords();
        seedComplaints();
        complaintCounter = complaints.size();
        noticeCounter = notices.size();
    }

    private void seedRooms() {
        String[] roomNums = {"101", "102", "103"};
        for (int i = 0; i < roomNums.length; i++) {
            Room r = new Room(roomNums[i], 1);
            roomMap.put(roomNums[i], r);
        }
    }

    private void seedStudents() {
        Student s1 = new Student("S001", "Arnab Saha", "2307029", "CSE",
                "+8801712345678", "arnab@university.edu",
                "101", "A", FeeRecord.STATUS_UNPAID, "pass123", "1001", "East");
        studentMap.put(s1.getId(), s1);
        roomMap.get("101").occupySeat(Room.SEAT_A, s1.getId());

        Student s2 = new Student("S002", "Rakib Hasan", "2307045", "EEE",
                "+8801812345679", "rakib@university.edu",
                "101", "B", FeeRecord.STATUS_UNPAID, "pass456", "1002", "West");
        studentMap.put(s2.getId(), s2);
        roomMap.get("101").occupySeat(Room.SEAT_B, s2.getId());

        Student s3 = new Student("S003", "Fatima Akter", "2307062", "ME",
                "+8801912345680", "fatima@university.edu",
                "102", "A", FeeRecord.STATUS_PAID, "pass789", "1003", "East");
        studentMap.put(s3.getId(), s3);
        roomMap.get("102").occupySeat(Room.SEAT_A, s3.getId());

        Student s4 = new Student("S004", "Mehedi Islam", "2307088", "CE",
                "+8801612345681", "mehedi@university.edu",
                "102", "C", FeeRecord.STATUS_UNPAID, "pass000", "1004", "West");
        studentMap.put(s4.getId(), s4);
        roomMap.get("102").occupySeat(Room.SEAT_C, s4.getId());
    }

    private void seedFeeRecords() {
        String[][] studentIds = {{"S001"}, {"S002"}, {"S003"}, {"S004"}};
        String[] months = {"August 2026", "September 2026", "October 2026"};
        String[] dueDates = {"31 Aug 2026", "30 Sep 2026", "31 Oct 2026"};
        String[][] statuses = {
            {FeeRecord.STATUS_PAID, FeeRecord.STATUS_PAID, FeeRecord.STATUS_UNPAID},
            {FeeRecord.STATUS_UNPAID, FeeRecord.STATUS_UNPAID, FeeRecord.STATUS_UNPAID},
            {FeeRecord.STATUS_PAID, FeeRecord.STATUS_PAID, FeeRecord.STATUS_PAID},
            {FeeRecord.STATUS_PAID, FeeRecord.STATUS_UNPAID, FeeRecord.STATUS_UNPAID}
        };
        int recordIndex = 1;
        for (int si = 0; si < studentIds.length; si++) {
            String sId = studentIds[si][0];
            for (int m = 0; m < months.length; m++) {
                FeeRecord fr = new FeeRecord(
                        "FR" + String.format("%03d", recordIndex++),
                        sId, months[m], 2500.0, dueDates[m], statuses[si][m]);
                feeRecords.add(fr);
            }
        }
    }

    private void seedComplaints() {
    }

    public synchronized String authenticateUser(String userId, String password, String role) {
        if ("ADMIN".equals(role)) {
            if (ADMIN_ID.equals(userId) && ADMIN_PASSWORD.equals(password)) {
                return "ADMIN";
            }
            return null;
        } else if ("STUDENT".equals(role)) {
            for (Student s : studentMap.values()) {
                if (s.getRollNumber().equals(userId) && s.getPassword().equals(password)) {
                    return s.getId();
                }
            }
            return null;
        }
        return null;
    }

    public synchronized List<Student> getStudents() {
        return new ArrayList<>(studentMap.values());
    }

    public synchronized Student getStudentById(String studentId) {
        return studentMap.get(studentId);
    }

    public synchronized Student getStudentByRoll(String rollNumber) {
        for (Student s : studentMap.values()) {
            if (s.getRollNumber().equals(rollNumber)) return s;
        }
        return null;
    }

    public synchronized List<Room> getRooms() {
        return new ArrayList<>(roomMap.values());
    }

    public synchronized Room getRoomByNumber(String roomNumber) {
        return roomMap.get(roomNumber);
    }

    public synchronized boolean allocateSeat(String studentId, String newRoomNumber, String newSeat) {
        Student student = studentMap.get(studentId);
        if (student == null) return false;
        Room newRoom = roomMap.get(newRoomNumber);
        if (newRoom == null) return false;

        String currentOccupant = newRoom.getOccupantId(newSeat);
        if (!currentOccupant.isEmpty() && !currentOccupant.equals(studentId)) {
            return false;
        }

        if (student.getAllocatedRoom() != null && !student.getAllocatedRoom().equalsIgnoreCase("Unassigned")) {
            Room oldRoom = roomMap.get(student.getAllocatedRoom());
            if (oldRoom != null && student.getAllocatedSeat() != null
                    && !student.getAllocatedSeat().equalsIgnoreCase("Unassigned")) {
                oldRoom.freeSeat(student.getAllocatedSeat());
            }
        }

        newRoom.occupySeat(newSeat, studentId);
        student.setAllocatedRoom(newRoomNumber);
        student.setAllocatedSeat(newSeat);
        return true;
    }

    public synchronized List<FeeRecord> getFeeRecordsByStudent(String studentId) {
        List<FeeRecord> result = new ArrayList<>();
        for (FeeRecord fr : feeRecords) {
            if (fr.getStudentId().equals(studentId)) result.add(fr);
        }
        return result;
    }

    public synchronized void updateFeeStatus(String feeRecordId, String newStatus) {
        for (FeeRecord fr : feeRecords) {
            if (fr.getId().equals(feeRecordId)) {
                fr.setStatus(newStatus);
                updateStudentFeeStatus(fr.getStudentId());
                break;
            }
        }
    }

    private void updateStudentFeeStatus(String studentId) {
        List<FeeRecord> records = getFeeRecordsByStudent(studentId);
        boolean allPaid = true;
        for (FeeRecord fr : records) {
            if (FeeRecord.STATUS_UNPAID.equals(fr.getStatus())) {
                allPaid = false;
                break;
            }
        }
        Student s = studentMap.get(studentId);
        if (s != null) {
            s.setFeeStatus(allPaid ? FeeRecord.STATUS_PAID : FeeRecord.STATUS_UNPAID);
        }
    }

    public synchronized List<Complaint> getAllComplaints() {
        return new ArrayList<>(complaints);
    }

    public synchronized List<Complaint> getComplaintsByStudent(String studentId) {
        List<Complaint> result = new ArrayList<>();
        for (Complaint c : complaints) {
            if (c.getStudentId().equals(studentId)) result.add(c);
        }
        return result;
    }

    public synchronized void addComplaint(Complaint complaint) {
        complaints.add(complaint);
    }

    public synchronized void toggleComplaintStatus(String complaintId) {
        for (Complaint c : complaints) {
            if (c.getId().equals(complaintId)) {
                if (Complaint.STATUS_PENDING.equals(c.getStatus())) {
                    c.setStatus(Complaint.STATUS_RESOLVED);
                } else {
                    c.setStatus(Complaint.STATUS_PENDING);
                }
                break;
            }
        }
    }

    public synchronized List<Notice> getNotices() {
        return new ArrayList<>(notices);
    }

    public synchronized void addNotice(Notice notice) {
        notices.add(0, notice);
    }

    public synchronized String generateComplaintId() {
        complaintCounter++;
        return "C" + String.format("%03d", complaintCounter);
    }

    public synchronized String generateNoticeId() {
        noticeCounter++;
        return "N" + String.format("%03d", noticeCounter);
    }
}
