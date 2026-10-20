package com.ubit.lms;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddUserActivity extends AppCompatActivity {

    private EditText nameInput, uniIdInput, tempPasswordInput;
    private EditText sectionInput, semesterInput, batchInput;
    private Spinner roleSpinner;
    private View createUserButton;
    private ProgressBar addUserProgress;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        db = FirebaseFirestore.getInstance();

        nameInput = findViewById(R.id.nameInput);
        uniIdInput = findViewById(R.id.uniIdInput);
        tempPasswordInput = findViewById(R.id.tempPasswordInput);
        sectionInput = findViewById(R.id.sectionInput);
        semesterInput = findViewById(R.id.semesterInput);
        batchInput = findViewById(R.id.batchInput);
        roleSpinner = findViewById(R.id.roleSpinner);
        createUserButton = findViewById(R.id.createUserButton);
        addUserProgress = findViewById(R.id.addUserProgress);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"student", "teacher"});
        roleSpinner.setAdapter(adapter);

        createUserButton.setOnClickListener(v -> attemptCreateUser());
    }

    private void attemptCreateUser() {
        String name = nameInput.getText().toString().trim();
        String uniId = uniIdInput.getText().toString().trim();
        String tempPassword = tempPasswordInput.getText().toString().trim();
        String role = roleSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(uniId) || TextUtils.isEmpty(tempPassword)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = uniIdToEmail(uniId);
        setLoading(true);

        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("SecondaryApp");
        } catch (IllegalStateException e) {
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            secondaryApp = FirebaseApp.initializeApp(this, options, "SecondaryApp");
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(email, tempPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String newUid = task.getResult().getUser().getUid();
                        saveUserProfile(newUid, name, uniId, role);
                        secondaryAuth.signOut();
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserProfile(String uid, String name, String uniId, String role) {
        String section = sectionInput.getText().toString().trim();
        String semester = semesterInput.getText().toString().trim();
        String batch = batchInput.getText().toString().trim();

        String finalSection = section.isEmpty() ? "N/A" : section;
        String finalSemester = semester.isEmpty() ? "N/A" : semester;
        String finalBatch = batch.isEmpty() ? "N/A" : batch;

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("uniId", uniId);
        user.put("role", role);
        user.put("classRole", "none");
        user.put("mustChangePassword", true);
        user.put("courses", new java.util.ArrayList<String>());
        user.put("section", finalSection);
        user.put("semester", finalSemester);
        user.put("batch", finalBatch);

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    if ("student".equals(role)) {
                        joinMatchingCourses(uid, finalBatch, finalSemester, finalSection);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "User created successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Auth created, but profile failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void joinMatchingCourses(String uid, String batch, String semester, String section) {
        db.collection("courses")
                .whereEqualTo("targetBatch", batch)
                .whereEqualTo("targetSemester", semester)
                .whereEqualTo("targetSection", section)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        setLoading(false);
                        Toast.makeText(this, "User created successfully!", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    for (com.google.firebase.firestore.QueryDocumentSnapshot courseDoc : querySnapshot) {
                        courseDoc.getReference().update("studentIds",
                                com.google.firebase.firestore.FieldValue.arrayUnion(uid));
                    }

                    setLoading(false);
                    Toast.makeText(this, "User created and enrolled in " + querySnapshot.size() + " matching course(s)!",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "User created, but course matching failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String uniIdToEmail(String uniId) {
        String cleaned = uniId.toLowerCase().replace("-", "");
        return cleaned + "@ubit-lms.app";
    }

    private void setLoading(boolean loading) {
        addUserProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        createUserButton.setEnabled(!loading);
        createUserButton.setAlpha(loading ? 0.5f : 1f);
    }
}