package com.ubit.lms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CoursesFragment extends Fragment {

    private LinearLayout coursesContainer;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courses, container, false);

        coursesContainer = view.findViewById(R.id.coursesContainer);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadEnrolledCoursesThenMaterials();

        return view;
    }

    private void loadEnrolledCoursesThenMaterials() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("courses").whereArrayContains("studentIds", uid).get()
                .addOnSuccessListener(courseSnapshot -> {
                    if (getContext() == null) return;

                    List<String> enrolledCourseIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : courseSnapshot) {
                        enrolledCourseIds.add(doc.getId());
                    }

                    if (enrolledCourseIds.isEmpty()) {
                        coursesContainer.removeAllViews();
                        TextView empty = new TextView(getContext());
                        empty.setText("You're not enrolled in any course yet.");
                        empty.setTextColor(0xFF607D8B);
                        coursesContainer.addView(empty);
                        return;
                    }

                    loadMaterials(enrolledCourseIds);
                });
    }

    private void loadMaterials(List<String> enrolledCourseIds) {
        db.collection("materials").whereIn("courseId", enrolledCourseIds).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    Map<String, List<MaterialItem>> grouped = new LinkedHashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String courseName = doc.getString("courseName");
                        String title = doc.getString("title");
                        String fileUrl = doc.getString("fileUrl");
                        String sizeLabel = doc.getString("sizeLabel");

                        if (courseName == null) courseName = "Other";

                        MaterialItem item = new MaterialItem(courseName, title, fileUrl, sizeLabel);

                        if (!grouped.containsKey(courseName)) {
                            grouped.put(courseName, new ArrayList<>());
                        }
                        grouped.get(courseName).add(item);
                    }

                    renderGroups(grouped);
                });
    }

    private void renderGroups(Map<String, List<MaterialItem>> grouped) {
        coursesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (grouped.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No materials uploaded yet for your courses.");
            empty.setTextColor(0xFF607D8B);
            coursesContainer.addView(empty);
            return;
        }

        for (Map.Entry<String, List<MaterialItem>> entry : grouped.entrySet()) {
            View groupView = inflater.inflate(R.layout.item_course_group, coursesContainer, false);

            TextView titleText = groupView.findViewById(R.id.courseGroupTitle);
            LinearLayout materialsContainer = groupView.findViewById(R.id.materialsContainer);

            titleText.setText(entry.getKey());

            for (MaterialItem item : entry.getValue()) {
                View row = inflater.inflate(R.layout.item_material_row, materialsContainer, false);

                TextView titleView = row.findViewById(R.id.materialTitleText);
                TextView sizeView = row.findViewById(R.id.materialSizeText);
                android.widget.Button downloadButton = row.findViewById(R.id.downloadButton);

                titleView.setText(item.getTitle());
                sizeView.setText(item.getSizeLabel());

                String fileUrl = item.getFileUrl();
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    downloadButton.setOnClickListener(v -> {
                        android.content.Intent intent = new android.content.Intent(
                                android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fileUrl));
                        startActivity(intent);
                    });
                } else {
                    downloadButton.setEnabled(false);
                    downloadButton.setText("No file");
                }

                materialsContainer.addView(row);
            }

            coursesContainer.addView(groupView);

            View spacer = new View(getContext());
            spacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 14));
            coursesContainer.addView(spacer);
        }
    }
}