package com.example.smarthall.ui.adapters;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.R;
import com.example.smarthall.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messageList;
    private final String currentUserRole;
    private int primaryColor;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatMessageAdapter(List<ChatMessage> messageList, String currentUserRole, int primaryColor) {
        this.messageList = messageList;
        this.currentUserRole = currentUserRole;
        this.primaryColor = primaryColor;
    }

    public ChatMessageAdapter(List<ChatMessage> messageList, String currentUserRole) {
        this(messageList, currentUserRole, 0xFF1A237E);
    }

    public void setPrimaryThemeColor(int color) {
        this.primaryColor = color;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message.getSenderRole() != null && message.getSenderRole().equalsIgnoreCase(currentUserRole)) {
            return VIEW_TYPE_SENT;
        }
        return VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == VIEW_TYPE_SENT) ? R.layout.item_message_sent : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.tvMessageText.setText(message.getMessage());
        if (message.getTimestamp() > 0) {
            holder.tvMessageTime.setText(timeFormat.format(new Date(message.getTimestamp())));
            holder.tvMessageTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvMessageTime.setVisibility(View.GONE);
        }

        if (holder.layoutSentBubble != null) {
            Drawable background = holder.layoutSentBubble.getBackground();
            if (background != null) {
                DrawableCompat.setTint(DrawableCompat.wrap(background.mutate()), primaryColor);
            }
            holder.tvMessageText.setTextColor(Color.WHITE);
            holder.tvMessageTime.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessageText;
        final TextView tvMessageTime;
        final View layoutSentBubble;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            layoutSentBubble = itemView.findViewById(R.id.layoutSentBubble);
        }
    }
}
