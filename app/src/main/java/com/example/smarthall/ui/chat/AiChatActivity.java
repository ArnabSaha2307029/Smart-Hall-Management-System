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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.ActivityAiChatBinding;
import com.example.smarthall.model.ChatMessage;
import com.example.smarthall.ui.adapters.AiChatAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.HallThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiChatActivity extends BaseActivity {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";
    private static final String[] MODEL_CHAIN = {
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-flash-latest",
            "gemini-2.5-pro",
            "gemini-3.5-pro"
    };
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
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
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
            if (TextUtils.isEmpty(studentRoll)) {
                studentRoll = intent.getStringExtra("studentRoll");
            }
            studentName = intent.getStringExtra("student_name");
            if (TextUtils.isEmpty(studentName)) {
                studentName = intent.getStringExtra("studentName");
            }
            hallName = intent.getStringExtra("hall_name");
            if (TextUtils.isEmpty(hallName)) {
                hallName = intent.getStringExtra("hallName");
            }
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
                if (TextUtils.isEmpty(studentName) && !TextUtils.isEmpty(user.getDisplayName())) {
                    studentName = user.getDisplayName();
                }
            }
        }

        applyTheming();
        setupToolbar();
        setupRecyclerView();
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

        dispatchPromptWithFallback(text, 0);
    }

    private void dispatchPromptWithFallback(String userMessageText, int modelIndex) {
        if (modelIndex >= MODEL_CHAIN.length) {
            runOnUiThread(() -> {
                if (binding != null) {
                    binding.layoutAiTyping.setVisibility(View.GONE);
                }
                Log.e("GeminiAPI", "All fallback models in chain exhausted");
                Toast.makeText(AiChatActivity.this, "AI Assistant is temporarily busy. Please try again in a moment.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        if (isFinishing() || isDestroyed()) {
            return;
        }

        try {
            JSONObject payload = new JSONObject();

            JSONObject systemInstructionObj = new JSONObject();
            JSONArray systemParts = new JSONArray();
            JSONObject systemTextObj = new JSONObject();
            systemTextObj.put("text", SYSTEM_INSTRUCTION_TEXT);
            systemParts.put(systemTextObj);
            systemInstructionObj.put("parts", systemParts);
            payload.put("system_instruction", systemInstructionObj);

            JSONArray contentsArray = new JSONArray();
            if (!messageList.isEmpty()) {
                String previousRole = null;
                int start = Math.max(0, messageList.size() - 10);
                for (int i = start; i < messageList.size(); i++) {
                    ChatMessage msg = messageList.get(i);
                    if (msg == null || TextUtils.isEmpty(msg.getMessage())) {
                        continue;
                    }
                    if (i == messageList.size() - 1 && userMessageText.equals(msg.getMessage())) {
                        continue;
                    }
                    String role = "ai".equalsIgnoreCase(msg.getSenderRole()) ? "model" : "user";
                    if (role.equals(previousRole)) {
                        continue;
                    }
                    if (previousRole == null && "model".equals(role)) {
                        continue;
                    }
                    JSONObject historyObj = new JSONObject();
                    historyObj.put("role", role);
                    JSONArray parts = new JSONArray();
                    JSONObject part = new JSONObject();
                    part.put("text", msg.getMessage());
                    parts.put(part);
                    historyObj.put("parts", parts);

                    contentsArray.put(historyObj);
                    previousRole = role;
                }
            }

            JSONObject currentTurn = new JSONObject();
            currentTurn.put("role", "user");
            JSONArray currentParts = new JSONArray();
            JSONObject currentPart = new JSONObject();
            currentPart.put("text", userMessageText);
            currentParts.put(currentPart);
            currentTurn.put("parts", currentParts);
            contentsArray.put(currentTurn);

            payload.put("contents", contentsArray);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1024);
            payload.put("generationConfig", generationConfig);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL_CHAIN[modelIndex]
                    + ":generateContent?key="
                    + GEMINI_API_KEY;

            MediaType mediaType = MediaType.get("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(payload.toString(), mediaType);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    handleModelFailure(userMessageText, modelIndex, "Network call failed: " + e.getMessage(), e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    String responseBody = "";
                    try {
                        if (response.body() != null) {
                            responseBody = response.body().string();
                        }
                    } catch (IOException e) {
                        Log.w("GeminiAPI", "Failed to read response body for model " + MODEL_CHAIN[modelIndex], e);
                    } finally {
                        response.close();
                    }

                    if (response.code() != 200) {
                        handleModelFailure(userMessageText, modelIndex, "HTTP " + response.code() + ": " + responseBody, null);
                        return;
                    }

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (!jsonResponse.has("candidates")) {
                            handleModelFailure(userMessageText, modelIndex, "Response missing candidates: " + responseBody, null);
                            return;
                        }
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        if (candidates.length() == 0) {
                            handleModelFailure(userMessageText, modelIndex, "Candidates array is empty: " + responseBody, null);
                            return;
                        }

                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        String finishReason = firstCandidate.optString("finishReason", "");
                        if ("SAFETY".equalsIgnoreCase(finishReason)) {
                            handleModelFailure(userMessageText, modelIndex, "Content generation blocked by SAFETY filter", null);
                            return;
                        }

                        if (!firstCandidate.has("content")) {
                            handleModelFailure(userMessageText, modelIndex, "Candidate missing content: " + responseBody, null);
                            return;
                        }

                        JSONObject contentObj = firstCandidate.getJSONObject("content");
                        if (!contentObj.has("parts")) {
                            handleModelFailure(userMessageText, modelIndex, "Candidate content missing parts: " + responseBody, null);
                            return;
                        }

                        JSONArray parts = contentObj.getJSONArray("parts");
                        if (parts.length() == 0) {
                            handleModelFailure(userMessageText, modelIndex, "Candidate parts array is empty: " + responseBody, null);
                            return;
                        }

                        StringBuilder extractedText = new StringBuilder();
                        for (int p = 0; p < parts.length(); p++) {
                            JSONObject partObj = parts.getJSONObject(p);
                            if (partObj.has("text")) {
                                extractedText.append(partObj.getString("text"));
                            }
                        }

                        String candidateText = extractedText.toString().trim();
                        if (candidateText.isEmpty()) {
                            handleModelFailure(userMessageText, modelIndex, "Extracted candidate text is empty", null);
                            return;
                        }

                        handleModelSuccess(candidateText);
                    } catch (Exception parseException) {
                        handleModelFailure(userMessageText, modelIndex, "JSON parse failure: " + parseException.getMessage(), parseException);
                    }
                }
            });
        } catch (Exception e) {
            handleModelFailure(userMessageText, modelIndex, "Request preparation failure: " + e.getMessage(), e);
        }
    }

    private void handleModelFailure(String userMessageText, int modelIndex, String reason, Throwable throwable) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            Log.w("GeminiAPI", "Model " + MODEL_CHAIN[modelIndex] + " failed: " + reason, throwable);
            if (modelIndex + 1 < MODEL_CHAIN.length) {
                Log.i("GeminiAPI", "Failing over to: " + MODEL_CHAIN[modelIndex + 1]);
                dispatchPromptWithFallback(userMessageText, modelIndex + 1);
            } else {
                if (binding != null) {
                    binding.layoutAiTyping.setVisibility(View.GONE);
                }
                Log.e("GeminiAPI", "All fallback models in chain exhausted", throwable);
                Toast.makeText(AiChatActivity.this, "AI Assistant is temporarily busy. Please try again in a moment.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleModelSuccess(String replyText) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (binding != null) {
                binding.layoutAiTyping.setVisibility(View.GONE);
            }

            if (aiChatRef != null) {
                String aiMsgId = aiChatRef.push().getKey();
                ChatMessage aiMsg = new ChatMessage(aiMsgId, "ai", replyText.trim(), System.currentTimeMillis(), true);
                if (aiMsgId != null) {
                    aiChatRef.child(aiMsgId).setValue(aiMsg);
                }
            }
            if (!messageList.isEmpty() && binding != null && binding.recyclerViewAiChat != null) {
                binding.recyclerViewAiChat.smoothScrollToPosition(messageList.size() - 1);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().cancelAll();
        }
        if (aiChatRef != null && chatListener != null) {
            aiChatRef.removeEventListener(chatListener);
            chatListener = null;
        }
    }
}
