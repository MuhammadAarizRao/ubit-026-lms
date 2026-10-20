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

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class UploadMaterialActivity extends AppCompatActivity {

    private TextView uploadCourseTitle, selectedFileNameText;
    private Button pickFileButton, uploadButton;
    private ProgressBar uploadProgress;

    private String courseId, courseName;
    private Uri selectedFileUri;
    private String selectedFileName;
    private long selectedFileSize;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_material);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        courseId = getIntent().getStringExtra("courseId");
        courseName = getIntent().getStringExtra("courseName");

        uploadCourseTitle = findViewById(R.id.uploadCourseTitle);
        selectedFileNameText = findViewById(R.id.selectedFileNameText);
        pickFileButton = findViewById(R.id.pickFileButton);
        uploadButton = findViewById(R.id.uploadButton);
        uploadProgress = findViewById(R.id.uploadProgress);

        uploadCourseTitle.setText("Upload to: " + courseName);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        readFileMetadata(uri);
                        uploadButton.setEnabled(true);
                    }
                });

        pickFileButton.setOnClickListener(v ->
                filePickerLauncher.launch(new String[]{"*/*"}));

        uploadButton.setOnClickListener(v -> uploadFile());
    }

    private void readFileMetadata(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            selectedFileName = cursor.getString(nameIndex);
            selectedFileSize = cursor.getLong(sizeIndex);
            cursor.close();

            selectedFileNameText.setText(selectedFileName + " (" + formatSize(selectedFileSize) + ")");
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.0f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void uploadFile() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please choose a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        StorageReference fileRef = storage.getReference()
                .child("materials")
                .child(courseId)
                .child(System.currentTimeMillis() + "_" + selectedFileName);

        UploadTask uploadTask = fileRef.putFile(selectedFileUri);

        uploadTask.addOnSuccessListener(taskSnapshot ->
                fileRef.getDownloadUrl().addOnSuccessListener(this::saveMaterialMetadata)
        ).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void saveMaterialMetadata(Uri downloadUrl) {
        Map<String, Object> material = new HashMap<>();
        material.put("courseId", courseId);
        material.put("courseName", courseName);
        material.put("title", selectedFileName);
        material.put("fileUrl", downloadUrl.toString());
        material.put("sizeLabel", formatSize(selectedFileSize));
        material.put("uploadedAt", FieldValue.serverTimestamp());

        db.collection("materials").add(material)
                .addOnSuccessListener(docRef -> {
                    setLoading(false);
                    Toast.makeText(this, "Material uploaded successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "File uploaded, but failed to save info: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        uploadProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        uploadButton.setEnabled(!loading);
        pickFileButton.setEnabled(!loading);
    }
}