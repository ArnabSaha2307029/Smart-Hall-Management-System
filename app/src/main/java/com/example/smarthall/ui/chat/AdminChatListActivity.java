package com.example.smarthall.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.ActivityAdminChatListBinding;
import com.example.smarthall.model.AdminChatItem;
import com.example.smarthall.ui.adapters.AdminChatListAdapter;
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

public class AdminChatListActivity extends BaseActivity {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";

    private ActivityAdminChatListBinding binding;
    private AdminChatListAdapter adapter;
    private final List<AdminChatItem> chatList = new ArrayList<>();
    private DatabaseReference adminChatListRef;
    private ValueEventListener chatListListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityAdminChatListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        setupFirebaseListener();
    }

    private void setupToolbar() {
        binding.btnChatListBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        binding.recyclerViewAdminChatList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminChatListAdapter(chatList, item -> {
            Intent intent = new Intent(AdminChatListActivity.this, ChatActivity.class);
            intent.putExtra("student_roll", item.getStudentRoll());
            intent.putExtra("chatting_with_role", "student");
            startActivity(intent);
        });
        binding.recyclerViewAdminChatList.setAdapter(adapter);
    }

    private void setupFirebaseListener() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(RTDB_URL);
        adminChatListRef = database.getReference("admin_chat_list");

        binding.progressBarChatList.setVisibility(View.VISIBLE);

        chatListListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String roll = child.getKey();
                    String lastMessage = child.child("lastMessage").getValue(String.class);
                    Long timestamp = child.child("timestamp").getValue(Long.class);
                    Object unreadVal = child.child("unreadCount").getValue();
                    int unreadCount = 0;
                    if (unreadVal instanceof Number) {
                        unreadCount = ((Number) unreadVal).intValue();
                    }

                    if (roll != null) {
                        chatList.add(new AdminChatItem(
                                roll,
                                lastMessage != null ? lastMessage : "",
                                timestamp != null ? timestamp : 0L,
                                unreadCount
                        ));
                    }
                }

                Collections.sort(chatList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                adapter.notifyDataSetChanged();
                binding.progressBarChatList.setVisibility(View.GONE);

                if (chatList.isEmpty()) {
                    binding.layoutEmptyChats.setVisibility(View.VISIBLE);
                    binding.recyclerViewAdminChatList.setVisibility(View.GONE);
                } else {
                    binding.layoutEmptyChats.setVisibility(View.GONE);
                    binding.recyclerViewAdminChatList.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBarChatList.setVisibility(View.GONE);
            }
        };

        adminChatListRef.addValueEventListener(chatListListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adminChatListRef != null && chatListListener != null) {
            adminChatListRef.removeEventListener(chatListListener);
        }
    }
}
