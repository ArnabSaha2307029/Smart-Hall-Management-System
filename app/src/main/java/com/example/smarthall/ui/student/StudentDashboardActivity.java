package com.example.smarthall.ui.student;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ActivityStudentDashboardBinding;
import com.example.smarthall.model.Complaint;
import com.example.smarthall.model.FeeRecord;
import com.example.smarthall.model.Notice;
import com.example.smarthall.model.Room;
import com.example.smarthall.model.Student;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.ui.adapters.ComplaintAdapter;
import com.example.smarthall.ui.adapters.FeeRecordAdapter;
import com.example.smarthall.ui.adapters.NoticeAdapter;
import com.example.smarthall.ui.auth.LoginActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.customview.RoomSeatView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentDashboardActivity extends BaseActivity {

    private ActivityStudentDashboardBinding binding;
    private HallRepository repository;
    private Student student;
    private FeeRecordAdapter feeRecordAdapter;
    private ComplaintAdapter complaintAdapter;
    private NoticeAdapter noticeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = HallRepository.getInstance();

        String studentId = getIntent().getStringExtra("EXTRA_USER_ID");
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_LONG).show();
            logout();
            return;
        }

        student = repository.getStudentById(studentId);
        if (student == null) {
            Toast.makeText(this, "Student account not found.", Toast.LENGTH_LONG).show();
            logout();
            return;
        }

        setupHeader();
        setupProfileCard();
        setupSeatView();
        setupFeeRecords();
        setupComplaints();
        setupNotices();
        setupSubmitComplaintButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (student != null) {
            student = repository.getStudentById(student.getId());
            setupProfileCard();
            setupSeatView();
            refreshFees();
            refreshComplaints();
            refreshNotices();
        }
    }

    private void setupHeader() {
        binding.headerLayout.tvHallName.setText(R.string.hall_name);
        binding.headerLayout.tvRoleSubtitle.setText("Logged in as: " + student.getName());
        binding.headerLayout.btnLogout.setOnClickListener(v -> logout());
    }

    private void setupProfileCard() {
        binding.tvStudentName.setText(student.getName());
        binding.tvStudentRoll.setText("Roll: " + student.getRollNumber());
        binding.tvStudentDept.setText("Department: " + student.getDepartment());
        binding.tvStudentPhone.setText("Phone: " + student.getPhone());
        binding.tvStudentEmail.setText("Email: " + student.getEmail());

        if (student.isAssigned()) {
            binding.tvRoomSeatInfo.setText("Room " + student.getAllocatedRoom()
                    + " — Seat " + student.getAllocatedSeat());
        } else {
            binding.tvRoomSeatInfo.setText("Room: Not Assigned");
        }

        if (FeeRecord.STATUS_PAID.equals(student.getFeeStatus())) {
            binding.tvFeeStatusCard.setText("Fee Status: PAID");
            binding.tvFeeStatusCard.setTextColor(getResources().getColor(R.color.statusPaid, null));
        } else {
            binding.tvFeeStatusCard.setText("Fee Status: UNPAID");
            binding.tvFeeStatusCard.setTextColor(getResources().getColor(R.color.statusUnpaid, null));
        }
    }

    private void setupSeatView() {
        binding.studentRoomSeatView.setInteractiveMode(false);

        if (!student.isAssigned()) {
            binding.studentRoomSeatView.setRoomTitle("No Room Assigned");
            binding.studentRoomSeatView.setSeatData(new int[]{0,0,0,0},
                    new String[]{"Empty","Empty","Empty","Empty"});
            return;
        }

        Room room = repository.getRoomByNumber(student.getAllocatedRoom());
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

        binding.studentRoomSeatView.setRoomTitle("Room " + student.getAllocatedRoom());
        binding.studentRoomSeatView.setSeatData(states, names);
    }

    private void setupFeeRecords() {
        List<FeeRecord> feeRecords = repository.getFeeRecordsByStudent(student.getId());
        feeRecordAdapter = new FeeRecordAdapter(feeRecords, null, false);
        binding.rvMyFees.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyFees.setAdapter(feeRecordAdapter);
        updateFeeEmptyState(feeRecords);
    }

    private void refreshFees() {
        List<FeeRecord> fees = repository.getFeeRecordsByStudent(student.getId());
        feeRecordAdapter.updateData(fees);
        updateFeeEmptyState(fees);
    }

    private void updateFeeEmptyState(List<FeeRecord> fees) {
        binding.tvEmptyFees.setVisibility(fees.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvMyFees.setVisibility(fees.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupComplaints() {
        List<Complaint> myComplaints = repository.getComplaintsByStudent(student.getId());
        complaintAdapter = new ComplaintAdapter(myComplaints, null, false);
        binding.rvMyComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyComplaints.setAdapter(complaintAdapter);
        updateComplaintEmptyState(myComplaints);
    }

    private void refreshComplaints() {
        List<Complaint> complaints = repository.getComplaintsByStudent(student.getId());
        complaintAdapter.updateData(complaints);
        updateComplaintEmptyState(complaints);
    }

    private void updateComplaintEmptyState(List<Complaint> complaints) {
        binding.tvEmptyComplaints.setVisibility(complaints.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvMyComplaints.setVisibility(complaints.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupNotices() {
        List<Notice> notices = repository.getNotices();
        noticeAdapter = new NoticeAdapter(notices);
        binding.rvNotices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotices.setAdapter(noticeAdapter);
        updateNoticeEmptyState(notices);
    }

    private void refreshNotices() {
        List<Notice> notices = repository.getNotices();
        noticeAdapter.updateData(notices);
        updateNoticeEmptyState(notices);
    }

    private void updateNoticeEmptyState(List<Notice> notices) {
        binding.tvEmptyNotices.setVisibility(notices.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvNotices.setVisibility(notices.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupSubmitComplaintButton() {
        binding.btnSubmitComplaint.setOnClickListener(v -> showSubmitComplaintDialog());
    }

    private void showSubmitComplaintDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Submit Complaint");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_submit_complaint, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etComplaintTitle);
        EditText etDesc = dialogView.findViewById(R.id.etComplaintDesc);

        builder.setPositiveButton("Submit", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Complaint title is required");
                etTitle.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(desc)) {
                etDesc.setError("Please describe your complaint");
                etDesc.requestFocus();
                return;
            }

            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
            String roomNo = student.isAssigned() ? student.getAllocatedRoom() : "N/A";
            Complaint complaint = new Complaint(
                    repository.generateComplaintId(),
                    student.getId(),
                    student.getName(),
                    roomNo,
                    title, desc, date,
                    Complaint.STATUS_PENDING
            );
            repository.addComplaint(complaint);
            refreshComplaints();
            Toast.makeText(this, "Complaint submitted successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void logout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
