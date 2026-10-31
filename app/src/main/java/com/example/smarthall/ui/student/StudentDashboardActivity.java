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
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ActivityStudentDashboardBinding;
import com.example.smarthall.model.Complaint;
import com.example.smarthall.model.FeeRecord;
import com.example.smarthall.model.Room;
import com.example.smarthall.model.Student;
import com.example.smarthall.receiver.NetworkChangeReceiver;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.ui.adapters.ComplaintAdapter;
import com.example.smarthall.ui.adapters.FeeRecordAdapter;
import com.example.smarthall.ui.auth.LoginActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.customview.RoomSeatView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentDashboardActivity extends BaseActivity {

    private ActivityStudentDashboardBinding binding;
    private HallRepository repository;
    private Student student;
    private FeeRecordAdapter feeRecordAdapter;
    private ComplaintAdapter complaintAdapter;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> uCropLauncher;
    private NetworkChangeReceiver networkChangeReceiver;
    private Snackbar networkSnackbar;
    private boolean isPreviouslyOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d("FCM", "POST_NOTIFICATIONS permission granted");
                    } else {
                        Log.w("FCM", "POST_NOTIFICATIONS permission denied");
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        createNotificationChannel();

        FirebaseMessaging.getInstance().subscribeToTopic("hall_notices")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Subscribed to hall_notices topic successfully");
                    } else {
                        Log.e("FCM", "Topic subscription failed", task.getException());
                    }
                });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d("FCM", "Device token: " + task.getResult());
                    } else {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    }
                });

        repository = HallRepository.getInstance();

        String studentId = getIntent().getStringExtra("EXTRA_USER_ID");
        if (studentId == null || studentId.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                studentId = "S001";
            } else {
                Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_LONG).show();
                navigateToLogin();
                return;
            }
        }

        student = repository.getStudentById(studentId);
        if (student == null) {
            student = repository.getStudentById("S001");
        }
        if (student == null) {
            Toast.makeText(this, "Student account not found.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        setupHeader();
        setupProfileCard();
        setupSeatView();
        setupFeeRecords();
        setupComplaints();
        setupSubmitComplaintButton();
        setupViewNoticesButton();
        loadProfileImage();
        binding.ivProfileAvatar.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        binding.cardProfileAvatar.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (student != null) {
            student = repository.getStudentById(student.getId());
            setupProfileCard();
            setupSeatView();
            refreshFees();
            refreshComplaints();
            loadProfileImage();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        networkChangeReceiver = new NetworkChangeReceiver(isConnected -> {
            if (!isConnected) {
                isPreviouslyOffline = true;
                if (networkSnackbar != null && networkSnackbar.isShown()) {
                    networkSnackbar.dismiss();
                }
                networkSnackbar = Snackbar.make(binding.getRoot(), "No Internet Connection. Offline mode.", Snackbar.LENGTH_INDEFINITE);
                networkSnackbar.getView().setBackgroundColor(Color.parseColor("#B71C1C"));
                ((android.widget.TextView) networkSnackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text)).setTextColor(Color.WHITE);
                networkSnackbar.show();
            } else {
                if (networkSnackbar != null && networkSnackbar.isShown()) {
                    networkSnackbar.dismiss();
                }
                if (isPreviouslyOffline) {
                    Snackbar onlineSnack = Snackbar.make(binding.getRoot(), "Back online", Snackbar.LENGTH_SHORT);
                    onlineSnack.getView().setBackgroundColor(Color.parseColor("#D32F2F"));
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
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "hall_notices_channel",
                    "Hall Announcements",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void setupViewNoticesButton() {
        binding.btnViewNotices.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoticeListActivity.class);
            startActivity(intent);
        });
    }

    private void setupHeader() {
        binding.headerLayout.tvHallName.setText(R.string.hall_name);
        binding.headerLayout.tvRoleSubtitle.setText("Logged in as: " + student.getName());
        binding.headerLayout.btnLogout.setOnClickListener(v -> logout());
    }

    private void setupProfileCard() {
        binding.tvStudentName.setText(student.getName());
        binding.tvStudentRoll.setText("Roll: " + student.getRollNumber());
        binding.tvStudentDept.setText("Department: " + student.getDepartment());
        binding.tvStudentPhone.setText("Phone: " + student.getPhone());
        binding.tvStudentEmail.setText("Email: " + student.getEmail());

        if (student.isAssigned()) {
            binding.tvRoomSeatInfo.setText("Room " + student.getAllocatedRoom()
                    + " — Seat " + student.getAllocatedSeat());
        } else {
            binding.tvRoomSeatInfo.setText("Room: Not Assigned");
        }

        if (FeeRecord.STATUS_PAID.equals(student.getFeeStatus())) {
            binding.tvFeeStatusCard.setText("Fee Status: PAID");
            binding.tvFeeStatusCard.setTextColor(getResources().getColor(R.color.statusPaid, null));
        } else {
            binding.tvFeeStatusCard.setText("Fee Status: UNPAID");
            binding.tvFeeStatusCard.setTextColor(getResources().getColor(R.color.statusUnpaid, null));
        }
    }

    private void setupSeatView() {
        binding.studentRoomSeatView.setInteractiveMode(false);

        if (!student.isAssigned()) {
            binding.studentRoomSeatView.setRoomTitle("No Room Assigned");
            binding.studentRoomSeatView.setSeatData(new int[]{0,0,0,0},
                    new String[]{"Empty","Empty","Empty","Empty"});
            return;
        }

        Room room = repository.getRoomByNumber(student.getAllocatedRoom());
        if (room == null) return;

        String[] seatKeys = {Room.SEAT_A, Room.SEAT_B, Room.SEAT_C, Room.SEAT_D};
        int[] states = new int[4];
        String[] names = new String[4];

        for (int i = 0; i < 4; i++) {
            String occupantId = room.getOccupantId(seatKeys[i]);
            if (occupantId == null || occupantId.isEmpty()) {
                states[i] = RoomSeatView.STATE_AVAILABLE;
                names[i] = "Empty";
            } else if (occupantId.equals(student.getId())) {
                states[i] = RoomSeatView.STATE_MY_SEAT;
                names[i] = student.getName();
            } else {
                Student occ = repository.getStudentById(occupantId);
                states[i] = RoomSeatView.STATE_OCCUPIED;
                names[i] = occ != null ? occ.getName() : "Occupied";
            }
        }

        binding.studentRoomSeatView.setRoomTitle("Room " + student.getAllocatedRoom());
        binding.studentRoomSeatView.setSeatData(states, names);
    }

    private void setupFeeRecords() {
        List<FeeRecord> feeRecords = repository.getFeeRecordsByStudent(student.getId());
        feeRecordAdapter = new FeeRecordAdapter(feeRecords, null, false);
        binding.rvMyFees.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyFees.setAdapter(feeRecordAdapter);
        updateFeeEmptyState(feeRecords);
    }

    private void refreshFees() {
        List<FeeRecord> fees = repository.getFeeRecordsByStudent(student.getId());
        feeRecordAdapter.updateData(fees);
        updateFeeEmptyState(fees);
    }

    private void updateFeeEmptyState(List<FeeRecord> fees) {
        binding.tvEmptyFees.setVisibility(fees.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvMyFees.setVisibility(fees.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupComplaints() {
        List<Complaint> myComplaints = repository.getComplaintsByStudent(student.getId());
        complaintAdapter = new ComplaintAdapter(myComplaints, null, false);
        binding.rvMyComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyComplaints.setAdapter(complaintAdapter);
        updateComplaintEmptyState(myComplaints);
    }

    private void refreshComplaints() {
        List<Complaint> complaints = repository.getComplaintsByStudent(student.getId());
        complaintAdapter.updateData(complaints);
        updateComplaintEmptyState(complaints);
    }

    private void updateComplaintEmptyState(List<Complaint> complaints) {
        binding.tvEmptyComplaints.setVisibility(complaints.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvMyComplaints.setVisibility(complaints.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupSubmitComplaintButton() {
        binding.btnSubmitComplaint.setOnClickListener(v -> showSubmitComplaintDialog());
    }

    private void showSubmitComplaintDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Submit Complaint");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_submit_complaint, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etComplaintTitle);
        EditText etDesc = dialogView.findViewById(R.id.etComplaintDesc);

        builder.setPositiveButton("Submit", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Complaint title is required");
                etTitle.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(desc)) {
                etDesc.setError("Please describe your complaint");
                etDesc.requestFocus();
                return;
            }

            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
            String roomNo = student.isAssigned() ? student.getAllocatedRoom() : "N/A";
            Complaint complaint = new Complaint(
                    repository.generateComplaintId(),
                    student.getId(),
                    student.getName(),
                    roomNo,
                    title, desc, date,
                    Complaint.STATUS_PENDING
            );
            repository.addComplaint(complaint);
            refreshComplaints();
            Toast.makeText(this, "Complaint submitted successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        navigateToLogin();
    }

    private void startCrop(Uri sourceUri) {
        File destinationFile = new File(getCacheDir(), "cropped_profile_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(destinationFile);
        com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropGrid(false);
        options.setCompressionQuality(90);
        options.setToolbarColor(android.graphics.Color.parseColor("#D32F2F"));
        options.setStatusBarColor(android.graphics.Color.parseColor("#B71C1C"));
        options.setToolbarWidgetColor(android.graphics.Color.WHITE);
        options.setActiveControlsWidgetColor(android.graphics.Color.parseColor("#D32F2F"));
        options.setToolbarTitle("Crop Profile Photo");
        Intent cropIntent = com.yalantis.ucrop.UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(500, 500)
                .withOptions(options)
                .getIntent(this);
        uCropLauncher.launch(cropIntent);
    }

    private void saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return;
            }
            File file = new File(getFilesDir(), "profile_picture.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationAngle = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotationAngle = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotationAngle = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotationAngle = 270;
            }

            if (rotationAngle != 0) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationAngle);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    FileOutputStream fos = new FileOutputStream(file);
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.flush();
                    fos.close();
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                    if (rotatedBitmap != null && !rotatedBitmap.isRecycled()) {
                        rotatedBitmap.recycle();
                    }
                }
            }

            SharedPreferences prefs = getSharedPreferences("student_prefs", Context.MODE_PRIVATE);
            prefs.edit().putString("profile_image_path", file.getAbsolutePath()).apply();

            loadProfileImage();
        } catch (Exception e) {
            Log.e("StudentDashboard", "Failed to save profile image", e);
            Toast.makeText(this, "Failed to save profile picture", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage() {
        SharedPreferences prefs = getSharedPreferences("student_prefs", Context.MODE_PRIVATE);
        String path = prefs.getString("profile_image_path", null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    binding.ivProfileAvatar.setImageBitmap(bitmap);
                    return;
                }
            }
        }
        binding.ivProfileAvatar.setImageResource(R.drawable.ic_person);
    }
}
