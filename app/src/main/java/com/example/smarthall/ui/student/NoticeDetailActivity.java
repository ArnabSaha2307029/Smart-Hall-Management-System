package com.example.smarthall.ui.student;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.smarthall.databinding.ActivityNoticeDetailBinding;
import com.example.smarthall.model.NoticeModel;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.ThemeHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoticeDetailActivity extends BaseActivity {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";

    private ActivityNoticeDetailBinding binding;
    private String noticeId;
    private String currentUserRole = "student";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityNoticeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarNoticeDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notice Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarNoticeDetail.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "student");

        Intent intent = getIntent();
        noticeId = intent != null ? intent.getStringExtra("notice_id") : null;
        String title = intent != null ? intent.getStringExtra("title") : null;
        String message = intent != null ? intent.getStringExtra("message") : null;
        String poster = intent != null ? intent.getStringExtra("poster") : null;
        if (TextUtils.isEmpty(poster)) {
            poster = "Admin";
        }

        long timestamp = 0;
        if (intent != null) {
            timestamp = intent.getLongExtra("timestamp", 0);
            if (timestamp == 0) {
                String tsStr = intent.getStringExtra("timestamp");
                if (tsStr != null && !tsStr.isEmpty()) {
                    try {
                        timestamp = Long.parseLong(tsStr);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        setupDeleteButton();

        if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(message)) {
            populateUI(title, poster, timestamp, message);
        } else if (!TextUtils.isEmpty(noticeId)) {
            fetchNoticeFromFirebase(noticeId);
        }
    }

    private void setupDeleteButton() {
        if ("admin".equalsIgnoreCase(currentUserRole)) {
            binding.btnDeleteNotice.setVisibility(View.VISIBLE);
            binding.btnDeleteNotice.setOnClickListener(v -> showDeleteNoticeConfirmation());
        } else {
            binding.btnDeleteNotice.setVisibility(View.GONE);
        }
    }

    private void showDeleteNoticeConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to permanently delete this notice?")
                .setPositiveButton("Delete", (d, which) -> deleteNotice())
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .show();
    }

    private void deleteNotice() {
        if (TextUtils.isEmpty(noticeId)) {
            Toast.makeText(this, "Cannot delete notice: Missing ID", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnDeleteNotice.setEnabled(false);
        FirebaseDatabase database = FirebaseDatabase.getInstance(RTDB_URL);
        database.getReference("notices").child(noticeId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Notice deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnDeleteNotice.setEnabled(true);
                    Log.e("NoticeDetail", "Failed to delete notice", e);
                    Toast.makeText(this, "Failed to delete notice. Please try again.", Toast.LENGTH_SHORT).show();
                });
        database.getReference("hall_notices").child(noticeId).removeValue();
        database.getReference("hall_announcements").child(noticeId).removeValue();
    }

    private void populateUI(String title, String poster, long timestamp, String message) {
        binding.tvDetailTitle.setText(title != null ? title : "");
        binding.tvDetailPoster.setText("Posted by: " + (poster != null && !poster.isEmpty() ? poster : "Admin"));
        if (timestamp > 0) {
            String formattedDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(new Date(timestamp));
            binding.tvDetailTimestamp.setText(formattedDate);
        } else {
            binding.tvDetailTimestamp.setText("");
        }
        binding.tvDetailMessage.setText(message != null ? message : "");
    }

    private void fetchNoticeFromFirebase(String id) {
        this.noticeId = id;
        FirebaseDatabase database = FirebaseDatabase.getInstance(RTDB_URL);
        database.getReference("notices")
                .child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            NoticeModel notice = snapshot.getValue(NoticeModel.class);
                            if (notice != null) {
                                if (notice.getId() != null && !notice.getId().isEmpty()) {
                                    noticeId = notice.getId();
                                }
                                populateUI(notice.getTitle(), notice.getPostedBy(), notice.getTimestamp(), notice.getMessage());
                            }
                        } else {
                            database.getReference("hall_announcements")
                                    .child(id)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot legacySnapshot) {
                                            if (legacySnapshot.exists()) {
                                                NoticeModel legacyNotice = legacySnapshot.getValue(NoticeModel.class);
                                                if (legacyNotice != null) {
                                                    if (legacyNotice.getId() != null && !legacyNotice.getId().isEmpty()) {
                                                        noticeId = legacyNotice.getId();
                                                    }
                                                    populateUI(legacyNotice.getTitle(), legacyNotice.getPostedBy(), legacyNotice.getTimestamp(), legacyNotice.getMessage());
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
