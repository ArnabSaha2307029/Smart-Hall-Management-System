package com.example.smarthall.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemPaymentHistoryBinding;
import com.example.smarthall.model.PaymentRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.HistoryViewHolder> {

    public interface OnDownloadReceiptListener {
        void onDownloadReceipt(PaymentRecord payment);
    }

    private List<PaymentRecord> paymentList;
    private final OnDownloadReceiptListener listener;

    public PaymentHistoryAdapter(List<PaymentRecord> paymentList, OnDownloadReceiptListener listener) {
        this.paymentList = paymentList != null ? paymentList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<PaymentRecord> newList) {
        this.paymentList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPaymentHistoryBinding binding = ItemPaymentHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        PaymentRecord record = paymentList.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return paymentList != null ? paymentList.size() : 0;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemPaymentHistoryBinding binding;

        HistoryViewHolder(ItemPaymentHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PaymentRecord payment, OnDownloadReceiptListener listener) {
            String trxId = payment.getTrxId() != null ? payment.getTrxId() : "N/A";
            String displayTrx = trxId.startsWith("Trx ID:") ? trxId : "Trx ID: " + trxId;
            binding.tvHistoryTrxId.setText(displayTrx);

            long time = payment.getPaidAt() > 0 ? payment.getPaidAt() : System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            String dateFormatted = sdf.format(new Date(time));
            String method = payment.getMethod() != null ? payment.getMethod() : "MFS";
            binding.tvHistoryMeta.setText(String.format("Paid on %s via %s", dateFormatted, method));

            binding.tvHistoryAmount.setText(String.format(Locale.US, "%.0f BDT", payment.getAmount()));

            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (binding.getRoot().getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
                binding.tvHistoryTrxId.setTextColor(typedValue.data);
                binding.tvHistoryAmount.setTextColor(typedValue.data);
                binding.btnDownloadReceipt.setTextColor(typedValue.data);
                binding.btnDownloadReceipt.setStrokeColor(android.content.res.ColorStateList.valueOf(typedValue.data));
            }

            binding.btnDownloadReceipt.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDownloadReceipt(payment);
                }
            });
        }
    }
}
