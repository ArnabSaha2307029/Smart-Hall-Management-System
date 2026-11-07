package com.example.smarthall.ui.student;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.smarthall.databinding.ActivityMealManagementBinding;
import com.example.smarthall.model.CalendarDay;
import com.example.smarthall.ui.adapters.CalendarDayAdapter;
import com.example.smarthall.ui.auth.LoginActivity;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.MealCutoffUtil;
import com.example.smarthall.util.ThemeHelper;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MealManagementActivity extends BaseActivity {

    private ActivityMealManagementBinding binding;
    private FirebaseFirestore db;
    private String studentRoll;

    private String selectedFromDateStr;
    private String selectedToDateStr;

    private Calendar currentDisplayMonth;
    private CalendarDayAdapter calendarDayAdapter;

    private Map<String, String> currentMonthHallSchedule = new HashMap<>();
    private Map<String, String> currentMonthStudentMeals = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMealManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        db = FirebaseFirestore.getInstance();
        currentDisplayMonth = Calendar.getInstance();
        currentDisplayMonth.set(Calendar.DAY_OF_MONTH, 1);

        resolveStudentRoll(currentUser.getUid());

        setupCutoffNotice();
        setupDateInputs();
        setupMonthNavigation();
        setupCalendarRecyclerView();
        setupUpdateButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }
        if (TextUtils.isEmpty(studentRoll)) {
            resolveStudentRoll(currentUser.getUid());
        } else {
            loadMonthData();
        }
    }

    private void resolveStudentRoll(String uid) {
        studentRoll = getIntent().getStringExtra("EXTRA_STUDENT_ROLL");
        if (TextUtils.isEmpty(studentRoll)) {
            SharedPreferences sessionPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
            studentRoll = sessionPrefs.getString("roll", null);
            if (TextUtils.isEmpty(studentRoll)) {
                studentRoll = sessionPrefs.getString("student_roll", null);
            }
        }
        if (TextUtils.isEmpty(studentRoll)) {
            SharedPreferences prefs = getSharedPreferences("student_prefs_" + uid, Context.MODE_PRIVATE);
            studentRoll = prefs.getString("student_roll", null);
            if (TextUtils.isEmpty(studentRoll)) {
                studentRoll = prefs.getString("roll", null);
            }
            if (TextUtils.isEmpty(studentRoll)) {
                studentRoll = prefs.getString("roll_number", null);
            }
        }

        if (TextUtils.isEmpty(studentRoll)) {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    studentRoll = doc.getString("roll");
                    if (TextUtils.isEmpty(studentRoll)) {
                        studentRoll = doc.getString("rollNumber");
                    }
                    if (TextUtils.isEmpty(studentRoll)) {
                        studentRoll = doc.getString("studentId");
                    }
                    if (!TextUtils.isEmpty(studentRoll)) {
                        SharedPreferences prefs = getSharedPreferences("student_prefs_" + uid, Context.MODE_PRIVATE);
                        prefs.edit().putString("student_roll", studentRoll).apply();
                    }
                }
                if (!TextUtils.isEmpty(studentRoll)) {
                    loadMonthData();
                } else {
                    renderCalendarGrid();
                }
            }).addOnFailureListener(e -> {
                Log.e("MEAL_ROLL", "Failed to resolve student roll from Firestore", e);
                renderCalendarGrid();
            });
        } else {
            loadMonthData();
        }
    }

    private void setupCutoffNotice() {
        Calendar earliest = MealCutoffUtil.getEarliestPermissibleDate();
        String dateFormatted = MealCutoffUtil.formatDisplayDate(earliest);
        binding.tvNoticeCutoffDate.setText("You can off/ on your meal from or later date of " + dateFormatted);
        binding.tvNoticeRule.setText("You can on/off your meal before 08:00pm previous day");

        if (TextUtils.isEmpty(selectedFromDateStr)) {
            selectedFromDateStr = MealCutoffUtil.getEarliestPermissibleDateString();
            binding.etFromDate.setText(dateFormatted);
        }
        if (TextUtils.isEmpty(selectedToDateStr)) {
            selectedToDateStr = selectedFromDateStr;
            binding.etToDate.setText(dateFormatted);
        }
    }

    private void setupDateInputs() {
        binding.etFromDate.setOnClickListener(v -> showDatePicker(true));
        binding.etToDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar earliest = MealCutoffUtil.getEarliestPermissibleDate();
        CalendarConstraints.Builder constraints = new CalendarConstraints.Builder();
        constraints.setValidator(DateValidatorPointForward.from(earliest.getTimeInMillis()));

        long preselect = MaterialDatePicker.todayInUtcMilliseconds();
        SimpleDateFormat utcDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (isFromDate && !TextUtils.isEmpty(selectedFromDateStr)) {
            try {
                Date d = utcDateFormat.parse(selectedFromDateStr);
                if (d != null) preselect = d.getTime();
            } catch (Exception ignored) {
            }
        } else if (!isFromDate && !TextUtils.isEmpty(selectedToDateStr)) {
            try {
                Date d = utcDateFormat.parse(selectedToDateStr);
                if (d != null) preselect = d.getTime();
            } catch (Exception ignored) {
            }
        }

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isFromDate ? "Select From Date" : "Select To Date")
                .setCalendarConstraints(constraints.build())
                .setSelection(preselect)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            displayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date selectedDate = new Date(selection);
            String formattedDate = utcDateFormat.format(selectedDate);
            String displayDate = displayFormat.format(selectedDate);

            if (isFromDate) {
                selectedFromDateStr = formattedDate;
                binding.etFromDate.setText(displayDate);
                if (selectedToDateStr == null || selectedToDateStr.compareTo(selectedFromDateStr) < 0) {
                    selectedToDateStr = selectedFromDateStr;
                    binding.etToDate.setText(displayDate);
                }
            } else {
                selectedToDateStr = formattedDate;
                binding.etToDate.setText(displayDate);
                if (selectedFromDateStr == null) {
                    selectedFromDateStr = selectedToDateStr;
                    binding.etFromDate.setText(displayDate);
                }
            }
        });

        datePicker.show(getSupportFragmentManager(), isFromDate ? "FROM_DATE_PICKER" : "TO_DATE_PICKER");
    }

    private void setupMonthNavigation() {
        updateMonthYearDisplay();
        binding.btnPrevMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, -1);
            updateMonthYearDisplay();
            loadMonthData();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, 1);
            updateMonthYearDisplay();
            loadMonthData();
        });
    }

    private void updateMonthYearDisplay() {
        String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentDisplayMonth.getTime());
        binding.tvCurrentMonthYear.setText(monthYear);
    }

    private void setupCalendarRecyclerView() {
        calendarDayAdapter = new CalendarDayAdapter(new ArrayList<>());
        binding.rvCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        binding.rvCalendarGrid.setAdapter(calendarDayAdapter);
    }

    private void loadMonthData() {
        if (TextUtils.isEmpty(studentRoll)) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                resolveStudentRoll(currentUser.getUid());
            }
            return;
        }

        db.collection("hall_schedule")
                .get()
                .addOnSuccessListener(scheduleSnapshots -> {
                    currentMonthHallSchedule.clear();
                    if (scheduleSnapshots != null) {
                        for (DocumentSnapshot doc : scheduleSnapshots.getDocuments()) {
                            String d = doc.getString("date");
                            String s = doc.getString("status");
                            if (d != null && s != null) {
                                currentMonthHallSchedule.put(d, s);
                            }
                        }
                    }

                    db.collection("meals")
                            .whereEqualTo("roll", studentRoll)
                            .get()
                            .addOnSuccessListener(mealsSnapshots -> {
                                currentMonthStudentMeals.clear();
                                if (mealsSnapshots != null) {
                                    for (DocumentSnapshot doc : mealsSnapshots.getDocuments()) {
                                        String d = doc.getString("date");
                                        String s = doc.getString("status");
                                        if (d != null && s != null) {
                                            currentMonthStudentMeals.put(d, s);
                                        }
                                    }
                                }
                                renderCalendarGrid();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("MEAL_LOAD", "Failed to fetch student meals", e);
                                renderCalendarGrid();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("MEAL_LOAD", "Failed to fetch hall schedule", e);
                    renderCalendarGrid();
                });
    }

    private void renderCalendarGrid() {
        Calendar cal = (Calendar) currentDisplayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<CalendarDay> days = new ArrayList<>();
        int leadingEmptyDays = firstDayOfWeek - 1;
        for (int i = 0; i < leadingEmptyDays; i++) {
            days.add(new CalendarDay(0, "", "", false));
        }

        for (int day = 1; day <= maxDays; day++) {
            String dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            String status = "ON";

            if (currentMonthHallSchedule.containsKey(dateStr)) {
                status = currentMonthHallSchedule.get(dateStr);
            } else if (currentMonthStudentMeals.containsKey(dateStr)) {
                status = currentMonthStudentMeals.get(dateStr);
            }

            days.add(new CalendarDay(day, dateStr, status, true));
        }

        calendarDayAdapter.updateDays(days);
    }

    private void setupUpdateButton() {
        binding.btnUpdateMeal.setOnClickListener(v -> updateMealStatus());
    }

    private void updateMealStatus() {
        if (TextUtils.isEmpty(selectedFromDateStr) || TextUtils.isEmpty(selectedToDateStr)) {
            Toast.makeText(this, "Please select both From and To dates", Toast.LENGTH_SHORT).show();
            return;
        }

        String earliestDateStr = MealCutoffUtil.getEarliestPermissibleDateString();
        if (selectedFromDateStr.compareTo(earliestDateStr) < 0 || selectedToDateStr.compareTo(earliestDateStr) < 0) {
            Toast.makeText(this, "Cannot update past cut-off time (08:00 PM)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedToDateStr.compareTo(selectedFromDateStr) < 0) {
            Toast.makeText(this, "To Date cannot be before From Date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(studentRoll)) {
            Toast.makeText(this, "Student roll not found. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String chosenStatus = binding.rbMealOn.isChecked() ? "ON" : "OFF";
        binding.btnUpdateMeal.setEnabled(false);

        db.collection("hall_schedule")
                .get()
                .addOnSuccessListener(closuresSnapshots -> {
                    Map<String, String> closuresInRange = new HashMap<>();
                    if (closuresSnapshots != null) {
                        for (DocumentSnapshot doc : closuresSnapshots.getDocuments()) {
                            String d = doc.getString("date");
                            String s = doc.getString("status");
                            if (d != null && s != null) {
                                if (d.compareTo(selectedFromDateStr) >= 0 && d.compareTo(selectedToDateStr) <= 0) {
                                    closuresInRange.put(d, s);
                                }
                            }
                        }
                    }

                    SimpleDateFormat utcDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                    Date fromDate;
                    Date toDate;
                    try {
                        fromDate = utcDateFormat.parse(selectedFromDateStr);
                        toDate = utcDateFormat.parse(selectedToDateStr);
                    } catch (Exception e) {
                        binding.btnUpdateMeal.setEnabled(true);
                        Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (fromDate == null || toDate == null) {
                        binding.btnUpdateMeal.setEnabled(true);
                        Toast.makeText(this, "Invalid date range", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Calendar loopCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    loopCal.setTime(fromDate);
                    Calendar endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    endCal.setTime(toDate);

                    WriteBatch batch = db.batch();
                    int updateCount = 0;
                    int closedCount = 0;
                    long now = System.currentTimeMillis();

                    while (!loopCal.after(endCal)) {
                        String dateStr = utcDateFormat.format(loopCal.getTime());
                        if (closuresInRange.containsKey(dateStr)) {
                            closedCount++;
                        } else {
                            DocumentReference docRef = db.collection("meals").document(studentRoll + "_" + dateStr);
                            Map<String, Object> mealData = new HashMap<>();
                            mealData.put("roll", studentRoll);
                            mealData.put("date", dateStr);
                            mealData.put("status", chosenStatus);
                            mealData.put("updatedAt", now);
                            batch.set(docRef, mealData, SetOptions.merge());
                            updateCount++;
                        }
                        loopCal.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    if (updateCount == 0) {
                        binding.btnUpdateMeal.setEnabled(true);
                        if (closedCount > 0) {
                            Toast.makeText(this, "Selected dates are locked by hall/meal closure", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "No dates to update", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    final int finalClosedCount = closedCount;
                    final int finalUpdateCount = updateCount;

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                binding.btnUpdateMeal.setEnabled(true);
                                Calendar memCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                memCal.setTime(fromDate);
                                while (!memCal.after(endCal)) {
                                    String d = utcDateFormat.format(memCal.getTime());
                                    if (!closuresInRange.containsKey(d)) {
                                        currentMonthStudentMeals.put(d, chosenStatus);
                                    }
                                    memCal.add(Calendar.DAY_OF_YEAR, 1);
                                }
                                renderCalendarGrid();
                                if (finalClosedCount > 0) {
                                    Toast.makeText(this, "Updated " + finalUpdateCount + " days (Skipped " + finalClosedCount + " closed dates)", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, "Meal updated successfully", Toast.LENGTH_SHORT).show();
                                }
                                loadMonthData();
                            })
                            .addOnFailureListener(e -> {
                                binding.btnUpdateMeal.setEnabled(true);
                                String msg = e != null ? e.getMessage() : "Unknown error";
                                Log.e("StudentDashboardError", "Operation failed: Failed to update meals: " + msg, e);
                                Toast.makeText(this, "Failed to update meals: " + msg, Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.btnUpdateMeal.setEnabled(true);
                    String msg = e != null ? e.getMessage() : "Unknown error";
                    Log.e("StudentDashboardError", "Operation failed: Failed to check closures: " + msg, e);
                    Toast.makeText(this, "Failed to update meals: " + msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
