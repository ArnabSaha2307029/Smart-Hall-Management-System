package com.example.smarthall.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ItemFeeRecordBinding;
import com.example.smarthall.model.FeeRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeeRecordAdapter extends RecyclerView.Adapter<FeeRecordAdapter.FeeViewHolder> {

    private List<FeeRecord> feeList;
    private OnFeeToggleListener listener;
    private boolean showToggle;
    private OnPayClickListener payClickListener;

    public interface OnFeeToggleListener {
        void onToggleFeeStatus(FeeRecord feeRecord);
    }

    public interface OnPayClickListener {
        void onPayClick(FeeRecord feeRecord);
    }

    public FeeRecordAdapter(List<FeeRecord> feeList, OnFeeToggleListener listener, boolean showToggle) {
        this(feeList, listener, showToggle, null);
    }

    public FeeRecordAdapter(List<FeeRecord> feeList, OnFeeToggleListener listener, boolean showToggle, OnPayClickListener payClickListener) {
        this.feeList = feeList != null ? feeList : new ArrayList<>();
        this.listener = listener;
        this.showToggle = showToggle;
        this.payClickListener = payClickListener;
    }

    public void setOnPayClickListener(OnPayClickListener payClickListener) {
        this.payClickListener = payClickListener;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFeeRecordBinding binding = ItemFeeRecordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FeeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FeeViewHolder holder, int position) {
        FeeRecord feeRecord = feeList.get(position);
        holder.bind(feeRecord, listener, showToggle, payClickListener);
    }

    @Override
    public int getItemCount() {
        return feeList.size();
    }

    public void updateData(List<FeeRecord> newList) {
        this.feeList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void markFeeAsPaid(String feeRecordId, String trxId) {
        if (feeRecordId == null) {
            return;
        }
        for (int i = 0; i < feeList.size(); i++) {
            FeeRecord fr = feeList.get(i);
            if (feeRecordId.equals(fr.getId())) {
                fr.setStatus(FeeRecord.STATUS_PAID);
                fr.setTrxId(trxId);
                fr.setPaidAt(System.currentTimeMillis());
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class FeeViewHolder extends RecyclerView.ViewHolder {
        private final ItemFeeRecordBinding binding;

        FeeViewHolder(ItemFeeRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FeeRecord fee, OnFeeToggleListener toggleListener, boolean showToggle, OnPayClickListener payClickListener) {
            binding.tvFeeMonth.setText(fee.getMonth());
            binding.tvFeeAmount.setText(String.format(Locale.US, "৳ %.0f BDT", fee.getAmount()));
            binding.tvFeeDueDate.setText("Due: " + (fee.getDueDate() != null ? fee.getDueDate() : "N/A"));

            if (fee.isPaid()) {
                binding.tvFeeStatus.setText("PAID");
                binding.tvFeeStatus.setBackgroundResource(R.drawable.bg_badge_paid);
                binding.tvFeeStatus.setTextColor(Color.parseColor("#B71C1C"));
                binding.btnPayNow.setVisibility(View.GONE);
            } else {
                binding.tvFeeStatus.setText("UNPAID");
                binding.tvFeeStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
                binding.tvFeeStatus.setTextColor(Color.parseColor("#D32F2F"));
                if (!showToggle && payClickListener != null) {
                    binding.btnPayNow.setVisibility(View.VISIBLE);
                    binding.btnPayNow.setOnClickListener(v -> payClickListener.onPayClick(fee));
                } else {
                    binding.btnPayNow.setVisibility(View.GONE);
                }
            }

            if (showToggle) {
                binding.btnToggleFee.setVisibility(View.VISIBLE);
                binding.btnToggleFee.setText(fee.isPaid() ? "Mark Unpaid" : "Mark Paid");
                binding.btnToggleFee.setOnClickListener(v -> {
                    if (toggleListener != null) {
                        toggleListener.onToggleFeeStatus(fee);
                    }
                });
            } else {
                binding.btnToggleFee.setVisibility(View.GONE);
            }
        }
    }
}
