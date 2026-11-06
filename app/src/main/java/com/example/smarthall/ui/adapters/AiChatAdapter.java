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

public class AiChatAdapter extends RecyclerView.Adapter<AiChatAdapter.AiMessageViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private final List<ChatMessage> messageList;
    private int primaryColor;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public AiChatAdapter(List<ChatMessage> messageList, int primaryColor) {
        this.messageList = messageList;
        this.primaryColor = primaryColor;
    }

    public void setPrimaryThemeColor(int color) {
        this.primaryColor = color;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        String role = message.getSenderRole() != null ? message.getSenderRole() : message.getSenderType();
        if (role != null && (role.equalsIgnoreCase("student") || role.equalsIgnoreCase("user"))) {
            return VIEW_TYPE_USER;
        }
        return VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public AiMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == VIEW_TYPE_USER) ? R.layout.item_message_sent : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new AiMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AiMessageViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        String text = message.getMessage() != null ? message.getMessage() : message.getText();
        holder.tvMessageText.setText(text);

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

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessageText;
        final TextView tvMessageTime;
        final View layoutSentBubble;

        AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            layoutSentBubble = itemView.findViewById(R.id.layoutSentBubble);
        }
    }
}
