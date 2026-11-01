package com.example.smarthall.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SeatOccupant implements Serializable {

    private String roll;
    private String name;
    private String uid;
    private long assignedAt;

    public SeatOccupant() {
    }

    public SeatOccupant(String roll, String name, String uid, long assignedAt) {
        this.roll = roll;
        this.name = name;
        this.uid = uid;
        this.assignedAt = assignedAt;
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(long assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("roll", roll);
        map.put("name", name);
        if (uid != null) {
            map.put("uid", uid);
        }
        map.put("assignedAt", assignedAt);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static SeatOccupant fromObject(Object obj) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            String roll = (String) map.get("roll");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");
            Long assignedAt = 0L;
            Object assignedObj = map.get("assignedAt");
            if (assignedObj instanceof Number) {
                assignedAt = ((Number) assignedObj).longValue();
            }
            return new SeatOccupant(roll, name, uid, assignedAt);
        }
        return null;
    }
}
