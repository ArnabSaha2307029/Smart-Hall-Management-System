package com.example.smarthall.ui.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemNoticeFirebaseBinding;
import com.example.smarthall.model.NoticeModel;
import com.example.smarthall.ui.student.NoticeDetailActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirebaseNoticeAdapter extends RecyclerView.Adapter<FirebaseNoticeAdapter.FirebaseNoticeViewHolder> {

    private List<NoticeModel> noticeList;

    public FirebaseNoticeAdapter(List<NoticeModel> noticeList) {
        this.noticeList = noticeList != null ? noticeList : new ArrayList<>();
    }

    @NonNull
    @Override
    public FirebaseNoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoticeFirebaseBinding binding = ItemNoticeFirebaseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FirebaseNoticeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FirebaseNoticeViewHolder holder, int position) {
        NoticeModel notice = noticeList.get(position);
        holder.bind(notice);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), NoticeDetailActivity.class);
            intent.putExtra("notice_id", notice.getId());
            intent.putExtra("title", notice.getTitle());
            intent.putExtra("message", notice.getMessage());
            intent.putExtra("poster", notice.getPostedBy());
            intent.putExtra("timestamp", notice.getTimestamp());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return noticeList.size();
    }

    public void setNotices(List<NoticeModel> newList) {
        this.noticeList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateData(List<NoticeModel> newList) {
        this.noticeList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class FirebaseNoticeViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoticeFirebaseBinding binding;

        FirebaseNoticeViewHolder(ItemNoticeFirebaseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NoticeModel notice) {
            binding.tvNoticeTitle.setText(notice.getTitle() != null ? notice.getTitle() : "");
            binding.tvNoticeMessage.setText(notice.getMessage() != null ? notice.getMessage() : "");
            if (notice.getTimestamp() > 0) {
                String formatted = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(new Date(notice.getTimestamp()));
                binding.tvNoticeTimestamp.setText(formatted);
            } else {
                binding.tvNoticeTimestamp.setText("");
            }
            String poster = notice.getPostedBy() != null && !notice.getPostedBy().isEmpty()
                    ? "Posted by: " + notice.getPostedBy()
                    : "Posted by: Admin";
            binding.tvPostedBy.setText(poster);
        }
    }
}
