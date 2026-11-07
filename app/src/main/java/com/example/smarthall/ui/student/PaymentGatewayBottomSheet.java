package com.example.smarthall.ui.student;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.smarthall.R;
import com.example.smarthall.databinding.BottomSheetMockPaymentBinding;
import com.example.smarthall.model.FeeRecord;
import com.example.smarthall.model.PaymentRecord;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class PaymentGatewayBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_RECORD_ID = "record_id";
    private static final String ARG_ROLL = "roll";
    private static final String ARG_MONTH = "month";
    private static final String ARG_DINING_FEE = "dining_fee";
    private static final String ARG_HALL_FEE = "hall_fee";
    private static final String ARG_TOTAL_AMOUNT = "total_amount";

    public interface PaymentCallback {
        void onPaymentSuccess(PaymentRecord payment);
    }

    private BottomSheetMockPaymentBinding binding;
    private PaymentCallback callback;

    private int generatedOtp = 0;
    private String validatedAccount = "";
    private String selectedMethod = "bKash";

    private ActivityResultLauncher<String> requestPermissionLauncher;

    public static PaymentGatewayBottomSheet newInstance(FeeRecord feeRecord, String roll) {
        PaymentGatewayBottomSheet sheet = new PaymentGatewayBottomSheet();
        Bundle args = new Bundle();
        if (feeRecord != null) {
            args.putString(ARG_RECORD_ID, feeRecord.getId());
            args.putString(ARG_ROLL, !TextUtils.isEmpty(roll) ? roll : feeRecord.getRoll());
            args.putString(ARG_MONTH, feeRecord.getMonth());
            args.putDouble(ARG_DINING_FEE, feeRecord.getDiningFee());
            args.putDouble(ARG_HALL_FEE, feeRecord.getHallFee());
            args.putDouble(ARG_TOTAL_AMOUNT, feeRecord.getAmount());
        }
        sheet.setArguments(args);
        return sheet;
    }

    public void setPaymentCallback(PaymentCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        dispatchOtpNotification(generatedOtp);
                    } else {
                        Context ctx = getContext();
                        if (ctx != null) {
                            Toast.makeText(ctx, "Your MFS OTP is: " + generatedOtp, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetMockPaymentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String recordId = args != null ? args.getString(ARG_RECORD_ID, "") : "";
        String roll = args != null ? args.getString(ARG_ROLL, "") : "";
        String month = args != null ? args.getString(ARG_MONTH, "Current Month") : "Current Month";
        double diningFee = args != null ? args.getDouble(ARG_DINING_FEE, 0.0) : 0.0;
        double hallFee = args != null ? args.getDouble(ARG_HALL_FEE, 0.0) : 0.0;
        double totalAmount = args != null ? args.getDouble(ARG_TOTAL_AMOUNT, 0.0) : 0.0;

        if (diningFee == 0.0 && hallFee == 0.0 && totalAmount > 0.0) {
            diningFee = totalAmount * 0.67;
            hallFee = totalAmount - diningFee;
        }

        binding.tvPaymentMonth.setText("Target Month: " + month);
        binding.tvDiningFee.setText(String.format(Locale.US, "Dining Fee: %.0f BDT", diningFee));
        binding.tvHallFee.setText(String.format(Locale.US, "Hall Utility Fee: %.0f BDT", hallFee));
        binding.tvPayableTotal.setText(String.format(Locale.US, "Payable Amount: %.0f BDT", totalAmount));

        binding.btnCloseSheet.setOnClickListener(v -> dismiss());

        setupInputValidationWatchers();

        binding.btnSendOtp.setOnClickListener(v -> handleSendOtp());

        binding.tvResendOtp.setOnClickListener(v -> handleResendOtp());

        binding.btnVerifyAndPay.setOnClickListener(v -> handleVerifyAndPay(recordId, roll, totalAmount));
    }

    private void setupInputValidationWatchers() {
        binding.etAccountNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilAccountNumber.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etOtpCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilOtpCode.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void handleSendOtp() {
        String account = binding.etAccountNumber.getText() != null
                ? binding.etAccountNumber.getText().toString().trim() : "";

        if (account.length() != 11 || !account.startsWith("01")) {
            binding.tilAccountNumber.setError("Enter a valid 11-digit mobile number");
            return;
        }

        validatedAccount = account;

        int selectedRadioId = binding.rgPaymentMethod.getCheckedRadioButtonId();
        if (selectedRadioId == R.id.rbNagad) {
            selectedMethod = "Nagad";
        } else if (selectedRadioId == R.id.rbRocket) {
            selectedMethod = "Rocket";
        } else {
            selectedMethod = "bKash";
        }

        generatedOtp = 1000 + new Random().nextInt(9000);
        dispatchOtpNotification(generatedOtp);

        binding.layoutStepMobile.setVisibility(View.GONE);
        binding.layoutStepOtp.setVisibility(View.VISIBLE);
        binding.etOtpCode.requestFocus();
    }

    private void handleResendOtp() {
        generatedOtp = 1000 + new Random().nextInt(9000);
        dispatchOtpNotification(generatedOtp);

        binding.etOtpCode.setText("");
        binding.tilOtpCode.setError(null);

        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, "New OTP sent", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleVerifyAndPay(String recordId, String roll, double totalAmount) {
        String enteredOtp = binding.etOtpCode.getText() != null
                ? binding.etOtpCode.getText().toString().trim() : "";

        if (TextUtils.isEmpty(enteredOtp) || !enteredOtp.equals(String.valueOf(generatedOtp))) {
            binding.tilOtpCode.setError("Invalid OTP code");
            return;
        }

        binding.btnVerifyAndPay.setVisibility(View.GONE);
        binding.pbPaymentProgress.setVisibility(View.VISIBLE);
        binding.tvResendOtp.setEnabled(false);
        binding.btnCloseSheet.setEnabled(false);
        setCancelable(false);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            executeFirestorePayment(recordId, roll, totalAmount, selectedMethod, validatedAccount);
        }, 1500);
    }

    private void dispatchOtpNotification(int otp) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        createOtpNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Your MFS OTP is: " + otp, Toast.LENGTH_LONG).show();
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "mfs_otp_channel")
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Smart Hall - MFS Verification")
                .setContentText("Your OTP for bill payment is " + otp + ". Do not share this code.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your OTP for bill payment is " + otp + ". Do not share this code."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(1001, builder.build());
        } catch (SecurityException e) {
            Toast.makeText(context, "Your MFS OTP is: " + otp, Toast.LENGTH_LONG).show();
        }
    }

    private void createOtpNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        "mfs_otp_channel",
                        "MFS OTP Verification",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Delivers mock MFS OTP verification codes");
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void executeFirestorePayment(String recordId, String roll, double amount, String method, String account) {
        String trxId = generateTrxId();
        long now = System.currentTimeMillis();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        if (!TextUtils.isEmpty(recordId)) {
            DocumentReference feeRef = db.collection("fee_records").document(recordId);
            Map<String, Object> feeUpdates = new HashMap<>();
            feeUpdates.put("status", "PAID");
            feeUpdates.put("trxId", trxId);
            feeUpdates.put("paidAt", now);
            batch.set(feeRef, feeUpdates, SetOptions.merge());
        }

        DocumentReference paymentRef = db.collection("payments").document(trxId);
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("trxId", trxId);
        paymentData.put("roll", roll);
        paymentData.put("amount", amount);
        paymentData.put("method", method);
        paymentData.put("accountNumber", account);
        paymentData.put("feeRecordId", recordId);
        paymentData.put("paidAt", now);
        paymentData.put("status", "SUCCESS");
        batch.set(paymentRef, paymentData);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> studentFeeUpdate = new HashMap<>();
        studentFeeUpdate.put("feeStatus", "PAID");

        if (currentUser != null) {
            DocumentReference studentUidRef = db.collection("students").document(currentUser.getUid());
            batch.set(studentUidRef, studentFeeUpdate, SetOptions.merge());
        }

        if (!TextUtils.isEmpty(roll)) {
            DocumentReference studentRollRef = db.collection("students").document(roll);
            batch.set(studentRollRef, studentFeeUpdate, SetOptions.merge());
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            PaymentRecord paymentRecord = new PaymentRecord(
                    trxId,
                    roll,
                    amount,
                    method,
                    account,
                    recordId,
                    now,
                    "SUCCESS"
            );

            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "Payment successful! Transaction ID: " + trxId, Toast.LENGTH_SHORT).show();
            }

            dismissAllowingStateLoss();

            if (callback != null) {
                callback.onPaymentSuccess(paymentRecord);
            }
        }).addOnFailureListener(e -> {
            binding.pbPaymentProgress.setVisibility(View.GONE);
            binding.btnVerifyAndPay.setVisibility(View.VISIBLE);
            binding.tvResendOtp.setEnabled(true);
            binding.btnCloseSheet.setEnabled(true);
            setCancelable(true);

            Context context = getContext();
            if (context != null) {
                String msg = e != null ? e.getMessage() : "Payment processing error";
                Log.e("StudentDashboardError", "Operation failed: Payment execution failed: " + msg, e);
                Toast.makeText(context, "Payment failed: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String generateTrxId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("TRX");
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
