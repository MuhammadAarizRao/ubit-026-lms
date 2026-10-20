package com.ubit.lms;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private List<AttendanceItem> items;

    public AttendanceAdapter(List<AttendanceItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceItem item = items.get(position);
        int percentage = item.getPercentage();

        holder.ring.setProgress(percentage);
        holder.percentText.setText(percentage + "%");
        holder.nameText.setText(item.getCourseName());

        int color;
        if (percentage >= 90) {
            color = Color.parseColor("#43A047"); // green
        } else if (percentage >= 75) {
            color = Color.parseColor("#FB8C00"); // orange
        } else {
            color = Color.parseColor("#E53935"); // red
        }
        holder.ring.setProgressColor(color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircularProgressView ring;
        TextView percentText, nameText;

        ViewHolder(View itemView) {
            super(itemView);
            ring = itemView.findViewById(R.id.attendanceRing);
            percentText = itemView.findViewById(R.id.attendancePercentText);
            nameText = itemView.findViewById(R.id.courseNameText);
        }
    }
}