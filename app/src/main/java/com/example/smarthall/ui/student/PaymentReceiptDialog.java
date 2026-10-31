package com.example.smarthall.ui.student;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.smarthall.databinding.DialogPaymentReceiptBinding;
import com.example.smarthall.model.PaymentRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentReceiptDialog extends DialogFragment {

    private static final String ARG_PAYMENT = "arg_payment";
    private DialogPaymentReceiptBinding binding;

    public static PaymentReceiptDialog newInstance(PaymentRecord payment) {
        PaymentReceiptDialog dialog = new PaymentReceiptDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PAYMENT, payment);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogPaymentReceiptBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PaymentRecord payment = null;
        if (getArguments() != null) {
            payment = (PaymentRecord) getArguments().getSerializable(ARG_PAYMENT);
        }

        if (payment != null) {
            binding.tvReceiptTrxId.setText(payment.getTrxId() != null ? payment.getTrxId() : "N/A");
            binding.tvReceiptRoll.setText(payment.getRoll() != null ? payment.getRoll() : "N/A");
            binding.tvReceiptMethod.setText(payment.getMethod() != null ? payment.getMethod() : "bKash");
            binding.tvReceiptAccount.setText(payment.getAccountNumber() != null ? payment.getAccountNumber() : "N/A");
            binding.tvReceiptAmount.setText(String.format(Locale.US, "%.0f BDT", payment.getAmount()));

            long time = payment.getPaidAt() > 0 ? payment.getPaidAt() : System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.US);
            binding.tvReceiptDate.setText(sdf.format(new Date(time)));

            binding.tvReceiptStatus.setText("PAID");
        }

        binding.btnReceiptDone.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
