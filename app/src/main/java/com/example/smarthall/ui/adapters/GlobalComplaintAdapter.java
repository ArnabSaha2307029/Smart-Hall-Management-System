package com.example.smarthall.ui.adapters;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.R;
import com.example.smarthall.model.Complaint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GlobalComplaintAdapter extends RecyclerView.Adapter<GlobalComplaintAdapter.ComplaintViewHolder> {

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint);
    }

    private final List<Complaint> complaintList;
    private final OnComplaintClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    public GlobalComplaintAdapter(List<Complaint> complaintList, OnComplaintClickListener listener) {
        this.complaintList = complaintList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_global_complaint, parent, false);
        return new ComplaintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        Complaint complaint = complaintList.get(position);
        holder.tvTitle.setText(complaint.getTitle() != null ? complaint.getTitle() : "");
        String roll = complaint.getStudentRoll();
        holder.tvRoll.setText("Submitter: " + (roll != null && !roll.isEmpty() ? roll : "Unknown"));

        if (complaint.getTimestamp() > 0) {
            holder.tvDate.setText(dateFormat.format(new Date(complaint.getTimestamp())));
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        boolean isResolved = Complaint.STATUS_RESOLVED.equalsIgnoreCase(complaint.getStatus())
                || "closed".equalsIgnoreCase(complaint.getStatus());
        holder.tvStatus.setVisibility(View.VISIBLE);
        if (isResolved) {
            holder.tvStatus.setText("RESOLVED");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_resolved);
            holder.tvStatus.setTextColor(Color.parseColor("#1B5E20"));
        } else {
            holder.tvStatus.setText("PENDING");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#0D47A1"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onComplaintClick(complaint);
            }
        });
    }

    @Override
    public int getItemCount() {
        return complaintList.size();
    }

    public void updateData(List<Complaint> newList) {
        this.complaintList.clear();
        if (newList != null) {
            this.complaintList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    static class ComplaintViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvRoll;
        final TextView tvStatus;
        final TextView tvDate;

        ComplaintViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvComplaintItemTitle);
            tvRoll = itemView.findViewById(R.id.tvComplaintItemRoll);
            tvStatus = itemView.findViewById(R.id.tvComplaintItemStatus);
            tvDate = itemView.findViewById(R.id.tvComplaintItemDate);
        }
    }
}
