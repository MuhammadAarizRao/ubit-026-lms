package com.ubit.lms;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.ViewHolder> {

    private List<AssignmentItem> items;
    private Set<Integer> expandedPositions = new HashSet<>();

    public AssignmentAdapter(List<AssignmentItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assignment_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssignmentItem item = items.get(position);
        holder.titleText.setText(item.getTitle());
        holder.courseText.setText(item.getCourseName());
        holder.dueText.setText(item.getDueDateLabel());

        String description = item.getDescription();
        boolean hasDescription = description != null && !description.isEmpty();
        boolean isExpanded = expandedPositions.contains(position);

        if (hasDescription) {
            holder.descriptionText.setText(description);
            holder.descriptionText.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (expandedPositions.contains(position)) {
                    expandedPositions.remove(position);
                } else {
                    expandedPositions.add(position);
                }
                notifyItemChanged(position);
            });
        } else {
            holder.descriptionText.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
        }

        if (item.isSubmitted()) {
            holder.submitButton.setText("Submitted");
            holder.submitButton.setEnabled(false);
            holder.submitButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#43A047")));
        } else if (item.isOverdue()) {
            holder.submitButton.setText("Closed");
            holder.submitButton.setEnabled(false);
            holder.submitButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#9E9E9E")));
        } else {
            holder.submitButton.setText("Submit");
            holder.submitButton.setEnabled(true);
            holder.submitButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#1565C0")));

            holder.submitButton.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), SubmitAssignmentActivity.class);
                intent.putExtra("assignmentId", item.getId());
                intent.putExtra("assignmentTitle", item.getTitle());
                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, courseText, dueText, descriptionText;
        Button submitButton;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.assignmentTitleText);
            courseText = itemView.findViewById(R.id.assignmentCourseText);
            dueText = itemView.findViewById(R.id.assignmentDueText);
            descriptionText = itemView.findViewById(R.id.assignmentDescriptionText);
            submitButton = itemView.findViewById(R.id.submitButton);
        }
    }
}