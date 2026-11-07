package com.example.smarthall.ui.student;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.ActivityUnpaidBillBinding;
import com.example.smarthall.model.FeeRecord;
import com.example.smarthall.model.PaymentRecord;
import com.example.smarthall.model.Student;
import com.example.smarthall.ui.adapters.PaymentHistoryAdapter;
import com.example.smarthall.ui.base.BaseActivity;
import com.example.smarthall.util.BillingEngine;
import com.example.smarthall.util.ThemeHelper;
import com.example.smarthall.util.MealCutoffUtil;
import com.example.smarthall.util.PdfReceiptGenerator;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class UnpaidBillActivity extends BaseActivity {

    private ActivityUnpaidBillBinding binding;
    private Calendar selectedDate;
    private BillingEngine.BillResult currentBillResult;
    private Student currentStudent;
    private String studentRoll;

    private PaymentHistoryAdapter paymentHistoryAdapter;
    private final List<PaymentRecord> paymentHistoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityUnpaidBillBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbarUnpaidBill.setTitle("Unpaid Bill");
        binding.toolbarUnpaidBill.setNavigationOnClickListener(v -> finish());

        selectedDate = Calendar.getInstance();

        resolveStudentProfile();

        updateDateDisplay();

        setupPaymentHistoryRecyclerView();

        binding.layoutTargetDate.setOnClickListener(v -> openDatePicker());

        binding.btnPayUnpaidBill.setOnClickListener(v -> initiatePayment());

        loadBillCalculation();
        loadPaymentHistory();
    }

    private void resolveStudentProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
        studentRoll = prefs.getString("student_roll", "");

        currentStudent = new Student();
        currentStudent.setId(user.getUid());
        currentStudent.setName(prefs.getString("student_name", "Student"));
        currentStudent.setRollNumber(studentRoll);
        currentStudent.setDepartment(prefs.getString("student_department", "N/A"));
        currentStudent.setEmail(prefs.getString("student_email", user.getEmail() != null ? user.getEmail() : ""));
        currentStudent.setPhone(prefs.getString("student_phone", ""));
        currentStudent.setAllocatedRoom(prefs.getString("student_room", "N/A"));
        currentStudent.setBoarderNo(prefs.getString("boarder_no", "N/A"));
        String dining = prefs.getString("dining_hall", "East");
        String cleanDining = dining != null ? dining.replaceAll("(?i)\\bhall\\b", "").trim() : "East";
        if (cleanDining.isEmpty()) cleanDining = "East";
        currentStudent.setDining(cleanDining);

        if (TextUtils.isEmpty(studentRoll)) {
            studentRoll = getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("roll", null);
        }
        if (TextUtils.isEmpty(studentRoll)) {
            studentRoll = getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("student_roll", null);
        }

        if (TextUtils.isEmpty(studentRoll)) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            String r = doc.getString("roll");
                            if (TextUtils.isEmpty(r)) {
                                r = doc.getString("rollNumber");
                            }
                            if (!TextUtils.isEmpty(r)) {
                                studentRoll = r;
                                currentStudent.setRollNumber(r);
                                String d = doc.getString("dining");
                                if (!TextUtils.isEmpty(d)) {
                                    String cd = d.replaceAll("(?i)\\bhall\\b", "").trim();
                                    if (cd.isEmpty()) cd = "East";
                                    currentStudent.setDining(cd);
                                }
                                loadBillCalculation();
                                loadPaymentHistory();
                            }
                        }
                    });
        }
    }

    private void setupPaymentHistoryRecyclerView() {
        paymentHistoryAdapter = new PaymentHistoryAdapter(paymentHistoryList, payment -> {
            PdfReceiptGenerator.generateAndOpenReceipt(UnpaidBillActivity.this, payment, currentStudent, null);
        });
        binding.rvPaymentHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPaymentHistory.setNestedScrollingEnabled(false);
        binding.rvPaymentHistory.setAdapter(paymentHistoryAdapter);
    }

    private void updateDateDisplay() {
        String display = MealCutoffUtil.formatDisplayDate(selectedDate);
        binding.tvSelectedTargetDate.setText("Billing Date: " + display);
    }

    private void openDatePicker() {
        Calendar startLimit = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        startLimit.set(2026, Calendar.OCTOBER, 1, 0, 0, 0);
        startLimit.set(Calendar.MILLISECOND, 0);
        long minUtcMillis = startLimit.getTimeInMillis();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(minUtcMillis)
                .setValidator(DateValidatorPointForward.from(minUtcMillis))
                .build();

        long selectionMillis = Math.max(minUtcMillis, selectedDate.getTimeInMillis());

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Billing Date")
                .setSelection(selectionMillis)
                .setCalendarConstraints(constraints)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCal.setTimeInMillis(selection);

            selectedDate = Calendar.getInstance();
            selectedDate.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);

            updateDateDisplay();
            loadBillCalculation();
        });

        picker.show(getSupportFragmentManager(), "TARGET_BILLING_DATE_PICKER");
    }

    private void loadBillCalculation() {
        if (TextUtils.isEmpty(studentRoll)) {
            Toast.makeText(this, "Student roll not found in active profile", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.pbLoadingBill.setVisibility(View.VISIBLE);
        binding.layoutBillContent.setVisibility(View.GONE);
        binding.btnPayUnpaidBill.setEnabled(false);

        String targetDateStr = MealCutoffUtil.formatFirestoreDate(selectedDate);

        BillingEngine.calculateBillUpToDate(studentRoll, targetDateStr, new BillingEngine.OnBillCalculatedListener() {
            @Override
            public void onBillCalculated(BillingEngine.BillResult result) {
                currentBillResult = result;
                binding.pbLoadingBill.setVisibility(View.GONE);
                binding.layoutBillContent.setVisibility(View.VISIBLE);

                binding.tvMealDays.setText(String.format(Locale.US, "Active Meal Days (%d Days x 65 BDT)", result.getActiveMealDays()));
                binding.tvMealCost.setText(String.format(Locale.US, "%.0f BDT", result.getMealBill()));

                binding.tvHallMonths.setText(String.format(Locale.US, "Hall Utility Fee (%d Mo x 250 BDT)", result.getHallMonths()));
                binding.tvHallFee.setText(String.format(Locale.US, "%.0f BDT", result.getHallFee()));

                binding.tvGrossBill.setText(String.format(Locale.US, "%.0f BDT", result.getGrossBill()));
                binding.tvPreviouslyPaid.setText(String.format(Locale.US, "%.0f BDT", result.getAlreadyPaid()));

                double unpaid = result.getNetUnpaidAmount();
                binding.tvNetUnpaidAmount.setText(String.format(Locale.US, "Unpaid Bill: %.0f BDT", unpaid));

                if (unpaid <= 0) {
                    binding.btnPayUnpaidBill.setEnabled(false);
                    binding.btnPayUnpaidBill.setText("All Dues Cleared");
                } else {
                    binding.btnPayUnpaidBill.setEnabled(true);
                    binding.btnPayUnpaidBill.setText(String.format(Locale.US, "Pay Unpaid Bill (%.0f BDT)", unpaid));
                }
            }

            @Override
            public void onError(Exception e) {
                binding.pbLoadingBill.setVisibility(View.GONE);
                binding.layoutBillContent.setVisibility(View.VISIBLE);
                String msg = e != null ? e.getMessage() : "Unknown error";
                Log.e("StudentDashboardError", "Operation failed: Failed to calculate bill: " + msg, e);
                Toast.makeText(UnpaidBillActivity.this, "Failed to calculate bill: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(studentRoll)) {
            loadPaymentHistory();
        }
    }

    private void loadPaymentHistory() {
        if (TextUtils.isEmpty(studentRoll)) {
            binding.tvEmptyPaymentHistory.setVisibility(View.VISIBLE);
            binding.rvPaymentHistory.setVisibility(View.GONE);
            return;
        }

        FirebaseFirestore.getInstance().collection("payments")
                .whereEqualTo("roll", studentRoll)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<PaymentRecord> historyList = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            PaymentRecord record = doc.toObject(PaymentRecord.class);
                            if (record == null) {
                                record = new PaymentRecord();
                            }
                            if (TextUtils.isEmpty(record.getTrxId())) {
                                String trxId = doc.getString("trxId");
                                record.setTrxId(TextUtils.isEmpty(trxId) ? doc.getId() : trxId);
                            }
                            if (TextUtils.isEmpty(record.getRoll())) {
                                record.setRoll(doc.getString("roll"));
                            }
                            if (record.getAmount() == 0.0 && doc.get("amount") != null) {
                                Double amt = doc.getDouble("amount");
                                if (amt != null) record.setAmount(amt);
                            }
                            if (TextUtils.isEmpty(record.getMethod())) {
                                record.setMethod(doc.getString("method"));
                            }
                            if (TextUtils.isEmpty(record.getAccountNumber())) {
                                record.setAccountNumber(doc.getString("accountNumber"));
                            }
                            if (TextUtils.isEmpty(record.getFeeRecordId())) {
                                record.setFeeRecordId(doc.getString("feeRecordId"));
                            }
                            if (record.getPaidAt() == 0L && doc.get("paidAt") != null) {
                                Long paidAt = doc.getLong("paidAt");
                                if (paidAt != null) record.setPaidAt(paidAt);
                            }
                            if (TextUtils.isEmpty(record.getStatus())) {
                                record.setStatus(doc.getString("status"));
                            }
                            historyList.add(record);
                        }
                        Collections.sort(historyList, (p1, p2) -> Long.compare(p2.getPaidAt(), p1.getPaidAt()));
                    }

                    paymentHistoryList.clear();
                    paymentHistoryList.addAll(historyList);

                    if (paymentHistoryList.isEmpty()) {
                        binding.tvEmptyPaymentHistory.setVisibility(View.VISIBLE);
                        binding.rvPaymentHistory.setVisibility(View.GONE);
                    } else {
                        binding.tvEmptyPaymentHistory.setVisibility(View.GONE);
                        binding.rvPaymentHistory.setVisibility(View.VISIBLE);
                    }
                    paymentHistoryAdapter.updateData(paymentHistoryList);
                    paymentHistoryAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (paymentHistoryList.isEmpty()) {
                        binding.tvEmptyPaymentHistory.setVisibility(View.VISIBLE);
                        binding.rvPaymentHistory.setVisibility(View.GONE);
                    }
                });
    }

    private void initiatePayment() {
        if (currentBillResult == null || currentBillResult.getNetUnpaidAmount() <= 0) {
            Toast.makeText(this, "No outstanding dues to pay", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetDisplayDate = MealCutoffUtil.formatDisplayDate(selectedDate);
        String targetDateKey = MealCutoffUtil.formatFirestoreDate(selectedDate).replace("-", "");
        String feeRecordId = "BILL_" + studentRoll + "_" + targetDateKey;

        FeeRecord feeRecord = new FeeRecord();
        feeRecord.setId(feeRecordId);
        feeRecord.setRoll(studentRoll);
        feeRecord.setMonth("Bill up to " + targetDisplayDate);
        feeRecord.setDiningFee(currentBillResult.getMealBill());
        feeRecord.setHallFee(currentBillResult.getHallFee());
        feeRecord.setTotalAmount(currentBillResult.getNetUnpaidAmount());
        feeRecord.setAmount(currentBillResult.getNetUnpaidAmount());
        feeRecord.setDueDate(targetDisplayDate);
        feeRecord.setStatus(FeeRecord.STATUS_UNPAID);

        PaymentGatewayBottomSheet sheet = PaymentGatewayBottomSheet.newInstance(feeRecord, studentRoll);
        sheet.setPaymentCallback(payment -> {
            Toast.makeText(UnpaidBillActivity.this, "Payment successful! Generating receipt...", Toast.LENGTH_SHORT).show();
            PdfReceiptGenerator.generateAndOpenReceipt(UnpaidBillActivity.this, payment, currentStudent, currentBillResult);
            PaymentReceiptDialog dialog = PaymentReceiptDialog.newInstance(payment);
            dialog.show(getSupportFragmentManager(), "RECEIPT_DIALOG");
            loadBillCalculation();
            loadPaymentHistory();
        });

        sheet.show(getSupportFragmentManager(), "PAYMENT_GATEWAY");
    }
}
