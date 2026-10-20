package com.ubit.lms;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherAttendanceFragment extends Fragment {

    private static final String ARG_COURSE_ID = "courseId";
    private static final String ARG_COURSE_NAME = "courseName";

    private LinearLayout attendanceContentContainer;
    private RecyclerView studentsRecyclerView;
    private Button markAttendanceButton, saveAttendanceButton, generateTodayPdfButton, viewHistoryButton;

    private FirebaseFirestore db;
    private String courseId, courseName;
    private List<StudentAttendanceRow> studentRows = new ArrayList<>();

    public static TeacherAttendanceFragment newInstance(String courseId, String courseName) {
        TeacherAttendanceFragment fragment = new TeacherAttendanceFragment();
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
        View view = inflater.inflate(R.layout.fragment_teacher_attendance, container, false);

        if (getArguments() != null) {
            courseId = getArguments().getString(ARG_COURSE_ID);
            courseName = getArguments().getString(ARG_COURSE_NAME);
        }

        db = FirebaseFirestore.getInstance();

        markAttendanceButton = view.findViewById(R.id.markAttendanceButton);
        attendanceContentContainer = view.findViewById(R.id.attendanceContentContainer);
        studentsRecyclerView = view.findViewById(R.id.studentsRecyclerView);
        saveAttendanceButton = view.findViewById(R.id.saveAttendanceButton);
        generateTodayPdfButton = view.findViewById(R.id.generateTodayPdfButton);
        viewHistoryButton = view.findViewById(R.id.viewHistoryButton);

        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        markAttendanceButton.setOnClickListener(v -> {
            boolean isVisible = attendanceContentContainer.getVisibility() == View.VISIBLE;
            attendanceContentContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            markAttendanceButton.setText(isVisible ? "Mark Attendance" : "Hide");

            if (!isVisible && studentRows.isEmpty()) {
                loadStudents();
            }
        });

        saveAttendanceButton.setOnClickListener(v -> saveAttendance());
        generateTodayPdfButton.setOnClickListener(v -> generateTodayPdf());
        viewHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CourseAttendanceActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseName", courseName);
            startActivity(intent);
        });

        return view;
    }

    private void loadStudents() {
        db.collection("courses").document(courseId).get()
                .addOnSuccessListener(courseDoc -> {
                    if (getContext() == null) return;

                    List<String> studentIds = (List<String>) courseDoc.get("studentIds");
                    if (studentIds == null || studentIds.isEmpty()) {
                        Toast.makeText(getContext(), "No students enrolled in this course", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    studentRows.clear();
                    final int[] remaining = {studentIds.size()};

                    for (String uid : studentIds) {
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (getContext() == null) return;
                                    String name = userDoc.getString("name");
                                    studentRows.add(new StudentAttendanceRow(uid, name != null ? name : uid, true));

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        studentsRecyclerView.setAdapter(new AttendanceMarkingAdapter(studentRows));
                                    }
                                });
                    }
                });
    }

    private void saveAttendance() {
        if (studentRows.isEmpty() || getContext() == null) {
            Toast.makeText(getContext(), "No students to save attendance for", Toast.LENGTH_SHORT).show();
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
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Attendance saved for " + today, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void generateTodayPdf() {
        if (studentRows.isEmpty() || getContext() == null) {
            Toast.makeText(getContext(), "Mark attendance first", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        List<String> lines = new ArrayList<>();
        lines.add("Date: " + today);
        lines.add("");

        for (StudentAttendanceRow row : studentRows) {
            lines.add(row.getName() + " — " + (row.isPresent() ? "Present" : "Absent"));
        }

        PdfHelper.generateAndSavePdf(getContext(), courseName + " Attendance " + today, lines);
    }
}