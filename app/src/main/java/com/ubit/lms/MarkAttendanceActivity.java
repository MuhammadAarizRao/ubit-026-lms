package com.ubit.lms;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MarkAttendanceActivity extends AppCompatActivity {

    private TextView courseTitleText;
    private RecyclerView studentsRecyclerView;
    private Button saveAttendanceButton;

    private FirebaseFirestore db;
    private String courseId, courseName;
    private List<StudentAttendanceRow> studentRows = new ArrayList<>();
    private AttendanceMarkingAdapter adapter;

    private Button generateTodayPdfButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        db = FirebaseFirestore.getInstance();

        courseId = getIntent().getStringExtra("courseId");
        courseName = getIntent().getStringExtra("courseName");

        courseTitleText = findViewById(R.id.attendanceCourseTitle);
        studentsRecyclerView = findViewById(R.id.studentsRecyclerView);
        saveAttendanceButton = findViewById(R.id.saveAttendanceButton);
        generateTodayPdfButton = findViewById(R.id.generateTodayPdfButton);
        generateTodayPdfButton.setOnClickListener(v -> generateTodayPdf());

        courseTitleText.setText(courseName != null ? courseName : "Course");
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadStudents();

        saveAttendanceButton.setOnClickListener(v -> saveAttendance());
    }

    private void loadStudents() {
        db.collection("courses").document(courseId).get()
                .addOnSuccessListener(courseDoc -> {
                    List<String> studentIds = (List<String>) courseDoc.get("studentIds");
                    if (studentIds == null || studentIds.isEmpty()) {
                        Toast.makeText(this, "No students enrolled in this course", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    studentRows.clear();
                    final int[] remaining = {studentIds.size()};

                    for (String uid : studentIds) {
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    studentRows.add(new StudentAttendanceRow(uid, name != null ? name : uid, true));

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        adapter = new AttendanceMarkingAdapter(studentRows);
                                        studentsRecyclerView.setAdapter(adapter);
                                    }
                                });
                    }
                });
    }

    private void saveAttendance() {
        if (studentRows.isEmpty()) {
            Toast.makeText(this, "No students to save attendance for", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        String teacherUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> entries = new HashMap<>();
        for (StudentAttendanceRow row : studentRows) {
            entries.put(row.getUid(), row.isPresent() ? "present" : "absent");
        }

        Map<String, Object> record = new HashMap<>();
        record.put("date", today);
        record.put("markedBy", teacherUid);
        record.put("entries", entries);

        db.collection("attendance").document(courseId)
                .collection("records").document(today)
                .set(record)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attendance saved for " + today, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void generateTodayPdf() {
        if (studentRows.isEmpty()) {
            Toast.makeText(this, "Mark attendance first", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        List<String> lines = new ArrayList<>();
        lines.add("Date: " + today);
        lines.add("");

        for (StudentAttendanceRow row : studentRows) {
            lines.add(row.getName() + " — " + (row.isPresent() ? "Present" : "Absent"));
        }

        PdfHelper.generateAndSavePdf(this, courseName + " Attendance " + today, lines);
    }
}