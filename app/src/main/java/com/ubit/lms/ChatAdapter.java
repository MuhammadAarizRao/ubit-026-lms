package com.ubit.lms;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatMessageItem> messages;
    private String currentUserUid;

    public ChatAdapter(List<ChatMessageItem> messages, String currentUserUid) {
        this.messages = messages;
        this.currentUserUid = currentUserUid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessageItem message = messages.get(position);
        holder.bubbleText.setText(message.getText());

        if (message.getTimestamp() != null) {
            holder.timeText.setText(new SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(message.getTimestamp()));
        }

        boolean isMine = message.getSenderId().equals(currentUserUid);
        LinearLayout container = (LinearLayout) holder.itemView;

        if (isMine) {
            container.setGravity(Gravity.END);
            holder.bubbleText.setBackgroundResource(R.drawable.chat_bubble_sent);
            holder.bubbleText.setTextColor(0xFFFFFFFF);
        } else {
            container.setGravity(Gravity.START);
            holder.bubbleText.setBackgroundResource(R.drawable.chat_bubble_received);
            holder.bubbleText.setTextColor(0xFF3E2E2E);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView bubbleText, timeText;

        ViewHolder(View itemView) {
            super(itemView);
            bubbleText = itemView.findViewById(R.id.chatBubbleText);
            timeText = itemView.findViewById(R.id.chatTimeText);
        }
    }
}