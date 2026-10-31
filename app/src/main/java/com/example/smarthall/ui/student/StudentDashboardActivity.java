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
import android.util.Base64;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class StudentDashboardActivity extends BaseActivity {

    private ActivityStudentDashboardBinding binding;
    private HallRepository repository;
    private Student student;
    private FeeRecordAdapter feeRecordAdapter;
    private ComplaintAdapter complaintAdapter;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> uCropLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri tempCameraUri;
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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("student_prefs_" + currentUser.getUid(), Context.MODE_PRIVATE);

        student = new Student();
        student.setId(currentUser.getUid());
        student.setName(prefs.getString("student_name", "Student"));
        student.setRollNumber(prefs.getString("student_roll", ""));
        student.setDepartment(prefs.getString("student_department", ""));
        student.setEmail(prefs.getString("student_email", currentUser.getEmail() != null ? currentUser.getEmail() : ""));
        student.setPhone(prefs.getString("student_phone", ""));
        student.setAllocatedRoom(prefs.getString("student_room", ""));
        student.setBoarderNo(prefs.getString("boarder_no", ""));
        student.setDining(prefs.getString("dining_hall", ""));

        setupHeader();
        setupProfileCard();
        loadStudentDataFromFirestore();
        loadBoarderAndDining();
        setupSeatView();
        setupFeeRecords();
        setupComplaints();
        setupSubmitComplaintButton();
        setupViewNoticesButton();
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
        if (student == null) {
            return;
        }
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

        if (FeeRecord.STATUS_PAID.equals(student.getFeeStatus())) {
            binding.tvFeeStatusCard.setText("Fee Status: PAID");
            binding.tvFeeStatusCard.setTextColor(getResources().getColor(R.color.statusPaid, null));
        } else {
            binding.tvFeeStatusCard.setText("Fee Status: UNPAID");
            binding.tvFeeStatusCard.setTextColor(getResources().getColor(R.color.statusUnpaid, null));
        }

        if (!TextUtils.isEmpty(student.getBoarderNo())) {
            binding.tvBoarderNo.setText("Boarder No: " + student.getBoarderNo());
        }
        if (!TextUtils.isEmpty(student.getDining())) {
            String diningText = student.getDining().endsWith("Hall") ? student.getDining() : (student.getDining() + " Hall");
            binding.tvDiningHall.setText("Dining: " + diningText);
        }
    }

    private void loadStudentDataFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("student_prefs_" + uid, Context.MODE_PRIVATE);

        db.collection("students").document(uid).get().addOnCompleteListener(task -> {
            DocumentSnapshot snapshot = null;
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                snapshot = task.getResult();
            }

            if (snapshot != null) {
                applyStudentSnapshot(snapshot, prefs);
            } else {
                db.collection("users").document(uid).get().addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                        applyStudentSnapshot(userTask.getResult(), prefs);
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
        String dept = doc.getString("department");
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        String room = doc.getString("roomNo");
        if (TextUtils.isEmpty(room)) {
            room = doc.getString("allocatedRoom");
        }
        String seat = doc.getString("allocatedSeat");
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
        if (!TextUtils.isEmpty(dining)) student.setDining(dining);
        if (!TextUtils.isEmpty(base64Image)) student.setProfileImageBase64(base64Image);

        SharedPreferences.Editor editor = prefs.edit();
        if (!TextUtils.isEmpty(name)) editor.putString("student_name", name);
        if (!TextUtils.isEmpty(roll)) editor.putString("student_roll", roll);
        if (!TextUtils.isEmpty(dept)) editor.putString("student_department", dept);
        if (!TextUtils.isEmpty(email)) editor.putString("student_email", email);
        if (!TextUtils.isEmpty(phone)) editor.putString("student_phone", phone);
        if (!TextUtils.isEmpty(room)) editor.putString("student_room", room);
        if (!TextUtils.isEmpty(boarderNo)) editor.putString("boarder_no", boarderNo);
        if (!TextUtils.isEmpty(dining)) editor.putString("dining_hall", dining);
        editor.apply();

        setupHeader();
        setupProfileCard();
        setupSeatView();
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            if (bitmap == null) {
                return;
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

            Bitmap scaledBitmap = null;
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
            }
            if (scaledBitmap != null && !scaledBitmap.isRecycled()) {
                scaledBitmap.recycle();
            }

            uploadProfileImageToFirestore(file);

            SharedPreferences prefs = getSharedPreferences("student_prefs_" + currentUser.getUid(), Context.MODE_PRIVATE);
            prefs.edit().putString("profile_image_path", file.getAbsolutePath()).apply();

            loadProfileImage();
        } catch (Exception e) {
            Log.e("StudentDashboard", "Failed to save profile image", e);
            Toast.makeText(this, "Failed to save profile picture", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProfileImageToFirestore(File file) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || file == null || !file.exists()) {
            return;
        }
        try {
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
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("students")
                    .document(currentUser.getUid())
                    .set(Collections.singletonMap("profileImageBase64", base64Image), SetOptions.merge())
                    .addOnFailureListener(e -> Log.e("StudentDashboard", "Failed to upload profile image to Firestore", e));

            if (student != null && !TextUtils.isEmpty(student.getRollNumber())) {
                db.collection("students")
                        .document(student.getRollNumber())
                        .set(Collections.singletonMap("profileImageBase64", base64Image), SetOptions.merge())
                        .addOnFailureListener(e -> Log.e("StudentDashboard", "Failed to sync roll profile image", e));
            }
        } catch (Exception e) {
            Log.e("StudentDashboard", "Failed to read profile image for upload", e);
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

        FirebaseFirestore.getInstance()
                .collection("students")
                .document(currentUser.getUid())
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
            String diningText = cachedDining.endsWith("Hall") ? cachedDining : (cachedDining + " Hall");
            binding.tvDiningHall.setText("Dining: " + diningText);
        }

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").document(uid).get().addOnCompleteListener(task -> {
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

            final String existingBoarderNo = firestoreBoarderNo;
            final String existingDining = firestoreDining;

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

                db.collection("students").document(uid).set(updates, SetOptions.merge())
                        .addOnFailureListener(e -> Log.e("StudentDashboard", "Failed to persist boarder/dining to students", e));
                db.collection("users").document(uid).set(updates, SetOptions.merge())
                        .addOnFailureListener(e -> Log.e("StudentDashboard", "Failed to persist boarder/dining to users", e));
            });
        });
    }

    private void applyBoarderAndDining(String boarderNo, String dining, SharedPreferences prefs) {
        if (student != null) {
            student.setBoarderNo(boarderNo);
            student.setDining(dining);
        }
        binding.tvBoarderNo.setText("Boarder No: " + boarderNo);
        String diningText = dining.endsWith("Hall") ? dining : (dining + " Hall");
        binding.tvDiningHall.setText("Dining: " + diningText);
        prefs.edit().putString("boarder_no", boarderNo).putString("dining_hall", dining).apply();
    }
}
