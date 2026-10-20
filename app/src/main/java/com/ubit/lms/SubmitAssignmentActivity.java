package com.ubit.lms;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class SubmitAssignmentActivity extends AppCompatActivity {

    private TextView titleText, selectedFileNameText;
    private Button pickFileButton, submitFileButton;
    private ProgressBar submitProgress;

    private String assignmentId, assignmentTitle;
    private Uri selectedFileUri;
    private String selectedFileName;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;

    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_assignment);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();

        assignmentId = getIntent().getStringExtra("assignmentId");
        assignmentTitle = getIntent().getStringExtra("assignmentTitle");

        titleText = findViewById(R.id.submitAssignmentTitle);
        selectedFileNameText = findViewById(R.id.selectedFileNameText);
        pickFileButton = findViewById(R.id.pickFileButton);
        submitFileButton = findViewById(R.id.submitFileButton);
        submitProgress = findViewById(R.id.submitProgress);

        titleText.setText("Submit: " + assignmentTitle);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        readFileName(uri);
                        submitFileButton.setEnabled(true);
                    }
                });

        pickFileButton.setOnClickListener(v -> filePickerLauncher.launch(new String[]{"*/*"}));
        submitFileButton.setOnClickListener(v -> submitAssignment());
    }

    private void readFileName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            selectedFileName = cursor.getString(nameIndex);
            cursor.close();
            selectedFileNameText.setText(selectedFileName);
        }
    }

    private void submitAssignment() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please choose a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        String uid = mAuth.getCurrentUser().getUid();

        StorageReference fileRef = storage.getReference()
                .child("submissions")
                .child(assignmentId)
                .child(uid + "_" + selectedFileName);

        fileRef.putFile(selectedFileUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                            Map<String, Object> update = new HashMap<>();
                            update.put("submittedBy", FieldValue.arrayUnion(uid));
                            update.put("submissionUrl_" + uid, downloadUrl.toString());
                            update.put("submissionTime_" + uid, FieldValue.serverTimestamp());

                            db.collection("assignments").document(assignmentId)
                                    .update(update)
                                    .addOnSuccessListener(aVoid -> {
                                        setLoading(false);
                                        Toast.makeText(this, "Assignment submitted!", Toast.LENGTH_LONG).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        setLoading(false);
                                        Toast.makeText(this, "File uploaded, but failed to update status: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });
                        })
                )
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        submitProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        submitFileButton.setEnabled(!loading);
        pickFileButton.setEnabled(!loading);
    }
}