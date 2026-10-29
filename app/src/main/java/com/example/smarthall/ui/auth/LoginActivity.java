package com.example.smarthall.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.smarthall.databinding.ActivityLoginBinding;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.ui.admin.AdminDashboardActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.student.StudentDashboardActivity;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private HallRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = HallRepository.getInstance();

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Demo Admin fill
        binding.btnFillAdmin.setOnClickListener(v -> {
            binding.radioAdmin.setChecked(true);
            binding.etUserId.setText("admin");
            binding.etPassword.setText("admin123");
        });

        // Demo Student fill
        binding.btnFillStudent.setOnClickListener(v -> {
            binding.radioStudent.setChecked(true);
            binding.etUserId.setText("2307029");
            binding.etPassword.setText("pass123");
        });

        // Login button
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String userId = binding.etUserId.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(userId)) {
            binding.etUserId.setError("User ID is required");
            binding.etUserId.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return;
        }

        String selectedRole;
        if (binding.radioAdmin.isChecked()) {
            selectedRole = "ADMIN";
        } else if (binding.radioStudent.isChecked()) {
            selectedRole = "STUDENT";
        } else {
            Toast.makeText(this, "Please select a role (Admin or Student)", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String authResult = repository.authenticateUser(userId, password, selectedRole);
            if (authResult == null) {
                Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_LONG).show();
                return;
            }

            if ("ADMIN".equals(selectedRole)) {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.putExtra("EXTRA_USER_ID", userId);
                intent.putExtra("EXTRA_USER_ROLE", "ADMIN");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                // authResult is the studentId for student login
                Intent intent = new Intent(this, StudentDashboardActivity.class);
                intent.putExtra("EXTRA_USER_ID", authResult);
                intent.putExtra("EXTRA_USER_ROLE", "STUDENT");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
