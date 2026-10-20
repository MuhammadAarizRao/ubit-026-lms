package com.ubit.lms;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminCoursesFragment extends Fragment {

    private LinearLayout coursesListContainer;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_courses, container, false);

        db = FirebaseFirestore.getInstance();
        coursesListContainer = view.findViewById(R.id.adminCoursesListContainer);

        loadCourses();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCourses();
    }

    private void loadCourses() {
        db.collection("courses").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    coursesListContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String courseId = doc.getId();
                        String name = doc.getString("name");
                        String teacherId = doc.getString("teacherId");

                        View row = inflater.inflate(R.layout.item_course_row, coursesListContainer, false);
                        TextView nameText = row.findViewById(R.id.courseRowName);
                        TextView teacherText = row.findViewById(R.id.courseRowTeacher);
                        TextView assignCrGrButton = row.findViewById(R.id.assignCrGrButton);

                        nameText.setText(name + " (" + courseId + ")");
                        teacherText.setText("Teacher: Loading...");
                        coursesListContainer.addView(row);

                        if (teacherId != null) {
                            db.collection("users").document(teacherId).get()
                                    .addOnSuccessListener(teacherDoc -> {
                                        if (getContext() == null) return;
                                        if (teacherDoc.exists()) {
                                            String tName = teacherDoc.getString("name");
                                            teacherText.setText("Teacher: " + (tName != null ? tName : "Unknown"));
                                        } else {
                                            teacherText.setText("Teacher: Unknown");
                                        }
                                    });
                        } else {
                            teacherText.setText("Teacher: Not assigned");
                        }

                        assignCrGrButton.setOnClickListener(v -> {
                            Intent intent = new Intent(getContext(), AssignCrGrActivity.class);
                            intent.putExtra("courseId", courseId);
                            intent.putExtra("courseName", name);
                            startActivity(intent);
                        });
                    }
                });
    }
}