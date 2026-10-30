package com.example.smarthall.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ActivityLoginBinding;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.ui.admin.AdminDashboardActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.student.StudentDashboardActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private HallRepository repository;
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = HallRepository.getInstance();
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        db = FirebaseFirestore.getInstance();

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnDemoAdmin.setOnClickListener(v -> {
            binding.radioAdmin.setChecked(true);
            binding.etEmail.setText("adm@aeh.edu");
            binding.etPassword.setText("adm123");
            binding.tilEmail.setError(null);
            binding.tilPassword.setError(null);
        });

        binding.btnDemoStudent.setOnClickListener(v -> {
            binding.radioStudent.setChecked(true);
            binding.etEmail.setText("saha2307029@stud.kuet.ac.bd");
            binding.etPassword.setText("2307029");
            binding.tilEmail.setError(null);
            binding.tilPassword.setError(null);
        });

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoogleSignIn.setOnClickListener(v -> launchGoogleSignIn());
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email");
            binding.etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return;
        }

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        binding.btnLogin.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                if (user != null) {
                    fetchUserRoleAndNavigate(user.getUid());
                } else {
                    binding.btnLogin.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                }
            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                binding.btnLogin.setEnabled(true);
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void fetchUserRoleAndNavigate(String uid) {
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String role = document.getString("role");
                    if ("admin".equalsIgnoreCase(role)) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnLogin.setEnabled(true);
                        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        intent.putExtra("EXTRA_USER_ID", uid);
                        intent.putExtra("EXTRA_USER_ROLE", "ADMIN");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else if ("student".equalsIgnoreCase(role)) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnLogin.setEnabled(true);
                        String studentId = document.getString("studentId");
                        if (studentId == null || studentId.isEmpty()) {
                            studentId = document.getString("rollNumber");
                        }
                        if (studentId == null || studentId.isEmpty() || repository.getStudentById(studentId) == null) {
                            studentId = repository.getStudentById(uid) != null ? uid : "S001";
                        }
                        Intent intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                        intent.putExtra("EXTRA_USER_ID", studentId);
                        intent.putExtra("EXTRA_USER_ROLE", "STUDENT");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Unknown user role: " + role, Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnLogin.setEnabled(true);
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "User profile not registered in hall database", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnLogin.setEnabled(true);
                }
            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to fetch user role";
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.GONE);
                binding.btnLogin.setEnabled(true);
            }
        });
    }

    private void launchGoogleSignIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                getMainExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        if (result.getCredential() instanceof CustomCredential) {
                            CustomCredential customCredential = (CustomCredential) result.getCredential();
                            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                                try {
                                    GoogleIdTokenCredential googleIdTokenCredential =
                                            GoogleIdTokenCredential.createFrom(customCredential.getData());
                                    String idToken = googleIdTokenCredential.getIdToken();
                                    firebaseAuthWithGoogle(idToken);
                                } catch (Exception e) {
                                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        binding.btnLogin.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                Toast.makeText(LoginActivity.this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show();
                if (user != null) {
                    fetchUserRoleAndNavigate(user.getUid());
                } else {
                    binding.btnLogin.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                }
            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                binding.btnLogin.setEnabled(true);
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }
}
