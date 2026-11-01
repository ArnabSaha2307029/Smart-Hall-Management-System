package com.example.smarthall.util;

import android.text.TextUtils;

import com.example.smarthall.model.Room;
import com.example.smarthall.model.SeatOccupant;
import com.example.smarthall.model.Student;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class RoomAllocationManager {

    public interface OnRoomOperationCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    public static void seedDefaultRoomsIfEmpty(OnRoomOperationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("rooms").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                if (callback != null) {
                    callback.onFailure(task.getException());
                }
                return;
            }

            if (task.getResult() != null && task.getResult().isEmpty()) {
                WriteBatch batch = db.batch();
                String[] defaultRooms = {"101", "102", "201", "202"};

                for (String rId : defaultRooms) {
                    DocumentReference ref = db.collection("rooms").document(rId);
                    int floor = Character.getNumericValue(rId.charAt(0));

                    Map<String, Object> roomData = new HashMap<>();
                    roomData.put("roomId", rId);
                    roomData.put("floor", floor);
                    roomData.put("capacity", 4);
                    roomData.put("occupiedCount", 0);

                    Map<String, Object> seats = new HashMap<>();
                    seats.put(Room.SEAT_A, null);
                    seats.put(Room.SEAT_B, null);
                    seats.put(Room.SEAT_C, null);
                    seats.put(Room.SEAT_D, null);
                    roomData.put("seats", seats);

                    batch.set(ref, roomData);
                }

                batch.commit().addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess("Default rooms seeded successfully");
                    }
                }).addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
            } else {
                if (callback != null) {
                    callback.onSuccess("Rooms already present");
                }
            }
        });
    }

    public static void addNewRoom(String roomId, OnRoomOperationCallback callback) {
        if (TextUtils.isEmpty(roomId) || !roomId.matches("\\d{3,4}")) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Room number must be 3 or 4 digits"));
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("rooms").document(roomId);

        ref.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                if (callback != null) {
                    callback.onFailure(new IllegalStateException("Room " + roomId + " already exists"));
                }
                return;
            }

            int floor = Character.getNumericValue(roomId.charAt(0));
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("roomId", roomId);
            roomData.put("floor", floor);
            roomData.put("capacity", 4);
            roomData.put("occupiedCount", 0);

            Map<String, Object> seats = new HashMap<>();
            seats.put(Room.SEAT_A, null);
            seats.put(Room.SEAT_B, null);
            seats.put(Room.SEAT_C, null);
            seats.put(Room.SEAT_D, null);
            roomData.put("seats", seats);

            ref.set(roomData).addOnSuccessListener(aVoid -> {
                if (callback != null) {
                    callback.onSuccess("Room " + roomId + " created successfully");
                }
            }).addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e);
                }
            });
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onFailure(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static void allocateSeatTransaction(String targetRoomId, String seatKey, Student student, OnRoomOperationCallback callback) {
        if (TextUtils.isEmpty(targetRoomId) || TextUtils.isEmpty(seatKey) || student == null) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Invalid allocation parameters"));
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.runTransaction(transaction -> {
            DocumentReference targetRoomRef = db.collection("rooms").document(targetRoomId);
            DocumentSnapshot targetRoomDoc = transaction.get(targetRoomRef);

            if (!targetRoomDoc.exists()) {
                throw new FirebaseFirestoreException("Target room does not exist",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            Map<String, Object> rawSeats = (Map<String, Object>) targetRoomDoc.get("seats");
            Map<String, Object> targetSeats = rawSeats != null ? new HashMap<>(rawSeats) : new HashMap<>();

            if (targetSeats.get(seatKey) != null) {
                throw new FirebaseFirestoreException("Seat " + seatKey + " is already occupied!",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            String studentUid = student.getId();
            String studentRoll = !TextUtils.isEmpty(student.getRollNumber()) ? student.getRollNumber() : student.getRoll();

            DocumentReference studentUidRef = !TextUtils.isEmpty(studentUid)
                    ? db.collection("students").document(studentUid) : null;
            DocumentReference studentRollRef = !TextUtils.isEmpty(studentRoll)
                    ? db.collection("students").document(studentRoll) : null;

            DocumentSnapshot studentUidDoc = studentUidRef != null ? transaction.get(studentUidRef) : null;
            DocumentSnapshot studentRollDoc = null;
            if (studentRollRef != null && (studentUidRef == null || !studentRollRef.getPath().equals(studentUidRef.getPath()))) {
                studentRollDoc = transaction.get(studentRollRef);
            }

            String oldRoom = null;
            String oldSeat = null;

            if (studentUidDoc != null && studentUidDoc.exists()) {
                oldRoom = studentUidDoc.getString("room");
                if (TextUtils.isEmpty(oldRoom)) {
                    oldRoom = studentUidDoc.getString("allocatedRoom");
                }
                oldSeat = studentUidDoc.getString("seat");
                if (TextUtils.isEmpty(oldSeat)) {
                    oldSeat = studentUidDoc.getString("allocatedSeat");
                }
            } else if (studentRollDoc != null && studentRollDoc.exists()) {
                oldRoom = studentRollDoc.getString("room");
                if (TextUtils.isEmpty(oldRoom)) {
                    oldRoom = studentRollDoc.getString("allocatedRoom");
                }
                oldSeat = studentRollDoc.getString("seat");
                if (TextUtils.isEmpty(oldSeat)) {
                    oldSeat = studentRollDoc.getString("allocatedSeat");
                }
            }

            DocumentReference oldRoomRef = null;
            DocumentSnapshot oldRoomDoc = null;

            if (!TextUtils.isEmpty(oldRoom) && !oldRoom.equalsIgnoreCase("Unassigned")
                    && !oldRoom.equals(targetRoomId) && !TextUtils.isEmpty(oldSeat)
                    && !oldSeat.equalsIgnoreCase("Unassigned")) {
                oldRoomRef = db.collection("rooms").document(oldRoom);
                oldRoomDoc = transaction.get(oldRoomRef);
            }

            if (oldRoomDoc != null && oldRoomDoc.exists() && oldRoomRef != null) {
                Map<String, Object> oldRawSeats = (Map<String, Object>) oldRoomDoc.get("seats");
                Map<String, Object> oldSeats = oldRawSeats != null ? new HashMap<>(oldRawSeats) : new HashMap<>();
                oldSeats.put(oldSeat, null);

                int oldOccupied = countOccupiedSeats(oldSeats);

                Map<String, Object> oldUpdates = new HashMap<>();
                oldUpdates.put("seats", oldSeats);
                oldUpdates.put("occupiedCount", oldOccupied);
                transaction.update(oldRoomRef, oldUpdates);
            }

            if (targetRoomId.equals(oldRoom) && !TextUtils.isEmpty(oldSeat) && !oldSeat.equals(seatKey)) {
                targetSeats.put(oldSeat, null);
            }

            SeatOccupant newOccupant = new SeatOccupant(studentRoll, student.getName(), studentUid, System.currentTimeMillis());
            targetSeats.put(seatKey, newOccupant.toMap());

            int targetOccupied = countOccupiedSeats(targetSeats);

            Map<String, Object> targetUpdates = new HashMap<>();
            targetUpdates.put("seats", targetSeats);
            targetUpdates.put("occupiedCount", targetOccupied);
            targetUpdates.put("roomId", targetRoomId);
            try {
                targetUpdates.put("floor", Character.getNumericValue(targetRoomId.charAt(0)));
            } catch (Exception ignored) {
                targetUpdates.put("floor", 1);
            }
            transaction.set(targetRoomRef, targetUpdates, SetOptions.merge());

            Map<String, Object> studentUpdates = new HashMap<>();
            studentUpdates.put("room", targetRoomId);
            studentUpdates.put("seat", seatKey);
            studentUpdates.put("allocatedRoom", targetRoomId);
            studentUpdates.put("allocatedSeat", seatKey);
            studentUpdates.put("allocationStatus", "ALLOCATED");

            if (studentUidRef != null) {
                transaction.set(studentUidRef, studentUpdates, SetOptions.merge());
            }
            if (studentRollRef != null) {
                transaction.set(studentRollRef, studentUpdates, SetOptions.merge());
            }
            if (!TextUtils.isEmpty(studentUid)) {
                DocumentReference userRef = db.collection("users").document(studentUid);
                transaction.set(userRef, studentUpdates, SetOptions.merge());
            }

            return null;
        }).addOnSuccessListener(result -> {
            if (callback != null) {
                callback.onSuccess("Student " + student.getName() + " assigned to Room " + targetRoomId + " Seat " + seatKey);
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onFailure(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static void vacateSeatTransaction(String roomId, String seatKey, OnRoomOperationCallback callback) {
        if (TextUtils.isEmpty(roomId) || TextUtils.isEmpty(seatKey)) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Invalid room or seat"));
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.runTransaction(transaction -> {
            DocumentReference roomRef = db.collection("rooms").document(roomId);
            DocumentSnapshot roomDoc = transaction.get(roomRef);

            if (!roomDoc.exists()) {
                throw new FirebaseFirestoreException("Room does not exist",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            Map<String, Object> rawSeats = (Map<String, Object>) roomDoc.get("seats");
            Map<String, Object> seats = rawSeats != null ? new HashMap<>(rawSeats) : new HashMap<>();

            Object occupantObj = seats.get(seatKey);
            if (occupantObj == null) {
                throw new FirebaseFirestoreException("Seat " + seatKey + " is already vacant",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            SeatOccupant occupant = SeatOccupant.fromObject(occupantObj);
            String roll = occupant != null ? occupant.getRoll() : null;
            String uid = occupant != null ? occupant.getUid() : null;

            DocumentReference rollRef = !TextUtils.isEmpty(roll) ? db.collection("students").document(roll) : null;
            DocumentSnapshot rollDoc = rollRef != null ? transaction.get(rollRef) : null;

            if (TextUtils.isEmpty(uid) && rollDoc != null && rollDoc.exists()) {
                uid = rollDoc.getString("uid");
                if (TextUtils.isEmpty(uid)) {
                    uid = rollDoc.getString("id");
                }
            }

            DocumentReference uidRef = !TextUtils.isEmpty(uid) ? db.collection("students").document(uid) : null;
            DocumentSnapshot uidDoc = null;
            if (uidRef != null && (rollRef == null || !uidRef.getPath().equals(rollRef.getPath()))) {
                uidDoc = transaction.get(uidRef);
            }

            seats.put(seatKey, null);
            int occupied = countOccupiedSeats(seats);

            Map<String, Object> roomUpdates = new HashMap<>();
            roomUpdates.put("seats", seats);
            roomUpdates.put("occupiedCount", occupied);
            transaction.update(roomRef, roomUpdates);

            Map<String, Object> studentUpdates = new HashMap<>();
            studentUpdates.put("room", null);
            studentUpdates.put("seat", null);
            studentUpdates.put("allocatedRoom", "Unassigned");
            studentUpdates.put("allocatedSeat", "Unassigned");
            studentUpdates.put("allocationStatus", "UNALLOCATED");

            if (rollRef != null) {
                transaction.set(rollRef, studentUpdates, SetOptions.merge());
            }
            if (uidRef != null) {
                transaction.set(uidRef, studentUpdates, SetOptions.merge());
            }
            if (!TextUtils.isEmpty(uid)) {
                DocumentReference userRef = db.collection("users").document(uid);
                transaction.set(userRef, studentUpdates, SetOptions.merge());
            }

            return null;
        }).addOnSuccessListener(result -> {
            if (callback != null) {
                callback.onSuccess("Seat " + seatKey + " in Room " + roomId + " vacated successfully");
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onFailure(e);
            }
        });
    }

    public static int countOccupiedSeats(Map<String, Object> seatsMap) {
        if (seatsMap == null) return 0;
        int count = 0;
        String[] keys = {Room.SEAT_A, Room.SEAT_B, Room.SEAT_C, Room.SEAT_D};
        for (String k : keys) {
            if (seatsMap.get(k) != null) {
                count++;
            }
        }
        return count;
    }
}
