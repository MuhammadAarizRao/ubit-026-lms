package com.ubit.lms;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewSubmissionsActivity extends AppCompatActivity {

    private LinearLayout submissionsListContainer;
    private FirebaseFirestore db;
    private String assignmentId, assignmentTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_submissions);

        db = FirebaseFirestore.getInstance();
        assignmentId = getIntent().getStringExtra("assignmentId");
        assignmentTitle = getIntent().getStringExtra("assignmentTitle");

        TextView titleText = findViewById(R.id.submissionsTitle);
        titleText.setText("Submissions: " + assignmentTitle);

        submissionsListContainer = findViewById(R.id.submissionsListContainer);

        loadSubmissions();
    }

    private void loadSubmissions() {
        db.collection("assignments").document(assignmentId).get()
                .addOnSuccessListener(doc -> {
                    List<String> submittedBy = (List<String>) doc.get("submittedBy");
                    Map<String, Object> data = doc.getData();

                    if (submittedBy == null || submittedBy.isEmpty()) {
                        showEmptyMessage();
                        return;
                    }

                    LayoutInflater inflater = LayoutInflater.from(this);

                    for (String uid : submittedBy) {
                        String fileUrl = data != null ? (String) data.get("submissionUrl_" + uid) : null;
                        Timestamp submissionTime = data != null ? (Timestamp) data.get("submissionTime_" + uid) : null;

                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");

                                    View row = inflater.inflate(R.layout.item_submission_row, submissionsListContainer, false);
                                    TextView nameText = row.findViewById(R.id.submissionStudentNameText);
                                    TextView timeText = row.findViewById(R.id.submissionTimeText);
                                    Button downloadButton = row.findViewById(R.id.downloadSubmissionButton);

                                    nameText.setText(name != null ? name : uid);

                                    if (submissionTime != null) {
                                        String formatted = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                                .format(submissionTime.toDate());
                                        timeText.setText("Submitted: " + formatted);
                                        timeText.setVisibility(View.VISIBLE);
                                    }

                                    if (fileUrl != null) {
                                        downloadButton.setOnClickListener(v -> {
                                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
                                            startActivity(intent);
                                        });
                                    } else {
                                        downloadButton.setEnabled(false);
                                        downloadButton.setText("No file");
                                    }

                                    submissionsListContainer.addView(row);
                                });
                    }
                });
    }

    private void showEmptyMessage() {
        TextView empty = new TextView(this);
        empty.setText("No submissions yet.");
        empty.setTextColor(0xFF607D8B);
        submissionsListContainer.addView(empty);
    }
}