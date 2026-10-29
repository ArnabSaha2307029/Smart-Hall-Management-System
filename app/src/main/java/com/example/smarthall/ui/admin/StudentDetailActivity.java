package com.example.smarthall.ui.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ActivityStudentDetailBinding;
import com.example.smarthall.model.FeeRecord;
import com.example.smarthall.model.Room;
import com.example.smarthall.model.Student;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.ui.adapters.FeeRecordAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.customview.RoomSeatView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentDetailActivity extends BaseActivity {

    private ActivityStudentDetailBinding binding;
    private HallRepository repository;
    private Student student;
    private FeeRecordAdapter feeRecordAdapter;
    private String selectedRoomNumber;
    private String selectedSeatLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = HallRepository.getInstance();

        String studentId = getIntent().getStringExtra("EXTRA_STUDENT_ID");
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Invalid student ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        student = repository.getStudentById(studentId);
        if (student == null) {
            Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupHeader();
        populateStudentProfile();
        setupRoomSpinner();
        setupRoomSeatView();
        setupFeeRecords();
        setupActionButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (student != null) {
            // Re-fetch student to pick up any changes
            student = repository.getStudentById(student.getId());
            populateStudentProfile();
            updateRoomSeatView();
            refreshFeeRecords();
        }
    }

    private void setupHeader() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void populateStudentProfile() {
        binding.tvDetailName.setText(student.getName());
        binding.tvDetailRoll.setText("Roll: " + student.getRollNumber());
        binding.tvDetailDept.setText("Dept: " + student.getDepartment());
        binding.tvDetailPhone.setText("Phone: " + student.getPhone());
        binding.tvDetailEmail.setText("Email: " + student.getEmail());

        if (student.isAssigned()) {
            binding.tvDetailAllocation.setText("Room " + student.getAllocatedRoom()
                    + " — Seat " + student.getAllocatedSeat());
        } else {
            binding.tvDetailAllocation.setText("Not Assigned");
        }

        if (FeeRecord.STATUS_PAID.equals(student.getFeeStatus())) {
            binding.tvDetailFeeStatus.setText("PAID");
            binding.tvDetailFeeStatus.setTextColor(getResources().getColor(R.color.statusPaid, null));
        } else {
            binding.tvDetailFeeStatus.setText("UNPAID");
            binding.tvDetailFeeStatus.setTextColor(getResources().getColor(R.color.statusUnpaid, null));
        }
    }

    private void setupRoomSpinner() {
        List<Room> rooms = repository.getRooms();
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("Select Room");
        for (Room r : rooms) roomNumbers.add(r.getRoomNumber());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roomNumbers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRoom.setAdapter(adapter);

        // Pre-select student's current room
        if (student.getAllocatedRoom() != null && !student.getAllocatedRoom().equalsIgnoreCase("Unassigned")) {
            int pos = roomNumbers.indexOf(student.getAllocatedRoom());
            if (pos > 0) {
                binding.spinnerRoom.setSelection(pos);
                selectedRoomNumber = student.getAllocatedRoom();
            }
        }

        binding.spinnerRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSeatLabel = null;
                binding.roomSeatView.clearSelection();
                if (position == 0) {
                    selectedRoomNumber = null;
                    binding.roomSeatView.setSeatData(new int[]{0,0,0,0},
                            new String[]{"Empty","Empty","Empty","Empty"});
                    binding.roomSeatView.setRoomTitle("Select a Room");
                } else {
                    selectedRoomNumber = roomNumbers.get(position);
                    updateRoomSeatView();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRoomSeatView() {
        binding.roomSeatView.setInteractiveMode(true);
        binding.roomSeatView.setOnSeatSelectedListener(new RoomSeatView.OnSeatSelectedListener() {
            @Override
            public void onSeatSelected(String seatLabel, int seatIndex) {
                selectedSeatLabel = seatLabel;
                Toast.makeText(StudentDetailActivity.this,
                        "Seat " + seatLabel + " selected. Tap Save to allocate.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSeatOccupied(String seatLabel) {
                Toast.makeText(StudentDetailActivity.this,
                        "Seat " + seatLabel + " is occupied by another student.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        updateRoomSeatView();
    }

    private void updateRoomSeatView() {
        String roomToShow = selectedRoomNumber;
        if (roomToShow == null && student.isAssigned()) {
            roomToShow = student.getAllocatedRoom();
        }
        if (roomToShow == null) {
            binding.roomSeatView.setRoomTitle("Select a Room");
            binding.roomSeatView.setSeatData(new int[]{0,0,0,0},
                    new String[]{"Empty","Empty","Empty","Empty"});
            return;
        }

        Room room = repository.getRoomByNumber(roomToShow);
        if (room == null) return;

        String[] seatKeys = {Room.SEAT_A, Room.SEAT_B, Room.SEAT_C, Room.SEAT_D};
        int[] states = new int[4];
        String[] names = new String[4];

        for (int i = 0; i < 4; i++) {
            String occupantId = room.getOccupantId(seatKeys[i]);
            if (occupantId == null || occupantId.isEmpty()) {
                states[i] = RoomSeatView.STATE_AVAILABLE;
                names[i] = "Empty";
            } else if (occupantId.equals(student.getId())) {
                states[i] = RoomSeatView.STATE_MY_SEAT;
                names[i] = student.getName();
            } else {
                Student occ = repository.getStudentById(occupantId);
                states[i] = RoomSeatView.STATE_OCCUPIED;
                names[i] = occ != null ? occ.getName() : "Occupied";
            }
        }

        binding.roomSeatView.setRoomTitle("Room " + roomToShow);
        binding.roomSeatView.setSeatData(states, names);
    }

    private void setupFeeRecords() {
        List<FeeRecord> feeRecords = repository.getFeeRecordsByStudent(student.getId());
        feeRecordAdapter = new FeeRecordAdapter(feeRecords, feeRecord -> {
            String newStatus = FeeRecord.STATUS_PAID.equals(feeRecord.getStatus())
                    ? FeeRecord.STATUS_UNPAID : FeeRecord.STATUS_PAID;
            repository.updateFeeStatus(feeRecord.getId(), newStatus);
            refreshFeeRecords();
            Toast.makeText(this, "Fee status updated", Toast.LENGTH_SHORT).show();
        }, true);
        binding.rvFeeRecords.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFeeRecords.setAdapter(feeRecordAdapter);
        updateFeeEmptyState(feeRecords);
    }

    private void refreshFeeRecords() {
        if (student == null) return;
        student = repository.getStudentById(student.getId());
        List<FeeRecord> updated = repository.getFeeRecordsByStudent(student.getId());
        feeRecordAdapter.updateData(updated);
        updateFeeEmptyState(updated);
        populateStudentProfile();
    }

    private void updateFeeEmptyState(List<FeeRecord> records) {
        binding.tvEmptyFees.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvFeeRecords.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupActionButtons() {
        binding.btnSaveSeat.setOnClickListener(v -> saveSeatAllocation());
        binding.btnCallStudent.setOnClickListener(v -> callStudent());
        binding.btnEmailStudent.setOnClickListener(v -> emailStudent());
    }

    private void saveSeatAllocation() {
        if (selectedRoomNumber == null || selectedRoomNumber.isEmpty()) {
            Toast.makeText(this, "Please select a room first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSeatLabel == null || selectedSeatLabel.isEmpty()) {
            Toast.makeText(this, "Please tap a seat on the room diagram to select it", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            boolean success = repository.allocateSeat(student.getId(), selectedRoomNumber, selectedSeatLabel);
            if (success) {
                student = repository.getStudentById(student.getId());
                populateStudentProfile();
                updateRoomSeatView();
                selectedSeatLabel = null;
                Toast.makeText(this, "Seat allocated successfully: Room "
                        + selectedRoomNumber + " Seat " + student.getAllocatedSeat(),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Seat is already occupied. Please select another seat.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error allocating seat: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void callStudent() {
        try {
            String phone = student.getPhone();
            if (phone == null || phone.isEmpty()) {
                Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open dialer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void emailStudent() {
        try {
            String email = student.getEmail();
            if (email == null || email.isEmpty()) {
                Toast.makeText(this, "No email address available", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Amar Ekushey Hall Administration Notice");
            startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open email client: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
