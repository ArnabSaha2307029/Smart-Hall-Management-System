package com.example.smarthall.ui.admin;

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
import com.example.smarthall.databinding.ActivityAdminDashboardBinding;
import com.example.smarthall.model.Complaint;
import com.example.smarthall.model.Notice;
import com.example.smarthall.model.Student;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.ui.adapters.ComplaintAdapter;
import com.example.smarthall.ui.adapters.NoticeAdapter;
import com.example.smarthall.ui.adapters.StudentAdapter;
import com.example.smarthall.ui.auth.LoginActivity;
import com.example.smarthall.ui.base.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminDashboardActivity extends BaseActivity {

    private ActivityAdminDashboardBinding binding;
    private HallRepository repository;
    private StudentAdapter studentAdapter;
    private ComplaintAdapter complaintAdapter;
    private NoticeAdapter noticeAdapter;

    private static final int TAB_STUDENTS = 0;
    private static final int TAB_COMPLAINTS = 1;
    private static final int TAB_NOTICES = 2;
    private int currentTab = TAB_STUDENTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = HallRepository.getInstance();

        setupHeader();
        setupRecyclerViews();
        setupTabButtons();
        switchTab(TAB_STUDENTS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentTab();
    }

    private void setupHeader() {
        binding.headerLayout.tvHallName.setText(R.string.hall_name);
        binding.headerLayout.tvRoleSubtitle.setText("Logged in as: Admin");
        binding.headerLayout.btnLogout.setOnClickListener(v -> logout());
    }

    private void setupRecyclerViews() {
        // Students RecyclerView
        studentAdapter = new StudentAdapter(repository.getStudents(), student -> {
            Intent intent = new Intent(this, StudentDetailActivity.class);
            intent.putExtra("EXTRA_STUDENT_ID", student.getId());
            startActivity(intent);
        });
        binding.rvStudents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStudents.setAdapter(studentAdapter);

        // Complaints RecyclerView
        complaintAdapter = new ComplaintAdapter(repository.getAllComplaints(),
                complaint -> {
                    repository.toggleComplaintStatus(complaint.getId());
                    refreshComplaints();
                    Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                }, true);
        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaints.setAdapter(complaintAdapter);

        // Notices RecyclerView
        noticeAdapter = new NoticeAdapter(repository.getNotices());
        binding.rvNotices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotices.setAdapter(noticeAdapter);
    }

    private void setupTabButtons() {
        binding.btnTabStudents.setOnClickListener(v -> switchTab(TAB_STUDENTS));
        binding.btnTabComplaints.setOnClickListener(v -> switchTab(TAB_COMPLAINTS));
        binding.btnTabNotices.setOnClickListener(v -> switchTab(TAB_NOTICES));
        binding.btnAddNotice.setOnClickListener(v -> showAddNoticeDialog());
    }

    private void switchTab(int tab) {
        currentTab = tab;
        // Hide all panels
        binding.panelStudents.setVisibility(View.GONE);
        binding.panelComplaints.setVisibility(View.GONE);
        binding.panelNotices.setVisibility(View.GONE);

        // Reset button styles
        int inactive = getResources().getColor(R.color.white, null);
        int active = getResources().getColor(R.color.colorPrimary, null);
        int inactiveText = getResources().getColor(R.color.colorPrimary, null);
        int activeText = getResources().getColor(R.color.white, null);

        binding.btnTabStudents.setBackgroundColor(inactive);
        binding.btnTabStudents.setTextColor(inactiveText);
        binding.btnTabComplaints.setBackgroundColor(inactive);
        binding.btnTabComplaints.setTextColor(inactiveText);
        binding.btnTabNotices.setBackgroundColor(inactive);
        binding.btnTabNotices.setTextColor(inactiveText);

        switch (tab) {
            case TAB_STUDENTS:
                binding.panelStudents.setVisibility(View.VISIBLE);
                binding.btnTabStudents.setBackgroundColor(active);
                binding.btnTabStudents.setTextColor(activeText);
                refreshStudents();
                break;
            case TAB_COMPLAINTS:
                binding.panelComplaints.setVisibility(View.VISIBLE);
                binding.btnTabComplaints.setBackgroundColor(active);
                binding.btnTabComplaints.setTextColor(activeText);
                refreshComplaints();
                break;
            case TAB_NOTICES:
                binding.panelNotices.setVisibility(View.VISIBLE);
                binding.btnTabNotices.setBackgroundColor(active);
                binding.btnTabNotices.setTextColor(activeText);
                refreshNotices();
                break;
        }
    }

    private void refreshCurrentTab() {
        switchTab(currentTab);
    }

    private void refreshStudents() {
        List<Student> students = repository.getStudents();
        studentAdapter.updateData(students);
        binding.tvEmptyStudents.setVisibility(students.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvStudents.setVisibility(students.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void refreshComplaints() {
        List<Complaint> complaints = repository.getAllComplaints();
        complaintAdapter.updateData(complaints);
        binding.tvEmptyComplaints.setVisibility(complaints.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvComplaints.setVisibility(complaints.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void refreshNotices() {
        List<Notice> notices = repository.getNotices();
        noticeAdapter.updateData(notices);
        binding.tvEmptyNotices.setVisibility(notices.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvNotices.setVisibility(notices.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showAddNoticeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Notice");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_notice, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etNoticeTitle);
        EditText etMessage = dialogView.findViewById(R.id.etNoticeMessage);

        builder.setPositiveButton("Publish", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Title is required");
                etTitle.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(message)) {
                etMessage.setError("Message is required");
                etMessage.requestFocus();
                return;
            }

            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
            Notice notice = new Notice(repository.generateNoticeId(), title, message, date);
            repository.addNotice(notice);
            refreshNotices();
            Toast.makeText(this, "Notice published successfully", Toast.LENGTH_SHORT).show();
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
