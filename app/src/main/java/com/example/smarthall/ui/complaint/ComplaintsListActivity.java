package com.example.smarthall.ui.complaint;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
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
import com.example.smarthall.databinding.ActivityComplaintsListBinding;
import com.example.smarthall.model.Complaint;
import com.example.smarthall.network.FcmNotificationSender;
import com.example.smarthall.ui.adapters.GlobalComplaintAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.ThemeHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComplaintsListActivity extends BaseActivity {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";

    private ActivityComplaintsListBinding binding;
    private GlobalComplaintAdapter adapter;
    private final List<Complaint> complaintList = new ArrayList<>();
    private DatabaseReference complaintsRef;
    private ValueEventListener complaintsListener;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityComplaintsListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "student");

        setupToolbar();
        setupRecyclerView();
        setupFab();
        setupFirebaseListener();
    }

    private void setupToolbar() {
        binding.toolbarComplaintsList.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        binding.rvComplaintsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GlobalComplaintAdapter(complaintList, complaint -> {
            Intent intent = new Intent(ComplaintsListActivity.this, ComplaintDetailActivity.class);
            intent.putExtra("complaint_id", complaint.getComplaintId());
            intent.putExtra("title", complaint.getTitle());
            intent.putExtra("description", complaint.getDescription());
            intent.putExtra("student_roll", complaint.getStudentRoll());
            intent.putExtra("student_phone", complaint.getStudentPhone());
            intent.putExtra("status", complaint.getStatus());
            intent.putExtra("timestamp", complaint.getTimestamp());
            startActivity(intent);
        });
        binding.rvComplaintsList.setAdapter(adapter);
    }

    private void setupFab() {
        if ("admin".equalsIgnoreCase(currentUserRole)) {
            binding.fabAddComplaint.setVisibility(View.GONE);
        } else {
            binding.fabAddComplaint.setVisibility(View.VISIBLE);
            binding.fabAddComplaint.setOnClickListener(v -> showCreateComplaintDialog());
        }
    }

    private void setupFirebaseListener() {
        binding.progressBarComplaintsList.setVisibility(View.VISIBLE);
        complaintsRef = FirebaseDatabase.getInstance(RTDB_URL).getReference("complaints");

        complaintsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                complaintList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.child("complaintId").getValue(String.class);
                    if (id == null) {
                        id = child.getKey();
                    }
                    String title = child.child("title").getValue(String.class);
                    String description = child.child("description").getValue(String.class);
                    String studentRoll = child.child("studentRoll").getValue(String.class);
                    String studentPhone = child.child("studentPhone").getValue(String.class);
                    String status = child.child("status").getValue(String.class);
                    Long timestampVal = child.child("timestamp").getValue(Long.class);
                    long timestamp = timestampVal != null ? timestampVal : 0L;

                    Complaint complaint = new Complaint(id, title, description, studentRoll, studentPhone, status, timestamp);
                    complaintList.add(complaint);
                }

                Collections.sort(complaintList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                adapter.notifyDataSetChanged();
                binding.progressBarComplaintsList.setVisibility(View.GONE);

                if (complaintList.isEmpty()) {
                    binding.layoutEmptyComplaints.setVisibility(View.VISIBLE);
                    binding.rvComplaintsList.setVisibility(View.GONE);
                } else {
                    binding.layoutEmptyComplaints.setVisibility(View.GONE);
                    binding.rvComplaintsList.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBarComplaintsList.setVisibility(View.GONE);
            }
        };

        complaintsRef.addValueEventListener(complaintsListener);
    }

    private void showCreateComplaintDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_global_complaint, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText etTitle = dialogView.findViewById(R.id.etNewComplaintTitle);
        EditText etDesc = dialogView.findViewById(R.id.etNewComplaintDesc);
        View btnCancel = dialogView.findViewById(R.id.btnCancelComplaint);
        View btnSubmit = dialogView.findViewById(R.id.btnSubmitComplaint);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Title is required");
                return;
            }
            if (TextUtils.isEmpty(desc)) {
                etDesc.setError("Description is required");
                return;
            }

            String roll = "";
            String phone = "";
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SharedPreferences studentPrefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                roll = studentPrefs.getString("student_roll", "");
                phone = studentPrefs.getString("student_phone", "");
            }

            String complaintId = complaintsRef.push().getKey();
            if (complaintId == null) {
                complaintId = "C_" + System.currentTimeMillis();
            }

            long timestamp = System.currentTimeMillis();
            Complaint newComplaint = new Complaint(complaintId, title, desc, roll, phone, "pending", timestamp);

            final String finalComplaintId = complaintId;
            final String finalTitle = title;
            final String finalDesc = desc;
            final String finalRoll = roll;
            final String finalPhone = phone;

            complaintsRef.child(complaintId).setValue(newComplaint).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FcmNotificationSender.sendComplaintNotificationToAdmin(
                            ComplaintsListActivity.this,
                            finalComplaintId,
                            finalTitle,
                            finalDesc,
                            finalRoll,
                            finalPhone
                    );
                    Toast.makeText(ComplaintsListActivity.this, "Complaint submitted successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Log.e("ComplaintsList", "Failed to submit complaint", task.getException());
                    Toast.makeText(ComplaintsListActivity.this, "Unable to complete request. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (complaintsRef != null && complaintsListener != null) {
            complaintsRef.removeEventListener(complaintsListener);
        }
    }
}
