package com.ubit.lms;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        bottomNavigation = findViewById(R.id.adminBottomNavigation);

        loadFragment(new AdminHomeFragment());

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selected;
            int id = item.getItemId();

            if (id == R.id.nav_admin_dashboard) {
                selected = new AdminHomeFragment();
            } else if (id == R.id.nav_admin_courses) {
                selected = new AdminCoursesFragment();
            } else {
                selected = new AdminUsersFragment();
            }

            loadFragment(selected);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.adminFragmentContainer, fragment);
        transaction.commit();
    }
}