package com.example.smarthall.ui.student;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.TextView;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ActivityStudentDashboardBinding;
import com.example.smarthall.databinding.DialogEmergencySosBinding;
import com.example.smarthall.model.Complaint;
import com.example.smarthall.model.FeeRecord;
import com.example.smarthall.model.PaymentRecord;
import com.example.smarthall.model.Room;
import com.example.smarthall.model.SeatOccupant;
import com.example.smarthall.model.Student;
import com.example.smarthall.network.FcmNotificationSender;
import com.example.smarthall.receiver.NetworkChangeReceiver;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.services.EmergencySosForegroundService;
import com.example.smarthall.ui.adapters.ComplaintAdapter;
import com.example.smarthall.ui.adapters.FeeRecordAdapter;
import com.example.smarthall.ui.auth.LoginActivity;
import com.example.smarthall.ui.chat.ChatActivity;
import com.example.smarthall.ui.chat.ChatSelectionActivity;
import com.example.smarthall.ui.complaint.ComplaintDetailActivity;
import com.example.smarthall.ui.complaint.ComplaintsListActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.customview.RoomSeatView;
import com.example.smarthall.ui.theme.ThemeSelectorBottomSheet;
import com.example.smarthall.util.BillingEngine;
import com.example.smarthall.util.EmergencySosManager;
import com.example.smarthall.util.ThemeHelper;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentDashboardActivity extends BaseActivity {

    private ActivityStudentDashboardBinding binding;
    private HallRepository repository;
    private Student student;
    private FeeRecordAdapter feeRecordAdapter;
    private ListenerRegistration feeListenerRegistration;
    private ListenerRegistration paymentListenerRegistration;
    private ListenerRegistration roomListener;
    private ComplaintAdapter complaintAdapter;
    private DatabaseReference rtdbComplaintsRef;
    private ValueEventListener rtdbComplaintsListener;
    private final List<Complaint> studentComplaintsList = new ArrayList<>();
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> uCropLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri tempCameraUri;
    private NetworkChangeReceiver networkChangeReceiver;
    private Snackbar networkSnackbar;
    private boolean isPreviouslyOffline = false;
    private boolean isNotificationPermissionGranted = false;
    private ValueEventListener chatBadgeListener;
    private DatabaseReference chatBadgeRef;
    private TextView tvChatBadgeCount;
    private GoogleSignInClient googleLinkClient;
    private ActivityResultLauncher<Intent> googleLinkLauncher;
    private String linkedGoogleEmail;
    private NavigationView navigationView;
    private DatabaseReference studentRtdbRef;
    private ValueEventListener studentRtdbListener;
    private final ExecutorService imageProcessingExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleLinkClient = GoogleSignIn.getClient(this, gso);

        googleLinkLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            linkGoogleAccount(account);
                        }
                    } catch (ApiException e) {
                        if (e.getStatusCode() != GoogleSignInStatusCodes.SIGN_IN_CANCELLED
                                && e.getStatusCode() != CommonStatusCodes.CANCELED) {
                            Log.e("AuthError", "Google sign-in error", e);
                            Toast.makeText(StudentDashboardActivity.this, "Google sign-in could not be completed. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d("FCM", "POST_NOTIFICATIONS permission granted");
                        isNotificationPermissionGranted = true;
                        subscribeToStudentTopics();
                    } else {
                        Log.w("FCM", "POST_NOTIFICATIONS permission denied");
                        isNotificationPermissionGranted = false;
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        startCrop(uri);
                    }
                }
        );

        uCropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri croppedUri = com.yalantis.ucrop.UCrop.getOutput(result.getData());
                        if (croppedUri != null) {
                            saveImageToInternalStorage(croppedUri);
                        }
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && tempCameraUri != null) {
                        startCrop(tempCameraUri);
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        dispatchTakePictureIntent();
                    } else {
                        Toast.makeText(this, "Camera permission is required to take photo", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                isNotificationPermissionGranted = false;
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                isNotificationPermissionGranted = true;
                subscribeToStudentTopics();
            }
        } else {
            isNotificationPermissionGranted = true;
            subscribeToStudentTopics();
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d("FCM", "Device token: " + task.getResult());
                    } else {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    }
                });

        getSharedPreferences("user_session", Context.MODE_PRIVATE)
                .edit()
                .putString("user_role", "student")
                .apply();

        repository = HallRepository.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        FirebaseMessaging.getInstance().subscribeToTopic("hall_sos_alerts");

        SharedPreferences prefs = getSharedPreferences("student_prefs_" + currentUser.getUid(), Context.MODE_PRIVATE);

        student = new Student();
        student.setId(currentUser.getUid());
        student.setName(prefs.getString("student_name", "Student"));
        String cachedRoll = prefs.getString("student_roll", "");
        if (cachedRoll.isEmpty()) {
            cachedRoll = getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("roll", "");
        }
        if (cachedRoll.isEmpty()) {
            cachedRoll = getIntent().getStringExtra("EXTRA_STUDENT_ROLL");
            if (cachedRoll == null) cachedRoll = "";
        }
        student.setRollNumber(cachedRoll);
        if (!cachedRoll.isEmpty()) {
            getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    .edit()
                    .putString("user_roll", cachedRoll)
                    .putString("student_roll", cachedRoll)
                    .putString("roll", cachedRoll)
                    .apply();
            cleanupGhostUidDocument(currentUser.getUid(), cachedRoll);
        }
        student.setDepartment(prefs.getString("student_department", ""));
        student.setEmail(prefs.getString("student_email", currentUser.getEmail() != null ? currentUser.getEmail() : ""));
        student.setPhone(prefs.getString("student_phone", ""));
        student.setAllocatedRoom(prefs.getString("student_room", ""));
        student.setBoarderNo(prefs.getString("boarder_no", ""));
        student.setDining(prefs.getString("dining_hall", ""));
        linkedGoogleEmail = prefs.getString("linked_google_email", "");
        navigationView = binding.navigationView;
        tvChatBadgeCount = binding.tvTopChatBadgeCount;

        boolean cachedDm = prefs.getBoolean("is_dining_manager", false);
        student.setDiningManager(cachedDm);
        binding.tvDiningManagerBadge.setVisibility(cachedDm ? View.VISIBLE : View.GONE);

        setupHeader();
        setupNavigationDrawer();
        setupProfileCard();
        loadStudentDataFromFirestore();
        loadBoarderAndDining();
        setupSeatView();
        setupFeeRecords();
        setupComplaints();
        setupSubmitComplaintButton();
        setupViewNoticesButton();
        setupMealManagementButton();
        setupUnpaidBillButton();
        setupDiningManagerListener();
        loadProfileImage();
        binding.ivProfileAvatar.setOnClickListener(v -> showImagePickerDialog());
        binding.cardProfileAvatar.setOnClickListener(v -> showImagePickerDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }
        loadStudentDataFromFirestore();
        setupProfileCard();
        loadBoarderAndDining();
        setupSeatView();
        refreshFees();
        refreshComplaints();
        loadProfileImage();
        setupChatBadgeListener();
        setupDiningManagerListener();
        Intent serviceIntent = new Intent(this, EmergencySosForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        checkAndRequestBatteryOptimizationExemption();
        updateDrawerMenuState();
    }

    private void checkAndRequestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    SharedPreferences prefs = getSharedPreferences("smart_hall_prefs", MODE_PRIVATE);
                    boolean alreadyPrompted = prefs.getBoolean("battery_optimization_prompted", false);
                    if (!alreadyPrompted) {
                        prefs.edit().putBoolean("battery_optimization_prompted", true).apply();
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                }
            } catch (Exception e) {
                Log.e("StudentDashboard", "Failed to request battery optimization exemption", e);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        networkChangeReceiver = new NetworkChangeReceiver(isConnected -> {
            int primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFFC62828);
            int primaryVariant = getThemeColor(com.google.android.material.R.attr.colorPrimaryVariant, 0xFF801313);
            if (!isConnected) {
                isPreviouslyOffline = true;
                if (networkSnackbar != null && networkSnackbar.isShown()) {
                    networkSnackbar.dismiss();
                }
                networkSnackbar = Snackbar.make(binding.getRoot(), "No Internet Connection. Offline mode.", Snackbar.LENGTH_INDEFINITE);
                networkSnackbar.getView().setBackgroundColor(primaryVariant);
                ((android.widget.TextView) networkSnackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text)).setTextColor(Color.WHITE);
                networkSnackbar.show();
            } else {
                if (networkSnackbar != null && networkSnackbar.isShown()) {
                    networkSnackbar.dismiss();
                }
                if (isPreviouslyOffline) {
                    Snackbar onlineSnack = Snackbar.make(binding.getRoot(), "Back online", Snackbar.LENGTH_SHORT);
                    onlineSnack.getView().setBackgroundColor(primaryColor);
                    ((android.widget.TextView) onlineSnack.getView().findViewById(com.google.android.material.R.id.snackbar_text)).setTextColor(Color.WHITE);
                    onlineSnack.show();
                    isPreviouslyOffline = false;
                }
            }
        });
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
        if (networkSnackbar != null && networkSnackbar.isShown()) {
            networkSnackbar.dismiss();
        }
        if (roomListener != null) {
            roomListener.remove();
            roomListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (feeListenerRegistration != null) {
            feeListenerRegistration.remove();
            feeListenerRegistration = null;
        }
        if (paymentListenerRegistration != null) {
            paymentListenerRegistration.remove();
            paymentListenerRegistration = null;
        }
        if (roomListener != null) {
            roomListener.remove();
            roomListener = null;
        }
        if (chatBadgeRef != null && chatBadgeListener != null) {
            chatBadgeRef.removeEventListener(chatBadgeListener);
            chatBadgeListener = null;
            chatBadgeRef = null;
        }
        if (studentRtdbRef != null && studentRtdbListener != null) {
            studentRtdbRef.removeEventListener(studentRtdbListener);
            studentRtdbListener = null;
            studentRtdbRef = null;
        }
        if (rtdbComplaintsRef != null && rtdbComplaintsListener != null) {
            rtdbComplaintsRef.removeEventListener(rtdbComplaintsListener);
            rtdbComplaintsListener = null;
            rtdbComplaintsRef = null;
        }
        if (imageProcessingExecutor != null && !imageProcessingExecutor.isShutdown()) {
            imageProcessingExecutor.shutdown();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (binding.drawerLayout != null) {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getActiveStudentRoll() {
        String roll = student != null && !TextUtils.isEmpty(student.getRollNumber())
                ? student.getRollNumber().trim()
                : "";
        if (TextUtils.isEmpty(roll)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SharedPreferences prefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                roll = prefs.getString("student_roll", "").trim();
            }
        }
        if (TextUtils.isEmpty(roll)) {
            SharedPreferences sessionPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
            roll = sessionPrefs.getString("roll", "").trim();
            if (TextUtils.isEmpty(roll)) {
                roll = sessionPrefs.getString("student_roll", "").trim();
            }
            if (TextUtils.isEmpty(roll)) {
                roll = sessionPrefs.getString("user_roll", "").trim();
            }
        }
        return roll;
    }

    private void cleanupGhostUidDocument(String uid, String studentRoll) {
        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(studentRoll) || uid.equals(studentRoll)) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("students").document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot != null && snapshot.exists()) {
                Map<String, Object> ghostData = snapshot.getData();
                if (ghostData != null && !ghostData.isEmpty()) {
                    Map<String, Object> fieldsToMerge = new HashMap<>();
                    String[] syncKeys = {"profileImageBase64", "boarderNo", "dining", "feeStatus", "room", "seat", "allocatedRoom", "allocatedSeat", "allocationStatus", "isDiningManager"};
                    for (String key : syncKeys) {
                        Object val = ghostData.get(key);
                        if (val != null) {
                            fieldsToMerge.put(key, val);
                        }
                    }

                    if (!fieldsToMerge.isEmpty()) {
                        db.collection("students").document(studentRoll)
                                .set(fieldsToMerge, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("students").document(uid).delete()
                                            .addOnSuccessListener(del -> Log.d("GhostCleanup", "Obsolete ghost UID document deleted: " + uid))
                                            .addOnFailureListener(e -> Log.e("GhostCleanup", "Failed to delete ghost UID document", e));
                                })
                                .addOnFailureListener(e -> Log.e("GhostCleanup", "Failed to merge ghost data into roll document", e));
                    } else {
                        db.collection("students").document(uid).delete()
                                .addOnSuccessListener(del -> Log.d("GhostCleanup", "Empty ghost UID document deleted: " + uid))
                                .addOnFailureListener(e -> Log.e("GhostCleanup", "Failed to delete ghost UID document", e));
                    }
                } else {
                    db.collection("students").document(uid).delete()
                            .addOnSuccessListener(del -> Log.d("GhostCleanup", "Empty ghost UID document deleted: " + uid))
                            .addOnFailureListener(e -> Log.e("GhostCleanup", "Failed to delete ghost UID document", e));
                }
            }
        }).addOnFailureListener(e -> Log.e("GhostCleanup", "Failed to check ghost UID document", e));
    }

    private void setupDiningManagerListener() {
        String roll = getActiveStudentRoll();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";

        if (studentRtdbRef != null && studentRtdbListener != null) {
            studentRtdbRef.removeEventListener(studentRtdbListener);
            studentRtdbListener = null;
            studentRtdbRef = null;
        }

        String targetKey = !TextUtils.isEmpty(roll) ? roll.trim() : uid.trim();
        targetKey = targetKey.replaceAll("[.#$\\[\\]]", "");
        if (TextUtils.isEmpty(targetKey) || "null".equalsIgnoreCase(targetKey)) {
            Log.w("StudentDashboardError", "Operation failed: Invalid targetKey for student RTDB listener: " + targetKey);
            return;
        }

        FirebaseDatabase rtdb = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com");
        studentRtdbRef = rtdb.getReference("students").child(targetKey);
        studentRtdbListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isManager = false;
                if (snapshot.exists()) {
                    Boolean val = snapshot.child("isDiningManager").getValue(Boolean.class);
                    isManager = Boolean.TRUE.equals(val);
                }

                if (student != null) {
                    student.setDiningManager(isManager);
                }
                if (binding != null && binding.tvDiningManagerBadge != null) {
                    binding.tvDiningManagerBadge.setVisibility(isManager ? View.VISIBLE : View.GONE);
                }
                if (currentUser != null) {
                    getSharedPreferences("student_prefs_" + currentUser.getUid(), Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("is_dining_manager", isManager)
                            .apply();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentDashboardError", "Operation failed: RTDB student profile listener cancelled - Code: " + error.getCode() + ", Message: " + error.getMessage(), error.toException());
            }
        };
        studentRtdbRef.addValueEventListener(studentRtdbListener);
    }

    private void setupChatBadgeListener() {
        if (tvChatBadgeCount == null && binding != null) {
            tvChatBadgeCount = binding.tvTopChatBadgeCount;
        }
        if (tvChatBadgeCount == null) {
            return;
        }
        String roll = getActiveStudentRoll();
        if (TextUtils.isEmpty(roll)) {
            return;
        }
        String cleanRoll = roll.replaceAll("\\s+", "");
        DatabaseReference newRef = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com")
                .getReference("chats")
                .child(cleanRoll);

        if (chatBadgeRef != null && chatBadgeListener != null) {
            chatBadgeRef.removeEventListener(chatBadgeListener);
        }
        chatBadgeRef = newRef;
        chatBadgeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String senderRole = child.child("senderRole").getValue(String.class);
                    Boolean isRead = child.child("read").getValue(Boolean.class);
                    if ("admin".equalsIgnoreCase(senderRole) && (isRead == null || !isRead)) {
                        count++;
                    }
                }
                updateBadgeCount(tvChatBadgeCount, count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        chatBadgeRef.addValueEventListener(chatBadgeListener);
    }

    private void updateBadgeCount(TextView tvBadgeCount, int count) {
        if (tvBadgeCount == null) {
            return;
        }
        if (count > 0) {
            tvBadgeCount.setVisibility(View.VISIBLE);
            if (count > 99) {
                tvBadgeCount.setText("99+");
            } else {
                tvBadgeCount.setText(String.valueOf(count));
            }
        } else {
            tvBadgeCount.setVisibility(View.GONE);
        }
    }

    private void showSosDialog() {
        DialogEmergencySosBinding dialogBinding = DialogEmergencySosBinding.inflate(getLayoutInflater());

        dialogBinding.etSosLocation.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialogBinding.tilSosLocation.setError(null);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Emergency SOS Broadcast")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("SEND EMERGENCY SOS", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String locationText = dialogBinding.etSosLocation.getText() != null
                        ? dialogBinding.etSosLocation.getText().toString().trim()
                        : "";
                String incidentText = dialogBinding.etSosIncident.getText() != null
                        ? dialogBinding.etSosIncident.getText().toString().trim()
                        : "";

                if (TextUtils.isEmpty(locationText)) {
                    dialogBinding.tilSosLocation.setError("Location is required");
                    dialogBinding.etSosLocation.requestFocus();
                    return;
                }

                dialog.dismiss();

                String currentStudentRoll = getActiveStudentRoll();
                String currentStudentName = "";
                if (student != null && !TextUtils.isEmpty(student.getName())) {
                    currentStudentName = student.getName().trim();
                }
                if (TextUtils.isEmpty(currentStudentName)) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        SharedPreferences prefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                        currentStudentName = prefs.getString("student_name", "").trim();
                        if (TextUtils.isEmpty(currentStudentName) && user.getDisplayName() != null) {
                            currentStudentName = user.getDisplayName().trim();
                        }
                    }
                }
                if (TextUtils.isEmpty(currentStudentName)) {
                    currentStudentName = !TextUtils.isEmpty(currentStudentRoll) ? "Student " + currentStudentRoll : "Hall Resident";
                }

                DatabaseReference targetRef = EmergencySosManager.getSosReference();
                String sosId = targetRef.push().getKey();
                if (sosId == null) {
                    sosId = java.util.UUID.randomUUID().toString();
                }

                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("sosId", sosId);
                payload.put("senderRoll", currentStudentRoll);
                payload.put("senderName", currentStudentName);
                payload.put("location", locationText);
                payload.put("incident", incidentText.isEmpty() ? "Not specified" : incidentText);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("status", "ACTIVE");

                targetRef.child(sosId).setValue(payload)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(StudentDashboardActivity.this, "Emergency alert sent to all hall members.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("EmergencySos", "Failed to broadcast emergency SOS", e);
                            Toast.makeText(StudentDashboardActivity.this, "Failed to broadcast emergency SOS. Please retry.", Toast.LENGTH_SHORT).show();
                        });

                FcmNotificationSender.sendSosAlertNotification(
                        StudentDashboardActivity.this,
                        sosId,
                        currentStudentName,
                        currentStudentRoll,
                        locationText,
                        incidentText.isEmpty() ? "Immediate assistance needed" : incidentText,
                        System.currentTimeMillis()
                );
            });
        });

        dialog.show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel noticeChannel = new NotificationChannel(
                        "hall_notices_channel",
                        "Hall Announcements",
                        NotificationManager.IMPORTANCE_HIGH);
                noticeChannel.enableLights(true);
                noticeChannel.enableVibration(true);
                noticeChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(noticeChannel);

                NotificationChannel chatChannel = new NotificationChannel(
                        "hall_chat_channel",
                        "Hall Chat Messages",
                        NotificationManager.IMPORTANCE_HIGH);
                chatChannel.enableLights(true);
                chatChannel.setLightColor(Color.RED);
                chatChannel.enableVibration(true);
                chatChannel.setVibrationPattern(new long[]{0, 250, 250, 250});
                chatChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                chatChannel.setBypassDnd(true);
                nm.createNotificationChannel(chatChannel);
            }
        }
    }

    private void subscribeToStudentTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic("hall_sos_alerts");
        if (!isNotificationPermissionGranted) {
            return;
        }

        String noticeTopic = "hall_notices";
        Log.e("TOPIC_CHECK", "Subscribing strictly to: [" + noticeTopic + "]");
        FirebaseMessaging.getInstance().subscribeToTopic(noticeTopic)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM_SUB", "Successfully subscribed to topic: " + noticeTopic);
                    } else {
                        Log.e("FCM_SUB", "Failed to subscribe to topic: " + noticeTopic, task.getException());
                    }
                });

        String roll = null;
        if (student != null && student.getRollNumber() != null && !student.getRollNumber().trim().isEmpty()) {
            roll = student.getRollNumber().trim();
        }
        if (roll == null || roll.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                SharedPreferences prefs = getSharedPreferences("student_prefs_" + currentUser.getUid(), Context.MODE_PRIVATE);
                String savedRoll = prefs.getString("student_roll", "");
                if (savedRoll != null && !savedRoll.trim().isEmpty()) {
                    roll = savedRoll.trim();
                }
            }
        }

        if (roll != null && !roll.trim().isEmpty()) {
            final String studentRoll = roll.trim().replaceAll("\\s+", "");
            final String chatTopic = "student_chat_" + studentRoll;
            Log.e("TOPIC_CHECK", "Subscribing strictly to: [" + chatTopic + "]");
            FirebaseMessaging.getInstance().subscribeToTopic(chatTopic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FCM_SUB", "Successfully subscribed to topic: " + chatTopic);
                        } else {
                            Log.e("FCM_SUB", "Failed to subscribe to topic: " + chatTopic, task.getException());
                        }
                    });
        }
    }

    private void setupViewNoticesButton() {
        binding.btnTopNotices.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoticeListActivity.class);
            startActivity(intent);
        });
        binding.btnNavViewNotices.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(this, NoticeListActivity.class);
            startActivity(intent);
        });
    }

    private void setupMealManagementButton() {
        View.OnClickListener listener = v -> {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
            Intent intent = new Intent(this, MealManagementActivity.class);
            if (student != null && !TextUtils.isEmpty(student.getRollNumber())) {
                intent.putExtra("EXTRA_STUDENT_ROLL", student.getRollNumber());
            }
            startActivity(intent);
        };
        binding.btnMealManagement.setOnClickListener(listener);
        binding.btnNavMealManagement.setOnClickListener(listener);
    }

    private void setupUnpaidBillButton() {
        View.OnClickListener listener = v -> {
            if (binding.drawerLayout != null && binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
            startActivity(new Intent(this, UnpaidBillActivity.class));
        };
        binding.btnOpenUnpaidBill.setOnClickListener(listener);
        binding.btnNavUnpaidBill.setOnClickListener(listener);
    }

    private void setupHeader() {
        binding.btnMenuToggle.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.btnSos.setOnClickListener(v -> showSosDialog());
        View.OnClickListener chatClickListener = v -> launchChatSelection();
        binding.layoutTopChat.setOnClickListener(chatClickListener);
        binding.btnTopChat.setOnClickListener(chatClickListener);
        binding.btnTopNotices.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoticeListActivity.class);
            startActivity(intent);
        });
        binding.btnTopLogout.setOnClickListener(v -> logout());
    }

    private void launchChatSelection() {
        Intent intent = new Intent(this, ChatSelectionActivity.class);
        intent.putExtra("student_roll", getActiveStudentRoll());
        if (student != null) {
            intent.putExtra("student_name", student.getName());
            intent.putExtra("hall_name", student.getHallName());
        } else {
            intent.putExtra("hall_name", getString(R.string.hall_name));
        }
        intent.putExtra(ChatActivity.EXTRA_PRIMARY_COLOR, com.example.smarthall.util.HallThemeManager.getActivePrimaryColor(this));
        intent.putExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR, com.example.smarthall.util.HallThemeManager.getActivePrimaryDarkColor(this));
        startActivity(intent);
    }

    private void setupNavigationDrawer() {
        if (student != null) {
            binding.tvDrawerStudentName.setText(student.getName());
            binding.tvDrawerStudentRoll.setText(student.getRollNumber());
        }
        binding.btnNavRoomSeat.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            binding.scrollViewContent.smoothScrollTo(0, binding.cardRoomSeat.getTop());
        });
        binding.btnNavSubmitComplaint.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(this, ComplaintsListActivity.class);
            startActivity(intent);
        });
        binding.btnNavFeeRecords.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            binding.scrollViewContent.smoothScrollTo(0, binding.cardFeeRecords.getTop());
        });
        binding.btnNavAppTheme.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            ThemeSelectorBottomSheet.newInstance().show(getSupportFragmentManager(), ThemeSelectorBottomSheet.TAG);
        });
        binding.btnNavLogout.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            logout();
        });
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int id = item.getItemId();
                    if (id == R.id.nav_chat) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START);
                        launchChatSelection();
                        return true;
                    } else if (id == R.id.nav_link_google) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START);
                        if (isGoogleAccountLinked()) {
                            Toast.makeText(StudentDashboardActivity.this, "Google account is already linked.", Toast.LENGTH_SHORT).show();
                        } else {
                            if (googleLinkClient != null) {
                                googleLinkClient.signOut().addOnCompleteListener(StudentDashboardActivity.this, task -> {
                                    googleLinkLauncher.launch(googleLinkClient.getSignInIntent());
                                });
                            }
                        }
                        return true;
                    } else if (id == R.id.nav_logout) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START);
                        logout();
                        return true;
                    }
                    return false;
                }
            });
            updateDrawerMenuState();
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout != null && binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setupProfileCard() {
        if (student == null) {
            return;
        }
        binding.tvDrawerStudentName.setText(student.getName());
        binding.tvDrawerStudentRoll.setText(student.getRollNumber());
        binding.tvStudentName.setText(student.getName());
        binding.tvStudentRoll.setText("Roll: " + (!TextUtils.isEmpty(student.getRollNumber()) ? student.getRollNumber() : ""));
        binding.tvStudentDept.setText("Department: " + (!TextUtils.isEmpty(student.getDepartment()) ? student.getDepartment() : ""));
        binding.tvStudentPhone.setText("Phone: " + (!TextUtils.isEmpty(student.getPhone()) ? student.getPhone() : ""));
        binding.tvStudentEmail.setText("Email: " + (!TextUtils.isEmpty(student.getEmail()) ? student.getEmail() : ""));

        if (student.isAssigned()) {
            binding.tvRoomSeatInfo.setText("Room " + student.getAllocatedRoom()
                    + " — Seat " + (!TextUtils.isEmpty(student.getAllocatedSeat()) ? student.getAllocatedSeat() : "A"));
        } else if (!TextUtils.isEmpty(student.getAllocatedRoom())) {
            binding.tvRoomSeatInfo.setText("Room " + student.getAllocatedRoom());
        } else {
            binding.tvRoomSeatInfo.setText("Room: Not Assigned");
        }

        int statusTextColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFFC62828);
        if (FeeRecord.STATUS_PAID.equals(student.getFeeStatus())) {
            binding.tvFeeStatusCard.setText("Fee Status: PAID");
            binding.tvFeeStatusCard.setTextColor(statusTextColor);
        } else {
            binding.tvFeeStatusCard.setText("Fee Status: UNPAID");
            binding.tvFeeStatusCard.setTextColor(statusTextColor);
        }

        if (!TextUtils.isEmpty(student.getBoarderNo())) {
            binding.tvBoarderNo.setText("Boarder No: " + student.getBoarderNo());
        }
        if (!TextUtils.isEmpty(student.getDining())) {
            String cleanDining = student.getDining() != null ? student.getDining().replaceAll("(?i)\\bhall\\b", "").trim() : "East";
            if (cleanDining.isEmpty()) cleanDining = "East";
            binding.tvDiningHall.setText("Dining: " + cleanDining);
        }
        if (binding.tvDiningManagerBadge != null) {
            binding.tvDiningManagerBadge.setVisibility(student.isDiningManager() ? View.VISIBLE : View.GONE);
        }
        updateDrawerMenuState();
    }

    private void loadStudentDataFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("student_prefs_" + uid, Context.MODE_PRIVATE);
        String roll = getActiveStudentRoll();

        if (!TextUtils.isEmpty(roll)) {
            db.collection("students").document(roll).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    applyStudentSnapshot(task.getResult(), prefs);
                    cleanupGhostUidDocument(uid, roll);
                } else {
                    fallbackLoadByUidOrUsers(db, uid, prefs);
                }
            });
        } else {
            fallbackLoadByUidOrUsers(db, uid, prefs);
        }
    }

    private void fallbackLoadByUidOrUsers(FirebaseFirestore db, String uid, SharedPreferences prefs) {
        db.collection("students").whereEqualTo("uid", uid).limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                applyStudentSnapshot(doc, prefs);
                String resolvedRoll = doc.getString("roll");
                if (TextUtils.isEmpty(resolvedRoll)) {
                    resolvedRoll = doc.getString("rollNumber");
                }
                if (TextUtils.isEmpty(resolvedRoll)) {
                    resolvedRoll = doc.getId();
                }
                if (!TextUtils.isEmpty(resolvedRoll)) {
                    cleanupGhostUidDocument(uid, resolvedRoll);
                }
            } else {
                db.collection("users").document(uid).get().addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                        DocumentSnapshot userDoc = userTask.getResult();
                        applyStudentSnapshot(userDoc, prefs);
                        String userRoll = userDoc.getString("roll");
                        if (TextUtils.isEmpty(userRoll)) {
                            userRoll = userDoc.getString("rollNumber");
                        }
                        if (!TextUtils.isEmpty(userRoll)) {
                            cleanupGhostUidDocument(uid, userRoll);
                            db.collection("students").document(userRoll).get().addOnSuccessListener(sDoc -> {
                                if (sDoc != null && sDoc.exists()) {
                                    applyStudentSnapshot(sDoc, prefs);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void applyStudentSnapshot(DocumentSnapshot doc, SharedPreferences prefs) {
        String name = doc.getString("name");
        String roll = doc.getString("roll");
        if (TextUtils.isEmpty(roll)) {
            roll = doc.getString("rollNumber");
        }
        if (TextUtils.isEmpty(roll)) {
            roll = doc.getString("studentId");
        }
        FirebaseUser currentAuthUser = FirebaseAuth.getInstance().getCurrentUser();
        if (TextUtils.isEmpty(roll) && doc.getId() != null && (currentAuthUser == null || !doc.getId().equals(currentAuthUser.getUid()))) {
            roll = doc.getId();
        }
        String dept = doc.getString("department");
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        String room = doc.getString("room");
        if (TextUtils.isEmpty(room)) {
            room = doc.getString("allocatedRoom");
        }
        if (TextUtils.isEmpty(room)) {
            room = doc.getString("roomNo");
        }
        String seat = doc.getString("seat");
        if (TextUtils.isEmpty(seat)) {
            seat = doc.getString("allocatedSeat");
        }
        String allocationStatus = doc.getString("allocationStatus");
        String feeStatus = doc.getString("feeStatus");
        String boarderNo = doc.getString("boarderNo");
        String dining = doc.getString("dining");
        String base64Image = doc.getString("profileImageBase64");

        if (student == null) {
            student = new Student();
            student.setId(doc.getId());
        }

        if (!TextUtils.isEmpty(name)) student.setName(name);
        if (!TextUtils.isEmpty(roll)) student.setRollNumber(roll);
        if (!TextUtils.isEmpty(dept)) student.setDepartment(dept);
        if (!TextUtils.isEmpty(email)) student.setEmail(email);
        if (!TextUtils.isEmpty(phone)) student.setPhone(phone);
        if (!TextUtils.isEmpty(room)) student.setAllocatedRoom(room);
        if (!TextUtils.isEmpty(seat)) student.setAllocatedSeat(seat);
        if (!TextUtils.isEmpty(feeStatus)) student.setFeeStatus(feeStatus);
        if (!TextUtils.isEmpty(boarderNo)) student.setBoarderNo(boarderNo);
        if (!TextUtils.isEmpty(dining)) {
            String cleanDining = dining.replaceAll("(?i)\\bhall\\b", "").trim();
            if (cleanDining.isEmpty()) cleanDining = "East";
            student.setDining(cleanDining);
        }
        if (!TextUtils.isEmpty(base64Image)) student.setProfileImageBase64(base64Image);

        Boolean isDm = doc.getBoolean("isDiningManager");
        boolean isDiningManager = Boolean.TRUE.equals(isDm);
        student.setDiningManager(isDiningManager);
        if (binding.tvDiningManagerBadge != null) {
            binding.tvDiningManagerBadge.setVisibility(isDiningManager ? View.VISIBLE : View.GONE);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_dining_manager", isDiningManager);
        if (!TextUtils.isEmpty(name)) editor.putString("student_name", name);
        if (roll != null && !roll.trim().isEmpty()) {
            final String studentRoll = roll.trim().replaceAll("\\s+", "");
            editor.putString("student_roll", studentRoll);
            getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    .edit()
                    .putString("user_roll", studentRoll)
                    .putString("student_roll", studentRoll)
                    .apply();
            if (student != null) {
                student.setRollNumber(studentRoll);
            }
            if (isNotificationPermissionGranted) {
                final String chatTopic = "student_chat_" + studentRoll;
                Log.e("TOPIC_CHECK", "Subscribing strictly to: [" + chatTopic + "]");
                FirebaseMessaging.getInstance().subscribeToTopic(chatTopic)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("FCM_SUB", "Successfully subscribed to topic: " + chatTopic);
                            } else {
                                Log.e("FCM_SUB", "Failed to subscribe to topic: " + chatTopic, task.getException());
                            }
                        });
            }
            setupChatBadgeListener();
        }
        if (!TextUtils.isEmpty(dept)) editor.putString("student_department", dept);
        if (!TextUtils.isEmpty(email)) editor.putString("student_email", email);
        if (!TextUtils.isEmpty(phone)) editor.putString("student_phone", phone);
        if (!TextUtils.isEmpty(room)) editor.putString("student_room", room);
        if (!TextUtils.isEmpty(boarderNo)) editor.putString("boarder_no", boarderNo);
        if (!TextUtils.isEmpty(dining)) {
            String cleanDining = dining.replaceAll("(?i)\\bhall\\b", "").trim();
            if (cleanDining.isEmpty()) cleanDining = "East";
            editor.putString("dining_hall", cleanDining);
        }
        editor.apply();

        linkedGoogleEmail = doc.getString("linkedGoogleEmail");
        if (!TextUtils.isEmpty(linkedGoogleEmail)) {
            editor.putString("linked_google_email", linkedGoogleEmail);
        } else {
            editor.remove("linked_google_email");
        }
        editor.apply();
        updateDrawerMenuState();

        setupHeader();
        setupNavigationDrawer();
        setupProfileCard();
        setupDiningManagerListener();
        setupSeatView();
        if (!TextUtils.isEmpty(roll)) {
            loadFeeRecordsFromFirestore();
            evaluatePreviousMonthFeeStatus();
            setupPaymentsListener();
            loadStudentComplaints();
        }
    }

    private void setupSeatView() {
        binding.studentRoomSeatView.setInteractiveMode(false);

        String room = student != null ? student.getAllocatedRoom() : null;
        if (TextUtils.isEmpty(room) || "Unassigned".equalsIgnoreCase(room)) {
            binding.layoutRoomMapContent.setVisibility(View.GONE);
            binding.tvNoRoomAllocatedNotice.setVisibility(View.VISIBLE);
            if (roomListener != null) {
                roomListener.remove();
                roomListener = null;
            }
            return;
        }

        binding.layoutRoomMapContent.setVisibility(View.VISIBLE);
        binding.tvNoRoomAllocatedNotice.setVisibility(View.GONE);

        if (roomListener != null) {
            roomListener.remove();
            roomListener = null;
        }

        roomListener = FirebaseFirestore.getInstance().collection("rooms").document(room)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }
                    updateRoomViewFromSnapshot(snapshot);
                });
    }

    @SuppressWarnings("unchecked")
    private void updateRoomViewFromSnapshot(DocumentSnapshot snapshot) {
        Map<String, Object> seats = (Map<String, Object>) snapshot.get("seats");
        String myRoll = student != null && !TextUtils.isEmpty(student.getRollNumber())
                ? student.getRollNumber() : (student != null ? student.getRoll() : "");
        String myName = student != null ? student.getName() : "Student";

        String[] seatKeys = {Room.SEAT_A, Room.SEAT_B, Room.SEAT_C, Room.SEAT_D};
        int[] states = new int[4];
        String[] names = new String[4];
        binding.layoutRoommatesList.removeAllViews();
        int roommateCount = 0;

        for (int i = 0; i < 4; i++) {
            String seatKey = seatKeys[i];
            Object occupantObj = seats != null ? seats.get(seatKey) : null;
            SeatOccupant occupant = SeatOccupant.fromObject(occupantObj);

            if (occupant == null) {
                states[i] = RoomSeatView.STATE_AVAILABLE;
                names[i] = "Available";
            } else {
                boolean isMySeat = !TextUtils.isEmpty(myRoll) && myRoll.equalsIgnoreCase(occupant.getRoll());
                if (isMySeat) {
                    states[i] = RoomSeatView.STATE_MY_SEAT;
                    names[i] = myName + " (" + myRoll + ")";
                } else {
                    states[i] = RoomSeatView.STATE_OCCUPIED;
                    names[i] = occupant.getName() + " (Seat " + seatKey + ")";

                    roommateCount++;
                    View roommateView = LayoutInflater.from(this).inflate(R.layout.item_roommate_card, binding.layoutRoommatesList, false);
                    TextView tvSeat = roommateView.findViewById(R.id.tvRoommateSeat);
                    TextView tvRName = roommateView.findViewById(R.id.tvRoommateName);
                    TextView tvRRoll = roommateView.findViewById(R.id.tvRoommateRoll);

                    tvSeat.setText(seatKey);
                    tvRName.setText(occupant.getName());
                    tvRRoll.setText("Roll: " + occupant.getRoll());
                    binding.layoutRoommatesList.addView(roommateView);
                }
            }
        }

        if (roommateCount == 0) {
            TextView tvEmptyRoommate = new TextView(this);
            tvEmptyRoommate.setText("No roommates currently assigned in this room.");
            tvEmptyRoommate.setTextColor(Color.parseColor("#757575"));
            tvEmptyRoommate.setTextSize(12);
            tvEmptyRoommate.setPadding(4, 4, 4, 4);
            binding.layoutRoommatesList.addView(tvEmptyRoommate);
        }

        String roomNumber = snapshot.getString("roomId");
        if (TextUtils.isEmpty(roomNumber)) {
            roomNumber = snapshot.getId();
        }
        Long floorLong = snapshot.getLong("floor");
        long floor = floorLong != null ? floorLong : 1;

        binding.studentRoomSeatView.setRoomTitle("Room " + roomNumber + " (Floor " + floor + ")");
        binding.studentRoomSeatView.setSeatData(states, names);
    }

    private void setupFeeRecords() {
        feeRecordAdapter = new FeeRecordAdapter(new ArrayList<>(), null, false, this::openPaymentGateway);
        binding.rvMyFees.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyFees.setAdapter(feeRecordAdapter);
        loadFeeRecordsFromFirestore();
        evaluatePreviousMonthFeeStatus();
        setupPaymentsListener();
    }

    private void refreshFees() {
        loadFeeRecordsFromFirestore();
        evaluatePreviousMonthFeeStatus();
        setupPaymentsListener();
    }

    private void loadFeeRecordsFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("student_prefs_" + currentUser.getUid(), Context.MODE_PRIVATE);
        String roll = student != null && !TextUtils.isEmpty(student.getRollNumber())
                ? student.getRollNumber()
                : prefs.getString("student_roll", "");

        if (TextUtils.isEmpty(roll)) {
            return;
        }

        if (feeListenerRegistration != null) {
            feeListenerRegistration.remove();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        feeListenerRegistration = db.collection("fee_records")
                .whereEqualTo("roll", roll)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("StudentDashboard", "Failed to listen for fee records", error);
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        feeRecordAdapter.updateData(new ArrayList<>());
                        updateFeeEmptyState(new ArrayList<>());
                        return;
                    }

                    List<FeeRecord> feeList = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        FeeRecord record = parseFeeRecord(doc);
                        feeList.add(record);
                    }

                    feeRecordAdapter.updateData(feeList);
                    updateFeeEmptyState(feeList);
                });
    }

    private String getPreviousMonthEndDateString() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.DATE, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(cal.getTime());
    }

    private void evaluatePreviousMonthFeeStatus() {
        if (student == null || binding == null) {
            return;
        }
        String roll = getActiveStudentRoll();
        if (TextUtils.isEmpty(roll)) {
            return;
        }

        String previousMonthEndDate = getPreviousMonthEndDateString();
        int statusTextColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFFC62828);

        if (previousMonthEndDate.compareTo(BillingEngine.BILLING_START_DATE) < 0) {
            student.setFeeStatus(FeeRecord.STATUS_PAID);
            binding.tvFeeStatusCard.setText("Fee Status: PAID");
            binding.tvFeeStatusCard.setTextColor(statusTextColor);
            return;
        }

        BillingEngine.calculateBillUpToDate(roll, previousMonthEndDate, new BillingEngine.OnBillCalculatedListener() {
            @Override
            public void onBillCalculated(BillingEngine.BillResult result) {
                if (result == null || binding == null) {
                    return;
                }
                double unpaid = result.getNetUnpaidAmount();
                if (unpaid <= 0.0) {
                    student.setFeeStatus(FeeRecord.STATUS_PAID);
                    binding.tvFeeStatusCard.setText("Fee Status: PAID");
                } else {
                    student.setFeeStatus(FeeRecord.STATUS_UNPAID);
                    binding.tvFeeStatusCard.setText("Fee Status: UNPAID");
                }
                binding.tvFeeStatusCard.setTextColor(statusTextColor);
            }

            @Override
            public void onError(Exception e) {
                String errorMsg = e != null ? e.getMessage() : "Unknown error";
                Log.e("BillingEngineError", "Failed to evaluate previous month fee status: " + errorMsg, e);
            }
        });
    }

    private void setupPaymentsListener() {
        String roll = getActiveStudentRoll();
        if (TextUtils.isEmpty(roll)) {
            return;
        }
        if (paymentListenerRegistration != null) {
            paymentListenerRegistration.remove();
        }
        paymentListenerRegistration = FirebaseFirestore.getInstance()
                .collection("payments")
                .whereEqualTo("roll", roll)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("BillingEngineError", "Failed to listen for payments", error);
                        return;
                    }
                    evaluatePreviousMonthFeeStatus();
                });
    }

    private FeeRecord parseFeeRecord(DocumentSnapshot doc) {
        FeeRecord record = new FeeRecord();
        record.setId(doc.getId());
        record.setRoll(doc.getString("roll"));
        record.setMonth(doc.getString("month") != null ? doc.getString("month") : "November 2026");

        Double dining = doc.getDouble("diningFee");
        record.setDiningFee(dining != null ? dining : 0.0);

        Double hall = doc.getDouble("hallFee");
        record.setHallFee(hall != null ? hall : 0.0);

        Double total = doc.getDouble("totalAmount");
        if (total == null) {
            total = doc.getDouble("amount");
        }
        record.setTotalAmount(total != null ? total : 0.0);
        record.setAmount(total != null ? total : 0.0);

        record.setDueDate(doc.getString("dueDate") != null ? doc.getString("dueDate") : "15/11/2026");
        record.setStatus(doc.getString("status") != null ? doc.getString("status") : "UNPAID");

        Long paidAt = doc.getLong("paidAt");
        record.setPaidAt(paidAt != null ? paidAt : 0L);

        record.setTrxId(doc.getString("trxId") != null ? doc.getString("trxId") : "");
        return record;
    }

    private void openPaymentGateway(FeeRecord feeRecord) {
        if (feeRecord == null) {
            return;
        }
        String roll = student != null && !TextUtils.isEmpty(student.getRollNumber())
                ? student.getRollNumber() : feeRecord.getRoll();

        PaymentGatewayBottomSheet bottomSheet = PaymentGatewayBottomSheet.newInstance(feeRecord, roll);
        bottomSheet.setPaymentCallback(payment -> {
            feeRecordAdapter.markFeeAsPaid(payment.getFeeRecordId(), payment.getTrxId());
            evaluatePreviousMonthFeeStatus();
            PaymentReceiptDialog receiptDialog = PaymentReceiptDialog.newInstance(payment);
            receiptDialog.show(getSupportFragmentManager(), "PAYMENT_RECEIPT_DIALOG");
        });
        bottomSheet.show(getSupportFragmentManager(), "PAYMENT_GATEWAY_SHEET");
    }

    private void updateFeeEmptyState(List<FeeRecord> fees) {
        binding.tvEmptyFees.setVisibility(fees.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvMyFees.setVisibility(fees.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupComplaints() {
        complaintAdapter = new ComplaintAdapter(studentComplaintsList, complaint -> {
            Intent intent = new Intent(StudentDashboardActivity.this, ComplaintDetailActivity.class);
            intent.putExtra("complaint_id", complaint.getComplaintId());
            intent.putExtra("title", complaint.getTitle());
            intent.putExtra("description", complaint.getDescription());
            intent.putExtra("student_roll", complaint.getStudentRoll());
            intent.putExtra("student_phone", complaint.getStudentPhone());
            intent.putExtra("status", complaint.getStatus());
            intent.putExtra("timestamp", complaint.getTimestamp());
            startActivity(intent);
        });
        binding.rvMyComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyComplaints.setAdapter(complaintAdapter);
        loadStudentComplaints();
    }

    private void refreshComplaints() {
        loadStudentComplaints();
    }

    private void loadStudentComplaints() {
        final String activeRoll = getActiveStudentRoll();
        if (TextUtils.isEmpty(activeRoll)) {
            studentComplaintsList.clear();
            if (complaintAdapter != null) {
                complaintAdapter.updateData(studentComplaintsList);
            }
            updateComplaintEmptyState(studentComplaintsList);
            return;
        }

        if (rtdbComplaintsRef != null && rtdbComplaintsListener != null) {
            rtdbComplaintsRef.removeEventListener(rtdbComplaintsListener);
        }

        rtdbComplaintsRef = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com")
                .getReference("complaints");

        rtdbComplaintsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentComplaintsList.clear();
                String targetRoll = activeRoll.trim();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String studentRoll = child.child("studentRoll").getValue(String.class);
                    if (studentRoll == null) {
                        studentRoll = child.child("roll").getValue(String.class);
                    }
                    if (studentRoll == null) {
                        studentRoll = child.child("submitterRoll").getValue(String.class);
                    }
                    if (studentRoll == null) {
                        studentRoll = child.child("studentId").getValue(String.class);
                    }

                    if (studentRoll != null && studentRoll.trim().equalsIgnoreCase(targetRoll)) {
                        String id = child.child("complaintId").getValue(String.class);
                        if (id == null) {
                            id = child.getKey();
                        }
                        String title = child.child("title").getValue(String.class);
                        String description = child.child("description").getValue(String.class);
                        String studentPhone = child.child("studentPhone").getValue(String.class);
                        String status = child.child("status").getValue(String.class);
                        Long timestampVal = child.child("timestamp").getValue(Long.class);
                        long timestamp = timestampVal != null ? timestampVal : 0L;

                        Complaint complaint = new Complaint(id, title, description, studentRoll.trim(), studentPhone, status, timestamp);
                        studentComplaintsList.add(complaint);
                    }
                }

                Collections.sort(studentComplaintsList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                if (complaintAdapter != null) {
                    complaintAdapter.updateData(studentComplaintsList);
                }
                updateComplaintEmptyState(studentComplaintsList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentDashboard", "Complaints query cancelled: " + error.getMessage());
            }
        };

        rtdbComplaintsRef.addValueEventListener(rtdbComplaintsListener);
    }

    private void updateComplaintEmptyState(List<Complaint> complaints) {
        boolean isEmpty = (complaints == null || complaints.isEmpty());
        binding.tvEmptyComplaints.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvMyComplaints.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setupSubmitComplaintButton() {
        binding.btnSubmitComplaint.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComplaintsListActivity.class);
            startActivity(intent);
        });
        binding.cardComplaints.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComplaintsListActivity.class);
            startActivity(intent);
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void logout() {
        stopService(new Intent(this, EmergencySosForegroundService.class));
        getSharedPreferences("user_session", Context.MODE_PRIVATE).edit().clear().apply();
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignIn.getClient(this, gso).signOut();
        LoginManager.getInstance().logOut();
        navigateToLogin();
    }

    private void showImagePickerDialog() {
        CharSequence[] options = new CharSequence[]{"Take Photo (Camera)", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Update Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            dispatchTakePictureIntent();
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else if (which == 1) {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void dispatchTakePictureIntent() {
        File imageDir = new File(getCacheDir(), "camera_photos");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        File tempFile = new File(imageDir, "camera_raw_" + System.currentTimeMillis() + ".jpg");
        tempCameraUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                tempFile
        );
        cameraLauncher.launch(tempCameraUri);
    }

    private void startCrop(Uri sourceUri) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "user";
        File destinationFile = new File(getCacheDir(), "cropped_profile_" + uid + "_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(destinationFile);
        int primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFFC62828);
        int primaryVariant = getThemeColor(com.google.android.material.R.attr.colorPrimaryVariant, 0xFF801313);
        com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropGrid(false);
        options.setCompressionQuality(90);
        options.setToolbarColor(primaryColor);
        options.setStatusBarColor(primaryVariant);
        options.setToolbarWidgetColor(android.graphics.Color.WHITE);
        options.setActiveControlsWidgetColor(primaryColor);
        options.setToolbarTitle("Crop Profile Photo");
        Intent cropIntent = com.yalantis.ucrop.UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(500, 500)
                .withOptions(options)
                .getIntent(this);
        uCropLauncher.launch(cropIntent);
    }

    private void saveImageToInternalStorage(Uri uri) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (binding != null) {
            if (binding.pbProfileAvatar != null) {
                binding.pbProfileAvatar.setVisibility(View.VISIBLE);
            }
            binding.ivProfileAvatar.setEnabled(false);
            binding.cardProfileAvatar.setEnabled(false);
        }

        imageProcessingExecutor.execute(() -> {
            Bitmap bitmap = null;
            Bitmap scaledBitmap = null;
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new IOException("Unable to open input stream from uri");
                }
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                if (bitmap == null) {
                    throw new IOException("Failed to decode bitmap from stream");
                }

                String fileName = "profile_picture_" + currentUser.getUid() + ".jpg";
                File file = new File(getFilesDir(), fileName);
                long maxSizeBytes = 100 * 1024;
                int quality = 95;
                boolean saved = false;

                while (quality > 15) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                    byte[] byteArray = stream.toByteArray();
                    if (byteArray.length <= maxSizeBytes) {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(byteArray);
                        fos.flush();
                        fos.close();
                        saved = true;
                        break;
                    }
                    quality -= 10;
                }

                if (!saved) {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) (width * 0.8), (int) (height * 0.8), true);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                    byte[] byteArray = stream.toByteArray();
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(byteArray);
                    fos.flush();
                    fos.close();
                }

                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    bitmap = null;
                }
                if (scaledBitmap != null && !scaledBitmap.isRecycled()) {
                    scaledBitmap.recycle();
                    scaledBitmap = null;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                FileInputStream fis = new FileInputStream(file);
                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                fis.close();
                byte[] fileBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(fileBytes, Base64.NO_WRAP);

                SharedPreferences prefs = getSharedPreferences("student_prefs_" + currentUser.getUid(), Context.MODE_PRIVATE);
                prefs.edit().putString("profile_image_path", file.getAbsolutePath()).apply();

                Bitmap displayBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (binding != null) {
                        if (binding.pbProfileAvatar != null) {
                            binding.pbProfileAvatar.setVisibility(View.GONE);
                        }
                        binding.ivProfileAvatar.setEnabled(true);
                        binding.cardProfileAvatar.setEnabled(true);
                        if (displayBitmap != null) {
                            binding.ivProfileAvatar.setImageBitmap(displayBitmap);
                        } else {
                            loadProfileImage();
                        }
                    }
                    uploadProfileImageToFirestore(file, base64Image);
                });

            } catch (OutOfMemoryError e) {
                Log.e("ProfileImageProcessing", "Image processing failed", e);
                handleImageProcessingError();
            } catch (IOException e) {
                Log.e("ProfileImageProcessing", "Image processing failed", e);
                handleImageProcessingError();
            } catch (Exception e) {
                Log.e("ProfileImageProcessing", "Image processing failed", e);
                handleImageProcessingError();
            } finally {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                if (scaledBitmap != null && !scaledBitmap.isRecycled()) {
                    scaledBitmap.recycle();
                }
            }
        });
    }

    private void handleImageProcessingError() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (binding != null) {
                if (binding.pbProfileAvatar != null) {
                    binding.pbProfileAvatar.setVisibility(View.GONE);
                }
                binding.ivProfileAvatar.setEnabled(true);
                binding.cardProfileAvatar.setEnabled(true);
            }
            Toast.makeText(this, "Failed to process image. Please try another photo.", Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadProfileImageToFirestore(File file, String base64Image) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || file == null || TextUtils.isEmpty(base64Image)) {
            return;
        }
        String roll = getActiveStudentRoll();
        if (TextUtils.isEmpty(roll) && student != null && !TextUtils.isEmpty(student.getRollNumber())) {
            roll = student.getRollNumber();
        }
        if (TextUtils.isEmpty(roll)) {
            Log.e("StudentDashboard", "Failed to upload profile image: student roll is missing");
            return;
        }
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("students")
                    .document(roll)
                    .set(Collections.singletonMap("profileImageBase64", base64Image), SetOptions.merge())
                    .addOnFailureListener(e -> Log.e("StudentDashboard", "Failed to upload profile image to Firestore", e));
        } catch (Exception e) {
            Log.e("StudentDashboard", "Failed to write profile image to Firestore", e);
        }
    }

    private void loadProfileImage() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            binding.ivProfileAvatar.setImageResource(R.drawable.ic_person);
            return;
        }

        String fileName = "profile_picture_" + currentUser.getUid() + ".jpg";
        File localFile = new File(getFilesDir(), fileName);
        if (localFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
            if (bitmap != null) {
                binding.ivProfileAvatar.setImageBitmap(bitmap);
                return;
            }
        }

        binding.ivProfileAvatar.setImageResource(R.drawable.ic_person);

        String roll = getActiveStudentRoll();
        if (TextUtils.isEmpty(roll)) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("students")
                .document(roll)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String base64Str = documentSnapshot.getString("profileImageBase64");
                        if (base64Str != null && !base64Str.isEmpty()) {
                            try {
                                byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
                                File file = new File(getFilesDir(), fileName);
                                FileOutputStream fos = new FileOutputStream(file);
                                fos.write(decodedBytes);
                                fos.flush();
                                fos.close();

                                SharedPreferences prefs = getSharedPreferences("student_prefs_" + currentUser.getUid(), Context.MODE_PRIVATE);
                                prefs.edit().putString("profile_image_path", file.getAbsolutePath()).apply();

                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                if (bitmap != null) {
                                    binding.ivProfileAvatar.setImageBitmap(bitmap);
                                }
                            } catch (Exception e) {
                                Log.e("StudentDashboard", "Failed to decode profile image from Firestore", e);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("StudentDashboard", "Failed to fetch profile image from Firestore", e));
    }

    private void loadBoarderAndDining() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("student_prefs_" + currentUser.getUid(), Context.MODE_PRIVATE);
        String cachedBoarderNo = prefs.getString("boarder_no", null);
        String cachedDining = prefs.getString("dining_hall", null);

        if (!TextUtils.isEmpty(cachedBoarderNo) && !TextUtils.isEmpty(cachedDining)) {
            if (student != null) {
                student.setBoarderNo(cachedBoarderNo);
                student.setDining(cachedDining);
            }
            binding.tvBoarderNo.setText("Boarder No: " + cachedBoarderNo);
            String cleanDining = cachedDining != null ? cachedDining.replaceAll("(?i)\\bhall\\b", "").trim() : "East";
            if (cleanDining.isEmpty()) cleanDining = "East";
            binding.tvDiningHall.setText("Dining: " + cleanDining);
        }

        String uid = currentUser.getUid();
        String roll = getActiveStudentRoll();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (!TextUtils.isEmpty(roll)) {
            db.collection("students").document(roll).get().addOnCompleteListener(task -> {
                String firestoreBoarderNo = null;
                String firestoreDining = null;

                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    firestoreBoarderNo = task.getResult().getString("boarderNo");
                    firestoreDining = task.getResult().getString("dining");
                }

                if (!TextUtils.isEmpty(firestoreBoarderNo) && !TextUtils.isEmpty(firestoreDining)) {
                    applyBoarderAndDining(firestoreBoarderNo, firestoreDining, prefs);
                    return;
                }

                resolveBoarderAndDiningFallback(db, uid, roll, firestoreBoarderNo, firestoreDining, prefs);
            });
        } else {
            resolveBoarderAndDiningFallback(db, uid, roll, null, null, prefs);
        }
    }

    private void resolveBoarderAndDiningFallback(FirebaseFirestore db, String uid, String roll,
                                                 String existingBoarderNo, String existingDining,
                                                 SharedPreferences prefs) {
        db.collection("users").document(uid).get().addOnCompleteListener(userTask -> {
            String finalBoarderNo = existingBoarderNo;
            String finalDining = existingDining;

            if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                if (TextUtils.isEmpty(finalBoarderNo)) {
                    finalBoarderNo = userTask.getResult().getString("boarderNo");
                }
                if (TextUtils.isEmpty(finalDining)) {
                    finalDining = userTask.getResult().getString("dining");
                }
            }

            if (TextUtils.isEmpty(finalBoarderNo)) {
                String cached = prefs.getString("boarder_no", null);
                if (!TextUtils.isEmpty(cached)) {
                    finalBoarderNo = cached;
                } else {
                    finalBoarderNo = String.valueOf(1000 + new Random().nextInt(9000));
                }
            }

            if (TextUtils.isEmpty(finalDining)) {
                String cached = prefs.getString("dining_hall", null);
                if (!TextUtils.isEmpty(cached)) {
                    finalDining = cached;
                } else {
                    finalDining = new Random().nextBoolean() ? "East" : "West";
                }
            }

            applyBoarderAndDining(finalBoarderNo, finalDining, prefs);

            Map<String, Object> updates = new HashMap<>();
            updates.put("boarderNo", finalBoarderNo);
            updates.put("dining", finalDining);

            if (!TextUtils.isEmpty(roll)) {
                db.collection("students").document(roll).set(updates, SetOptions.merge())
                        .addOnFailureListener(e -> Log.e("StudentDashboard", "Failed to persist boarder/dining to students", e));
            }
            db.collection("users").document(uid).set(updates, SetOptions.merge())
                    .addOnFailureListener(e -> Log.e("StudentDashboard", "Failed to persist boarder/dining to users", e));
        });
    }

    private void applyBoarderAndDining(String boarderNo, String dining, SharedPreferences prefs) {
        String cleanDining = dining != null ? dining.replaceAll("(?i)\\bhall\\b", "").trim() : "East";
        if (cleanDining.isEmpty()) cleanDining = "East";
        if (student != null) {
            student.setBoarderNo(boarderNo);
            student.setDining(cleanDining);
        }
        binding.tvBoarderNo.setText("Boarder No: " + boarderNo);
        binding.tvDiningHall.setText("Dining: " + cleanDining);
        prefs.edit().putString("boarder_no", boarderNo).putString("dining_hall", cleanDining).apply();
    }

    private void linkGoogleAccount(GoogleSignInAccount account) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        user.linkWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                String email = account.getEmail();
                if (TextUtils.isEmpty(email)) {
                    email = user.getEmail();
                }
                linkedGoogleEmail = email != null ? email : "";
                getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE)
                        .edit()
                        .putString("linked_google_email", linkedGoogleEmail)
                        .apply();
                Map<String, Object> updates = new HashMap<>();
                updates.put("linkedGoogleEmail", linkedGoogleEmail);
                String studentRoll = getActiveStudentRoll();
                if (!TextUtils.isEmpty(studentRoll)) {
                    FirebaseDatabase rtdb = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com");
                    Map<String, Object> rtdbUpdates = new HashMap<>();
                    rtdbUpdates.put("googleEmail", linkedGoogleEmail);
                    rtdb.getReference("students").child(studentRoll).updateChildren(rtdbUpdates);

                    FirebaseFirestore.getInstance().collection("students").document(studentRoll)
                            .set(updates, SetOptions.merge())
                            .addOnFailureListener(e -> Log.e("StudentDashboard", "Failed to persist linked Google to students", e));
                }
                FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                        .set(updates, SetOptions.merge())
                        .addOnCompleteListener(t -> {
                            Toast.makeText(StudentDashboardActivity.this, "Account linked successfully", Toast.LENGTH_SHORT).show();
                            updateDrawerMenuState();
                        });
            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Log.e("StudentDashboardError", "Operation failed: Google account link failed: " + errorMsg, task.getException());
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    Toast.makeText(StudentDashboardActivity.this, "This account is already linked to another user.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(StudentDashboardActivity.this, "Failed to link Google account: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isGoogleAccountLinked() {
        if (!TextUtils.isEmpty(linkedGoogleEmail)) {
            return true;
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            for (UserInfo userInfo : currentUser.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(userInfo.getProviderId())
                        || "google.com".equals(userInfo.getProviderId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateDrawerMenuState() {
        if (navigationView == null) {
            return;
        }
        Menu navMenu = navigationView.getMenu();
        if (navMenu == null) {
            return;
        }
        MenuItem googleItem = navMenu.findItem(R.id.nav_link_google);
        if (googleItem != null) {
            boolean isGoogleLinked = isGoogleAccountLinked();
            if (isGoogleLinked) {
                googleItem.setTitle(R.string.btn_google_linked);
                googleItem.setIcon(R.drawable.ic_baseline_link_24);
                googleItem.setEnabled(true);
            } else {
                googleItem.setTitle(R.string.btn_link_google);
                googleItem.setIcon(R.drawable.ic_baseline_link_24);
                googleItem.setEnabled(true);
            }
        }
    }
}
