package com.example.smarthall.ui.complaint;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ActivityComplaintDetailBinding;
import com.example.smarthall.model.Complaint;
import com.example.smarthall.model.ComplaintComment;
import com.example.smarthall.network.FcmNotificationSender;
import com.example.smarthall.ui.adapters.ComplaintCommentAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.chat.ChatActivity;
import com.example.smarthall.util.ThemeHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComplaintDetailActivity extends BaseActivity {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";

    private ActivityComplaintDetailBinding binding;
    private DatabaseReference complaintRef;
    private ValueEventListener complaintListener;
    private DatabaseReference commentsRef;
    private ValueEventListener commentsListener;

    private final List<ComplaintComment> commentList = new ArrayList<>();
    private ComplaintCommentAdapter commentAdapter;

    private String complaintId;
    private String title;
    private String description;
    private String studentRoll;
    private String studentPhone;
    private String currentStatus = "pending";
    private long timestamp;
    private boolean isAdmin;
    private String currentUserRole = "student";
    private String currentUserRoll = "";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityComplaintDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "student");
        isAdmin = "admin".equalsIgnoreCase(currentUserRole);

        currentUserRoll = prefs.getString("user_roll", "");
        if (TextUtils.isEmpty(currentUserRoll)) {
            currentUserRoll = prefs.getString("student_roll", "");
        }
        if (TextUtils.isEmpty(currentUserRoll)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SharedPreferences studentPrefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                currentUserRoll = studentPrefs.getString("student_roll", "");
            }
        }

        complaintId = getIntent().getStringExtra("complaint_id");
        title = getIntent().getStringExtra("title");
        description = getIntent().getStringExtra("description");
        studentRoll = getIntent().getStringExtra("student_roll");
        studentPhone = getIntent().getStringExtra("student_phone");
        currentStatus = getIntent().getStringExtra("status");
        if (currentStatus == null) {
            currentStatus = "pending";
        }
        timestamp = getIntent().getLongExtra("timestamp", System.currentTimeMillis());

        setupToolbar();
        setupComments();
        bindData();
        setupActions();
        listenForRealtimeUpdates();
    }

    private void setupToolbar() {
        binding.toolbarComplaintDetail.setNavigationOnClickListener(v -> finish());
    }

    private void setupComments() {
        commentAdapter = new ComplaintCommentAdapter(commentList);
        binding.rvComplaintComments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaintComments.setAdapter(commentAdapter);

        if (TextUtils.isEmpty(complaintId)) {
            return;
        }

        commentsRef = FirebaseDatabase.getInstance(RTDB_URL)
                .getReference("complaints")
                .child(complaintId)
                .child("comments");

        commentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ComplaintComment comment = child.getValue(ComplaintComment.class);
                    if (comment != null) {
                        if (comment.getCommentId() == null) {
                            comment.setCommentId(child.getKey());
                        }
                        commentList.add(comment);
                    }
                }

                Collections.sort(commentList, (c1, c2) -> Long.compare(c1.getTimestamp(), c2.getTimestamp()));
                commentAdapter.notifyDataSetChanged();

                if (commentList.isEmpty()) {
                    binding.tvEmptyComments.setVisibility(View.VISIBLE);
                    binding.rvComplaintComments.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyComments.setVisibility(View.GONE);
                    binding.rvComplaintComments.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        commentsRef.addValueEventListener(commentsListener);
    }

    private void bindData() {
        binding.tvDetailTitle.setText(title != null ? title : "");
        binding.tvDetailDescription.setText(description != null ? description : "");
        binding.tvDetailRoll.setText("Submitter Roll: " + (studentRoll != null && !studentRoll.isEmpty() ? studentRoll : "Unknown"));

        if (isAdmin) {
            binding.layoutAdminActions.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(studentPhone)) {
                binding.tvDetailPhone.setText("Contact: " + studentPhone);
                binding.tvDetailPhone.setVisibility(View.VISIBLE);
                binding.btnCallSubmitter.setEnabled(true);
            } else {
                binding.tvDetailPhone.setVisibility(View.GONE);
                binding.btnCallSubmitter.setEnabled(false);
            }
        } else {
            binding.layoutAdminActions.setVisibility(View.GONE);
            binding.tvDetailPhone.setVisibility(View.GONE);
            binding.btnCallSubmitter.setEnabled(false);
        }

        if (timestamp > 0) {
            binding.tvDetailDate.setText(dateFormat.format(new Date(timestamp)));
        } else {
            binding.tvDetailDate.setText("");
        }

        updateStatusUI(currentStatus);
        updateDeleteButtonVisibility();
    }

    private void updateDeleteButtonVisibility() {
        boolean canDelete = false;
        if ("admin".equalsIgnoreCase(currentUserRole)) {
            canDelete = true;
        } else if ("student".equalsIgnoreCase(currentUserRole)) {
            if (!TextUtils.isEmpty(studentRoll) && !TextUtils.isEmpty(currentUserRoll)
                    && studentRoll.trim().equalsIgnoreCase(currentUserRoll.trim())) {
                canDelete = true;
            }
        }
        binding.btnDeleteComplaint.setVisibility(canDelete ? View.VISIBLE : View.GONE);
    }

    private void updateStatusUI(String status) {
        currentStatus = status != null ? status : "pending";
        if (Complaint.STATUS_RESOLVED.equalsIgnoreCase(currentStatus)) {
            binding.tvDetailStatus.setText("RESOLVED");
            binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_badge_resolved);
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.textColorSecondary, tv, true);
            binding.tvDetailStatus.setTextColor(tv.data);
            binding.btnMarkResolved.setVisibility(View.GONE);
        } else {
            binding.tvDetailStatus.setText("PENDING");
            binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true);
            binding.tvDetailStatus.setTextColor(tv.data);
            if (isAdmin) {
                binding.btnMarkResolved.setVisibility(View.VISIBLE);
            } else {
                binding.btnMarkResolved.setVisibility(View.GONE);
            }
        }
    }

    private void setupActions() {
        binding.btnCallSubmitter.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(studentPhone)) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + studentPhone));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "No phone number available for this student", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnMessageSubmitter.setOnClickListener(v -> {
            Intent chatIntent = new Intent(this, ChatActivity.class);
            chatIntent.putExtra("student_roll", studentRoll != null ? studentRoll : "");
            chatIntent.putExtra("chatting_with_role", "student");
            chatIntent.putExtra(ChatActivity.EXTRA_PRIMARY_COLOR, com.example.smarthall.util.HallThemeManager.getActivePrimaryColor(this));
            chatIntent.putExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR, com.example.smarthall.util.HallThemeManager.getActivePrimaryDarkColor(this));
            startActivity(chatIntent);
        });

        binding.btnMarkResolved.setOnClickListener(v -> markComplaintAsResolved());
        binding.btnDeleteComplaint.setOnClickListener(v -> showDeleteComplaintConfirmation());
        binding.btnSendComment.setOnClickListener(v -> sendComment());
    }

    private void sendComment() {
        if (TextUtils.isEmpty(complaintId)) {
            Toast.makeText(this, "Cannot post comment: Missing complaint ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = binding.etCommentInput.getText() != null
                ? binding.etCommentInput.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(text)) {
            return;
        }

        binding.btnSendComment.setEnabled(false);

        DatabaseReference targetRef = FirebaseDatabase.getInstance(RTDB_URL)
                .getReference("complaints")
                .child(complaintId)
                .child("comments");

        String commentId = targetRef.push().getKey();
        if (commentId == null) {
            binding.btnSendComment.setEnabled(true);
            return;
        }

        String senderRoll = isAdmin ? "Admin" : (!TextUtils.isEmpty(currentUserRoll) ? currentUserRoll : "Student");
        String senderRole = isAdmin ? "admin" : "student";
        long now = System.currentTimeMillis();

        ComplaintComment comment = new ComplaintComment(commentId, senderRoll, senderRole, text, now);
        targetRef.child(commentId).setValue(comment).addOnCompleteListener(task -> {
            binding.btnSendComment.setEnabled(true);
            if (task.isSuccessful()) {
                binding.etCommentInput.setText("");
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Log.e("StudentDashboardError", "Operation failed: Failed to post comment: " + msg, task.getException());
                Toast.makeText(this, "Failed to post comment: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteComplaintConfirmation() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Complaint")
                .setMessage("Are you sure you want to delete this complaint?")
                .setPositiveButton("Delete", (d, which) -> deleteComplaint())
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();
        dialog.show();
        if (dialog.getButton(DialogInterface.BUTTON_POSITIVE) != null) {
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, tv, true);
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(tv.data);
        }
        if (dialog.getButton(DialogInterface.BUTTON_NEGATIVE) != null) {
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.textColorSecondary, tv, true);
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(tv.data);
        }
    }

    private void deleteComplaint() {
        if (TextUtils.isEmpty(complaintId)) {
            Toast.makeText(this, "Cannot delete complaint: Missing ID", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnDeleteComplaint.setEnabled(false);
        FirebaseDatabase.getInstance(RTDB_URL)
                .getReference("complaints")
                .child(complaintId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Complaint deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        binding.btnDeleteComplaint.setEnabled(true);
                        String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e("StudentDashboardError", "Operation failed: Failed to delete complaint: " + msg, task.getException());
                        Toast.makeText(this, "Failed to delete complaint: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markComplaintAsResolved() {
        if (complaintRef == null) {
            return;
        }

        binding.btnMarkResolved.setEnabled(false);
        complaintRef.child("status").setValue("resolved").addOnCompleteListener(task -> {
            binding.btnMarkResolved.setEnabled(true);
            if (task.isSuccessful()) {
                currentStatus = "resolved";
                updateStatusUI("resolved");
                FcmNotificationSender.sendComplaintResolvedNotificationToStudent(
                        this,
                        studentRoll,
                        complaintId,
                        title
                );
                Toast.makeText(this, "Complaint marked as resolved", Toast.LENGTH_SHORT).show();
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Log.e("StudentDashboardError", "Operation failed: Failed to update complaint status: " + msg, task.getException());
                Toast.makeText(this, "Failed to update complaint status: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForRealtimeUpdates() {
        if (TextUtils.isEmpty(complaintId)) {
            return;
        }

        complaintRef = FirebaseDatabase.getInstance(RTDB_URL).getReference("complaints").child(complaintId);
        complaintListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    return;
                }

                String updatedTitle = snapshot.child("title").getValue(String.class);
                String updatedDesc = snapshot.child("description").getValue(String.class);
                String updatedStatus = snapshot.child("status").getValue(String.class);
                String updatedRoll = snapshot.child("studentRoll").getValue(String.class);
                String updatedPhone = snapshot.child("studentPhone").getValue(String.class);
                Long updatedTime = snapshot.child("timestamp").getValue(Long.class);

                if (updatedTitle != null) title = updatedTitle;
                if (updatedDesc != null) description = updatedDesc;
                if (updatedRoll != null) studentRoll = updatedRoll;
                if (updatedPhone != null) studentPhone = updatedPhone;
                if (updatedTime != null) timestamp = updatedTime;

                bindData();
                updateStatusUI(updatedStatus);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        complaintRef.addValueEventListener(complaintListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (complaintRef != null && complaintListener != null) {
            complaintRef.removeEventListener(complaintListener);
        }
        if (commentsRef != null && commentsListener != null) {
            commentsRef.removeEventListener(commentsListener);
        }
    }
}
