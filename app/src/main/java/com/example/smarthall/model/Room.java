package com.example.smarthall.model;

import java.util.HashMap;
import java.util.Map;

public class Room {
    public static final String SEAT_A = "A";
    public static final String SEAT_B = "B";
    public static final String SEAT_C = "C";
    public static final String SEAT_D = "D";
    public static final String UNOCCUPIED = "";

    private String roomNumber;
    private int floor;
    // Maps seat label (A/B/C/D) -> studentId occupying it, or empty string if free
    private Map<String, String> seatOccupancy;

    public Room(String roomNumber, int floor) {
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.seatOccupancy = new HashMap<>();
        seatOccupancy.put(SEAT_A, UNOCCUPIED);
        seatOccupancy.put(SEAT_B, UNOCCUPIED);
        seatOccupancy.put(SEAT_C, UNOCCUPIED);
        seatOccupancy.put(SEAT_D, UNOCCUPIED);
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public Map<String, String> getSeatOccupancy() { return seatOccupancy; }

    public boolean isSeatAvailable(String seat) {
        String occupant = seatOccupancy.get(seat);
        return occupant == null || occupant.isEmpty();
    }

    public String getOccupantId(String seat) {
        return seatOccupancy.getOrDefault(seat, UNOCCUPIED);
    }

    public void occupySeat(String seat, String studentId) {
        if (seatOccupancy.containsKey(seat)) {
            seatOccupancy.put(seat, studentId);
        }
    }

    public void freeSeat(String seat) {
        if (seatOccupancy.containsKey(seat)) {
            seatOccupancy.put(seat, UNOCCUPIED);
        }
    }

    public int getAvailableSeatCount() {
        int count = 0;
        for (String occupant : seatOccupancy.values()) {
            if (occupant == null || occupant.isEmpty()) count++;
        }
        return count;
    }

    public int getTotalSeats() {
        return seatOccupancy.size();
    }

    @Override
    public String toString() {
        return roomNumber;
    }
}
