package com.ubit.lms;

import android.os.Bundle;
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

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherMarksFragment extends Fragment {

    private static final String ARG_COURSE_ID = "courseId";
    private static final String ARG_COURSE_NAME = "courseName";

    private LinearLayout marksListContainer;
    private Button saveMarksButton;
    private FirebaseFirestore db;
    private String courseId, courseName;

    private List<String> studentUids = new ArrayList<>();
    private List<EditText> scoreInputs = new ArrayList<>();

    public static TeacherMarksFragment newInstance(String courseId, String courseName) {
        TeacherMarksFragment fragment = new TeacherMarksFragment();
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
        View view = inflater.inflate(R.layout.fragment_teacher_marks, container, false);

        if (getArguments() != null) {
            courseId = getArguments().getString(ARG_COURSE_ID);
            courseName = getArguments().getString(ARG_COURSE_NAME);
        }

        db = FirebaseFirestore.getInstance();
        marksListContainer = view.findViewById(R.id.marksListContainer);
        saveMarksButton = view.findViewById(R.id.saveMarksButton);

        saveMarksButton.setOnClickListener(v -> saveAllMarks());

        loadStudents();

        return view;
    }

    private void loadStudents() {
        db.collection("courses").document(courseId).get()
                .addOnSuccessListener(courseDoc -> {
                    if (getContext() == null) return;

                    List<String> ids = (List<String>) courseDoc.get("studentIds");
                    if (ids == null || ids.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No students enrolled in this course.");
                        empty.setTextColor(0xFF607D8B);
                        marksListContainer.addView(empty);
                        return;
                    }

                    studentUids.clear();
                    scoreInputs.clear();
                    marksListContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    for (String uid : ids) {
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (getContext() == null) return;
                                    String name = userDoc.getString("name");

                                    View row = inflater.inflate(R.layout.item_student_marks_row, marksListContainer, false);
                                    TextView nameText = row.findViewById(R.id.marksStudentNameText);
                                    EditText scoreInput = row.findViewById(R.id.marksScoreInput);

                                    nameText.setText(name != null ? name : uid);

                                    studentUids.add(uid);
                                    scoreInputs.add(scoreInput);
                                    marksListContainer.addView(row);

                                    // Pre-fill existing score if already entered
                                    db.collection("marks").document(uid).get()
                                            .addOnSuccessListener(marksDoc -> {
                                                Map<String, Object> subjects = (Map<String, Object>) marksDoc.get("subjects");
                                                if (subjects != null && subjects.containsKey(courseId)) {
                                                    Object existing = subjects.get(courseId);
                                                    scoreInput.setText(String.valueOf(existing));
                                                }
                                            });
                                });
                    }
                });
    }

    private void saveAllMarks() {
        if (studentUids.isEmpty()) {
            Toast.makeText(getContext(), "No students to save marks for", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < studentUids.size(); i++) {
            String uid = studentUids.get(i);
            String scoreText = scoreInputs.get(i).getText().toString().trim();

            if (scoreText.isEmpty()) continue; // skip empty entries

            double score;
            try {
                score = Double.parseDouble(scoreText);
            } catch (NumberFormatException e) {
                continue;
            }

            saveMarkForStudent(uid, score);
        }

        Toast.makeText(getContext(), "Marks saved!", Toast.LENGTH_SHORT).show();
    }

    private void saveMarkForStudent(String uid, double score) {
        Map<String, Object> subjectMap = new HashMap<>();
        subjectMap.put(courseId, score);

        Map<String, Object> update = new HashMap<>();
        update.put("subjects", subjectMap);

        db.collection("marks").document(uid)
                .set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> recalculateGpa(uid))
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed for one student: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void recalculateGpa(String uid) {
        db.collection("marks").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> subjects = (Map<String, Object>) doc.get("subjects");
                    if (subjects == null || subjects.isEmpty()) return;

                    double total = 0;
                    int count = 0;
                    for (Object value : subjects.values()) {
                        if (value instanceof Number) {
                            total += ((Number) value).doubleValue();
                            count++;
                        }
                    }

                    if (count == 0) return;

                    double avgPercentage = total / count;
                    double gpa = Math.round((avgPercentage / 100.0 * 4.0) * 10.0) / 10.0;

                    Map<String, Object> gpaUpdate = new HashMap<>();
                    gpaUpdate.put("semesterGpa", gpa);

                    db.collection("users").document(uid).update(gpaUpdate)
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "GPA update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                });
    }
}
