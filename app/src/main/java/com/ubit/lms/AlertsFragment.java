package com.ubit.lms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

public class AlertsFragment extends Fragment {

    private RecyclerView allNotificationsRecyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        allNotificationsRecyclerView = view.findViewById(R.id.allNotificationsRecyclerView);
        allNotificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadEnrolledCoursesThenNotifications();

        return view;
    }

    private void loadEnrolledCoursesThenNotifications() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("courses").whereArrayContains("studentIds", uid).get()
                .addOnSuccessListener(courseSnapshot -> {
                    if (getContext() == null) return;

                    List<String> courseIds = new ArrayList<>();
                    courseIds.add("general");
                    for (QueryDocumentSnapshot doc : courseSnapshot) {
                        courseIds.add(doc.getId());
                    }

                    loadAllNotifications(courseIds);
                });
    }

    private void loadAllNotifications(List<String> courseIds) {
        db.collection("notifications")
                .whereIn("courseId", courseIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
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
                    allNotificationsRecyclerView.setAdapter(new NotificationAdapter(notifList));
                });
    }
}