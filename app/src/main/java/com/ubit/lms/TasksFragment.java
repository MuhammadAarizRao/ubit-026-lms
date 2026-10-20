package com.ubit.lms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment {

    private RecyclerView assignmentsRecyclerView;
    private TextView pendingCountText, submittedCountText, totalCountText;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        assignmentsRecyclerView = view.findViewById(R.id.assignmentsRecyclerView);
        pendingCountText = view.findViewById(R.id.pendingCountText);
        submittedCountText = view.findViewById(R.id.submittedCountText);
        totalCountText = view.findViewById(R.id.totalCountText);

        assignmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadEnrolledCoursesThenAssignments();

        return view;
    }

    private void loadEnrolledCoursesThenAssignments() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("courses").whereArrayContains("studentIds", uid).get()
                .addOnSuccessListener(courseSnapshot -> {
                    if (getContext() == null) return;

                    List<String> enrolledCourseIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : courseSnapshot) {
                        enrolledCourseIds.add(doc.getId());
                    }

                    if (enrolledCourseIds.isEmpty()) {
                        pendingCountText.setText("0");
                        submittedCountText.setText("0");
                        totalCountText.setText("0");
                        assignmentsRecyclerView.setAdapter(new AssignmentAdapter(new ArrayList<>()));
                        return;
                    }

                    loadAssignments(uid, enrolledCourseIds);
                });
    }

    private void loadAssignments(String uid, List<String> enrolledCourseIds) {
        db.collection("assignments")
                .whereIn("courseId", enrolledCourseIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        docs.add(doc);
                    }

                    docs.sort((a, b) -> {
                        long aTime = 0, bTime = 0;
                        try { if (a.getTimestamp("createdAt") != null) aTime = a.getTimestamp("createdAt").toDate().getTime(); } catch (Exception ignored) {}
                        try { if (b.getTimestamp("createdAt") != null) bTime = b.getTimestamp("createdAt").toDate().getTime(); } catch (Exception ignored) {}
                        return Long.compare(bTime, aTime);
                    });

                    List<AssignmentItem> items = new ArrayList<>();
                    int pending = 0, submitted = 0;

                    for (QueryDocumentSnapshot doc : docs) {
                        try {
                            String courseName = doc.getString("courseName");
                            String title = doc.getString("title");
                            String description = doc.getString("description");
                            String dueDateLabel = doc.getString("dueDateLabel");

                            long dueMillis = 0;
                            try {
                                Timestamp dueTimestamp = doc.getTimestamp("dueTimestamp");
                                if (dueTimestamp != null) dueMillis = dueTimestamp.toDate().getTime();
                            } catch (Exception ignored) {}

                            List<String> submittedBy = (List<String>) doc.get("submittedBy");
                            boolean isSubmitted = submittedBy != null && submittedBy.contains(uid);

                            items.add(new AssignmentItem(doc.getId(), courseName, title, description,
                                    dueDateLabel != null ? "Due: " + dueDateLabel : "No due date",
                                    dueMillis, isSubmitted));

                            if (isSubmitted) submitted++;
                            else pending++;
                        } catch (Exception e) {
                            // Skip this one document instead of breaking the whole list
                        }
                    }

                    assignmentsRecyclerView.setAdapter(new AssignmentAdapter(items));
                    pendingCountText.setText(String.valueOf(pending));
                    submittedCountText.setText(String.valueOf(submitted));
                    totalCountText.setText(String.valueOf(items.size()));
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}