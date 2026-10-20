package com.ubit.lms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class AdminUsersFragment extends Fragment {

    private LinearLayout usersListContainer;
    private Spinner filterBatchSpinner, filterSemesterSpinner, filterSectionSpinner;
    private FirebaseFirestore db;

    private List<UserRowData> allUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);

        db = FirebaseFirestore.getInstance();
        usersListContainer = view.findViewById(R.id.adminUsersListContainer);
        filterBatchSpinner = view.findViewById(R.id.filterBatchSpinner);
        filterSemesterSpinner = view.findViewById(R.id.filterSemesterSpinner);
        filterSectionSpinner = view.findViewById(R.id.filterSectionSpinner);

        loadUsers();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (getContext() == null) return;

                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        UserRowData u = new UserRowData();
                        u.name = doc.getString("name");
                        u.uniId = doc.getString("uniId");
                        u.role = doc.getString("role");
                        u.section = doc.getString("section");
                        u.semester = doc.getString("semester");
                        u.batch = doc.getString("batch");
                        allUsers.add(u);
                    }

                    setupFilters();
                    applyFilters();
                });
    }

    private void setupFilters() {
        TreeSet<String> batches = new TreeSet<>();
        TreeSet<String> semesters = new TreeSet<>();
        TreeSet<String> sections = new TreeSet<>();

        for (UserRowData u : allUsers) {
            batches.add(u.batch != null ? u.batch : "N/A");
            semesters.add(u.semester != null ? u.semester : "N/A");
            sections.add(u.section != null ? u.section : "N/A");
        }

        List<String> batchOptions = new ArrayList<>();
        batchOptions.add("All Batches");
        batchOptions.addAll(batches);

        List<String> semesterOptions = new ArrayList<>();
        semesterOptions.add("All Semesters");
        semesterOptions.addAll(semesters);

        List<String> sectionOptions = new ArrayList<>();
        sectionOptions.add("All Sections");
        sectionOptions.addAll(sections);

        filterBatchSpinner.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, batchOptions));
        filterSemesterSpinner.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, semesterOptions));
        filterSectionSpinner.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, sectionOptions));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        filterBatchSpinner.setOnItemSelectedListener(listener);
        filterSemesterSpinner.setOnItemSelectedListener(listener);
        filterSectionSpinner.setOnItemSelectedListener(listener);
    }

    private void applyFilters() {
        if (getContext() == null) return;

        String selectedBatch = (String) filterBatchSpinner.getSelectedItem();
        String selectedSemester = (String) filterSemesterSpinner.getSelectedItem();
        String selectedSection = (String) filterSectionSpinner.getSelectedItem();

        usersListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (UserRowData u : allUsers) {
            String uBatch = u.batch != null ? u.batch : "N/A";
            String uSemester = u.semester != null ? u.semester : "N/A";
            String uSection = u.section != null ? u.section : "N/A";

            boolean batchMatch = selectedBatch == null || selectedBatch.equals("All Batches") || selectedBatch.equals(uBatch);
            boolean semesterMatch = selectedSemester == null || selectedSemester.equals("All Semesters") || selectedSemester.equals(uSemester);
            boolean sectionMatch = selectedSection == null || selectedSection.equals("All Sections") || selectedSection.equals(uSection);

            if (!batchMatch || !semesterMatch || !sectionMatch) continue;

            View row = inflater.inflate(R.layout.item_user_row, usersListContainer, false);
            TextView nameText = row.findViewById(R.id.userRowName);
            TextView idRoleText = row.findViewById(R.id.userRowIdRole);
            TextView metaText = row.findViewById(R.id.userRowMeta);

            nameText.setText(u.name != null ? u.name : "Unnamed");
            idRoleText.setText((u.uniId != null ? u.uniId : "--") + " · " + (u.role != null ? u.role : "--"));
            metaText.setText(uSection + " · Sem " + uSemester + " · " + uBatch);

            usersListContainer.addView(row);
        }

        if (usersListContainer.getChildCount() == 0) {
            TextView empty = new TextView(getContext());
            empty.setText("No users match this filter.");
            empty.setTextColor(0xFF607D8B);
            empty.setPadding(0, 20, 0, 0);
            usersListContainer.addView(empty);
        }
    }

    private static class UserRowData {
        String name, uniId, role, section, semester, batch;
    }
}