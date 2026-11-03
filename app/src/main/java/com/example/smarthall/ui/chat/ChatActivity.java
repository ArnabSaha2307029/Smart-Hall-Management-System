package com.example.smarthall.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.ActivityChatBinding;
import com.example.smarthall.model.ChatMessage;
import com.example.smarthall.network.FcmNotificationSender;
import com.example.smarthall.ui.adapters.ChatMessageAdapter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends BaseActivity {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";

    private ActivityChatBinding binding;
    private ChatMessageAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private DatabaseReference messagesRef;
    private DatabaseReference adminChatListRef;
    private ValueEventListener messagesListener;

    private String studentRoll;
    private String chattingWithRole;
    private String currentUserRole;
    private boolean isStudentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chattingWithRole = getIntent().getStringExtra("chatting_with_role");
        studentRoll = getIntent().getStringExtra("student_roll");

        if (chattingWithRole == null) {
            SharedPreferences userPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
            String savedRole = userPrefs.getString("user_role", "student");
            chattingWithRole = "admin".equalsIgnoreCase(savedRole) ? "student" : "admin";
        }

        isStudentUser = "admin".equalsIgnoreCase(chattingWithRole);
        currentUserRole = isStudentUser ? "student" : "admin";

        if (TextUtils.isEmpty(studentRoll) && isStudentUser) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SharedPreferences studentPrefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                studentRoll = studentPrefs.getString("student_roll", "");
            }
        }

        setupToolbar();
        setupRecyclerView();
        setupFirebaseReferences();
        setupSendButton();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            String newRole = intent.getStringExtra("chatting_with_role");
            String newRoll = intent.getStringExtra("student_roll");
            if (newRole != null) {
                chattingWithRole = newRole;
                isStudentUser = "admin".equalsIgnoreCase(chattingWithRole);
                currentUserRole = isStudentUser ? "student" : "admin";
            }
            if (newRoll != null) {
                studentRoll = newRoll;
            }
            setupToolbar();
            if (messagesRef != null && messagesListener != null) {
                messagesRef.removeEventListener(messagesListener);
            }
            setupFirebaseReferences();
        }
    }

    private void setupToolbar() {
        binding.btnChatBack.setOnClickListener(v -> finish());
        if (isStudentUser) {
            binding.tvChatTitle.setText("Admin");
        } else {
            binding.tvChatTitle.setText(studentRoll != null && !studentRoll.isEmpty() ? studentRoll : "Student");
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerViewChat.setLayoutManager(layoutManager);
        chatAdapter = new ChatMessageAdapter(messageList, currentUserRole);
        binding.recyclerViewChat.setAdapter(chatAdapter);
    }

    private void setupFirebaseReferences() {
        if (TextUtils.isEmpty(studentRoll)) {
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance(RTDB_URL);
        messagesRef = database.getReference("chats").child(studentRoll);
        adminChatListRef = database.getReference("admin_chat_list").child(studentRoll);

        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ChatMessage msg = child.getValue(ChatMessage.class);
                    if (msg != null) {
                        messageList.add(msg);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    binding.recyclerViewChat.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        messagesRef.addValueEventListener(messagesListener);
    }

    private void setupSendButton() {
        binding.btnChatSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String messageText = binding.etChatMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        if (TextUtils.isEmpty(studentRoll)) {
            Toast.makeText(this, "Student roll not found", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.etChatMessage.setText("");

        long timestamp = System.currentTimeMillis();
        DatabaseReference newMsgRef = messagesRef.push();
        String messageId = newMsgRef.getKey();

        ChatMessage chatMessage = new ChatMessage(messageId, currentUserRole, messageText, timestamp);
        newMsgRef.setValue(chatMessage);

        Map<String, Object> chatListEntry = new HashMap<>();
        chatListEntry.put("studentRoll", studentRoll);
        chatListEntry.put("lastMessage", messageText);
        chatListEntry.put("timestamp", timestamp);
        adminChatListRef.setValue(chatListEntry);

        String targetTopic;
        String fcmTitle;
        String cleanRoll = studentRoll != null ? studentRoll.trim().replaceAll("\\s+", "") : "";
        if (isStudentUser) {
            targetTopic = "admin_inbox";
            fcmTitle = "Student " + (studentRoll != null ? studentRoll.trim() : "");
        } else {
            targetTopic = "student_chat_" + cleanRoll;
            fcmTitle = "Admin";
        }

        FcmNotificationSender.sendChatNotification(
                this,
                targetTopic,
                fcmTitle,
                messageText,
                currentUserRole,
                cleanRoll
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}
