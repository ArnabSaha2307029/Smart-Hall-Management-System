package com.example.smarthall.ui.admin;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ActivityAdminDashboardBinding;
import com.example.smarthall.databinding.DialogAdminClosureBinding;
import com.example.smarthall.databinding.DialogStudentDetailsBinding;
import com.example.smarthall.model.Complaint;
import com.example.smarthall.model.NoticeModel;
import com.example.smarthall.model.Student;
import com.example.smarthall.network.FcmNotificationSender;
import com.example.smarthall.receiver.NetworkChangeReceiver;
import com.example.smarthall.repository.HallRepository;
import com.example.smarthall.services.EmergencySosForegroundService;
import com.example.smarthall.ui.adapters.FirebaseNoticeAdapter;
import com.example.smarthall.ui.adapters.GlobalComplaintAdapter;
import com.example.smarthall.ui.adapters.StudentAdapter;
import com.example.smarthall.ui.auth.LoginActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.chat.AdminChatListActivity;
import com.example.smarthall.ui.chat.ChatActivity;
import com.example.smarthall.ui.complaint.ComplaintDetailActivity;
import com.example.smarthall.ui.complaint.ComplaintsListActivity;
import com.example.smarthall.ui.theme.ThemeSelectorBottomSheet;
import com.example.smarthall.util.BillingEngine;
import com.example.smarthall.util.EmergencySosManager;
import com.example.smarthall.util.MealCutoffUtil;
import com.example.smarthall.util.RoomAllocationManager;
import com.example.smarthall.util.ThemeHelper;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AdminDashboardActivity extends BaseActivity {

    private ActivityAdminDashboardBinding binding;
    private HallRepository repository;
    private StudentAdapter studentAdapter;
    private GlobalComplaintAdapter complaintAdapter;
    private List<Complaint> complaintList = new ArrayList<>();
    private FirebaseNoticeAdapter noticeAdapter;
    private List<NoticeModel> noticeList = new ArrayList<>();
    private DatabaseReference noticeRef;
    private ValueEventListener noticeListener;
    private ListenerRegistration studentListener;
    private NetworkChangeReceiver networkChangeReceiver;
    private Snackbar networkSnackbar;
    private boolean isPreviouslyOffline = false;

    private static final int TAB_STUDENTS = 0;
    private static final int TAB_COMPLAINTS = 1;
    private static final int TAB_NOTICES = 2;
    private int currentTab = TAB_STUDENTS;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ValueEventListener chatBadgeListener;
    private DatabaseReference chatListBadgeRef;
    private TextView tvChatBadgeCount;
    private DatabaseReference complaintsRef;
    private ValueEventListener complaintsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        createNotificationChannels();

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        subscribeToAdminTopics();
                    }
                }
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                subscribeToAdminTopics();
            }
        } else {
            subscribeToAdminTopics();
        }

        getSharedPreferences("user_session", Context.MODE_PRIVATE)
                .edit()
                .putString("user_role", "admin")
                .apply();

        repository = HallRepository.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com");
        noticeRef = database.getReference("notices");
        migrateLegacyNoticesIfNeeded(database);

        setupHeader();
        setupNavigationDrawer();
        setupRecyclerViews();
        setupTabButtons();
        setupNoticesListener();
        setupStudentsListener();
        setupComplaintsListener();
        RoomAllocationManager.seedDefaultRoomsIfEmpty(null);
        switchTab(TAB_STUDENTS);
        Intent serviceIntent = new Intent(this, EmergencySosForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentTab();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noticeRef != null && noticeListener != null) {
            noticeRef.removeEventListener(noticeListener);
        }
        if (studentListener != null) {
            studentListener.remove();
        }
        if (chatListBadgeRef != null && chatBadgeListener != null) {
            chatListBadgeRef.removeEventListener(chatBadgeListener);
        }
        if (complaintsRef != null && complaintsListener != null) {
            complaintsRef.removeEventListener(complaintsListener);
        }
    }

    private void createNotificationChannels() {
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

    private void subscribeToAdminTopics() {
        String topicName = "admin_inbox";
        Log.e("TOPIC_CHECK", "Subscribing strictly to: [" + topicName + "]");
        FirebaseMessaging.getInstance().subscribeToTopic(topicName)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM_SUB", "Successfully subscribed to topic: " + topicName);
                    } else {
                        Log.e("FCM_SUB", "Failed to subscribe to topic: " + topicName, task.getException());
                    }
                });
    }

    private void setupHeader() {
        binding.btnAdminMenuToggle.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.btnTopBroadcastNotice.setOnClickListener(v -> {
            switchTab(TAB_NOTICES);
            showAddNoticeDialog();
        });
        binding.btnAdminTopLogout.setOnClickListener(v -> logout());
    }

    private void setupNavigationDrawer() {
        binding.btnNavStudents.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            switchTab(TAB_STUDENTS);
        });
        binding.btnNavRoomAllocation.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(AdminDashboardActivity.this, RoomManagementActivity.class);
            startActivity(intent);
        });
        binding.btnHallClosureManagement.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            showAdminClosureDialog();
        });
        binding.btnNavBroadcastNotice.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            switchTab(TAB_NOTICES);
            showAddNoticeDialog();
        });
        binding.btnNavComplaints.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(AdminDashboardActivity.this, ComplaintsListActivity.class);
            startActivity(intent);
        });
        binding.btnNavStudentMessages.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(AdminDashboardActivity.this, AdminChatListActivity.class);
            startActivity(intent);
        });
        binding.btnNavAppTheme.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            ThemeSelectorBottomSheet.newInstance().show(getSupportFragmentManager(), ThemeSelectorBottomSheet.TAG);
        });
        binding.btnNavAdminLogout.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            logout();
        });
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout != null && binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setupRecyclerViews() {
        studentAdapter = new StudentAdapter(new ArrayList<>(), student -> {
            showStudentDetailsDialog(student);
        });
        binding.rvStudents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStudents.setAdapter(studentAdapter);

        complaintAdapter = new GlobalComplaintAdapter(complaintList, complaint -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ComplaintDetailActivity.class);
            intent.putExtra("complaint_id", complaint.getComplaintId());
            intent.putExtra("title", complaint.getTitle());
            intent.putExtra("description", complaint.getDescription());
            intent.putExtra("student_roll", complaint.getStudentRoll());
            intent.putExtra("student_phone", complaint.getStudentPhone());
            intent.putExtra("status", complaint.getStatus());
            intent.putExtra("timestamp", complaint.getTimestamp());
            startActivity(intent);
        });
        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaints.setAdapter(complaintAdapter);

        noticeAdapter = new FirebaseNoticeAdapter(noticeList, true, this::showDeleteNoticeDialog);
        binding.rvNotices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotices.setAdapter(noticeAdapter);
    }

    private void migrateLegacyNoticesIfNeeded(FirebaseDatabase database) {
        database.getReference("hall_announcements").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        NoticeModel model = child.getValue(NoticeModel.class);
                        if (model != null && child.getKey() != null) {
                            database.getReference("notices").child(child.getKey()).setValue(model);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminDashboard", "Failed to migrate legacy announcements", error.toException());
            }
        });
    }

    private void showDeleteNoticeDialog(NoticeModel notice) {
        if (notice == null || TextUtils.isEmpty(notice.getId())) {
            Toast.makeText(this, "Cannot delete notice: Invalid notice", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to permanently delete this notice?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String noticeId = notice.getId();
                    FirebaseDatabase database = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com");
                    database.getReference("notices").child(noticeId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminDashboardActivity.this, "Notice deleted successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("AdminDashboard", "Failed to delete notice", e);
                                Toast.makeText(AdminDashboardActivity.this, "Failed to delete notice. Please try again.", Toast.LENGTH_SHORT).show();
                            });
                    database.getReference("hall_announcements").child(noticeId).removeValue();
                    database.getReference("hall_notices").child(noticeId).removeValue();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setupNoticesListener() {
        noticeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noticeList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NoticeModel notice = child.getValue(NoticeModel.class);
                    if (notice != null) {
                        noticeList.add(notice);
                    }
                }
                Collections.sort(noticeList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                noticeAdapter.notifyDataSetChanged();
                binding.tvStatNotices.setText(String.valueOf(noticeList.size()));
                if (noticeList.isEmpty()) {
                    binding.tvEmptyNotices.setVisibility(View.VISIBLE);
                    binding.rvNotices.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyNotices.setVisibility(View.GONE);
                    binding.rvNotices.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ADMIN_NOTICES", "Failed to load notices", error.toException());
            }
        };
        noticeRef.addValueEventListener(noticeListener);
    }

    private void setupTabButtons() {
        binding.btnTabStudents.setOnClickListener(v -> switchTab(TAB_STUDENTS));
        binding.btnTabComplaints.setOnClickListener(v -> switchTab(TAB_COMPLAINTS));
        binding.btnTabNotices.setOnClickListener(v -> switchTab(TAB_NOTICES));
        binding.btnAddNotice.setOnClickListener(v -> showAddNoticeDialog());
    }

    private void switchTab(int tab) {
        currentTab = tab;
        binding.panelStudents.setVisibility(View.GONE);
        binding.panelComplaints.setVisibility(View.GONE);
        binding.panelNotices.setVisibility(View.GONE);

        TypedValue typedValue = new TypedValue();
        int active = 0xFFB71C1C;
        if (getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            active = typedValue.data;
        }
        int inactive = getResources().getColor(R.color.white, null);
        int inactiveText = active;
        int activeText = getResources().getColor(R.color.white, null);

        binding.btnTabStudents.setBackgroundColor(inactive);
        binding.btnTabStudents.setTextColor(inactiveText);
        binding.btnTabComplaints.setBackgroundColor(inactive);
        binding.btnTabComplaints.setTextColor(inactiveText);
        binding.btnTabNotices.setBackgroundColor(inactive);
        binding.btnTabNotices.setTextColor(inactiveText);

        switch (tab) {
            case TAB_STUDENTS:
                binding.panelStudents.setVisibility(View.VISIBLE);
                binding.btnTabStudents.setBackgroundColor(active);
                binding.btnTabStudents.setTextColor(activeText);
                refreshStudents();
                break;
            case TAB_COMPLAINTS:
                binding.panelComplaints.setVisibility(View.VISIBLE);
                binding.btnTabComplaints.setBackgroundColor(active);
                binding.btnTabComplaints.setTextColor(activeText);
                refreshComplaints();
                break;
            case TAB_NOTICES:
                binding.panelNotices.setVisibility(View.VISIBLE);
                binding.btnTabNotices.setBackgroundColor(active);
                binding.btnTabNotices.setTextColor(activeText);
                refreshNotices();
                break;
        }
    }

    private void refreshCurrentTab() {
        switchTab(currentTab);
    }

    private void setupStudentsListener() {
        if (studentListener != null) {
            studentListener.remove();
        }
        studentListener = FirebaseFirestore.getInstance()
                .collection("students")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    Map<String, Student> uniqueStudentsMap = new LinkedHashMap<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Student student = doc.toObject(Student.class);
                        if (student != null) {
                            if (TextUtils.isEmpty(student.getId())) {
                                student.setId(doc.getId());
                            }
                            String roll = student.getRoll();
                            if (!TextUtils.isEmpty(roll)) {
                                if (uniqueStudentsMap.containsKey(roll)) {
                                    Student existing = uniqueStudentsMap.get(roll);
                                    if (existing != null && TextUtils.isEmpty(existing.getProfileImageBase64()) && !TextUtils.isEmpty(student.getProfileImageBase64())) {
                                        uniqueStudentsMap.put(roll, student);
                                    }
                                } else {
                                    uniqueStudentsMap.put(roll, student);
                                }
                            }
                        }
                    }
                    List<Student> liveStudentList = new ArrayList<>(uniqueStudentsMap.values());
                    java.util.Collections.sort(liveStudentList, (s1, s2) -> s1.getRoll().compareTo(s2.getRoll()));
                    studentAdapter.updateData(liveStudentList);
                    binding.tvStatTotalStudents.setText(String.valueOf(liveStudentList.size()));
                    binding.tvEmptyStudents.setVisibility(liveStudentList.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.rvStudents.setVisibility(liveStudentList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void refreshStudents() {
        if (studentListener == null) {
            setupStudentsListener();
        }
    }

    private void refreshComplaints() {
        binding.tvStatComplaints.setText(String.valueOf(complaintList.size()));
        binding.tvEmptyComplaints.setVisibility(complaintList.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvComplaints.setVisibility(complaintList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void refreshNotices() {
        boolean isEmpty = noticeList.isEmpty();
        binding.tvEmptyNotices.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvNotices.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showAddNoticeDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add New Notice");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_notice, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etNoticeTitle);
        EditText etMessage = dialogView.findViewById(R.id.etNoticeMessage);

        builder.setPositiveButton("Publish", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Title is required");
                etTitle.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(message)) {
                etMessage.setError("Message is required");
                etMessage.requestFocus();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            String poster = auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null
                    ? auth.getCurrentUser().getEmail()
                    : "Admin";
            String id = noticeRef.push().getKey();
            if (id == null) {
                Toast.makeText(AdminDashboardActivity.this, "Failed to generate notice ID", Toast.LENGTH_SHORT).show();
                return;
            }

            NoticeModel noticeModel = new NoticeModel(id, title, message, poster, System.currentTimeMillis());
            noticeRef.child(id).setValue(noticeModel).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FcmNotificationSender.sendNoticeTopicNotification(AdminDashboardActivity.this, id, title, message, poster, noticeModel.getTimestamp());
                    Toast.makeText(AdminDashboardActivity.this, "Notice published successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("AdminDashboard", "Failed to publish notice", task.getException());
                    Toast.makeText(AdminDashboardActivity.this, "Unable to complete request. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.dismiss();
        });
    }

    private void showStudentDetailsDialog(Student student) {
        if (student == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogStudentDetailsBinding dialogBinding = DialogStudentDetailsBinding.inflate(LayoutInflater.from(this));
        builder.setView(dialogBinding.getRoot());

        dialogBinding.tvDetailName.setText(student.getName());
        String roll = !TextUtils.isEmpty(student.getRollNumber()) ? student.getRollNumber() : student.getRoll();
        dialogBinding.tvDetailRoll.setText("Roll: " + roll);

        String boarderNo = student.getBoarderNo();
        dialogBinding.tvDetailBoarderNo.setText("Boarder No: " + (!TextUtils.isEmpty(boarderNo) ? boarderNo : "Not Assigned"));

        String dining = student.getDining();
        String cleanDining = dining != null ? dining.replaceAll("(?i)\\bhall\\b", "").trim() : "East";
        if (cleanDining.isEmpty()) cleanDining = "East";
        dialogBinding.tvDetailDining.setText("Dining: " + cleanDining);

        String room = !TextUtils.isEmpty(student.getAllocatedRoom()) ? student.getAllocatedRoom() : student.getRoomNo();
        String seat = student.getAllocatedSeat();
        String roomText = (TextUtils.isEmpty(room) || room.equalsIgnoreCase("Unassigned")) ? "Unassigned" : (room + (!TextUtils.isEmpty(seat) ? " • Seat " + seat : ""));
        dialogBinding.tvDetailRoom.setText("Room: " + roomText);

        dialogBinding.tvDetailDepartment.setText("Department: " + (!TextUtils.isEmpty(student.getDepartment()) ? student.getDepartment() : "N/A"));
        dialogBinding.tvDetailEmail.setText("Email: " + (!TextUtils.isEmpty(student.getEmail()) ? student.getEmail() : "N/A"));
        dialogBinding.tvDetailPhone.setText("Phone: " + (!TextUtils.isEmpty(student.getPhone()) ? student.getPhone() : "N/A"));

        String base64Str = student.getProfileImageBase64();
        if (!TextUtils.isEmpty(base64Str)) {
            try {
                byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    dialogBinding.ivDetailAvatar.setImageBitmap(bitmap);
                } else {
                    dialogBinding.ivDetailAvatar.setImageResource(R.drawable.ic_person);
                }
            } catch (Exception e) {
                dialogBinding.ivDetailAvatar.setImageResource(R.drawable.ic_person);
            }
        } else {
            dialogBinding.ivDetailAvatar.setImageResource(R.drawable.ic_person);
        }

        if (!TextUtils.isEmpty(student.getId())) {
            FirebaseFirestore.getInstance().collection("students").document(student.getId()).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot != null && snapshot.exists()) {
                            String firestoreBase64 = snapshot.getString("profileImageBase64");
                            if (!TextUtils.isEmpty(firestoreBase64)) {
                                student.setProfileImageBase64(firestoreBase64);
                                try {
                                    byte[] bytes = Base64.decode(firestoreBase64, Base64.DEFAULT);
                                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    if (bmp != null) {
                                        dialogBinding.ivDetailAvatar.setImageBitmap(bmp);
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                            String fsBoarderNo = snapshot.getString("boarderNo");
                            if (!TextUtils.isEmpty(fsBoarderNo)) {
                                student.setBoarderNo(fsBoarderNo);
                                dialogBinding.tvDetailBoarderNo.setText("Boarder No: " + fsBoarderNo);
                            }
                            String fsDining = snapshot.getString("dining");
                            if (!TextUtils.isEmpty(fsDining)) {
                                student.setDining(fsDining);
                                String fsCleanDining = fsDining.replaceAll("(?i)\\bhall\\b", "").trim();
                                if (fsCleanDining.isEmpty()) fsCleanDining = "East";
                                dialogBinding.tvDetailDining.setText("Dining: " + fsCleanDining);
                            }
                        }
                    });
        }

        if (!TextUtils.isEmpty(roll)) {
            dialogBinding.pbDetailBill.setVisibility(View.VISIBLE);
            dialogBinding.layoutDetailBillContent.setVisibility(View.GONE);
            String todayStr = MealCutoffUtil.formatFirestoreDate(Calendar.getInstance());
            BillingEngine.calculateBillUpToDate(roll, todayStr, new BillingEngine.OnBillCalculatedListener() {
                @Override
                public void onBillCalculated(BillingEngine.BillResult result) {
                    dialogBinding.pbDetailBill.setVisibility(View.GONE);
                    dialogBinding.layoutDetailBillContent.setVisibility(View.VISIBLE);

                    double unpaid = result.getNetUnpaidAmount();
                    dialogBinding.tvDetailUnpaidAmount.setText(String.format(Locale.US, "Unpaid Bill (up to today): %.0f BDT", unpaid));
                    dialogBinding.tvDetailBillBreakdown.setText(String.format(Locale.US,
                            "Meal: %.0f BDT (%d days) | Hall Fee: %.0f BDT | Paid: %.0f BDT",
                            result.getMealBill(), result.getActiveMealDays(), result.getHallFee(), result.getAlreadyPaid()));

                    if (unpaid <= 0) {
                        dialogBinding.tvDetailNoDueBadge.setVisibility(View.VISIBLE);
                    } else {
                        dialogBinding.tvDetailNoDueBadge.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(Exception e) {
                    dialogBinding.pbDetailBill.setVisibility(View.GONE);
                    dialogBinding.layoutDetailBillContent.setVisibility(View.VISIBLE);
                    dialogBinding.tvDetailUnpaidAmount.setText("Unpaid Bill: Unavailable");
                    dialogBinding.tvDetailBillBreakdown.setText("Failed to calculate live dues");
                }
            });
        } else {
            dialogBinding.pbDetailBill.setVisibility(View.GONE);
            dialogBinding.layoutDetailBillContent.setVisibility(View.VISIBLE);
            dialogBinding.tvDetailUnpaidAmount.setText("Unpaid Bill: No Roll Found");
            dialogBinding.tvDetailBillBreakdown.setText("Student roll is missing");
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogBinding.btnDirectCall.setOnClickListener(v -> {
            String phone = student.getPhone();
            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            } else {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phone.trim()));
                startActivity(callIntent);
            }
        });

        dialogBinding.btnSendMessage.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("student_roll", student.getRollNumber());
            intent.putExtra("chatting_with_role", "student");
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_COLOR, com.example.smarthall.util.HallThemeManager.getActivePrimaryColor(this));
            intent.putExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR, com.example.smarthall.util.HallThemeManager.getActivePrimaryDarkColor(this));
            startActivity(intent);
        });

        final boolean[] isDiningManager = {student.isDiningManager()};
        updateDiningManagerButton(dialogBinding.btnAssignDiningManager, isDiningManager[0]);

        String studentRoll = roll;
        String studentId = student.getId();

        DatabaseReference rtdbStudentsRef = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com")
                .getReference("students");

        if (!TextUtils.isEmpty(studentRoll)) {
            rtdbStudentsRef.child(studentRoll).child("isDiningManager").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Boolean val = snapshot.getValue(Boolean.class);
                        isDiningManager[0] = Boolean.TRUE.equals(val);
                        student.setDiningManager(isDiningManager[0]);
                        updateDiningManagerButton(dialogBinding.btnAssignDiningManager, isDiningManager[0]);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        dialogBinding.btnAssignDiningManager.setOnClickListener(v -> {
            if (TextUtils.isEmpty(studentRoll)) {
                Toast.makeText(this, "Student roll not found", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean newStatus = !isDiningManager[0];
            dialogBinding.btnAssignDiningManager.setEnabled(false);

            rtdbStudentsRef.child(studentRoll).child("isDiningManager").setValue(newStatus)
                    .addOnCompleteListener(task -> {
                        dialogBinding.btnAssignDiningManager.setEnabled(true);
                        if (task.isSuccessful()) {
                            isDiningManager[0] = newStatus;
                            student.setDiningManager(newStatus);
                            updateDiningManagerButton(dialogBinding.btnAssignDiningManager, newStatus);
                            String toastMsg = newStatus ? "Assigned as Dining Manager" : "Dining Manager role removed";
                            Toast.makeText(AdminDashboardActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("AdminDashboard", "Failed to update dining manager status", task.getException());
                            Toast.makeText(AdminDashboardActivity.this, "Failed to update Dining Manager status. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });

            if (!TextUtils.isEmpty(studentId) && !studentId.equals(studentRoll)) {
                rtdbStudentsRef.child(studentId).child("isDiningManager").setValue(newStatus);
                FirebaseFirestore.getInstance().collection("students").document(studentId).update("isDiningManager", newStatus);
            }
            FirebaseFirestore.getInstance().collection("students").document(studentRoll).update("isDiningManager", newStatus);
        });

        dialogBinding.btnDismissDetail.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateDiningManagerButton(com.google.android.material.button.MaterialButton button, boolean isManager) {
        if (button == null) return;
        if (isManager) {
            button.setText("Remove Dining Manager");
        } else {
            button.setText("Assign as Dining Manager");
        }
    }

    private void showAdminClosureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogAdminClosureBinding dialogBinding = DialogAdminClosureBinding.inflate(LayoutInflater.from(this));
        builder.setView(dialogBinding.getRoot());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final Calendar[] closureFrom = new Calendar[1];
        final Calendar[] closureTo = new Calendar[1];

        dialogBinding.etClosureFromDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Closure From Date")
                    .setSelection(closureFrom[0] != null ? closureFrom[0].getTimeInMillis() : MaterialDatePicker.todayInUtcMilliseconds())
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utcCal.setTimeInMillis(selection);
                Calendar localCal = Calendar.getInstance();
                localCal.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                localCal.set(Calendar.MILLISECOND, 0);
                closureFrom[0] = localCal;
                dialogBinding.etClosureFromDate.setText(MealCutoffUtil.formatDisplayDate(closureFrom[0]));
                if (closureTo[0] == null || closureTo[0].before(closureFrom[0])) {
                    closureTo[0] = (Calendar) closureFrom[0].clone();
                    dialogBinding.etClosureToDate.setText(MealCutoffUtil.formatDisplayDate(closureTo[0]));
                }
            });
            picker.show(getSupportFragmentManager(), "CLOSURE_FROM_PICKER");
        });

        dialogBinding.etClosureToDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Closure To Date")
                    .setSelection(closureTo[0] != null ? closureTo[0].getTimeInMillis() : (closureFrom[0] != null ? closureFrom[0].getTimeInMillis() : MaterialDatePicker.todayInUtcMilliseconds()))
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utcCal.setTimeInMillis(selection);
                Calendar localCal = Calendar.getInstance();
                localCal.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                localCal.set(Calendar.MILLISECOND, 0);
                closureTo[0] = localCal;
                dialogBinding.etClosureToDate.setText(MealCutoffUtil.formatDisplayDate(closureTo[0]));
                if (closureFrom[0] == null) {
                    closureFrom[0] = (Calendar) closureTo[0].clone();
                    dialogBinding.etClosureFromDate.setText(MealCutoffUtil.formatDisplayDate(closureFrom[0]));
                }
            });
            picker.show(getSupportFragmentManager(), "CLOSURE_TO_PICKER");
        });

        dialogBinding.btnCancelClosure.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnApplyClosure.setOnClickListener(v -> {
            if (closureFrom[0] == null || closureTo[0] == null) {
                Toast.makeText(this, "Please select both From and To dates", Toast.LENGTH_SHORT).show();
                return;
            }
            if (closureTo[0].before(closureFrom[0])) {
                Toast.makeText(this, "To Date must be equal to or after From Date", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = dialogBinding.rgClosureType.getCheckedRadioButtonId();
            boolean isClear = selectedId == R.id.rbClearClosure;
            String status = selectedId == R.id.rbHallClose ? "HALL_CLOSE" : "MEAL_CLOSE";

            WriteBatch batch = FirebaseFirestore.getInstance().batch();
            Calendar cursor = (Calendar) closureFrom[0].clone();
            cursor.set(Calendar.HOUR_OF_DAY, 0);
            cursor.set(Calendar.MINUTE, 0);
            cursor.set(Calendar.SECOND, 0);
            cursor.set(Calendar.MILLISECOND, 0);

            Calendar endLimit = (Calendar) closureTo[0].clone();
            endLimit.set(Calendar.HOUR_OF_DAY, 0);
            endLimit.set(Calendar.MINUTE, 0);
            endLimit.set(Calendar.SECOND, 0);
            endLimit.set(Calendar.MILLISECOND, 0);

            long now = System.currentTimeMillis();

            while (!cursor.after(endLimit)) {
                String docId = MealCutoffUtil.formatFirestoreDate(cursor);
                DocumentReference docRef = FirebaseFirestore.getInstance().collection("hall_schedule").document(docId);
                if (isClear) {
                    batch.delete(docRef);
                } else {
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", docId);
                    data.put("status", status);
                    data.put("updatedAt", now);
                    batch.set(docRef, data);
                }
                cursor.add(Calendar.DAY_OF_MONTH, 1);
            }

            String startDateStr = MealCutoffUtil.formatDisplayDate(closureFrom[0]);
            String endDateStr = MealCutoffUtil.formatDisplayDate(closureTo[0]);
            String typePrefix = selectedId == R.id.rbHallClose ? "Hall" : "Meal";
            String noticeTitle = typePrefix + " Closure from " + startDateStr + " to " + endDateStr;
            String noticeMessage = typePrefix + " will be closed from " + startDateStr + " to " + endDateStr;

            if (!isClear) {
                DocumentReference noticeDocRef = FirebaseFirestore.getInstance().collection("notices").document();
                Map<String, Object> noticeData = new HashMap<>();
                noticeData.put("id", noticeDocRef.getId());
                noticeData.put("title", noticeTitle);
                noticeData.put("message", noticeMessage);
                noticeData.put("postedBy", "Hall Administration");
                noticeData.put("timestamp", now);
                noticeData.put("priority", "HIGH");
                noticeData.put("type", "CLOSURE");
                batch.set(noticeDocRef, noticeData);
            }

            dialogBinding.btnApplyClosure.setEnabled(false);
            batch.commit().addOnSuccessListener(aVoid -> {
                if (!isClear) {
                    String noticeId = noticeRef.push().getKey();
                    if (noticeId != null) {
                        NoticeModel rtdbNotice = new NoticeModel(noticeId, noticeTitle, noticeMessage, "Hall Administration", now);
                        noticeRef.child(noticeId).setValue(rtdbNotice);
                        FcmNotificationSender.sendNoticeTopicNotification(AdminDashboardActivity.this, noticeId, noticeTitle, noticeMessage, "Hall Administration", now);
                    }
                }
                Toast.makeText(this, isClear ? "Closure cleared successfully" : "Hall schedule updated successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                dialogBinding.btnApplyClosure.setEnabled(true);
                Log.e("AdminDashboard", "Failed to update schedule", e);
                Toast.makeText(this, "Unable to complete request. Please check your internet connection.", Toast.LENGTH_LONG).show();
            });
        });

        dialog.show();
    }

    private void logout() {
        stopService(new Intent(this, EmergencySosForegroundService.class));
        getSharedPreferences("user_session", Context.MODE_PRIVATE).edit().clear().apply();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        MenuItem chatItem = menu.findItem(R.id.action_chat);
        if (chatItem != null) {
            View actionView = chatItem.getActionView();
            if (actionView != null) {
                actionView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, AdminChatListActivity.class);
                    startActivity(intent);
                });
                tvChatBadgeCount = actionView.findViewById(R.id.tvBadgeCount);
                setupChatBadgeListener();
            }
        }
        return true;
    }

    private void setupChatBadgeListener() {
        if (tvChatBadgeCount == null) {
            return;
        }
        chatListBadgeRef = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com")
                .getReference("admin_chat_list");
        if (chatBadgeListener != null) {
            chatListBadgeRef.removeEventListener(chatBadgeListener);
        }
        chatBadgeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalUnread = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Object val = child.child("unreadCount").getValue();
                    if (val instanceof Number) {
                        totalUnread += ((Number) val).intValue();
                    }
                }
                updateBadgeCount(tvChatBadgeCount, totalUnread);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        chatListBadgeRef.addValueEventListener(chatBadgeListener);
    }

    private void updateBadgeCount(TextView tvBadgeCount, int count) {
        if (tvBadgeCount == null) {
            return;
        }
        if (count > 0) {
            tvBadgeCount.setVisibility(View.VISIBLE);
            if (count > 9) {
                tvBadgeCount.setText("9+");
            } else {
                tvBadgeCount.setText(String.valueOf(count));
            }
        } else {
            tvBadgeCount.setVisibility(View.GONE);
        }
    }

    private void setupComplaintsListener() {
        complaintsRef = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com")
                .getReference("complaints");
        complaintsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long total = snapshot.getChildrenCount();
                int pending = 0;
                int resolved = 0;
                complaintList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String status = child.child("status").getValue(String.class);
                    if (status != null && status.equalsIgnoreCase("resolved")) {
                        resolved++;
                    } else {
                        pending++;
                    }

                    String id = child.child("complaintId").getValue(String.class);
                    if (id == null) {
                        id = child.getKey();
                    }
                    String title = child.child("title").getValue(String.class);
                    String description = child.child("description").getValue(String.class);
                    String studentRoll = child.child("studentRoll").getValue(String.class);
                    String studentPhone = child.child("studentPhone").getValue(String.class);
                    Long timestampVal = child.child("timestamp").getValue(Long.class);
                    long timestamp = timestampVal != null ? timestampVal : 0L;

                    Complaint complaint = new Complaint(id, title, description, studentRoll, studentPhone, status, timestamp);
                    complaintList.add(complaint);
                }

                Collections.sort(complaintList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                binding.tvComplaintAnalytics.setText("Total Complaints: " + total + " | Pending: " + pending + " | Resolved: " + resolved);
                binding.tvStatComplaints.setText(String.valueOf(total));

                complaintAdapter.notifyDataSetChanged();
                refreshComplaints();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        complaintsRef.addValueEventListener(complaintsListener);
    }
}
