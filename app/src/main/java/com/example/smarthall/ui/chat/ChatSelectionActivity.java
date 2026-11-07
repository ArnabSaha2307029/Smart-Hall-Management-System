package com.example.smarthall.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import com.example.smarthall.databinding.ActivityChatSelectionBinding;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.HallThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChatSelectionActivity extends BaseActivity {

    private ActivityChatSelectionBinding binding;
    private String studentRoll;
    private String studentName;
    private String hallName;
    private int primaryColor;
    private int primaryDarkColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        primaryColor = HallThemeManager.getActivePrimaryColor(this);
        primaryDarkColor = HallThemeManager.getActivePrimaryDarkColor(this);

        Intent incoming = getIntent();
        if (incoming != null) {
            studentRoll = incoming.getStringExtra("student_roll");
            if (TextUtils.isEmpty(studentRoll)) {
                studentRoll = incoming.getStringExtra("studentRoll");
            }
            studentName = incoming.getStringExtra("student_name");
            if (TextUtils.isEmpty(studentName)) {
                studentName = incoming.getStringExtra("studentName");
            }
            hallName = incoming.getStringExtra("hall_name");
            if (TextUtils.isEmpty(hallName)) {
                hallName = incoming.getStringExtra("hallName");
            }
            if (incoming.hasExtra(ChatActivity.EXTRA_PRIMARY_COLOR)) {
                primaryColor = incoming.getIntExtra(ChatActivity.EXTRA_PRIMARY_COLOR, primaryColor);
            }
            if (incoming.hasExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR)) {
                primaryDarkColor = incoming.getIntExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR, primaryDarkColor);
            }
        }

        if (TextUtils.isEmpty(studentRoll)) {
            SharedPreferences userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE);
            studentRoll = userSession.getString("student_roll", userSession.getString("roll", ""));
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
                SharedPreferences prefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                studentRoll = prefs.getString("student_roll", "");
                if (TextUtils.isEmpty(studentName)) {
                    studentName = prefs.getString("student_name", "");
                }
                if (TextUtils.isEmpty(hallName)) {
                    hallName = prefs.getString("hall_name", "");
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

        binding.toolbarChatSelection.setBackgroundColor(primaryColor);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.cardChatAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("student_roll", studentRoll);
            intent.putExtra("studentRoll", studentRoll);
            intent.putExtra("student_name", studentName);
            intent.putExtra("studentName", studentName);
            intent.putExtra("hall_name", hallName);
            intent.putExtra("hallName", hallName);
            intent.putExtra("chatting_with_role", "admin");
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_COLOR, primaryColor);
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR, primaryDarkColor);
            startActivity(intent);
        });

        binding.cardChatAi.setOnClickListener(v -> {
            Intent intent = new Intent(this, AiChatActivity.class);
            intent.putExtra("student_roll", studentRoll);
            intent.putExtra("studentRoll", studentRoll);
            intent.putExtra("student_name", studentName);
            intent.putExtra("studentName", studentName);
            intent.putExtra("hall_name", hallName);
            intent.putExtra("hallName", hallName);
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_COLOR, primaryColor);
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR, primaryDarkColor);
            startActivity(intent);
        });
    }
}
