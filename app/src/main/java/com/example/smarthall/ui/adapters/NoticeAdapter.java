package com.example.smarthall.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemNoticeBinding;
import com.example.smarthall.model.Notice;

import java.util.ArrayList;
import java.util.List;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

    private List<Notice> noticeList;

    public NoticeAdapter(List<Notice> noticeList) {
        this.noticeList = noticeList != null ? noticeList : new ArrayList<>();
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
        holder.bind(noticeList.get(position));
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

        void bind(Notice notice) {
            binding.tvNoticeTitle.setText(notice.getTitle());
            binding.tvNoticeDate.setText(notice.getDatePosted());
            binding.tvNoticeMessage.setText(notice.getMessage());
        }
    }
}
