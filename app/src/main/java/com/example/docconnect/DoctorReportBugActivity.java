package com.example.docconnect;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * DoctorReportBugActivity: Professional Support Implementation.
 * Uses HashMap for Firebase submission to maintain consistency with the Admin Dashboard.
 */
public class DoctorReportBugActivity extends AppCompatActivity {

    // Cloudinary Credentials
    private static final String CLOUD_NAME = "dps6a4fvu";
    private static final String UPLOAD_PRESET = "ds132213";

    // UI Components
    private LinearLayout cardSync, cardCalendar, cardError, cardRecords, cardOther, layoutAttachment;
    private TextView tvDiagnosticInfo, tvAttachmentStatus;
    private EditText etDescription;
    private MaterialButton btnSubmit;
    private ImageView btnBack;

    // Data Variables
    private String selectedCategory = "Data Sync Issue";
    private DatabaseReference databaseReference;
    private Uri selectedImageUri = null;

    // Image Picker Launcher
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && !isFinishing()) {
                    selectedImageUri = uri;
                    tvAttachmentStatus.setText("Image Selected (Ready to Upload)");
                    tvAttachmentStatus.setTextColor(ContextCompat.getColor(this, R.color.doc_primary));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_bug_report);

        // Session Guard
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        // Initialize Firebase Reference
        String doctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("doctors")
                .child(doctorId).child("support_tickets");

        initCloudinary();
        initViews();
        populateDeviceInfo();
        setupCategoryListeners();

        // Click Listeners
        layoutAttachment.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSubmit.setOnClickListener(v -> validateAndStart());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initCloudinary() {
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            MediaManager.init(this, config);
        }
    }

    private void initViews() {
        cardSync = findViewById(R.id.card_sync);
        cardCalendar = findViewById(R.id.card_calendar);
        cardError = findViewById(R.id.card_error);
        cardRecords = findViewById(R.id.card_records);
        cardOther = findViewById(R.id.card_other);
        layoutAttachment = findViewById(R.id.layout_attachment);
        tvAttachmentStatus = findViewById(R.id.tv_attachment_status);
        tvDiagnosticInfo = findViewById(R.id.tv_diagnostic_info);
        etDescription = findViewById(R.id.et_description);
        btnSubmit = findViewById(R.id.btn_submit);
        btnBack = findViewById(R.id.btn_back);
    }

    private void populateDeviceInfo() {
        String diagnosticText = "Device: " + capitalize(Build.MANUFACTURER) + " " + Build.MODEL +
                ", OS: " + Build.VERSION.RELEASE;
        tvDiagnosticInfo.setText(diagnosticText);
    }

    private void validateAndStart() {
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        if (description.isEmpty()) {
            etDescription.setError("Please briefly describe the issue");
            return;
        }

        btnSubmit.setText("Processing...");
        btnSubmit.setEnabled(false); // Prevent double submission

        fetchDoctorDetailsAndProceed(description);
    }

    /**
     * Fetches Doctor's name and Profile Image for the ticket.
     */
    private void fetchDoctorDetailsAndProceed(String description) {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference doctorRef = FirebaseDatabase.getInstance().getReference("doctors").child(uid);

        doctorRef.get().addOnCompleteListener(task -> {
            if (isFinishing()) return;

            String doctorName = "Unknown Doctor";
            String profileImg = "";

            if (task.isSuccessful() && task.getResult().exists()) {
                doctorName = task.getResult().child("fullName").getValue(String.class);
                profileImg = task.getResult().child("profileImageUrl").getValue(String.class);
            }

            if (selectedImageUri != null) {
                uploadImageToCloudinary(description, doctorName, profileImg);
            } else {
                submitTicketToFirebase(description, "", doctorName, profileImg);
            }
        });
    }

    private void uploadImageToCloudinary(String description, String doctorName, String profileImg) {
        if (isFinishing()) return;
        btnSubmit.setText("Uploading Screenshot...");

        MediaManager.get().upload(selectedImageUri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String evidenceUrl = (String) resultData.get("secure_url");
                        runOnUiThread(() -> {
                            if (!isFinishing())
                                submitTicketToFirebase(description, evidenceUrl, doctorName, profileImg);
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo errorInfo) {
                        runOnUiThread(() -> {
                            if (!isFinishing()) {
                                Toast.makeText(DoctorReportBugActivity.this, "Upload failed, sending text only", Toast.LENGTH_SHORT).show();
                                submitTicketToFirebase(description, "", doctorName, profileImg);
                            }
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo errorInfo) {}
                }).dispatch();
    }

    /**
     * Final submission using HashMap to ensure compatibility with Admin model.
     */
    private void submitTicketToFirebase(String description, String evidenceUrl, String doctorName, String profileImg) {
        if (isFinishing()) return;
        btnSubmit.setText("Finalizing...");

        String doctorId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        String currentDate = new SimpleDateFormat("dd MMM yyyy ", Locale.getDefault()).format(new Date());
        String ticketId = databaseReference.push().getKey();

        // Create HashMap to match AdminComplaintModel keys
        Map<String, Object> ticketMap = new HashMap<>();
        ticketMap.put("ticketId", ticketId);
        ticketMap.put("userId", doctorId); // Stored as userId for Admin view consistency
        ticketMap.put("fullName", doctorName);
        ticketMap.put("profileImageUrl", profileImg);
        ticketMap.put("category", selectedCategory);
        ticketMap.put("description", description);
        ticketMap.put("evidenceUrl", evidenceUrl);
        ticketMap.put("date", currentDate);
        ticketMap.put("status", "New");
        ticketMap.put("deviceInfo", Build.MANUFACTURER + " " + Build.MODEL);

        if (ticketId != null) {
            databaseReference.child(ticketId).setValue(ticketMap)
                    .addOnSuccessListener(aVoid -> {
                        if (!isFinishing()) {
                            Toast.makeText(this, "Support Ticket Submitted!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isFinishing()) {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("Submit Ticket");
                            Toast.makeText(this, "Firebase Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void setupCategoryListeners() {
        View.OnClickListener listener = v -> {
            resetCardStyles();
            v.setBackgroundResource(R.drawable.bg_card_selected);
            int id = v.getId();
            if (id == R.id.card_sync) selectedCategory = "Data Sync Issue";
            else if (id == R.id.card_calendar) selectedCategory = "Calendar/Booking";
            else if (id == R.id.card_error) selectedCategory = "Appointment Error";
            else if (id == R.id.card_records) selectedCategory = "Patient Records";
            else if (id == R.id.card_other) selectedCategory = "Other Issue";
        };
        cardSync.setOnClickListener(listener);
        cardCalendar.setOnClickListener(listener);
        cardError.setOnClickListener(listener);
        cardRecords.setOnClickListener(listener);
        cardOther.setOnClickListener(listener);
    }

    private void resetCardStyles() {
        cardSync.setBackgroundResource(R.drawable.bg_card_unselected);
        cardCalendar.setBackgroundResource(R.drawable.bg_card_unselected);
        cardError.setBackgroundResource(R.drawable.bg_card_unselected);
        cardRecords.setBackgroundResource(R.drawable.bg_card_unselected);
        cardOther.setBackgroundResource(R.drawable.bg_card_unselected);
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}