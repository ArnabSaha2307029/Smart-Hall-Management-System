package com.example.smarthall.ui.admin;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.ActivityRoomManagementBinding;
import com.example.smarthall.databinding.DialogRoomSeatAllocationBinding;
import com.example.smarthall.databinding.DialogStudentSelectorBinding;
import com.example.smarthall.model.Room;
import com.example.smarthall.model.SeatOccupant;
import com.example.smarthall.model.Student;
import com.example.smarthall.ui.adapters.RoomCardAdapter;
import com.example.smarthall.ui.adapters.UnallocatedStudentAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.RoomAllocationManager;
import com.example.smarthall.util.ThemeHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RoomManagementActivity extends BaseActivity {

    private ActivityRoomManagementBinding binding;
    private RoomCardAdapter roomAdapter;
    private final List<Room> roomList = new ArrayList<>();
    private ListenerRegistration roomsRegistration;
    private AlertDialog activeAllocationDialog;
    private String activeDialogRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityRoomManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbarRoomManagement.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        binding.fabAddRoom.setOnClickListener(v -> showAddRoomDialog());

        listenToRooms();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomsRegistration != null) {
            roomsRegistration.remove();
            roomsRegistration = null;
        }
    }

    private void setupRecyclerView() {
        roomAdapter = new RoomCardAdapter(roomList, this::showRoomAllocationDialog);
        binding.rvRooms.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvRooms.setAdapter(roomAdapter);
    }

    private void listenToRooms() {
        binding.pbLoadingRooms.setVisibility(View.VISIBLE);

        roomsRegistration = FirebaseFirestore.getInstance().collection("rooms")
                .addSnapshotListener((snapshots, error) -> {
                    binding.pbLoadingRooms.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e("RoomManagement", "Failed to load rooms", error);
                        Toast.makeText(this, "Unable to complete request. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        RoomAllocationManager.seedDefaultRoomsIfEmpty(new RoomAllocationManager.OnRoomOperationCallback() {
                            @Override
                            public void onSuccess(String message) {
                            }

                            @Override
                            public void onFailure(Exception e) {
                                binding.tvEmptyRooms.setVisibility(View.VISIBLE);
                            }
                        });
                        return;
                    }

                    roomList.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            if (TextUtils.isEmpty(room.getRoomId())) {
                                room.setRoomId(doc.getId());
                            }
                            roomList.add(room);
                        }
                    }

                    Collections.sort(roomList, (r1, r2) -> {
                        try {
                            int n1 = Integer.parseInt(r1.getRoomId());
                            int n2 = Integer.parseInt(r2.getRoomId());
                            return Integer.compare(n1, n2);
                        } catch (Exception e) {
                            return r1.getRoomId().compareTo(r2.getRoomId());
                        }
                    });

                    int totalRooms = roomList.size();
                    int totalSeats = totalRooms * 4;
                    int totalOccupiedSeats = 0;
                    for (Room r : roomList) {
                        totalOccupiedSeats += r.getActualOccupiedSeatsCount();
                    }
                    int totalAvailableSeats = totalSeats - totalOccupiedSeats;

                    binding.tvTotalRoomsCount.setText(String.valueOf(totalRooms));
                    binding.tvTotalOccupiedSeats.setText(String.valueOf(totalOccupiedSeats));
                    binding.tvTotalAvailableSeats.setText(String.valueOf(Math.max(0, totalAvailableSeats)));

                    binding.tvEmptyRooms.setVisibility(roomList.isEmpty() ? View.VISIBLE : View.GONE);
                    roomAdapter.updateData(roomList);

                    if (activeAllocationDialog != null && activeAllocationDialog.isShowing() && !TextUtils.isEmpty(activeDialogRoomId)) {
                        for (Room r : roomList) {
                            if (activeDialogRoomId.equals(r.getRoomId())) {
                                refreshAllocationDialogView(r);
                                break;
                            }
                        }
                    }
                });
    }

    private void showAddRoomDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add New Room");

        final EditText input = new EditText(this);
        input.setHint("Room Number (e.g. 203)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        builder.setView(input);

        builder.setPositiveButton("Create Room", (dialog, which) -> {
            String roomNum = input.getText().toString().trim();
            if (TextUtils.isEmpty(roomNum)) {
                Toast.makeText(this, "Room number cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            RoomAllocationManager.addNewRoom(roomNum, new RoomAllocationManager.OnRoomOperationCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(RoomManagementActivity.this, message, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("RoomManagement", "Failed to add room", e);
                    Toast.makeText(RoomManagementActivity.this, "Unable to complete request. Please check your internet connection.", Toast.LENGTH_LONG).show();
                }
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private DialogRoomSeatAllocationBinding currentDialogBinding;

    private void showRoomAllocationDialog(Room room) {
        if (room == null) return;

        activeDialogRoomId = room.getRoomId();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        currentDialogBinding = DialogRoomSeatAllocationBinding.inflate(LayoutInflater.from(this));
        builder.setView(currentDialogBinding.getRoot());

        activeAllocationDialog = builder.create();

        currentDialogBinding.btnDialogClose.setOnClickListener(v -> activeAllocationDialog.dismiss());
        refreshAllocationDialogView(room);

        activeAllocationDialog.setOnDismissListener(dialog -> {
            activeDialogRoomId = null;
            currentDialogBinding = null;
        });

        activeAllocationDialog.show();
    }

    private void refreshAllocationDialogView(Room room) {
        if (currentDialogBinding == null || room == null) return;

        currentDialogBinding.tvDialogRoomTitle.setText("Room " + room.getRoomId());
        currentDialogBinding.tvDialogRoomSubtitle.setText("Floor " + room.getFloor() + " • " + room.getActualOccupiedSeatsCount() + "/4 Occupied");

        bindSeatRow(Room.SEAT_A, room.getSeatOccupant(Room.SEAT_A),
                currentDialogBinding.tvSeatAOccupantName, currentDialogBinding.tvSeatAOccupantRoll,
                currentDialogBinding.btnActionSeatA, room);

        bindSeatRow(Room.SEAT_B, room.getSeatOccupant(Room.SEAT_B),
                currentDialogBinding.tvSeatBOccupantName, currentDialogBinding.tvSeatBOccupantRoll,
                currentDialogBinding.btnActionSeatB, room);

        bindSeatRow(Room.SEAT_C, room.getSeatOccupant(Room.SEAT_C),
                currentDialogBinding.tvSeatCOccupantName, currentDialogBinding.tvSeatCOccupantRoll,
                currentDialogBinding.btnActionSeatC, room);

        bindSeatRow(Room.SEAT_D, room.getSeatOccupant(Room.SEAT_D),
                currentDialogBinding.tvSeatDOccupantName, currentDialogBinding.tvSeatDOccupantRoll,
                currentDialogBinding.btnActionSeatD, room);
    }

    private void bindSeatRow(String seatKey, SeatOccupant occupant,
                             android.widget.TextView tvName, android.widget.TextView tvRoll,
                             com.google.android.material.button.MaterialButton btnAction,
                             Room room) {
        int colorPrimary = 0xFFC62828;
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            colorPrimary = typedValue.data;
        }

        if (occupant == null) {
            tvName.setText("Available");
            tvName.setTextColor(colorPrimary);
            tvRoll.setText("Tap to assign student");

            btnAction.setText("Assign");
            btnAction.setBackgroundColor(colorPrimary);
            btnAction.setTextColor(Color.WHITE);
            btnAction.setStrokeColor(null);
            btnAction.setStrokeWidth(0);

            btnAction.setOnClickListener(v -> showStudentSelectorDialog(room, seatKey));
        } else {
            tvName.setText(occupant.getName());
            tvName.setTextColor(Color.parseColor("#212121"));
            tvRoll.setText("Roll: " + occupant.getRoll());

            btnAction.setText("Vacate");
            btnAction.setBackgroundColor(Color.WHITE);
            btnAction.setTextColor(colorPrimary);
            btnAction.setStrokeColor(android.content.res.ColorStateList.valueOf(colorPrimary));
            btnAction.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));

            btnAction.setOnClickListener(v -> confirmVacateSeat(room.getRoomId(), seatKey, occupant));
        }
    }

    private void confirmVacateSeat(String roomId, String seatKey, SeatOccupant occupant) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Vacate Seat " + seatKey)
                .setMessage("Are you sure you want to vacate Seat " + seatKey + " in Room " + roomId + " occupied by " + occupant.getName() + " (" + occupant.getRoll() + ")?")
                .setPositiveButton("Vacate Seat", (dialog, which) -> {
                    RoomAllocationManager.vacateSeatTransaction(roomId, seatKey, new RoomAllocationManager.OnRoomOperationCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(RoomManagementActivity.this, message, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("RoomManagement", "Vacate failed", e);
                            Toast.makeText(RoomManagementActivity.this, "Unable to complete request. Please check your internet connection.", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void showStudentSelectorDialog(String roomId, String seatKey) {
        Room targetRoom = null;
        for (Room r : roomList) {
            if (r != null && roomId.equals(r.getRoomId())) {
                targetRoom = r;
                break;
            }
        }
        if (targetRoom == null) {
            int floor = 1;
            try {
                floor = Character.getNumericValue(roomId.charAt(0));
            } catch (Exception ignored) {
            }
            targetRoom = new Room(roomId, floor);
        }
        showStudentSelectorDialog(targetRoom, seatKey);
    }

    public void showStudentSelectorDialog(Room targetRoom, String seatKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogStudentSelectorBinding selectorBinding = DialogStudentSelectorBinding.inflate(LayoutInflater.from(this));
        builder.setView(selectorBinding.getRoot());

        AlertDialog selectorDialog = builder.create();
        selectorBinding.tvSelectorTitle.setText("Assign Seat " + seatKey + " (Room " + targetRoom.getRoomId() + ")");
        selectorBinding.btnSelectorClose.setOnClickListener(v -> selectorDialog.dismiss());

        List<Student> unallocatedStudents = new ArrayList<>();
        UnallocatedStudentAdapter adapter = new UnallocatedStudentAdapter(unallocatedStudents, student -> {
            confirmAssignStudent(targetRoom, seatKey, student, selectorDialog);
        });

        selectorBinding.rvStudentList.setLayoutManager(new LinearLayoutManager(this));
        selectorBinding.rvStudentList.setAdapter(adapter);

        loadUnallocatedStudents(selectorBinding, adapter, unallocatedStudents);

        selectorBinding.etStudentSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                String query = s != null ? s.toString() : "";
                int countResults = adapter.filter(query);
                if (countResults == 0) {
                    selectorBinding.tvEmptyStudents.setVisibility(View.VISIBLE);
                    selectorBinding.rvStudentList.setVisibility(View.GONE);
                } else {
                    selectorBinding.tvEmptyStudents.setVisibility(View.GONE);
                    selectorBinding.rvStudentList.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        selectorDialog.show();
    }

    private void loadUnallocatedStudents(DialogStudentSelectorBinding selectorBinding,
                                         UnallocatedStudentAdapter adapter,
                                         List<Student> unallocatedStudents) {
        selectorBinding.pbLoadingStudents.setVisibility(View.VISIBLE);
        selectorBinding.tvEmptyStudents.setVisibility(View.GONE);
        selectorBinding.rvStudentList.setVisibility(View.GONE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (!roomList.isEmpty()) {
            Set<String> occupiedRolls = extractOccupiedRolls(roomList);
            fetchStudentsAndFilter(db, occupiedRolls, selectorBinding, adapter, unallocatedStudents);
        } else {
            db.collection("rooms").get().addOnSuccessListener(roomSnapshots -> {
                List<Room> fetchedRooms = new ArrayList<>();
                if (roomSnapshots != null) {
                    for (DocumentSnapshot doc : roomSnapshots.getDocuments()) {
                        Room r = doc.toObject(Room.class);
                        if (r != null) {
                            if (TextUtils.isEmpty(r.getRoomId())) {
                                r.setRoomId(doc.getId());
                            }
                            fetchedRooms.add(r);
                        }
                    }
                }
                Set<String> occupiedRolls = extractOccupiedRolls(fetchedRooms);
                fetchStudentsAndFilter(db, occupiedRolls, selectorBinding, adapter, unallocatedStudents);
            }).addOnFailureListener(e -> {
                Set<String> occupiedRolls = extractOccupiedRolls(roomList);
                fetchStudentsAndFilter(db, occupiedRolls, selectorBinding, adapter, unallocatedStudents);
            });
        }
    }

    private Set<String> extractOccupiedRolls(List<Room> rooms) {
        Set<String> occupiedRolls = new HashSet<>();
        String[] seatKeys = {Room.SEAT_A, Room.SEAT_B, Room.SEAT_C, Room.SEAT_D};
        if (rooms != null) {
            for (Room r : rooms) {
                if (r == null) continue;
                for (String key : seatKeys) {
                    SeatOccupant occupant = r.getSeatOccupant(key);
                    if (occupant != null && !TextUtils.isEmpty(occupant.getRoll())) {
                        occupiedRolls.add(occupant.getRoll().trim());
                    } else {
                        String occId = r.getOccupantId(key);
                        if (!TextUtils.isEmpty(occId) && !Room.UNOCCUPIED.equals(occId)) {
                            occupiedRolls.add(occId.trim());
                        }
                    }
                }
            }
        }
        return occupiedRolls;
    }

    private void fetchStudentsAndFilter(FirebaseFirestore db,
                                       Set<String> occupiedRolls,
                                       DialogStudentSelectorBinding selectorBinding,
                                       UnallocatedStudentAdapter adapter,
                                       List<Student> unallocatedStudents) {
        db.collection("students").get().addOnSuccessListener(snapshots -> {
            selectorBinding.pbLoadingStudents.setVisibility(View.GONE);
            unallocatedStudents.clear();

            if (snapshots != null) {
                Map<String, Student> unallocatedMap = new LinkedHashMap<>();

                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String roll = doc.getString("roll");
                    if (TextUtils.isEmpty(roll)) {
                        roll = doc.getString("rollNumber");
                    }
                    if (TextUtils.isEmpty(roll)) {
                        continue;
                    }
                    roll = roll.trim();

                    if (occupiedRolls.contains(roll)) {
                        continue;
                    }

                    Student student = doc.toObject(Student.class);
                    if (student == null) {
                        student = new Student();
                    }
                    student.setRollNumber(roll);

                    String uid = doc.getString("uid");
                    if (TextUtils.isEmpty(uid)) {
                        uid = doc.getString("id");
                    }
                    if (TextUtils.isEmpty(uid)) {
                        uid = doc.getId();
                    }
                    student.setId(uid);

                    String name = doc.getString("name");
                    if (!TextUtils.isEmpty(name)) {
                        student.setName(name.trim());
                    }

                    String dept = doc.getString("department");
                    if (!TextUtils.isEmpty(dept)) {
                        student.setDepartment(dept.trim());
                    }

                    Student existing = unallocatedMap.get(roll);
                    if (existing == null) {
                        unallocatedMap.put(roll, student);
                    } else {
                        if (TextUtils.isEmpty(existing.getId()) && !TextUtils.isEmpty(student.getId())) {
                            existing.setId(student.getId());
                        }
                        if ((TextUtils.isEmpty(existing.getName()) || existing.getName().startsWith("Student ")) && !TextUtils.isEmpty(student.getName())) {
                            existing.setName(student.getName());
                        }
                        if ((TextUtils.isEmpty(existing.getDepartment()) || "N/A".equals(existing.getDepartment())) && !TextUtils.isEmpty(student.getDepartment())) {
                            existing.setDepartment(student.getDepartment());
                        }
                    }
                }

                unallocatedStudents.addAll(unallocatedMap.values());
                for (Student s : unallocatedStudents) {
                    if (TextUtils.isEmpty(s.getName())) {
                        s.setName("Student " + s.getRoll());
                    }
                    if (TextUtils.isEmpty(s.getDepartment())) {
                        s.setDepartment("N/A");
                    }
                    s.setAllocationStatus("UNALLOCATED");
                }

                Collections.sort(unallocatedStudents, (s1, s2) -> {
                    String r1 = s1.getRoll();
                    String r2 = s2.getRoll();
                    try {
                        long n1 = Long.parseLong(r1);
                        long n2 = Long.parseLong(r2);
                        return Long.compare(n1, n2);
                    } catch (Exception e) {
                        return r1.compareToIgnoreCase(r2);
                    }
                });
            }

            adapter.updateList(unallocatedStudents);

            String currentQuery = selectorBinding.etStudentSearch.getText() != null
                    ? selectorBinding.etStudentSearch.getText().toString() : "";
            int visibleCount = adapter.filter(currentQuery);
            if (visibleCount == 0) {
                selectorBinding.tvEmptyStudents.setVisibility(View.VISIBLE);
                selectorBinding.rvStudentList.setVisibility(View.GONE);
            } else {
                selectorBinding.tvEmptyStudents.setVisibility(View.GONE);
                selectorBinding.rvStudentList.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            selectorBinding.pbLoadingStudents.setVisibility(View.GONE);
            selectorBinding.tvEmptyStudents.setVisibility(View.VISIBLE);
            selectorBinding.rvStudentList.setVisibility(View.GONE);
            Log.e("RoomManagement", "Failed to load students", e);
            selectorBinding.tvEmptyStudents.setText("Unable to load students. Please check your connection.");
        });
    }

    private void confirmAssignStudent(Room targetRoom, String seatKey, Student student, AlertDialog selectorDialog) {
        String studentName = student.getName();
        String studentRoll = !TextUtils.isEmpty(student.getRollNumber()) ? student.getRollNumber() : student.getRoll();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Seat Allocation")
                .setMessage("Assign " + studentName + " (" + studentRoll + ") to Room " + targetRoom.getRoomId() + " Seat " + seatKey + "?")
                .setPositiveButton("Confirm Allocation", (dialog, which) -> {
                    selectorDialog.dismiss();
                    RoomAllocationManager.allocateSeatTransaction(targetRoom.getRoomId(), seatKey, student,
                            new RoomAllocationManager.OnRoomOperationCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    Toast.makeText(RoomManagementActivity.this, message, Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e("RoomManagement", "Allocation failed", e);
                                    Toast.makeText(RoomManagementActivity.this, "Unable to complete request. Please check your internet connection.", Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
