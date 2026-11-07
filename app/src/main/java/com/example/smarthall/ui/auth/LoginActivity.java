package com.example.smarthall.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ActivityLoginBinding;
import com.example.smarthall.databinding.DialogForgotPasswordBinding;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.ui.admin.AdminDashboardActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.student.StudentDashboardActivity;
import com.example.smarthall.ui.theme.ThemeSelectorBottomSheet;
import com.example.smarthall.util.ThemeHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends BaseActivity {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";

    private ActivityLoginBinding binding;
    private HallRepository repository;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private final int[] slideImages = {
        R.drawable.slide_1,
        R.drawable.slide_2,
        R.drawable.slide_3
    };
    private int currentSlideIndex = 0;
    private Timer sliderTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = HallRepository.getInstance();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            restoreLoginForm();
                        }
                    } catch (ApiException e) {
                        if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED
                                || e.getStatusCode() == CommonStatusCodes.CANCELED) {
                            restoreLoginForm();
                        } else {
                            Log.e("AuthError", "Google sign-in error", e);
                            Toast.makeText(LoginActivity.this, "Google sign-in could not be completed. Please try again.", Toast.LENGTH_SHORT).show();
                            restoreLoginForm();
                        }
                    }
                }
        );

        setupImageSwitcher();
        setupClickListeners();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            binding.layoutFormInputs.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.VISIBLE);
            fetchUserRoleAndNavigate(currentUser.getUid());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startImageSlider();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopImageSlider();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopImageSlider();
    }

    private void setupImageSwitcher() {
        binding.imageSwitcher.setFactory(() -> {
            ImageView imageView = new ImageView(LoginActivity.this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return imageView;
        });

        binding.imageSwitcher.setInAnimation(this, android.R.anim.slide_in_left);
        binding.imageSwitcher.setOutAnimation(this, android.R.anim.slide_out_right);

        if (slideImages.length > 0) {
            binding.imageSwitcher.setImageResource(slideImages[0]);
        }
    }

    private void startImageSlider() {
        stopImageSlider();
        sliderTimer = new Timer();
        sliderTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (slideImages.length > 0 && binding != null && binding.imageSwitcher != null) {
                        currentSlideIndex = (currentSlideIndex + 1) % slideImages.length;
                        binding.imageSwitcher.setImageResource(slideImages[currentSlideIndex]);
                    }
                });
            }
        }, 2000, 2000);
    }

    private void stopImageSlider() {
        if (sliderTimer != null) {
            sliderTimer.cancel();
            sliderTimer = null;
        }
    }

    private void setupClickListeners() {
        binding.ivThemeToggle.setOnClickListener(this::showThemePopupMenu);

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });

        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void showThemePopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_theme_selection, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_theme_system) {
                applyNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                return true;
            } else if (itemId == R.id.action_theme_light) {
                applyNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                return true;
            } else if (itemId == R.id.action_theme_dark) {
                applyNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                return true;
            } else if (itemId == R.id.action_theme_colors) {
                ThemeSelectorBottomSheet.newInstance().show(getSupportFragmentManager(), ThemeSelectorBottomSheet.TAG);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void applyNightMode(int mode) {
        AppCompatDelegate.setDefaultNightMode(mode);
        getSharedPreferences("smart_hall_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("night_mode_preference", mode)
                .apply();
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
                    restoreLoginForm();
                }
            } else {
                Log.e("AuthError", "Email sign-in failed", task.getException());
                Toast.makeText(LoginActivity.this, "Authentication failed. Please verify your credentials and try again.", Toast.LENGTH_SHORT).show();
                restoreLoginForm();
            }
        });
    }

    private void fetchUserRoleAndNavigate(String uid) {
        if (TextUtils.isEmpty(uid)) {
            restoreLoginForm();
            return;
        }

        DatabaseReference adminsRef = FirebaseDatabase.getInstance(RTDB_URL)
                .getReference("admins").child(uid);

        adminsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    navigateToAdmin(uid);
                } else {
                    db.collection("users").document(uid).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()
                                && "admin".equalsIgnoreCase(task.getResult().getString("role"))) {
                            navigateToAdmin(uid);
                        } else {
                            navigateToStudent(uid);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AuthError", "Failed to check admins node in RTDB", error.toException());
                db.collection("users").document(uid).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()
                            && "admin".equalsIgnoreCase(task.getResult().getString("role"))) {
                        navigateToAdmin(uid);
                    } else {
                        navigateToStudent(uid);
                    }
                });
            }
        });
    }

    private void navigateToAdmin(String uid) {
        getSharedPreferences("user_session", Context.MODE_PRIVATE)
                .edit()
                .putString("user_role", "admin")
                .apply();
        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        intent.putExtra("EXTRA_USER_ID", uid);
        intent.putExtra("EXTRA_USER_ROLE", "ADMIN");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void completeStudentNavigation(String uid, String roll) {
        SharedPreferences.Editor editor = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                .edit()
                .putString("user_role", "student");
        if (!TextUtils.isEmpty(roll)) {
            editor.putString("roll", roll);
            editor.putString("student_roll", roll);
            editor.putString("user_roll", roll);

            getSharedPreferences("student_prefs_" + uid, Context.MODE_PRIVATE)
                    .edit()
                    .putString("student_roll", roll)
                    .apply();
        }
        editor.apply();

        Intent intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
        intent.putExtra("EXTRA_USER_ID", uid);
        intent.putExtra("EXTRA_USER_ROLE", "STUDENT");
        if (!TextUtils.isEmpty(roll)) {
            intent.putExtra("EXTRA_STUDENT_ROLL", roll);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToStudent(String uid) {
        SharedPreferences sessionPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String cachedRoll = sessionPrefs.getString("roll", "");
        if (TextUtils.isEmpty(cachedRoll)) {
            cachedRoll = sessionPrefs.getString("student_roll", "");
        }
        if (TextUtils.isEmpty(cachedRoll)) {
            cachedRoll = getSharedPreferences("student_prefs_" + uid, Context.MODE_PRIVATE)
                    .getString("student_roll", "");
        }

        if (!TextUtils.isEmpty(cachedRoll)) {
            final String rollToUse = cachedRoll;
            db.collection("students").document(rollToUse).get().addOnCompleteListener(task -> {
                completeStudentNavigation(uid, rollToUse);
            });
            return;
        }

        db.collection("students").whereEqualTo("uid", uid).limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                String roll = doc.getString("roll");
                if (TextUtils.isEmpty(roll)) {
                    roll = doc.getString("rollNumber");
                }
                if (TextUtils.isEmpty(roll)) {
                    roll = doc.getId();
                }
                completeStudentNavigation(uid, roll);
            } else {
                db.collection("users").document(uid).get().addOnCompleteListener(userTask -> {
                    String roll = "";
                    if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                        DocumentSnapshot userDoc = userTask.getResult();
                        roll = userDoc.getString("roll");
                        if (TextUtils.isEmpty(roll)) {
                            roll = userDoc.getString("rollNumber");
                        }
                    }
                    if (!TextUtils.isEmpty(roll)) {
                        final String finalRoll = roll;
                        db.collection("students").document(finalRoll).get().addOnCompleteListener(studentTask -> {
                            completeStudentNavigation(uid, finalRoll);
                        });
                    } else {
                        completeStudentNavigation(uid, "");
                    }
                });
            }
        });
    }

    private void restoreLoginForm() {
        binding.progressBar.setVisibility(View.GONE);
        binding.layoutFormInputs.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(true);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        binding.btnLogin.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    fetchUserRoleAndNavigate(user.getUid());
                } else {
                    restoreLoginForm();
                }
            } else {
                Log.e("AuthError", "Firebase auth with Google failed", task.getException());
                Toast.makeText(LoginActivity.this, "Authentication failed. Please verify your credentials and try again.", Toast.LENGTH_SHORT).show();
                restoreLoginForm();
            }
        });
    }

    private void showForgotPasswordDialog() {
        DialogForgotPasswordBinding dialogBinding = DialogForgotPasswordBinding.inflate(getLayoutInflater());
        String currentEmail = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(currentEmail)) {
            dialogBinding.etResetEmail.setText(currentEmail);
            dialogBinding.etResetEmail.setSelection(currentEmail.length());
        }

        dialogBinding.etResetEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialogBinding.tilResetEmail.setError(null);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_password_title)
                .setMessage(R.string.reset_password_instruction)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_send_reset_link, null)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String email = dialogBinding.etResetEmail.getText() != null
                        ? dialogBinding.etResetEmail.getText().toString().trim()
                        : "";
                dialogBinding.tilResetEmail.setError(null);

                if (TextUtils.isEmpty(email)) {
                    dialogBinding.tilResetEmail.setError(getString(R.string.err_empty_email_reset));
                    dialogBinding.etResetEmail.requestFocus();
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    dialogBinding.tilResetEmail.setError(getString(R.string.err_invalid_email_reset));
                    dialogBinding.etResetEmail.requestFocus();
                    return;
                }

                dialog.dismiss();
                binding.progressBar.setVisibility(View.VISIBLE);

                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            binding.progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                showSuccessMessage(getString(R.string.reset_email_sent_success));
                            } else {
                                Log.e("AuthReset", "Password reset failed", task.getException());
                                showErrorMessage(getString(R.string.err_reset_failed));
                            }
                        });
            });
        });

        dialog.show();
    }

    private void showSuccessMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
