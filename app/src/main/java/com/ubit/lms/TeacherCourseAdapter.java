package com.ubit.lms;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TeacherCourseAdapter extends RecyclerView.Adapter<TeacherCourseAdapter.ViewHolder> {

    private List<TeacherCourseItem> items;

    public TeacherCourseAdapter(List<TeacherCourseItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher_course_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TeacherCourseItem item = items.get(position);
        holder.nameText.setText(item.getName());

        int count = item.getStudentIds().size();
        holder.studentCountText.setText(count + (count == 1 ? " student enrolled" : " students enrolled"));

        String section = item.getTargetSection();
        if (section != null && !section.equals("N/A")) {
            holder.tagText.setText("SECTION " + section.toUpperCase());
            holder.tagText.setVisibility(View.VISIBLE);
        } else {
            holder.tagText.setVisibility(View.GONE);
        }

        holder.openCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CourseDetailActivity.class);
            intent.putExtra("courseId", item.getCourseId());
            intent.putExtra("courseName", item.getName());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, studentCountText, tagText;
        View openCourseButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.teacherCourseNameText);
            studentCountText = itemView.findViewById(R.id.teacherCourseStudentCountText);
            tagText = itemView.findViewById(R.id.teacherCourseTagText);
            openCourseButton = itemView.findViewById(R.id.openCourseButton);
        }
    }
}