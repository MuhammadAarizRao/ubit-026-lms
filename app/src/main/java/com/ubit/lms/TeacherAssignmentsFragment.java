package com.ubit.lms;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherAssignmentsFragment extends Fragment {

    private static final String ARG_COURSE_ID = "courseId";
    private static final String ARG_COURSE_NAME = "courseName";

    private EditText titleInput, descriptionInput;
    private Button postButton, pickDueDateButton;
    private TextView selectedDueDateText;
    private LinearLayout assignmentsListContainer;

    private FirebaseFirestore db;
    private String courseId, courseName;
    private Date selectedDueDate;

    public static TeacherAssignmentsFragment newInstance(String courseId, String courseName) {
        TeacherAssignmentsFragment fragment = new TeacherAssignmentsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_COURSE_ID, courseId);
        args.putString(ARG_COURSE_NAME, courseName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_assignments, container, false);

        if (getArguments() != null) {
            courseId = getArguments().getString(ARG_COURSE_ID);
            courseName = getArguments().getString(ARG_COURSE_NAME);
        }

        db = FirebaseFirestore.getInstance();

        titleInput = view.findViewById(R.id.assignmentTitleInput);
        descriptionInput = view.findViewById(R.id.assignmentDescriptionInput);
        pickDueDateButton = view.findViewById(R.id.pickDueDateButton);
        selectedDueDateText = view.findViewById(R.id.selectedDueDateText);
        postButton = view.findViewById(R.id.postAssignmentButton);
        assignmentsListContainer = view.findViewById(R.id.assignmentsListContainer);

        pickDueDateButton.setOnClickListener(v -> showDatePicker());
        postButton.setOnClickListener(v -> postAssignment());

        loadAssignments();

        return view;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth, 23, 59, 59);
                    selectedDueDate = picked.getTime();

                    String formatted = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            .format(selectedDueDate);
                    selectedDueDateText.setText("Due: " + formatted);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void postAssignment() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        if (TextUtils.isEmpty(title) || selectedDueDate == null) {
            Toast.makeText(getContext(), "Please enter a title and pick a due date", Toast.LENGTH_SHORT).show();
            return;
        }

        String dueDateLabel = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(selectedDueDate);

        Map<String, Object> assignment = new HashMap<>();
        assignment.put("courseId", courseId);
        assignment.put("courseName", courseName);
        assignment.put("title", title);
        assignment.put("description", description);
        assignment.put("dueTimestamp", new Timestamp(selectedDueDate));
        assignment.put("dueDateLabel", dueDateLabel);
        assignment.put("submittedBy", new ArrayList<String>());
        assignment.put("createdAt", FieldValue.serverTimestamp());

        db.collection("assignments").add(assignment)
                .addOnSuccessListener(docRef -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "Assignment posted!", Toast.LENGTH_SHORT).show();
                    titleInput.setText("");
                    descriptionInput.setText("");
                    selectedDueDate = null;
                    selectedDueDateText.setText("No due date selected");
                    loadAssignments();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadAssignments() {
        db.collection("assignments")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        docs.add(doc);
                    }

                    docs.sort((a, b) -> {
                        long aTime = a.getTimestamp("createdAt") != null ? a.getTimestamp("createdAt").toDate().getTime() : 0;
                        long bTime = b.getTimestamp("createdAt") != null ? b.getTimestamp("createdAt").toDate().getTime() : 0;
                        return Long.compare(bTime, aTime);
                    });

                    assignmentsListContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    if (docs.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No assignments posted yet.");
                        empty.setTextColor(0xFF607D8B);
                        assignmentsListContainer.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : docs) {
                        String assignmentId = doc.getId();
                        String title = doc.getString("title");
                        String description = doc.getString("description");
                        String dueDateLabel = doc.getString("dueDateLabel");
                        List<String> submittedBy = (List<String>) doc.get("submittedBy");
                        int submissionCount = submittedBy != null ? submittedBy.size() : 0;

                        View row = inflater.inflate(R.layout.item_teacher_assignment_row, assignmentsListContainer, false);
                        TextView titleView = row.findViewById(R.id.teacherAssignmentTitleText);
                        TextView descView = row.findViewById(R.id.teacherAssignmentDescText);
                        TextView dueView = row.findViewById(R.id.teacherAssignmentDueText);
                        TextView submissionsView = row.findViewById(R.id.teacherAssignmentSubmissionsText);
                        Button viewSubmissionsButton = row.findViewById(R.id.viewSubmissionsButton);

                        titleView.setText(title);

                        if (description != null && !description.isEmpty()) {
                            descView.setText(description);
                            descView.setVisibility(View.VISIBLE);
                        }

                        dueView.setText(dueDateLabel != null ? "Due: " + dueDateLabel : "No due date");
                        submissionsView.setText(submissionCount + " submissions");

                        viewSubmissionsButton.setOnClickListener(v -> {
                            Intent intent = new Intent(getContext(), ViewSubmissionsActivity.class);
                            intent.putExtra("assignmentId", assignmentId);
                            intent.putExtra("assignmentTitle", title);
                            startActivity(intent);
                        });

                        assignmentsListContainer.addView(row);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}