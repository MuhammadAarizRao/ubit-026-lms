package com.ubit.lms;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText uniIdInput, passwordInput;
    private Button loginButton;
    private ImageButton togglePasswordButton;
    private ProgressBar loginProgress;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        requestNotificationPermission();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        uniIdInput = findViewById(R.id.uniIdInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        loginProgress = findViewById(R.id.loginProgress);

        togglePasswordButton = findViewById(R.id.togglePasswordButton);
        togglePasswordButton.setOnClickListener(v -> togglePasswordVisibility());

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            togglePasswordButton.setImageResource(R.drawable.ic_eye);
        } else {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            togglePasswordButton.setImageResource(R.drawable.ic_eye_off);
        }
        passwordInput.setSelection(passwordInput.getText().length());
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void attemptLogin() {
        String uniId = uniIdInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(uniId) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter both ID and password", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = uniIdToEmail(uniId);

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        fetchUserProfile(uid);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserProfile(String uid) {
        FcmTokenHelper.saveTokenForCurrentUser();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists()) {
                        Boolean mustChange = documentSnapshot.getBoolean("mustChangePassword");
                        String role = documentSnapshot.getString("role");

                        if (mustChange != null && mustChange) {
                            startActivity(new Intent(LoginActivity.this, SetPasswordActivity.class));
                            finish();
                        } else if ("admin".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                            finish();
                        } else if ("teacher".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, TeacherDashboardActivity.class));
                            finish();
                        } else {
                            startActivity(new Intent(LoginActivity.this, StudentDashboardActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "No profile found for this account.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String uniIdToEmail(String uniId) {
        String cleaned = uniId.toLowerCase().replace("-", "");
        return cleaned + "@ubit-lms.app";
    }

    private void setLoading(boolean loading) {
        loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
    }
}