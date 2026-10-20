package com.ubit.lms;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignCrGrActivity extends AppCompatActivity {

    private Spinner crSpinner, grSpinner;
    private FirebaseFirestore db;
    private String courseId, courseName;

    private List<String> studentUids = new ArrayList<>();
    private List<String> studentNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_cr_gr);

        db = FirebaseFirestore.getInstance();
        courseId = getIntent().getStringExtra("courseId");
        courseName = getIntent().getStringExtra("courseName");

        TextView title = findViewById(R.id.assignCrGrTitle);
        title.setText("Assign CR/GR — " + courseName);

        crSpinner = findViewById(R.id.crSpinner);
        grSpinner = findViewById(R.id.grSpinner);
        View saveButton = findViewById(R.id.saveCrGrButton);

        loadStudents();

        saveButton.setOnClickListener(v -> saveCrGr());
    }

    private void loadStudents() {
        db.collection("courses").document(courseId).get()
                .addOnSuccessListener(courseDoc -> {
                    List<String> ids = (List<String>) courseDoc.get("studentIds");
                    if (ids == null || ids.isEmpty()) {
                        Toast.makeText(this, "No students enrolled in this course", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    studentUids.clear();
                    studentNames.clear();
                    final int[] remaining = {ids.size()};

                    for (String uid : ids) {
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    studentUids.add(uid);
                                    studentNames.add(name != null ? name : uid);

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        setupSpinners();
                                    }
                                });
                    }
                });
    }

    private void setupSpinners() {
        List<String> options = new ArrayList<>();
        options.add("None");
        options.addAll(studentNames);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, options);
        crSpinner.setAdapter(adapter);
        grSpinner.setAdapter(adapter);
    }

    private void saveCrGr() {
        int crIndex = crSpinner.getSelectedItemPosition();
        int grIndex = grSpinner.getSelectedItemPosition();

        String crUid = crIndex == 0 ? null : studentUids.get(crIndex - 1);
        String grUid = grIndex == 0 ? null : studentUids.get(grIndex - 1);

        Map<String, Object> update = new HashMap<>();
        update.put("crStudentId", crUid);
        update.put("grStudentId", grUid);

        db.collection("courses").document(courseId).update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "CR/GR updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}