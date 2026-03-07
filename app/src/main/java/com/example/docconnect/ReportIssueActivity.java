package com.example.docconnect;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportIssueActivity extends AppCompatActivity {

    // --- UI Components ---
    private AutoCompleteTextView categoryDropdown;
    private EditText etAppointmentId, etDescription;
    private ImageView ivPreview;

    private TextView readFaqs;
    private View btnUpload;
    private ProgressDialog progressDialog;

    // --- Data Variables ---
    private Uri imageUri;
    private final String[] CATEGORIES = {"Delay", "Cancellation", "Refund", "No-Slots", "Billing", "Technical"};

    // --- Firebase References ---
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        // 1. Initialize Cloudinary safely
        initCloudinary();

        // 2. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getUid() != null) {
            // Path where issues are stored: users -> [UID] -> report_issue
            mDatabase = FirebaseDatabase.getInstance().getReference("users")
                    .child(mAuth.getUid()).child("report_issue");
        }

        // 3. Setup UI
        initViews();
        setupProgressDialog();
        readFaqs.setOnClickListener(v -> startActivity(new Intent(this, FAQsActivity.class)));
    }

    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", "dps6a4fvu");
            config.put("secure", true);
            MediaManager.init(this, config);
        } catch (Exception e) {
            // Handled: MediaManager already initialized
        }
    }

    private void initViews() {
        categoryDropdown = findViewById(R.id.category_dropdown);
        etAppointmentId = findViewById(R.id.et_appointment_id);
        etDescription = findViewById(R.id.et_description);
        ivPreview = findViewById(R.id.iv_screenshot_preview);
        btnUpload = findViewById(R.id.btn_upload_image);
        readFaqs = findViewById(R.id.readFaqs);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, CATEGORIES);
        categoryDropdown.setAdapter(adapter);

        btnUpload.setOnClickListener(v -> openGallery());
        findViewById(R.id.btn_submit_ticket).setOnClickListener(v -> validateAndProcess());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            ivPreview.setImageURI(imageUri);
            ivPreview.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Logic flow: Validation -> (Cloudinary Upload) -> Fetch Profile -> Save to Firebase
     */
    private void validateAndProcess() {
        String category = categoryDropdown.getText().toString();
        String description = etDescription.getText().toString().trim();

        if (category.isEmpty() || TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (imageUri != null) {
            uploadToCloudinary();
        } else {
            fetchUserDataAndSubmit("");
        }
    }

    private void uploadToCloudinary() {
        progressDialog.setMessage("Uploading Evidence...");
        MediaManager.get().upload(imageUri)
                .unsigned("ds132213")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        fetchUserDataAndSubmit((String) resultData.get("secure_url"));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressDialog.dismiss();
                        Toast.makeText(ReportIssueActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    /**
     * Retrieves the current user's profile info from the "users/[uid]" node.
     */
    private void fetchUserDataAndSubmit(String evidenceUrl) {
        progressDialog.setMessage("Finalizing...");
        String uid = mAuth.getUid();

        if (uid == null) {
            progressDialog.dismiss();
            return;
        }

        // Access the profile node to get current user details
        DatabaseReference userProfileRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userProfileRef.get().addOnCompleteListener(task -> {
            String userName = "Anonymous";
            String userPic = "";

            if (task.isSuccessful() && task.getResult().exists()) {
                // Fetching existing profile data
                String fetchedName = task.getResult().child("fullName").getValue(String.class);
                String fetchedPic = task.getResult().child("profilePhotoUrl").getValue(String.class);

                if (fetchedName != null) userName = fetchedName;
                if (fetchedPic != null) userPic = fetchedPic;
            }
            saveTicketToDatabase(userName, userPic, evidenceUrl);
        }).addOnFailureListener(e -> saveTicketToDatabase("Anonymous", "", evidenceUrl));
    }

    /**
     * Saves the ticket under the key 'fullName' as requested.
     */
    private void saveTicketToDatabase(String fullName, String profileImageUrl, String evidenceUrl) {
        String ticketId = mDatabase.push().getKey();

        Map<String, Object> ticket = new HashMap<>();
        ticket.put("ticketId", ticketId);
        ticket.put("userId", mAuth.getUid());

        // --- Updated Keys ---
        ticket.put("fullName", fullName); // Saved as fullName per request
        ticket.put("profileImageUrl", profileImageUrl); // Added profile image key
        // --------------------

        ticket.put("category", categoryDropdown.getText().toString());
        ticket.put("appointmentId", etAppointmentId.getText().toString());
        ticket.put("description", etDescription.getText().toString());
        ticket.put("evidenceUrl", evidenceUrl);
        ticket.put("status", "New");
        ticket.put("date", new SimpleDateFormat("dd MMM yyyy ", Locale.getDefault()).format(new Date()));

        if (ticketId != null) {
            mDatabase.child(ticketId).setValue(ticket).addOnSuccessListener(aVoid -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Issue Reported Successfully!", Toast.LENGTH_LONG).show();
                finish();
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}