package com.ubit.lms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> items;

    public NotificationAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.courseText.setText(item.getCourseTag());
        holder.typeText.setText(item.getType());
        holder.messageText.setText(item.getMessage());
        holder.timeText.setText(item.getTimeAgo());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseText, typeText, messageText, timeText;

        ViewHolder(View itemView) {
            super(itemView);
            courseText = itemView.findViewById(R.id.notifCourseText);
            typeText = itemView.findViewById(R.id.notifTypeText);
            messageText = itemView.findViewById(R.id.notifMessageText);
            timeText = itemView.findViewById(R.id.notifTimeText);
        }
    }
}