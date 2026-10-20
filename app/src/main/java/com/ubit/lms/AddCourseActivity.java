package com.ubit.lms;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddCourseActivity extends AppCompatActivity {

    private EditText courseIdInput, courseNameInput;
    private EditText targetBatchInput, targetSemesterInput, targetSectionInput;
    private Spinner teacherSpinner;
    private View createCourseButton;
    private ProgressBar addCourseProgress;

    private FirebaseFirestore db;
    private List<String> teacherUids = new ArrayList<>();
    private List<String> teacherNames = new ArrayList<>();

    private int sectionsRemaining = 0;
    private int totalMatched = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        db = FirebaseFirestore.getInstance();

        courseIdInput = findViewById(R.id.courseIdInput);
        courseNameInput = findViewById(R.id.courseNameInput);
        targetBatchInput = findViewById(R.id.targetBatchInput);
        targetSemesterInput = findViewById(R.id.targetSemesterInput);
        targetSectionInput = findViewById(R.id.targetSectionInput);
        teacherSpinner = findViewById(R.id.teacherSpinner);
        createCourseButton = findViewById(R.id.createCourseButton);
        addCourseProgress = findViewById(R.id.addCourseProgress);

        loadTeachers();

        createCourseButton.setOnClickListener(v -> attemptCreateCourse());
    }

    private void loadTeachers() {
        db.collection("users").whereEqualTo("role", "teacher").get()
                .addOnSuccessListener(querySnapshot -> {
                    teacherUids.clear();
                    teacherNames.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        teacherUids.add(doc.getId());
                        String name = doc.getString("name");
                        teacherNames.add(name != null ? name : doc.getId());
                    }

                    if (teacherNames.isEmpty()) {
                        teacherNames.add("No teachers found — add one first");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, teacherNames);
                    teacherSpinner.setAdapter(adapter);
                });
    }

    private void attemptCreateCourse() {
        String baseCourseId = courseIdInput.getText().toString().trim();
        String courseName = courseNameInput.getText().toString().trim();
        String sectionsRaw = targetSectionInput.getText().toString().trim();

        if (TextUtils.isEmpty(baseCourseId) || TextUtils.isEmpty(courseName)) {
            Toast.makeText(this, "Please fill in Course ID and Name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (teacherUids.isEmpty()) {
            Toast.makeText(this, "No teacher available to assign", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIndex = teacherSpinner.getSelectedItemPosition();
        String teacherId = teacherUids.get(selectedIndex);

        String targetBatch = targetBatchInput.getText().toString().trim();
        String targetSemester = targetSemesterInput.getText().toString().trim();
        if (targetBatch.isEmpty()) targetBatch = "N/A";
        if (targetSemester.isEmpty()) targetSemester = "N/A";

        // Split sections by comma; blank → single "N/A" section
        List<String> sections = new ArrayList<>();
        if (sectionsRaw.isEmpty()) {
            sections.add("N/A");
        } else {
            for (String s : sectionsRaw.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) sections.add(trimmed);
            }
        }
        if (sections.isEmpty()) sections.add("N/A");

        setLoading(true);
        sectionsRemaining = sections.size();
        totalMatched = 0;

        for (String section : sections) {
            String sectionSlug = section.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            String courseId = sections.size() == 1 ? baseCourseId : baseCourseId + "_" + sectionSlug;

            Map<String, Object> course = new HashMap<>();
            course.put("name", courseName);
            course.put("teacherId", teacherId);
            course.put("studentIds", new ArrayList<String>());
            course.put("crStudentId", null);
            course.put("grStudentId", null);
            course.put("targetBatch", targetBatch);
            course.put("targetSemester", targetSemester);
            course.put("targetSection", section);

            String finalTargetBatch = targetBatch;
            String finalTargetSemester = targetSemester;

            db.collection("courses").document(courseId).set(course)
                    .addOnSuccessListener(aVoid ->
                            enrollMatchingStudents(courseId, finalTargetBatch, finalTargetSemester, section))
                    .addOnFailureListener(e -> {
                        sectionsRemaining--;
                        Toast.makeText(this, "Failed for section " + section + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                        if (sectionsRemaining == 0) finishUp();
                    });
        }
    }

    private void enrollMatchingStudents(String courseId, String batch, String semester, String section) {
        db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("batch", batch)
                .whereEqualTo("semester", semester)
                .whereEqualTo("section", section)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> matchingUids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        matchingUids.add(doc.getId());
                    }

                    db.collection("courses").document(courseId)
                            .update("studentIds", matchingUids)
                            .addOnCompleteListener(task -> {
                                totalMatched += matchingUids.size();
                                sectionsRemaining--;
                                if (sectionsRemaining == 0) finishUp();
                            });
                })
                .addOnFailureListener(e -> {
                    sectionsRemaining--;
                    if (sectionsRemaining == 0) finishUp();
                });
    }

    private void finishUp() {
        setLoading(false);
        Toast.makeText(this, "Course created! " + totalMatched + " matching student(s) enrolled across all sections.",
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void setLoading(boolean loading) {
        addCourseProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        createCourseButton.setEnabled(!loading);
        createCourseButton.setAlpha(loading ? 0.5f : 1f);
    }
}