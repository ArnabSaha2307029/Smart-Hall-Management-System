package com.example.smarthall.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemFeeRecordBinding;
import com.example.smarthall.model.FeeRecord;

import java.util.ArrayList;
import java.util.List;

public class FeeRecordAdapter extends RecyclerView.Adapter<FeeRecordAdapter.FeeViewHolder> {

    private List<FeeRecord> feeList;
    private OnFeeToggleListener listener;
    private boolean showToggle;

    public interface OnFeeToggleListener {
        void onToggleFeeStatus(FeeRecord feeRecord);
    }

    public FeeRecordAdapter(List<FeeRecord> feeList, OnFeeToggleListener listener, boolean showToggle) {
        this.feeList = feeList != null ? feeList : new ArrayList<>();
        this.listener = listener;
        this.showToggle = showToggle;
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
        holder.bind(feeRecord, listener, showToggle);
    }

    @Override
    public int getItemCount() {
        return feeList.size();
    }

    public void updateData(List<FeeRecord> newList) {
        this.feeList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class FeeViewHolder extends RecyclerView.ViewHolder {
        private final ItemFeeRecordBinding binding;

        FeeViewHolder(ItemFeeRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FeeRecord fee, OnFeeToggleListener listener, boolean showToggle) {
            binding.tvFeeMonth.setText(fee.getMonth());
            binding.tvFeeAmount.setText(String.format("৳ %.0f BDT", fee.getAmount()));
            binding.tvFeeDueDate.setText("Due: " + fee.getDueDate());

            if (FeeRecord.STATUS_PAID.equals(fee.getStatus())) {
                binding.tvFeeStatus.setText("PAID");
                binding.tvFeeStatus.setBackgroundColor(Color.parseColor("#FFCDD2"));
                binding.tvFeeStatus.setTextColor(Color.parseColor("#B71C1C"));
            } else {
                binding.tvFeeStatus.setText("UNPAID");
                binding.tvFeeStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                binding.tvFeeStatus.setTextColor(Color.parseColor("#D32F2F"));
            }

            if (showToggle) {
                binding.btnToggleFee.setVisibility(android.view.View.VISIBLE);
                binding.btnToggleFee.setText(FeeRecord.STATUS_PAID.equals(fee.getStatus()) ? "Mark Unpaid" : "Mark Paid");
                binding.btnToggleFee.setOnClickListener(v -> {
                    if (listener != null) listener.onToggleFeeStatus(fee);
                });
            } else {
                binding.btnToggleFee.setVisibility(android.view.View.GONE);
            }
        }
    }
}
