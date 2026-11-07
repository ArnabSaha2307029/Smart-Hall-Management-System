package com.example.smarthall.ui.adapters;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ItemComplaintBinding;
import com.example.smarthall.model.Complaint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder> {

    public interface OnComplaintActionListener {
        void onToggleStatus(Complaint complaint);
    }

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint);
    }

    private List<Complaint> complaintList;
    private OnComplaintActionListener actionListener;
    private OnComplaintClickListener clickListener;
    private boolean showAdminControls;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    public ComplaintAdapter(List<Complaint> complaintList, OnComplaintActionListener listener, boolean showAdminControls) {
        this.complaintList = complaintList != null ? complaintList : new ArrayList<>();
        this.actionListener = listener;
        this.showAdminControls = showAdminControls;
    }

    public ComplaintAdapter(List<Complaint> complaintList, OnComplaintClickListener clickListener) {
        this.complaintList = complaintList != null ? complaintList : new ArrayList<>();
        this.clickListener = clickListener;
        this.showAdminControls = false;
    }

    public void setOnComplaintClickListener(OnComplaintClickListener clickListener) {
        this.clickListener = clickListener;
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
        holder.bind(complaint, actionListener, clickListener, showAdminControls, dateFormat);
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

        void bind(Complaint complaint, OnComplaintActionListener actionListener,
                  OnComplaintClickListener clickListener, boolean showAdminControls,
                  SimpleDateFormat dateFormat) {
            binding.tvComplaintTitle.setText(complaint.getTitle() != null ? complaint.getTitle() : "");
            binding.tvComplaintDesc.setText(complaint.getDescription() != null ? complaint.getDescription() : "");

            StringBuilder info = new StringBuilder();
            if (!TextUtils.isEmpty(complaint.getStudentRoll())) {
                info.append("Roll: ").append(complaint.getStudentRoll());
            } else if (!TextUtils.isEmpty(complaint.getStudentName())) {
                info.append(complaint.getStudentName());
            }
            if (!TextUtils.isEmpty(complaint.getRoomNo())) {
                if (info.length() > 0) {
                    info.append(" | Room ").append(complaint.getRoomNo());
                } else {
                    info.append("Room ").append(complaint.getRoomNo());
                }
            }
            binding.tvStudentInfo.setText(info.toString());

            if (!TextUtils.isEmpty(complaint.getDate())) {
                binding.tvComplaintDate.setText(complaint.getDate());
                binding.tvComplaintDate.setVisibility(android.view.View.VISIBLE);
            } else if (complaint.getTimestamp() > 0) {
                binding.tvComplaintDate.setText(dateFormat.format(new Date(complaint.getTimestamp())));
                binding.tvComplaintDate.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvComplaintDate.setVisibility(android.view.View.GONE);
            }

            boolean isResolved = Complaint.STATUS_RESOLVED.equalsIgnoreCase(complaint.getStatus())
                    || "closed".equalsIgnoreCase(complaint.getStatus());
            binding.tvComplaintStatus.setVisibility(android.view.View.VISIBLE);
            if (isResolved) {
                binding.tvComplaintStatus.setText("RESOLVED");
                binding.tvComplaintStatus.setBackgroundResource(R.drawable.bg_badge_resolved);
                binding.tvComplaintStatus.setTextColor(Color.parseColor("#1B5E20"));
                if (showAdminControls) binding.btnToggleStatus.setText("Reopen");
            } else {
                binding.tvComplaintStatus.setText("PENDING");
                binding.tvComplaintStatus.setBackgroundResource(R.drawable.bg_badge_pending);
                binding.tvComplaintStatus.setTextColor(Color.parseColor("#0D47A1"));
                if (showAdminControls) binding.btnToggleStatus.setText("Resolve");
            }

            if (showAdminControls) {
                binding.btnToggleStatus.setVisibility(android.view.View.VISIBLE);
                binding.btnToggleStatus.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onToggleStatus(complaint);
                });
            } else {
                binding.btnToggleStatus.setVisibility(android.view.View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onComplaintClick(complaint);
                }
            });
        }
    }
}
