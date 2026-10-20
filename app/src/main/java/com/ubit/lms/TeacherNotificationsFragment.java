package com.ubit.lms;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherNotificationsFragment extends Fragment {

    private static final String ARG_COURSE_ID = "courseId";
    private static final String ARG_COURSE_NAME = "courseName";

    private Spinner typeSpinner;
    private EditText messageInput;
    private Button postButton;
    private LinearLayout notificationsListContainer;

    private FirebaseFirestore db;
    private String courseId, courseName;

    public static TeacherNotificationsFragment newInstance(String courseId, String courseName) {
        TeacherNotificationsFragment fragment = new TeacherNotificationsFragment();
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
        View view = inflater.inflate(R.layout.fragment_teacher_notifications, container, false);

        if (getArguments() != null) {
            courseId = getArguments().getString(ARG_COURSE_ID);
            courseName = getArguments().getString(ARG_COURSE_NAME);
        }

        db = FirebaseFirestore.getInstance();

        typeSpinner = view.findViewById(R.id.notificationTypeSpinner);
        messageInput = view.findViewById(R.id.notificationMessageInput);
        postButton = view.findViewById(R.id.postNotificationButton);
        notificationsListContainer = view.findViewById(R.id.notificationsListContainer);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Class Update", "Exam Alert", "Quiz Alert", "General"});
        typeSpinner.setAdapter(adapter);

        postButton.setOnClickListener(v -> postNotification());

        loadNotifications();

        return view;
    }

    private void postNotification() {
        String type = typeSpinner.getSelectedItem().toString();
        String message = messageInput.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("courseId", courseId);
        notification.put("type", type);
        notification.put("message", message);
        notification.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notifications").add(notification)
                .addOnSuccessListener(docRef -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "Notification posted!", Toast.LENGTH_SHORT).show();
                    messageInput.setText("");
                    loadNotifications();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadNotifications() {
        db.collection("notifications")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        docs.add(doc);
                    }

                    docs.sort((a, b) -> {
                        long aTime = 0, bTime = 0;
                        try { if (a.getTimestamp("timestamp") != null) aTime = a.getTimestamp("timestamp").toDate().getTime(); } catch (Exception ignored) {}
                        try { if (b.getTimestamp("timestamp") != null) bTime = b.getTimestamp("timestamp").toDate().getTime(); } catch (Exception ignored) {}
                        return Long.compare(bTime, aTime);
                    });

                    notificationsListContainer.removeAllViews();

                    if (docs.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No notifications posted yet.");
                        empty.setTextColor(0xFF607D8B);
                        notificationsListContainer.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : docs) {
                        String type = doc.getString("type");
                        String message = doc.getString("message");
                        Date timestamp = doc.getDate("timestamp");

                        String timeLabel = timestamp != null
                                ? new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(timestamp)
                                : "";

                        TextView row = new TextView(getContext());
                        row.setText(type + " — " + message + "\n" + timeLabel);
                        row.setTextSize(13);
                        row.setPadding(0, 12, 0, 12);
                        notificationsListContainer.addView(row);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}