package com.example.smarthall.ui.student;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.ActivityNoticeListBinding;
import com.example.smarthall.model.NoticeModel;
import com.example.smarthall.ui.adapters.FirebaseNoticeAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.ThemeHelper;
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

        adapter = new FirebaseNoticeAdapter(new ArrayList<>());
        binding.rvNotices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotices.setAdapter(adapter);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvNotices.setVisibility(View.GONE);
        binding.tvNoNotices.setVisibility(View.GONE);

        noticesRef = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com")
                .getReference("hall_announcements");

        noticesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<NoticeModel> notices = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NoticeModel notice = child.getValue(NoticeModel.class);
                    if (notice != null) {
                        notices.add(notice);
                    }
                }
                Collections.reverse(notices);
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
            public void onCancelled(DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvNoNotices.setVisibility(View.VISIBLE);
                binding.rvNotices.setVisibility(View.GONE);
            }
        };

        noticesRef.addValueEventListener(noticesListener);
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
