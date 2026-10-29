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

    // ---------- Singleton ----------
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

    // ---------- In-Memory Store ----------
    private final Map<String, Student> studentMap = new HashMap<>();
    private final Map<String, Room> roomMap = new HashMap<>();
    private final List<FeeRecord> feeRecords = new ArrayList<>();
    private final List<Complaint> complaints = new ArrayList<>();
    private final List<Notice> notices = new ArrayList<>();
    private int complaintCounter = 0;
    private int noticeCounter = 0;

    // Admin credentials
    private static final String ADMIN_ID = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    // ---------- Constructor ----------
    private HallRepository() {
        seedRooms();
        seedStudents();
        seedFeeRecords();
        seedComplaints();
        seedNotices();
        complaintCounter = complaints.size();
        noticeCounter = notices.size();
    }

    // ---------- Seed Data ----------
    private void seedRooms() {
        String[] roomNums = {"101", "102", "103"};
        for (int i = 0; i < roomNums.length; i++) {
            Room r = new Room(roomNums[i], 1);
            roomMap.put(roomNums[i], r);
        }
    }

    private void seedStudents() {
        // Student 1 - the demo student
        Student s1 = new Student("S001", "Arnab Saha", "2307029", "CSE",
                "+8801712345678", "arnab@university.edu",
                "101", "A", FeeRecord.STATUS_UNPAID, "pass123");
        studentMap.put(s1.getId(), s1);
        roomMap.get("101").occupySeat(Room.SEAT_A, s1.getId());

        // Student 2
        Student s2 = new Student("S002", "Rakib Hasan", "2307045", "EEE",
                "+8801812345679", "rakib@university.edu",
                "101", "B", FeeRecord.STATUS_UNPAID, "pass456");
        studentMap.put(s2.getId(), s2);
        roomMap.get("101").occupySeat(Room.SEAT_B, s2.getId());

        // Student 3
        Student s3 = new Student("S003", "Fatima Akter", "2307062", "ME",
                "+8801912345680", "fatima@university.edu",
                "102", "A", FeeRecord.STATUS_PAID, "pass789");
        studentMap.put(s3.getId(), s3);
        roomMap.get("102").occupySeat(Room.SEAT_A, s3.getId());

        // Student 4
        Student s4 = new Student("S004", "Mehedi Islam", "2307088", "CE",
                "+8801612345681", "mehedi@university.edu",
                "102", "C", FeeRecord.STATUS_UNPAID, "pass000");
        studentMap.put(s4.getId(), s4);
        roomMap.get("102").occupySeat(Room.SEAT_C, s4.getId());
    }

    private void seedFeeRecords() {
        // Fee records for all students - 3 months each
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
        complaints.add(new Complaint("C001", "S001", "Arnab Saha", "101",
                "Water leakage in bathroom",
                "There is a persistent water leakage from the shower tap in Room 101 bathroom. It has been leaking for 3 days and is wasting water.",
                "13 Oct 2026", Complaint.STATUS_PENDING));

        complaints.add(new Complaint("C002", "S002", "Rakib Hasan", "101",
                "Broken ceiling fan",
                "The ceiling fan in Room 101 stopped working since last week. The weather is hot and it is very uncomfortable to study and sleep.",
                "11 Oct 2026", Complaint.STATUS_RESOLVED));

        complaints.add(new Complaint("C003", "S003", "Fatima Akter", "102",
                "Wi-Fi not working",
                "The hall Wi-Fi connection has been very unstable for Room 102. It disconnects every few minutes making it impossible to attend online classes.",
                "14 Oct 2026", Complaint.STATUS_PENDING));
    }

    private void seedNotices() {
        notices.add(new Notice("N001",
                "Hall Fee Payment Deadline Extended",
                "Dear residents of Amar Ekushey Hall, the October 2026 hall fee payment deadline has been extended to 20th October 2026. Please clear dues at the accounts office (Room G-02) between 9 AM - 4 PM on working days.",
                "10 Oct 2026"));

        notices.add(new Notice("N002",
                "Scheduled Maintenance - Water Supply",
                "Please be informed that water supply to all floors will be suspended on 17th October 2026 (Saturday) from 8:00 AM to 2:00 PM due to scheduled maintenance of the overhead tank. Residents are requested to store water beforehand.",
                "12 Oct 2026"));

        notices.add(new Notice("N003",
                "Amar Ekushey Hall Annual Sports Day",
                "The Amar Ekushey Hall Annual Sports Day will be held on 25th October 2026. We are looking for enthusiastic student volunteers to help organize events. Interested students may contact the Hall Provost Office by 18th October 2026.",
                "15 Oct 2026"));
    }

    // ---------- Auth ----------
    /**
     * Authenticates a user. Returns "ADMIN", "STUDENT", or null on failure.
     */
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

    // ---------- Students ----------
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

    // ---------- Rooms ----------
    public synchronized List<Room> getRooms() {
        return new ArrayList<>(roomMap.values());
    }

    public synchronized Room getRoomByNumber(String roomNumber) {
        return roomMap.get(roomNumber);
    }

    /**
     * Allocates seat for a student. Frees old seat if any, then occupies new seat.
     * Returns true on success, false if seat is already taken.
     */
    public synchronized boolean allocateSeat(String studentId, String newRoomNumber, String newSeat) {
        Student student = studentMap.get(studentId);
        if (student == null) return false;
        Room newRoom = roomMap.get(newRoomNumber);
        if (newRoom == null) return false;

        // Check if the target seat is available (ignore the student's own current seat)
        String currentOccupant = newRoom.getOccupantId(newSeat);
        if (!currentOccupant.isEmpty() && !currentOccupant.equals(studentId)) {
            return false; // seat taken by someone else
        }

        // Free old seat
        if (student.getAllocatedRoom() != null && !student.getAllocatedRoom().equalsIgnoreCase("Unassigned")) {
            Room oldRoom = roomMap.get(student.getAllocatedRoom());
            if (oldRoom != null && student.getAllocatedSeat() != null
                    && !student.getAllocatedSeat().equalsIgnoreCase("Unassigned")) {
                oldRoom.freeSeat(student.getAllocatedSeat());
            }
        }

        // Assign new seat
        newRoom.occupySeat(newSeat, studentId);
        student.setAllocatedRoom(newRoomNumber);
        student.setAllocatedSeat(newSeat);
        return true;
    }

    // ---------- Fee Records ----------
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
                // Also update student's overall fee status based on latest month
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

    // ---------- Complaints ----------
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

    // ---------- Notices ----------
    public synchronized List<Notice> getNotices() {
        return new ArrayList<>(notices);
    }

    public synchronized void addNotice(Notice notice) {
        notices.add(0, notice); // newest first
    }

    // ---------- ID Generation ----------
    public synchronized String generateComplaintId() {
        complaintCounter++;
        return "C" + String.format("%03d", complaintCounter);
    }

    public synchronized String generateNoticeId() {
        noticeCounter++;
        return "N" + String.format("%03d", noticeCounter);
    }
}
