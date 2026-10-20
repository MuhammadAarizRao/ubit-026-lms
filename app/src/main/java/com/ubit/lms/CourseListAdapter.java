package com.ubit.lms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CourseListAdapter extends RecyclerView.Adapter<CourseListAdapter.ViewHolder> {

    private List<CourseListItem> items;

    public CourseListAdapter(List<CourseListItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CourseListItem item = items.get(position);
        holder.nameText.setText(item.getName() + " (" + item.getCourseId() + ")");
        holder.teacherText.setText("Teacher: " + item.getTeacherName());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, teacherText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.courseRowName);
            teacherText = itemView.findViewById(R.id.courseRowTeacher);
        }
    }
}