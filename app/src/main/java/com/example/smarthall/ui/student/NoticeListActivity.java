package com.example.smarthall.ui.student;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.ActivityNoticeListBinding;
import com.example.smarthall.model.NoticeModel;
import com.example.smarthall.ui.adapters.FirebaseNoticeAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.ThemeHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoticeListActivity extends BaseActivity {

    private ActivityNoticeListBinding binding;
    private FirebaseNoticeAdapter adapter;
    private DatabaseReference noticesRef;
    private ValueEventListener noticesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityNoticeListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Hall Notices & Alerts");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        boolean isAdmin = "admin".equalsIgnoreCase(prefs.getString("user_role", "student"));

        adapter = new FirebaseNoticeAdapter(new ArrayList<>(), isAdmin, isAdmin ? this::showDeleteNoticeDialog : null);
        binding.rvNotices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotices.setAdapter(adapter);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvNotices.setVisibility(View.GONE);
        binding.tvNoNotices.setVisibility(View.GONE);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com");
        noticesRef = database.getReference("notices");

        noticesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NoticeModel> notices = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NoticeModel notice = child.getValue(NoticeModel.class);
                    if (notice != null) {
                        notices.add(notice);
                    }
                }
                Collections.sort(notices, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                binding.progressBar.setVisibility(View.GONE);
                if (notices.isEmpty()) {
                    binding.tvNoNotices.setVisibility(View.VISIBLE);
                    binding.rvNotices.setVisibility(View.GONE);
                } else {
                    binding.tvNoNotices.setVisibility(View.GONE);
                    binding.rvNotices.setVisibility(View.VISIBLE);
                }
                adapter.setNotices(notices);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvNoNotices.setVisibility(View.VISIBLE);
                binding.rvNotices.setVisibility(View.GONE);
            }
        };

        noticesRef.addValueEventListener(noticesListener);
    }

    private void showDeleteNoticeDialog(NoticeModel notice) {
        if (notice == null || TextUtils.isEmpty(notice.getId())) {
            Toast.makeText(this, "Cannot delete notice: Invalid notice", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to permanently delete this notice?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String noticeId = notice.getId();
                    FirebaseDatabase database = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com");
                    database.getReference("notices").child(noticeId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(NoticeListActivity.this, "Notice deleted successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("NoticeListActivity", "Failed to delete notice", e);
                                Toast.makeText(NoticeListActivity.this, "Failed to delete notice. Please try again.", Toast.LENGTH_SHORT).show();
                            });
                    database.getReference("hall_announcements").child(noticeId).removeValue();
                    database.getReference("hall_notices").child(noticeId).removeValue();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noticesRef != null && noticesListener != null) {
            noticesRef.removeEventListener(noticesListener);
        }
    }
}
