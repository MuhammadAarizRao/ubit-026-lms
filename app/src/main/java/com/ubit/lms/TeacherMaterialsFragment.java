package com.ubit.lms;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class TeacherMaterialsFragment extends Fragment {

    private static final String ARG_COURSE_ID = "courseId";
    private static final String ARG_COURSE_NAME = "courseName";

    private LinearLayout materialsListContainer;
    private FirebaseFirestore db;
    private String courseId, courseName;

    public static TeacherMaterialsFragment newInstance(String courseId, String courseName) {
        TeacherMaterialsFragment fragment = new TeacherMaterialsFragment();
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
        View view = inflater.inflate(R.layout.fragment_teacher_materials, container, false);

        if (getArguments() != null) {
            courseId = getArguments().getString(ARG_COURSE_ID);
            courseName = getArguments().getString(ARG_COURSE_NAME);
        }

        db = FirebaseFirestore.getInstance();
        materialsListContainer = view.findViewById(R.id.materialsListContainer);

        Button uploadButton = view.findViewById(R.id.uploadMaterialButton);
        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UploadMaterialActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseName", courseName);
            startActivity(intent);
        });

        loadMaterials();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMaterials(); // refresh after returning from upload screen
    }

    private void loadMaterials() {
        db.collection("materials").whereEqualTo("courseId", courseId).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    materialsListContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    if (querySnapshot.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No materials uploaded yet.");
                        empty.setTextColor(0xFF607D8B);
                        materialsListContainer.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        String sizeLabel = doc.getString("sizeLabel");

                        View row = inflater.inflate(R.layout.item_material_row, materialsListContainer, false);
                        TextView titleView = row.findViewById(R.id.materialTitleText);
                        TextView sizeView = row.findViewById(R.id.materialSizeText);

                        titleView.setText(title);
                        sizeView.setText(sizeLabel);

                        materialsListContainer.addView(row);
                    }
                });
    }
}