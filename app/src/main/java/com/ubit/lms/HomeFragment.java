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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView greetingText, studentNameText, studentIdText, studentMetaText;
    private TextView avgAttendanceText, pendingTasksText, gpaText;
    private RecyclerView attendanceRecyclerView, notificationsRecyclerView;
    private View chatWithTeacherButton, groupChatButton;
    private LinearLayout marksContainer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        greetingText = view.findViewById(R.id.greetingText);
        studentNameText = view.findViewById(R.id.studentNameText);
        studentIdText = view.findViewById(R.id.studentMetaText);
        studentMetaText = view.findViewById(R.id.studentMetaText);
        avgAttendanceText = view.findViewById(R.id.avgAttendanceText);
        pendingTasksText = view.findViewById(R.id.pendingTasksText);
        gpaText = view.findViewById(R.id.gpaText);
        attendanceRecyclerView = view.findViewById(R.id.attendanceRecyclerView);
        notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView);
        chatWithTeacherButton = view.findViewById(R.id.chatWithTeacherButton);
        groupChatButton = view.findViewById(R.id.groupChatButton);
        marksContainer = view.findViewById(R.id.marksContainer);

        attendanceRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadStudentProfile();
        loadNotifications();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            checkIfCrOrGr(mAuth.getCurrentUser().getUid());
        }
    }

    private void loadStudentProfile() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || getContext() == null) return;

                    String name = doc.getString("name");
                    String uniId = doc.getString("uniId");

                    studentNameText.setText(name != null ? name : "Student");
                    studentIdText.setText(uniId != null ? uniId : "");
                    greetingText.setText(getGreeting());
                    pendingTasksText.setText("--");

                    String section = doc.getString("section");
                    String semester = doc.getString("semester");
                    String batch = doc.getString("batch");

                    String metaLine = (section != null ? section : "N/A") + " · "
                            + "Sem " + (semester != null ? semester : "N/A") + " · "
                            + (batch != null ? batch : "N/A");
                    studentMetaText.setText(metaLine);

                    loadRealAttendance(uid);
                    checkIfCrOrGr(uid);
                    loadMarksAndGpa(uid);
                });
    }

    private void loadMarksAndGpa(String uid) {
        db.collection("marks").document(uid).get()
                .addOnSuccessListener(marksDoc -> {
                    if (getContext() == null) return;

                    Map<String, Object> subjects = (Map<String, Object>) marksDoc.get("subjects");
                    marksContainer.removeAllViews();

                    if (subjects == null || subjects.isEmpty()) {
                        gpaText.setText("--");
                        TextView empty = new TextView(getContext());
                        empty.setText("No marks entered yet.");
                        empty.setTextColor(0xFF607D8B);
                        empty.setPadding(16, 16, 16, 16);
                        marksContainer.addView(empty);
                        return;
                    }

                    double total = 0;
                    int count = 0;
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    for (Map.Entry<String, Object> entry : subjects.entrySet()) {
                        String courseId = entry.getKey();
                        double marks = ((Number) entry.getValue()).doubleValue();
                        total += marks;
                        count++;

                        db.collection("courses").document(courseId).get()
                                .addOnSuccessListener(courseDoc -> {
                                    if (getContext() == null) return;
                                    String courseName = courseDoc.getString("name");

                                    View row = inflater.inflate(R.layout.item_marks_summary_row, marksContainer, false);
                                    TextView nameText = row.findViewById(R.id.marksCourseNameText);
                                    TextView marksText = row.findViewById(R.id.marksObtainedText);
                                    TextView gradeText = row.findViewById(R.id.marksGradeText);

                                    nameText.setText(courseName != null ? courseName : courseId);
                                    marksText.setText((int) marks + "/100");
                                    gradeText.setText(GradeHelper.getGrade(marks));

                                    marksContainer.addView(row);
                                });
                    }

                    double avgPercentage = total / count;
                    double gpa = Math.round((avgPercentage / 100.0 * 4.0) * 10.0) / 10.0;
                    gpaText.setText(String.valueOf(gpa));
                });
    }

    private void loadRealAttendance(String uid) {
        db.collection("courses").whereArrayContains("studentIds", uid).get()
                .addOnSuccessListener(courseSnapshot -> {
                    if (getContext() == null) return;

                    List<QueryDocumentSnapshot> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : courseSnapshot) {
                        courses.add(doc);
                    }

                    if (courses.isEmpty()) {
                        attendanceRecyclerView.setAdapter(new AttendanceAdapter(new ArrayList<>()));
                        avgAttendanceText.setText("--");
                        return;
                    }

                    List<AttendanceItem> attendanceList = new ArrayList<>();
                    final int[] remaining = {courses.size()};
                    final int[] totalSum = {0};

                    for (QueryDocumentSnapshot courseDoc : courses) {
                        String courseId = courseDoc.getId();
                        String courseName = courseDoc.getString("name");

                        db.collection("attendance").document(courseId).collection("records").get()
                                .addOnSuccessListener(recordsSnapshot -> {
                                    if (getContext() == null) return;

                                    int totalDays = recordsSnapshot.size();
                                    int presentDays = 0;

                                    for (QueryDocumentSnapshot record : recordsSnapshot) {
                                        Map<String, Object> entries = (Map<String, Object>) record.get("entries");
                                        if (entries != null && "present".equals(entries.get(uid))) {
                                            presentDays++;
                                        }
                                    }

                                    int percentage = totalDays > 0 ? (presentDays * 100 / totalDays) : 0;
                                    attendanceList.add(new AttendanceItem(
                                            courseName != null ? courseName : courseId, percentage));
                                    totalSum[0] += percentage;

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        attendanceRecyclerView.setAdapter(new AttendanceAdapter(attendanceList));
                                        int avg = attendanceList.isEmpty() ? 0 : totalSum[0] / attendanceList.size();
                                        avgAttendanceText.setText(avg + "%");
                                    }
                                });
                    }
                });
    }

    private void checkIfCrOrGr(String uid) {
        db.collection("courses")
                .whereEqualTo("crStudentId", uid)
                .get()
                .addOnSuccessListener(crSnapshot -> {
                    if (getContext() == null) return;

                    if (!crSnapshot.isEmpty()) {
                        showChatButton(crSnapshot.getDocuments().get(0).getId());
                    } else {
                        db.collection("courses")
                                .whereEqualTo("grStudentId", uid)
                                .get()
                                .addOnSuccessListener(grSnapshot -> {
                                    if (getContext() == null) return;
                                    if (!grSnapshot.isEmpty()) {
                                        showChatButton(grSnapshot.getDocuments().get(0).getId());
                                    }
                                });
                    }
                });
    }

    private void showChatButton(String courseId) {
        if (getContext() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        String individualChatId = courseId + "_" + uid;
        String groupChatId = courseId + "_group";

        chatWithTeacherButton.setVisibility(View.VISIBLE);
        chatWithTeacherButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("chatId", individualChatId);
            intent.putExtra("otherPartyName", "Teacher");
            startActivity(intent);
        });
        checkUnreadForButton(individualChatId, chatWithTeacherButton, "Chat with Teacher (1-on-1)");

        groupChatButton.setVisibility(View.VISIBLE);
        groupChatButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("chatId", groupChatId);
            intent.putExtra("otherPartyName", "Group Chat (CR, GR & Teacher)");
            startActivity(intent);
        });
        checkUnreadForButton(groupChatId, groupChatButton, "Group Chat (CR, GR & Teacher)");
    }

    private void checkUnreadForButton(String chatId, View button, String baseLabel) {
        // Unread indicator temporarily simplified during UI redesign — button is now a styled pill, not plain text
    }

    private void loadNotifications() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("courses").whereArrayContains("studentIds", uid).get()
                .addOnSuccessListener(courseSnapshot -> {
                    if (getContext() == null) return;

                    List<String> courseIds = new ArrayList<>();
                    courseIds.add("general");
                    for (QueryDocumentSnapshot doc : courseSnapshot) {
                        courseIds.add(doc.getId());
                    }

                    db.collection("notifications")
                            .whereIn("courseId", courseIds)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(10)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (getContext() == null) return;

                                List<NotificationItem> notifList = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    String courseId = doc.getString("courseId");
                                    String type = doc.getString("type");
                                    String message = doc.getString("message");
                                    Date timestamp = doc.getDate("timestamp");

                                    String timeAgo = timestamp != null
                                            ? new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(timestamp)
                                            : "";

                                    notifList.add(new NotificationItem(
                                            courseId != null ? courseId : "General",
                                            type != null ? type : "",
                                            message != null ? message : "",
                                            timeAgo));
                                }
                                notificationsRecyclerView.setAdapter(new NotificationAdapter(notifList));
                            });
                });
    }

    private String getGreeting() {
        int hour = new java.util.GregorianCalendar().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }
}