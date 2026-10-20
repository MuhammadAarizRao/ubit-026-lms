package com.ubit.lms;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardActivity extends AppCompatActivity {

    private TextView teacherGreetingText, teacherNameText, teacherMetaText;
    private RecyclerView teacherCoursesRecyclerView;
    private View openChatsButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        teacherGreetingText = findViewById(R.id.teacherGreetingText);
        teacherNameText = findViewById(R.id.teacherNameText);
        teacherMetaText = findViewById(R.id.teacherMetaText);
        teacherCoursesRecyclerView = findViewById(R.id.teacherCoursesRecyclerView);
        teacherCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        openChatsButton = findViewById(R.id.openChatsButton);
        openChatsButton.setOnClickListener(v ->
                startActivity(new Intent(this, ChatInboxActivity.class)));

        loadTeacherProfile();
        loadTeacherCourses();
    }

    private void loadTeacherProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name = doc.getString("name");
                    teacherNameText.setText(name != null ? name : "Teacher");
                    teacherGreetingText.setText("Welcome,");

                    String section = doc.getString("section");
                    String semester = doc.getString("semester");
                    String batch = doc.getString("batch");

                    String metaLine = (section != null ? section : "N/A") + " · "
                            + "Sem " + (semester != null ? semester : "N/A") + " · "
                            + (batch != null ? batch : "N/A");
                    teacherMetaText.setText(metaLine);
                });
    }

    private void loadTeacherCourses() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("courses").whereEqualTo("teacherId", uid).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TeacherCourseItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String courseId = doc.getId();
                        String name = doc.getString("name");
                        List<String> studentIds = (List<String>) doc.get("studentIds");
                        if (studentIds == null) studentIds = new ArrayList<>();

                        String targetSection = doc.getString("targetSection");
                        items.add(new TeacherCourseItem(courseId, name, studentIds, targetSection));
                    }
                    teacherCoursesRecyclerView.setAdapter(new TeacherCourseAdapter(items));
                });
    }
}