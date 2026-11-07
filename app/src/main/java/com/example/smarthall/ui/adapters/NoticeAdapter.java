package com.example.smarthall.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemNoticeBinding;
import com.example.smarthall.model.Notice;

import java.util.ArrayList;
import java.util.List;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

    public interface OnNoticeDeleteListener {
        void onDeleteClick(Notice notice);
    }

    private List<Notice> noticeList;
    private final boolean isAdmin;
    private final OnNoticeDeleteListener deleteListener;

    public NoticeAdapter(List<Notice> noticeList) {
        this(noticeList, false, null);
    }

    public NoticeAdapter(List<Notice> noticeList, boolean isAdmin, OnNoticeDeleteListener deleteListener) {
        this.noticeList = noticeList != null ? noticeList : new ArrayList<>();
        this.isAdmin = isAdmin;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoticeBinding binding = ItemNoticeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NoticeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        Notice notice = noticeList.get(position);
        holder.bind(notice, isAdmin, deleteListener);
    }

    @Override
    public int getItemCount() {
        return noticeList.size();
    }

    public void updateData(List<Notice> newList) {
        this.noticeList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class NoticeViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoticeBinding binding;

        NoticeViewHolder(ItemNoticeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Notice notice, boolean isAdmin, OnNoticeDeleteListener deleteListener) {
            binding.tvNoticeTitle.setText(notice.getTitle());
            binding.tvNoticeDate.setText(notice.getDatePosted());
            binding.tvNoticeMessage.setText(notice.getMessage());

            if (isAdmin) {
                binding.btnDeleteNotice.setVisibility(View.VISIBLE);
                binding.btnDeleteNotice.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDeleteClick(notice);
                    }
                });
            } else {
                binding.btnDeleteNotice.setVisibility(View.GONE);
                binding.btnDeleteNotice.setOnClickListener(null);
            }
        }
    }
}
