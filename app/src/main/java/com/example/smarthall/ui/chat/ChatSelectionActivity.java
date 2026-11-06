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
    private int primaryColor;
    private int primaryDarkColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        primaryColor = HallThemeManager.getActivePrimaryColor(this);
        primaryDarkColor = HallThemeManager.getActivePrimaryDarkColor(this);

        studentRoll = getIntent().getStringExtra("student_roll");
        if (TextUtils.isEmpty(studentRoll)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SharedPreferences prefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                studentRoll = prefs.getString("student_roll", "");
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
            intent.putExtra("chatting_with_role", "admin");
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_COLOR, primaryColor);
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR, primaryDarkColor);
            startActivity(intent);
        });

        binding.cardChatAi.setOnClickListener(v -> {
            Intent intent = new Intent(this, AiChatActivity.class);
            intent.putExtra("student_roll", studentRoll);
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_COLOR, primaryColor);
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR, primaryDarkColor);
            startActivity(intent);
        });
    }
}
