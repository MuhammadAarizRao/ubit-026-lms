package com.ubit.lms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

public class CourseDetailActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FirebaseFirestore db;
    private String courseId, courseName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        db = FirebaseFirestore.getInstance();

        courseId = getIntent().getStringExtra("courseId");
        courseName = getIntent().getStringExtra("courseName");

        TextView title = findViewById(R.id.courseDetailTitle);
        title.setText(courseName);

        findViewById(R.id.courseBackButton).setOnClickListener(v -> finish());

        Button chatCrButton = findViewById(R.id.chatWithCrButton);
        Button chatGrButton = findViewById(R.id.chatWithGrButton);
        Button chatGroupButton = findViewById(R.id.chatGroupButton);

        chatCrButton.setOnClickListener(v -> openIndividualChat("crStudentId", "CR"));
        chatGrButton.setOnClickListener(v -> openIndividualChat("grStudentId", "GR"));
        chatGroupButton.setOnClickListener(v -> openGroupChat());

        bottomNavigation = findViewById(R.id.courseBottomNavigation);

        loadFragment(TeacherAttendanceFragment.newInstance(courseId, courseName));

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selected;
            int id = item.getItemId();

            if (id == R.id.nav_attendance) {
                selected = TeacherAttendanceFragment.newInstance(courseId, courseName);
            } else if (id == R.id.nav_materials) {
                selected = TeacherMaterialsFragment.newInstance(courseId, courseName);
            } else if (id == R.id.nav_assignments) {
                selected = TeacherAssignmentsFragment.newInstance(courseId, courseName);
            } else if (id == R.id.nav_marks) {
                selected = TeacherMarksFragment.newInstance(courseId, courseName);
            } else {
                selected = TeacherNotificationsFragment.newInstance(courseId, courseName);
            }

            loadFragment(selected);
            return true;
        });
    }

    private void openIndividualChat(String fieldName, String label) {
        db.collection("courses").document(courseId).get()
                .addOnSuccessListener(doc -> {
                    String targetUid = doc.getString(fieldName);

                    if (targetUid == null) {
                        Toast.makeText(this, "No " + label + " assigned for this course yet", Toast.LENGTH_LONG).show();
                        return;
                    }

                    db.collection("users").document(targetUid).get()
                            .addOnSuccessListener(userDoc -> {
                                String name = userDoc.getString("name");
                                String chatId = courseId + "_" + targetUid;

                                Intent intent = new Intent(this, ChatActivity.class);
                                intent.putExtra("chatId", chatId);
                                intent.putExtra("otherPartyName", (name != null ? name : label) + " (" + label + ")");
                                startActivity(intent);
                            });
                });
    }

    private void openGroupChat() {
        db.collection("courses").document(courseId).get()
                .addOnSuccessListener(doc -> {
                    String crUid = doc.getString("crStudentId");
                    String grUid = doc.getString("grStudentId");

                    if (crUid == null && grUid == null) {
                        Toast.makeText(this, "No CR or GR assigned yet", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String chatId = courseId + "_group";

                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("otherPartyName", courseName + " — Group (CR & GR)");
                    startActivity(intent);
                });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.courseFragmentContainer, fragment);
        transaction.commit();
    }
}