package com.example.smarthall.ui.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ActivityAdminDashboardBinding;
import com.example.smarthall.model.Complaint;
import com.example.smarthall.model.NoticeModel;
import com.example.smarthall.model.Student;
import com.example.smarthall.network.FcmNotificationSender;
import com.example.smarthall.receiver.NetworkChangeReceiver;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.ui.adapters.ComplaintAdapter;
import com.example.smarthall.ui.adapters.FirebaseNoticeAdapter;
import com.example.smarthall.ui.adapters.StudentAdapter;
import com.example.smarthall.ui.auth.LoginActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminDashboardActivity extends BaseActivity {

    private ActivityAdminDashboardBinding binding;
    private HallRepository repository;
    private StudentAdapter studentAdapter;
    private ComplaintAdapter complaintAdapter;
    private FirebaseNoticeAdapter noticeAdapter;
    private List<NoticeModel> noticeList = new ArrayList<>();
    private DatabaseReference noticeRef;
    private ValueEventListener noticeListener;
    private NetworkChangeReceiver networkChangeReceiver;
    private Snackbar networkSnackbar;
    private boolean isPreviouslyOffline = false;

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
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com");
        noticeRef = database.getReference("hall_announcements");

        setupHeader();
        setupRecyclerViews();
        setupTabButtons();
        setupBroadcastNotice();
        setupNoticesListener();
        switchTab(TAB_STUDENTS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentTab();
    }

    @Override
    protected void onStart() {
        super.onStart();
        networkChangeReceiver = new NetworkChangeReceiver(isConnected -> {
            if (!isConnected) {
                isPreviouslyOffline = true;
                if (networkSnackbar != null && networkSnackbar.isShown()) {
                    networkSnackbar.dismiss();
                }
                networkSnackbar = Snackbar.make(binding.getRoot(), "No Internet Connection. Offline mode.", Snackbar.LENGTH_INDEFINITE);
                networkSnackbar.getView().setBackgroundColor(Color.parseColor("#B71C1C"));
                ((android.widget.TextView) networkSnackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text)).setTextColor(Color.WHITE);
                networkSnackbar.show();
            } else {
                if (networkSnackbar != null && networkSnackbar.isShown()) {
                    networkSnackbar.dismiss();
                }
                if (isPreviouslyOffline) {
                    Snackbar onlineSnack = Snackbar.make(binding.getRoot(), "Back online", Snackbar.LENGTH_SHORT);
                    onlineSnack.getView().setBackgroundColor(Color.parseColor("#D32F2F"));
                    ((android.widget.TextView) onlineSnack.getView().findViewById(com.google.android.material.R.id.snackbar_text)).setTextColor(Color.WHITE);
                    onlineSnack.show();
                    isPreviouslyOffline = false;
                }
            }
        });
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
        if (networkSnackbar != null && networkSnackbar.isShown()) {
            networkSnackbar.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noticeRef != null && noticeListener != null) {
            noticeRef.removeEventListener(noticeListener);
        }
    }

    private void setupHeader() {
        binding.headerLayout.tvHallName.setText(R.string.hall_name);
        binding.headerLayout.tvRoleSubtitle.setText("Logged in as: Admin");
        binding.headerLayout.btnLogout.setOnClickListener(v -> logout());
    }

    private void setupRecyclerViews() {
        studentAdapter = new StudentAdapter(repository.getStudents(), student -> {
            Intent intent = new Intent(this, StudentDetailActivity.class);
            intent.putExtra("EXTRA_STUDENT_ID", student.getId());
            startActivity(intent);
        });
        binding.rvStudents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStudents.setAdapter(studentAdapter);

        complaintAdapter = new ComplaintAdapter(repository.getAllComplaints(),
                complaint -> {
                    repository.toggleComplaintStatus(complaint.getId());
                    refreshComplaints();
                    Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                }, true);
        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaints.setAdapter(complaintAdapter);

        noticeAdapter = new FirebaseNoticeAdapter(noticeList);
        binding.rvNotices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotices.setAdapter(noticeAdapter);
    }

    private void setupNoticesListener() {
        noticeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noticeList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NoticeModel notice = child.getValue(NoticeModel.class);
                    if (notice != null) {
                        noticeList.add(notice);
                    }
                }
                Collections.sort(noticeList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                noticeAdapter.notifyDataSetChanged();
                if (noticeList.isEmpty()) {
                    binding.tvEmptyNotices.setVisibility(View.VISIBLE);
                    binding.rvNotices.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyNotices.setVisibility(View.GONE);
                    binding.rvNotices.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ADMIN_NOTICES", "Failed to load notices", error.toException());
            }
        };
        noticeRef.addValueEventListener(noticeListener);
    }

    private void setupTabButtons() {
        binding.btnTabStudents.setOnClickListener(v -> switchTab(TAB_STUDENTS));
        binding.btnTabComplaints.setOnClickListener(v -> switchTab(TAB_COMPLAINTS));
        binding.btnTabNotices.setOnClickListener(v -> switchTab(TAB_NOTICES));
        binding.btnAddNotice.setOnClickListener(v -> showAddNoticeDialog());
    }

    private void setupBroadcastNotice() {
        binding.btnBroadcastNotice.setOnClickListener(v -> {
            String title = binding.etNoticeTitle.getText() != null
                    ? binding.etNoticeTitle.getText().toString().trim()
                    : "";
            String message = binding.etNoticeMessage.getText() != null
                    ? binding.etNoticeMessage.getText().toString().trim()
                    : "";

            if (TextUtils.isEmpty(title)) {
                binding.etNoticeTitle.setError("Notice title is required");
                binding.etNoticeTitle.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(message)) {
                binding.etNoticeMessage.setError("Notice content is required");
                binding.etNoticeMessage.requestFocus();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            String poster = auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null
                    ? auth.getCurrentUser().getEmail()
                    : "Admin";

            String id = noticeRef.push().getKey();
            if (id == null) {
                Toast.makeText(AdminDashboardActivity.this, "Failed to generate notice ID", Toast.LENGTH_SHORT).show();
                return;
            }

            NoticeModel noticeModel = new NoticeModel(id, title, message, poster, System.currentTimeMillis());

            noticeRef.child(id).setValue(noticeModel).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FcmNotificationSender.sendNoticeTopicNotification(AdminDashboardActivity.this, id, title, message, poster, noticeModel.getTimestamp());
                    binding.etNoticeTitle.setText("");
                    binding.etNoticeMessage.setText("");
                    Toast.makeText(AdminDashboardActivity.this, "Emergency notice broadcasted live", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to broadcast notice";
                    Toast.makeText(AdminDashboardActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void switchTab(int tab) {
        currentTab = tab;
        binding.panelStudents.setVisibility(View.GONE);
        binding.panelComplaints.setVisibility(View.GONE);
        binding.panelNotices.setVisibility(View.GONE);

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
        boolean isEmpty = noticeList.isEmpty();
        binding.tvEmptyNotices.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvNotices.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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

            FirebaseAuth auth = FirebaseAuth.getInstance();
            String poster = auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null
                    ? auth.getCurrentUser().getEmail()
                    : "Admin";
            String id = noticeRef.push().getKey();
            if (id == null) {
                Toast.makeText(AdminDashboardActivity.this, "Failed to generate notice ID", Toast.LENGTH_SHORT).show();
                return;
            }

            NoticeModel noticeModel = new NoticeModel(id, title, message, poster, System.currentTimeMillis());
            noticeRef.child(id).setValue(noticeModel).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FcmNotificationSender.sendNoticeTopicNotification(AdminDashboardActivity.this, id, title, message, poster, noticeModel.getTimestamp());
                    Toast.makeText(AdminDashboardActivity.this, "Notice published successfully", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to publish notice";
                    Toast.makeText(AdminDashboardActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });

            dialog.dismiss();
        });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
