package com.example.smarthall.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemComplaintBinding;
import com.example.smarthall.model.Complaint;

import java.util.ArrayList;
import java.util.List;

public class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder> {

    private List<Complaint> complaintList;
    private OnComplaintActionListener listener;
    private boolean showAdminControls;

    public interface OnComplaintActionListener {
        void onToggleStatus(Complaint complaint);
    }

    public ComplaintAdapter(List<Complaint> complaintList, OnComplaintActionListener listener, boolean showAdminControls) {
        this.complaintList = complaintList != null ? complaintList : new ArrayList<>();
        this.listener = listener;
        this.showAdminControls = showAdminControls;
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemComplaintBinding binding = ItemComplaintBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ComplaintViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        Complaint complaint = complaintList.get(position);
        holder.bind(complaint, listener, showAdminControls);
    }

    @Override
    public int getItemCount() {
        return complaintList.size();
    }

    public void updateData(List<Complaint> newList) {
        this.complaintList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ComplaintViewHolder extends RecyclerView.ViewHolder {
        private final ItemComplaintBinding binding;

        ComplaintViewHolder(ItemComplaintBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Complaint complaint, OnComplaintActionListener listener, boolean showAdminControls) {
            binding.tvComplaintTitle.setText(complaint.getTitle());
            binding.tvComplaintDesc.setText(complaint.getDescription());
            binding.tvStudentInfo.setText(complaint.getStudentName() + " | Room " + complaint.getRoomNo());
            binding.tvComplaintDate.setText(complaint.getDate());

            boolean isResolved = Complaint.STATUS_RESOLVED.equalsIgnoreCase(complaint.getStatus())
                    || "closed".equalsIgnoreCase(complaint.getStatus());
            binding.tvComplaintStatus.setVisibility(android.view.View.VISIBLE);
            if (isResolved) {
                binding.tvComplaintStatus.setText("RESOLVED");
                binding.tvComplaintStatus.setBackgroundResource(com.example.smarthall.R.drawable.bg_badge_resolved);
                binding.tvComplaintStatus.setTextColor(Color.parseColor("#1B5E20"));
                if (showAdminControls) binding.btnToggleStatus.setText("Reopen");
            } else {
                binding.tvComplaintStatus.setText("PENDING");
                binding.tvComplaintStatus.setBackgroundResource(com.example.smarthall.R.drawable.bg_badge_pending);
                binding.tvComplaintStatus.setTextColor(Color.parseColor("#0D47A1"));
                if (showAdminControls) binding.btnToggleStatus.setText("Resolve");
            }

            if (showAdminControls) {
                binding.btnToggleStatus.setVisibility(android.view.View.VISIBLE);
                binding.btnToggleStatus.setOnClickListener(v -> {
                    if (listener != null) listener.onToggleStatus(complaint);
                });
            } else {
                binding.btnToggleStatus.setVisibility(android.view.View.GONE);
            }
        }
    }
}
