package com.example.smarthall.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Room implements Serializable {

    public static final String SEAT_A = "A";
    public static final String SEAT_B = "B";
    public static final String SEAT_C = "C";
    public static final String SEAT_D = "D";
    public static final String UNOCCUPIED = "";

    private String roomId;
    private int floor;
    private int capacity = 4;
    private int occupiedCount = 0;
    private Map<String, Object> seats;
    private Map<String, String> seatOccupancy;

    public Room() {
        this.seats = new HashMap<>();
        this.seats.put(SEAT_A, null);
        this.seats.put(SEAT_B, null);
        this.seats.put(SEAT_C, null);
        this.seats.put(SEAT_D, null);

        this.seatOccupancy = new HashMap<>();
        this.seatOccupancy.put(SEAT_A, UNOCCUPIED);
        this.seatOccupancy.put(SEAT_B, UNOCCUPIED);
        this.seatOccupancy.put(SEAT_C, UNOCCUPIED);
        this.seatOccupancy.put(SEAT_D, UNOCCUPIED);
    }

    public Room(String roomId, int floor) {
        this();
        this.roomId = roomId;
        this.floor = floor;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomId;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomId = roomNumber;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getOccupiedCount() {
        if (seats != null) {
            return getActualOccupiedSeatsCount();
        }
        return occupiedCount;
    }

    public void setOccupiedCount(int occupiedCount) {
        this.occupiedCount = occupiedCount;
    }

    public int getActualOccupiedSeatsCount() {
        int count = 0;
        if (seats != null) {
            String[] keys = new String[]{SEAT_A, SEAT_B, SEAT_C, SEAT_D};
            for (String key : keys) {
                if (seats.get(key) != null) {
                    count++;
                }
            }
        }
        return count;
    }

    public Map<String, Object> getSeats() {
        return seats;
    }

    public void setSeats(Map<String, Object> seats) {
        this.seats = seats;
        if (seats != null) {
            this.occupiedCount = getActualOccupiedSeatsCount();
        }
    }

    public boolean isSeatAvailable(String seatKey) {
        if (seats != null && seats.containsKey(seatKey)) {
            return seats.get(seatKey) == null;
        }
        if (seatOccupancy != null && seatOccupancy.containsKey(seatKey)) {
            String occ = seatOccupancy.get(seatKey);
            return occ == null || occ.isEmpty();
        }
        return true;
    }

    public SeatOccupant getSeatOccupant(String seatKey) {
        if (seats != null && seats.containsKey(seatKey)) {
            Object obj = seats.get(seatKey);
            return SeatOccupant.fromObject(obj);
        }
        return null;
    }

    public Map<String, String> getSeatOccupancy() {
        if (seatOccupancy == null) {
            seatOccupancy = new HashMap<>();
        }
        if (seats != null) {
            for (Map.Entry<String, Object> entry : seats.entrySet()) {
                SeatOccupant occupant = SeatOccupant.fromObject(entry.getValue());
                seatOccupancy.put(entry.getKey(), occupant != null ? occupant.getRoll() : UNOCCUPIED);
            }
        }
        return seatOccupancy;
    }

    public String getOccupantId(String seat) {
        SeatOccupant occupant = getSeatOccupant(seat);
        if (occupant != null) {
            return occupant.getRoll();
        }
        if (seatOccupancy != null) {
            return seatOccupancy.getOrDefault(seat, UNOCCUPIED);
        }
        return UNOCCUPIED;
    }

    public void occupySeat(String seat, String studentId) {
        if (seats == null) {
            seats = new HashMap<>();
        }
        SeatOccupant occupant = new SeatOccupant(studentId, studentId, null, System.currentTimeMillis());
        seats.put(seat, occupant.toMap());
        if (seatOccupancy != null) {
            seatOccupancy.put(seat, studentId);
        }
        occupiedCount = Math.min(capacity, occupiedCount + 1);
    }

    public void freeSeat(String seat) {
        if (seats != null) {
            seats.put(seat, null);
        }
        if (seatOccupancy != null) {
            seatOccupancy.put(seat, UNOCCUPIED);
        }
        occupiedCount = Math.max(0, occupiedCount - 1);
    }

    public int getAvailableSeatCount() {
        if (seats != null) {
            int available = 0;
            String[] keys = {SEAT_A, SEAT_B, SEAT_C, SEAT_D};
            for (String k : keys) {
                if (seats.get(k) == null) {
                    available++;
                }
            }
            return available;
        }
        return Math.max(0, capacity - occupiedCount);
    }

    public int getTotalSeats() {
        return capacity;
    }

    @Override
    public String toString() {
        return roomId != null ? roomId : "";
    }
}
