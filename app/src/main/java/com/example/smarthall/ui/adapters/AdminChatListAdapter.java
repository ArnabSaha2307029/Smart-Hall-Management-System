package com.example.smarthall.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.R;
import com.example.smarthall.model.AdminChatItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminChatListAdapter extends RecyclerView.Adapter<AdminChatListAdapter.ChatViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(AdminChatItem item);
    }

    private final List<AdminChatItem> chatList;
    private final OnChatClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

    public AdminChatListAdapter(List<AdminChatItem> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        AdminChatItem item = chatList.get(position);
        holder.tvStudentRoll.setText(item.getStudentRoll());
        holder.tvLastMessage.setText(item.getLastMessage());
        if (item.getTimestamp() > 0) {
            holder.tvLastMessageTime.setText(timeFormat.format(new Date(item.getTimestamp())));
            holder.tvLastMessageTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvLastMessageTime.setVisibility(View.GONE);
        }

        if (item.getUnreadCount() > 0) {
            holder.tvAdminChatUnread.setVisibility(View.VISIBLE);
            if (item.getUnreadCount() > 9) {
                holder.tvAdminChatUnread.setText("9+");
            } else {
                holder.tvAdminChatUnread.setText(String.valueOf(item.getUnreadCount()));
            }
        } else {
            holder.tvAdminChatUnread.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        final TextView tvStudentRoll;
        final TextView tvLastMessage;
        final TextView tvLastMessageTime;
        final TextView tvAdminChatUnread;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentRoll = itemView.findViewById(R.id.tvStudentRoll);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
            tvAdminChatUnread = itemView.findViewById(R.id.tvAdminChatUnread);
        }
    }
}
