package com.example.smarthall.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.ActivityChatBinding;
import com.example.smarthall.model.ChatMessage;
import com.example.smarthall.network.FcmNotificationSender;
import com.example.smarthall.ui.adapters.ChatMessageAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.HallThemeManager;
import com.example.smarthall.util.ThemeHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends BaseActivity {

    public static final String EXTRA_PRIMARY_COLOR = "EXTRA_PRIMARY_COLOR";
    public static final String EXTRA_PRIMARY_DARK_COLOR = "EXTRA_PRIMARY_DARK_COLOR";

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
    private int primaryColor;
    private int primaryDarkColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        resolveThemeColors();
        applyDynamicTheming();

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

    private void resolveThemeColors() {
        int defaultPrimary = HallThemeManager.getActivePrimaryColor(this);
        int defaultDark = HallThemeManager.getActivePrimaryDarkColor(this);

        Intent intent = getIntent();
        int colorExtra = 0;
        int darkExtra = 0;

        if (intent != null) {
            colorExtra = intent.getIntExtra(EXTRA_PRIMARY_COLOR, 0);
            if (colorExtra == 0) {
                colorExtra = intent.getIntExtra("extra_primary_color", 0);
            }
            darkExtra = intent.getIntExtra(EXTRA_PRIMARY_DARK_COLOR, 0);
            if (darkExtra == 0) {
                darkExtra = intent.getIntExtra("extra_primary_dark_color", 0);
            }
        }

        if (colorExtra != 0) {
            primaryColor = colorExtra;
        } else {
            primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, defaultPrimary);
        }

        if (darkExtra != 0) {
            primaryDarkColor = darkExtra;
        } else {
            primaryDarkColor = getThemeColor(com.google.android.material.R.attr.colorPrimaryVariant, defaultDark);
        }
    }

    private void applyDynamicTheming() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(primaryDarkColor);
        }

        binding.toolbarChat.setBackgroundColor(primaryColor);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(primaryColor));
        }

        Drawable sendBg = binding.btnChatSend.getBackground();
        if (sendBg != null) {
            DrawableCompat.setTint(DrawableCompat.wrap(sendBg.mutate()), primaryColor);
        }
        ViewCompat.setBackgroundTintList(binding.btnChatSend, ColorStateList.valueOf(primaryColor));
        ImageViewCompat.setImageTintList(binding.btnChatSend, ColorStateList.valueOf(Color.WHITE));

        setupMessageInputField(primaryColor);
    }

    private void setupMessageInputField(int primaryColor) {
        float r = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22, getResources().getDisplayMetrics());
        float strokeDefault = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        float strokeFocused = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f, getResources().getDisplayMetrics());

        GradientDrawable defaultBg = new GradientDrawable();
        defaultBg.setCornerRadius(r);
        defaultBg.setColor(Color.parseColor("#F5F5F5"));
        defaultBg.setStroke((int) strokeDefault, Color.parseColor("#E0E0E0"));
        binding.etChatMessage.setBackground(defaultBg);

        binding.etChatMessage.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(r);
            if (hasFocus) {
                gd.setColor(Color.WHITE);
                gd.setStroke((int) strokeFocused, primaryColor);
            } else {
                gd.setColor(Color.parseColor("#F5F5F5"));
                gd.setStroke((int) strokeDefault, Color.parseColor("#E0E0E0"));
            }
            binding.etChatMessage.setBackground(gd);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        resetUnreadCounter();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            resolveThemeColors();
            applyDynamicTheming();
            if (chatAdapter != null) {
                chatAdapter.setPrimaryThemeColor(primaryColor);
            }
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
            resetUnreadCounter();
        }
    }

    private void resetUnreadCounter() {
        if (TextUtils.isEmpty(studentRoll)) {
            return;
        }
        String cleanRoll = studentRoll.trim().replaceAll("\\s+", "");
        if (cleanRoll.isEmpty()) {
            return;
        }
        if (isStudentUser) {
            FirebaseDatabase.getInstance(RTDB_URL).getReference("student_chat_meta")
                    .child(cleanRoll)
                    .child("unreadCount")
                    .setValue(0);
            FirebaseDatabase.getInstance(RTDB_URL).getReference("chats").child(cleanRoll)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String senderRole = child.child("senderRole").getValue(String.class);
                                Boolean read = child.child("read").getValue(Boolean.class);
                                if ("admin".equalsIgnoreCase(senderRole) && (read == null || !read)) {
                                    child.getRef().child("read").setValue(true);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
        } else {
            FirebaseDatabase.getInstance(RTDB_URL).getReference("admin_chat_list")
                    .child(cleanRoll)
                    .child("unreadCount")
                    .setValue(0);
            FirebaseDatabase.getInstance(RTDB_URL).getReference("chats").child(cleanRoll)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String senderRole = child.child("senderRole").getValue(String.class);
                                Boolean read = child.child("read").getValue(Boolean.class);
                                if ("student".equalsIgnoreCase(senderRole) && (read == null || !read)) {
                                    child.getRef().child("read").setValue(true);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
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
        chatAdapter = new ChatMessageAdapter(messageList, currentUserRole, primaryColor);
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
                    if (isStudentUser) {
                        String senderRole = child.child("senderRole").getValue(String.class);
                        Boolean read = child.child("read").getValue(Boolean.class);
                        if ("admin".equalsIgnoreCase(senderRole) && (read == null || !read)) {
                            child.getRef().child("read").setValue(true);
                        }
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

        ChatMessage chatMessage = new ChatMessage(messageId, currentUserRole, messageText, timestamp, false);
        newMsgRef.setValue(chatMessage);

        Map<String, Object> chatListEntry = new HashMap<>();
        chatListEntry.put("studentRoll", studentRoll);
        chatListEntry.put("lastMessage", messageText);
        chatListEntry.put("timestamp", timestamp);
        adminChatListRef.updateChildren(chatListEntry);

        String cleanRoll = studentRoll.trim().replaceAll("\\s+", "");
        if (isStudentUser) {
            adminChatListRef.child("unreadCount").setValue(ServerValue.increment(1));
        } else {
            FirebaseDatabase.getInstance(RTDB_URL).getReference("student_chat_meta")
                    .child(cleanRoll)
                    .child("unreadCount")
                    .setValue(ServerValue.increment(1));
        }

        String targetTopic;
        String fcmTitle;
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
