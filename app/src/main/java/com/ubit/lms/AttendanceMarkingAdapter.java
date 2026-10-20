package com.ubit.lms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttendanceMarkingAdapter extends RecyclerView.Adapter<AttendanceMarkingAdapter.ViewHolder> {

    private List<StudentAttendanceRow> rows;

    public AttendanceMarkingAdapter(List<StudentAttendanceRow> rows) {
        this.rows = rows;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_student_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentAttendanceRow row = rows.get(position);
        holder.nameText.setText(row.getName());
        holder.toggle.setChecked(row.isPresent());

        holder.toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            row.setPresent(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        Switch toggle;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.studentNameText);
            toggle = itemView.findViewById(R.id.presentToggle);
        }
    }
}