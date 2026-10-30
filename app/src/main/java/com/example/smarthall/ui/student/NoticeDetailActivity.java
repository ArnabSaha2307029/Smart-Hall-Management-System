package com.example.smarthall.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smarthall.databinding.ActivityNoticeDetailBinding;
import com.example.smarthall.model.NoticeModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoticeDetailActivity extends AppCompatActivity {

    private ActivityNoticeDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoticeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarNoticeDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notice Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarNoticeDetail.setNavigationOnClickListener(v -> finish());

        Intent intent = getIntent();
        String noticeId = intent != null ? intent.getStringExtra("notice_id") : null;
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

        if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(message)) {
            populateUI(title, poster, timestamp, message);
        } else if (!TextUtils.isEmpty(noticeId)) {
            fetchNoticeFromFirebase(noticeId);
        }
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

    private void fetchNoticeFromFirebase(String noticeId) {
        FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com")
                .getReference("hall_announcements")
                .child(noticeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            NoticeModel notice = snapshot.getValue(NoticeModel.class);
                            if (notice != null) {
                                populateUI(notice.getTitle(), notice.getPostedBy(), notice.getTimestamp(), notice.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
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
