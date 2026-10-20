package com.ubit.lms;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class StudentDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bottomNavigation = findViewById(R.id.bottomNavigation);

        loadFragment(new HomeFragment());

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selected;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selected = new HomeFragment();
            } else if (id == R.id.nav_courses) {
                selected = new CoursesFragment();
                clearBadge(R.id.nav_courses, "lastMaterialCheck");
            } else if (id == R.id.nav_tasks) {
                selected = new TasksFragment();
                clearBadge(R.id.nav_tasks, "lastAssignmentCheck");
            } else {
                selected = new AlertsFragment();
                clearBadge(R.id.nav_alerts, "lastNotificationCheck");
            }

            loadFragment(selected);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForNewContent("notifications", "timestamp", R.id.nav_alerts, "lastNotificationCheck");
        checkForNewContent("materials", "uploadedAt", R.id.nav_courses, "lastMaterialCheck");
        checkForNewContent("assignments", "createdAt", R.id.nav_tasks, "lastAssignmentCheck");
    }

    private void checkForNewContent(String collection, String timestampField, int navId, String lastCheckField) {
        db.collection(collection)
                .orderBy(timestampField, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) return;

                    com.google.firebase.Timestamp latestTime =
                            querySnapshot.getDocuments().get(0).getTimestamp(timestampField);

                    if (latestTime == null) return;

                    String uid = mAuth.getCurrentUser().getUid();
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(userDoc -> {
                                com.google.firebase.Timestamp lastCheck = userDoc.getTimestamp(lastCheckField);

                                boolean hasNew = lastCheck == null ||
                                        latestTime.toDate().getTime() > lastCheck.toDate().getTime();

                                showBadge(navId, hasNew);
                            });
                });
    }

    private void showBadge(int navId, boolean show) {
        BadgeDrawable badge = bottomNavigation.getOrCreateBadge(navId);
        badge.setVisible(show);
    }

    private void clearBadge(int navId, String lastCheckField) {
        String uid = mAuth.getCurrentUser().getUid();
        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put(lastCheckField, com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> showBadge(navId, false));
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }
}