package com.ubit.lms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CourseAttendanceActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private FirebaseFirestore db;
    private String courseId, courseName;
    private List<String> pdfLines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_attendance);

        db = FirebaseFirestore.getInstance();
        courseId = getIntent().getStringExtra("courseId");
        courseName = getIntent().getStringExtra("courseName");

        TextView titleText = findViewById(R.id.historyTitle);
        titleText.setText(courseName + " — Attendance History");

        historyContainer = findViewById(R.id.historyContainer);

        Button generateFullPdfButton = findViewById(R.id.generateFullPdfButton);
        generateFullPdfButton.setOnClickListener(v -> {
            if (pdfLines.isEmpty()) {
                Toast.makeText(this, "No attendance records yet", Toast.LENGTH_SHORT).show();
            } else {
                PdfHelper.generateAndSavePdf(this, courseName + " Full Attendance", pdfLines);
            }
        });

        loadHistory();
    }

    private void loadHistory() {
        db.collection("attendance").document(courseId).collection("records")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    historyContainer.removeAllViews();
                    pdfLines.clear();

                    if (querySnapshot.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No attendance records yet.");
                        empty.setTextColor(0xFF607D8B);
                        historyContainer.addView(empty);
                        return;
                    }

                    java.util.Set<String> allUids = new java.util.HashSet<>();
                    List<QueryDocumentSnapshot> allDocs = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        allDocs.add(doc);
                        Map<String, Object> entries = (Map<String, Object>) doc.get("entries");
                        if (entries != null) {
                            allUids.addAll(entries.keySet());
                        }
                    }

                    resolveNamesThenRender(allDocs, allUids);
                });
    }

    private void resolveNamesThenRender(List<QueryDocumentSnapshot> allDocs, java.util.Set<String> allUids) {
        Map<String, String> uidToName = new java.util.HashMap<>();
        final int[] remaining = {allUids.size()};

        if (allUids.isEmpty()) {
            renderDateList(allDocs, uidToName);
            return;
        }

        for (String uid : allUids) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(userDoc -> {
                        String name = userDoc.getString("name");
                        uidToName.put(uid, name != null ? name : uid);

                        remaining[0]--;
                        if (remaining[0] == 0) {
                            renderDateList(allDocs, uidToName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        uidToName.put(uid, uid);
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            renderDateList(allDocs, uidToName);
                        }
                    });
        }
    }

    private void renderDateList(List<QueryDocumentSnapshot> allDocs, Map<String, String> uidToName) {
        LayoutInflater inflater = LayoutInflater.from(this);

        for (QueryDocumentSnapshot doc : allDocs) {
            String date = doc.getString("date");
            Map<String, Object> entries = (Map<String, Object>) doc.get("entries");

            Map<String, String> sortedEntries = new TreeMap<>();
            if (entries != null) {
                for (Map.Entry<String, Object> entry : entries.entrySet()) {
                    sortedEntries.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            // Add to PDF lines regardless of expand state
            pdfLines.add("Date: " + date);
            for (Map.Entry<String, String> entry : sortedEntries.entrySet()) {
                String name = uidToName.getOrDefault(entry.getKey(), entry.getKey());
                pdfLines.add("  " + name + " — " + entry.getValue());
            }
            pdfLines.add("");

            // Collapsible date row
            LinearLayout dateCard = new LinearLayout(this);
            dateCard.setOrientation(LinearLayout.VERTICAL);
            dateCard.setBackgroundResource(R.drawable.rounded_card);
            dateCard.setPadding(28, 24, 28, 24);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = 20;
            dateCard.setLayoutParams(cardParams);
            dateCard.setElevation(4);

            TextView dateHeader = new TextView(this);
            dateHeader.setText(date);
            dateHeader.setTextSize(15);
            dateHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            dateHeader.setTextColor(getResources().getColor(R.color.maroon_primary));
            dateCard.addView(dateHeader);

            LinearLayout studentsList = new LinearLayout(this);
            studentsList.setOrientation(LinearLayout.VERTICAL);
            studentsList.setVisibility(View.GONE);
            studentsList.setPadding(0, 16, 0, 0);

            for (Map.Entry<String, String> entry : sortedEntries.entrySet()) {
                String name = uidToName.getOrDefault(entry.getKey(), entry.getKey());
                TextView row = new TextView(this);
                row.setText(name + " — " + entry.getValue());
                row.setTextSize(13);
                row.setPadding(0, 4, 0, 4);
                studentsList.addView(row);
            }

            dateCard.addView(studentsList);

            dateCard.setOnClickListener(v -> {
                boolean isVisible = studentsList.getVisibility() == View.VISIBLE;
                studentsList.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            });

            historyContainer.addView(dateCard);
        }
    }
}