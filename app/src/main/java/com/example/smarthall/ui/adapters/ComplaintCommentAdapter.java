package com.example.smarthall.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.R;
import com.example.smarthall.model.ComplaintComment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComplaintCommentAdapter extends RecyclerView.Adapter<ComplaintCommentAdapter.CommentViewHolder> {

    private final List<ComplaintComment> commentList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    public ComplaintCommentAdapter(List<ComplaintComment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_complaint_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        ComplaintComment comment = commentList.get(position);

        String sender;
        if ("admin".equalsIgnoreCase(comment.getSenderRole())) {
            sender = "Admin";
        } else if (comment.getSenderRoll() != null && !comment.getSenderRoll().isEmpty()) {
            if (comment.getSenderRoll().toLowerCase().startsWith("roll")) {
                sender = comment.getSenderRoll();
            } else {
                sender = "Roll: " + comment.getSenderRoll();
            }
        } else {
            sender = "Student";
        }

        holder.tvSender.setText(sender);
        holder.tvText.setText(comment.getText() != null ? comment.getText() : "");

        if (comment.getTimestamp() > 0) {
            holder.tvTime.setText(dateFormat.format(new Date(comment.getTimestamp())));
            holder.tvTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTime.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        final TextView tvSender;
        final TextView tvTime;
        final TextView tvText;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvCommentSender);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvText = itemView.findViewById(R.id.tvCommentText);
        }
    }
}