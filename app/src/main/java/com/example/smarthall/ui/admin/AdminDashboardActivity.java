package com.example.smarthall.ui.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.example.smarthall.ui.adapters.ComplaintAdapter;
import com.example.smarthall.ui.adapters.FirebaseNoticeAdapter;
import com.example.smarthall.ui.adapters.StudentAdapter;
import com.example.smarthall.ui.auth.LoginActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.ui.theme.ThemeSelectorBottomSheet;
import com.example.smarthall.util.BillingEngine;
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
    private ComplaintAdapter complaintAdapter;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = HallRepository.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com");
        noticeRef = database.getReference("hall_announcements");

        setupHeader();
        setupNavigationDrawer();
        setupRecyclerViews();
        setupTabButtons();
        setupBroadcastNotice();
        setupNoticesListener();
        setupStudentsListener();
        RoomAllocationManager.seedDefaultRoomsIfEmpty(null);
        switchTab(TAB_STUDENTS);
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
            switchTab(TAB_COMPLAINTS);
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

        complaintAdapter = new ComplaintAdapter(repository.getAllComplaints(),
                complaint -> {
                    repository.toggleComplaintStatus(complaint.getId());
                    refreshComplaints();
                    Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                }, true);
        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaints.setAdapter(complaintAdapter);

        noticeAdapter = new FirebaseNoticeAdapter(noticeList);
        binding.rvNotices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotices.setAdapter(noticeAdapter);
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

    private void setupBroadcastNotice() {
        binding.btnBroadcastNotice.setOnClickListener(v -> {
            String title = binding.etNoticeTitle.getText() != null
                    ? binding.etNoticeTitle.getText().toString().trim()
                    : "";
            String message = binding.etNoticeMessage.getText() != null
                    ? binding.etNoticeMessage.getText().toString().trim()
                    : "";

            if (TextUtils.isEmpty(title)) {
                binding.etNoticeTitle.setError("Notice title is required");
                binding.etNoticeTitle.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(message)) {
                binding.etNoticeMessage.setError("Notice content is required");
                binding.etNoticeMessage.requestFocus();
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
                    binding.etNoticeTitle.setText("");
                    binding.etNoticeMessage.setText("");
                    Toast.makeText(AdminDashboardActivity.this, "Emergency notice broadcasted live", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to broadcast notice";
                    Toast.makeText(AdminDashboardActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });
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
        List<Complaint> complaints = repository.getAllComplaints();
        complaintAdapter.updateData(complaints);
        binding.tvStatComplaints.setText(String.valueOf(complaints.size()));
        binding.tvEmptyComplaints.setVisibility(complaints.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvComplaints.setVisibility(complaints.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void refreshNotices() {
        boolean isEmpty = noticeList.isEmpty();
        binding.tvEmptyNotices.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvNotices.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showAddNoticeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to publish notice";
                    Toast.makeText(AdminDashboardActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Messaging feature will be available soon", Toast.LENGTH_SHORT).show();
        });

        dialogBinding.btnAssignDiningManager.setOnClickListener(v -> {
            Toast.makeText(this, "Dining Manager assignment will be available soon", Toast.LENGTH_SHORT).show();
        });

        dialogBinding.btnDismissDetail.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
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
                Toast.makeText(this, "Failed to update schedule: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });

        dialog.show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
