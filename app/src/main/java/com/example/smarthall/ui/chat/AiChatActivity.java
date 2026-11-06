package com.example.smarthall.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.ActivityAiChatBinding;
import com.example.smarthall.model.ChatMessage;
import com.example.smarthall.ui.adapters.AiChatAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.HallThemeManager;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AiChatActivity extends BaseActivity {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";
    private static final String GEMINI_MODEL_NAME = "gemini-1.5-flash";
    private static final String GEMINI_API_KEY = "AQ.Ab8RN6KZSwV0Q0ZmVJRgYpVJbLSdDxDKMHdwCZaJBX6MCdEh4g";
    private static final String SYSTEM_INSTRUCTION_TEXT =
            "You are the official AI Resident Assistant for 'SmartHall', a digital Hall Management System app. " +
            "Your job is to assist hall residents (students) and administration (provost, hall staff) by providing " +
            "accurate operational guidance, procedural steps, and support regarding hall policies and app features. " +
            "Core Architecture & App Knowledge: Roles: - Student: Students can submit complaints, check notices, " +
            "use real-time chat with admin, On/Off meal, see room allocation, pay bills. - Admin: Admin can allocate seat, " +
            "update hall notices, watch & resolve complaints, chat with student, see student list, close hall/meal. " +
            "Behavioral Rules & Tone: Tone: Professional, helpful, concise, and structured. " +
            "Security & Authority: You cannot directly modify database records, approve seats, or resolve complaints yourself. " +
            "Always clarify that approvals and administrative actions must be taken by the Admin via the dashboard. " +
            "Language: Respond in the exact language used by the user (Bengali or English).";

    private ActivityAiChatBinding binding;
    private AiChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private DatabaseReference aiChatRef;
    private ValueEventListener chatListener;
    private GenerativeModelFutures geminiModel;
    private String studentRoll;
    private String studentName;
    private String hallName;
    private int primaryColor;
    private int primaryDarkColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        primaryColor = HallThemeManager.getActivePrimaryColor(this);
        primaryDarkColor = HallThemeManager.getActivePrimaryDarkColor(this);

        Intent intent = getIntent();
        if (intent != null) {
            studentRoll = intent.getStringExtra("student_roll");
            studentName = intent.getStringExtra("student_name");
            hallName = intent.getStringExtra("hall_name");
        }

        if (TextUtils.isEmpty(studentRoll)) {
            SharedPreferences userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE);
            studentRoll = userSession.getString("roll", userSession.getString("student_roll", ""));
            if (TextUtils.isEmpty(studentName)) {
                studentName = userSession.getString("student_name", userSession.getString("name", ""));
            }
            if (TextUtils.isEmpty(hallName)) {
                hallName = userSession.getString("hall_name", "");
            }
        }

        if (TextUtils.isEmpty(studentRoll)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SharedPreferences studentPrefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                studentRoll = studentPrefs.getString("student_roll", "");
                if (TextUtils.isEmpty(studentName)) {
                    studentName = studentPrefs.getString("student_name", "");
                }
                if (TextUtils.isEmpty(hallName)) {
                    hallName = studentPrefs.getString("hall_name", "");
                }
            }
        }

        if (TextUtils.isEmpty(studentRoll)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && !TextUtils.isEmpty(user.getUid())) {
                studentRoll = user.getUid();
            }
        }

        applyTheming();
        setupToolbar();
        setupRecyclerView();
        setupGemini();
        setupFirebaseRealtimeDatabase();
        setupSendButton();
    }

    private void applyTheming() {
        if (binding == null) {
            return;
        }
        binding.toolbarAiChat.setBackgroundColor(primaryColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(primaryDarkColor);
        }
        binding.pbAiTyping.setIndeterminateTintList(ColorStateList.valueOf(primaryColor));
        binding.btnChatSend.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        binding.btnAiChatBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerViewAiChat.setLayoutManager(layoutManager);
        adapter = new AiChatAdapter(messageList, primaryColor);
        binding.recyclerViewAiChat.setAdapter(adapter);
    }

    private void setupGemini() {
        try {
            Content systemInstruction = new Content.Builder()
                    .addText(SYSTEM_INSTRUCTION_TEXT)
                    .build();

            GenerativeModel gm = new GenerativeModel(
                    GEMINI_MODEL_NAME,
                    GEMINI_API_KEY,
                    null,
                    null,
                    new RequestOptions(),
                    null,
                    null,
                    systemInstruction
            );

            geminiModel = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e("AiChatActivity", "Failed to initialize Gemini GenerativeModel", e);
        }
    }

    private void setupFirebaseRealtimeDatabase() {
        if (TextUtils.isEmpty(studentRoll)) {
            Log.w("AiChatActivity", "Cannot initialize Realtime Database: studentRoll is null or empty");
            return;
        }

        String cleanRoll = studentRoll.trim().replaceAll("[.#$\\[\\]/\\s+]", "_");
        if (TextUtils.isEmpty(cleanRoll)) {
            Log.w("AiChatActivity", "Cannot initialize Realtime Database: cleanRoll is empty");
            return;
        }

        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(RTDB_URL);
            aiChatRef = database.getReference("ai_chats").child(cleanRoll);

            chatListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    messageList.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ChatMessage msg = child.getValue(ChatMessage.class);
                        if (msg != null) {
                            messageList.add(msg);
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    if (!messageList.isEmpty() && binding != null && binding.recyclerViewAiChat != null) {
                        binding.recyclerViewAiChat.scrollToPosition(messageList.size() - 1);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("AiChatActivity", "Failed to load chat history", error.toException());
                }
            };

            aiChatRef.addValueEventListener(chatListener);
        } catch (Exception e) {
            Log.e("AiChatActivity", "Error initializing Firebase Realtime Database reference", e);
        }
    }

    private void setupSendButton() {
        binding.btnChatSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        if (binding == null) {
            return;
        }

        String text = binding.etChatMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }

        if (geminiModel == null) {
            Toast.makeText(this, "AI Assistant is initializing. Please try again.", Toast.LENGTH_SHORT).show();
            setupGemini();
            return;
        }

        binding.etChatMessage.setText("");

        if (aiChatRef == null) {
            setupFirebaseRealtimeDatabase();
            if (aiChatRef == null) {
                Toast.makeText(this, "Student profile could not be loaded. Please re-open the chat.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String userMsgId = aiChatRef.push().getKey();
        ChatMessage userMsg = new ChatMessage(userMsgId, "student", text, System.currentTimeMillis(), true);
        if (userMsgId != null) {
            aiChatRef.child(userMsgId).setValue(userMsg);
        }

        binding.layoutAiTyping.setVisibility(View.VISIBLE);
        if (!messageList.isEmpty()) {
            binding.recyclerViewAiChat.smoothScrollToPosition(messageList.size() - 1);
        }

        try {
            Content userContent = new Content.Builder()
                    .addText(text)
                    .build();

            ListenableFuture<GenerateContentResponse> response = geminiModel.generateContent(userContent);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (binding != null) {
                            binding.layoutAiTyping.setVisibility(View.GONE);
                        }
                        String aiReply = result != null ? result.getText() : null;
                        if (aiReply == null || aiReply.trim().isEmpty()) {
                            aiReply = "I could not generate a response. Please try again.";
                        }

                        if (aiChatRef != null) {
                            String aiMsgId = aiChatRef.push().getKey();
                            ChatMessage aiMsg = new ChatMessage(aiMsgId, "ai", aiReply.trim(), System.currentTimeMillis(), true);
                            if (aiMsgId != null) {
                                aiChatRef.child(aiMsgId).setValue(aiMsg);
                            }
                        }
                        if (!messageList.isEmpty() && binding != null) {
                            binding.recyclerViewAiChat.smoothScrollToPosition(messageList.size() - 1);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (binding != null) {
                            binding.layoutAiTyping.setVisibility(View.GONE);
                        }
                        Log.e("AiChatActivity", "Gemini API call failed", t);
                        Toast.makeText(AiChatActivity.this, "Unable to get response from AI. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            Log.e("AiChatActivity", "Failed to dispatch generateContent request", e);
            binding.layoutAiTyping.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to send message. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiChatRef != null && chatListener != null) {
            aiChatRef.removeEventListener(chatListener);
            chatListener = null;
        }
    }
}
