package com.example.docconnect;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * DoctorEditProfileActivity: A2Z Professional Implementation.
 * * FIXES: UI Hanging via Delayed Initialization.
 * * FIXES: WindowLeaked exceptions via lifecycle-aware dialogs.
 * * FEATURES: Cloudinary integration, Gender Toggle, and Firebase Sync.
 */
public class DoctorEditProfileActivity extends AppCompatActivity {

    private static final String TAG = "DoctorProfile";

    // --- UI ELEMENTS ---
    private ImageView btnBack, profileImageView, ivEditIcon;
    private EditText etName, etSpecialization, etEmail, etPhone, etExperience, etFees;
    private AppCompatButton btnSave;
    private LinearLayout layoutFemale, layoutMale;
    private TextView txtFemale, txtMale;
    private ImageView iconFemale, iconMale;

    // --- STATE VARIABLES ---
    private String selectedGender = "Male";
    private Uri imageUri;
    private String currentCloudinaryUrl = "";
    private static final int IMAGE_REQUEST_CODE = 100;
    private static final String UPLOAD_PRESET = "ds132213";

    // --- FIREBASE ---
    private DatabaseReference doctorRef;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_edit_profile);

        // 1. SESSION GUARD
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        doctorRef = FirebaseDatabase.getInstance().getReference("doctors").child(uid);

        // 2. INITIALIZE VIEWS
        initViews();
        setupListeners();

        // 3. THE HANG-FREE FIX (A2Z OPTIMIZATION)
        // We wait 300ms for the Activity transition animation to finish.
        // This prevents the CPU from overloading and causing the 'Hanging' feeling.
        getWindow().getDecorView().postDelayed(this::loadDoctorData, 300);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        profileImageView = findViewById(R.id.profileImageView);
        ivEditIcon = findViewById(R.id.iv_edit_icon);
        etName = findViewById(R.id.etName);
        etSpecialization = findViewById(R.id.etSpecialization);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etExperience = findViewById(R.id.etExperience);
        etFees = findViewById(R.id.etBio);

        layoutFemale = findViewById(R.id.layout_gender_female);
        layoutMale = findViewById(R.id.layout_gender_male);
        txtFemale = findViewById(R.id.txt_gender_female);
        txtMale = findViewById(R.id.txt_gender_male);
        iconFemale = findViewById(R.id.icon_gender_female);
        iconMale = findViewById(R.id.icon_gender_male);
        btnSave = findViewById(R.id.btnSave);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        profileImageView.setOnClickListener(v -> openGallery());
        ivEditIcon.setOnClickListener(v -> openGallery());
        layoutFemale.setOnClickListener(v -> updateGenderUI("Female"));
        layoutMale.setOnClickListener(v -> updateGenderUI("Male"));
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void loadDoctorData() {
        if (isFinishing()) return;
        showProgress("Retrieving Profile...");

        doctorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dismissProgress();
                if (!snapshot.exists() || isFinishing()) return;

                //

                // Use safe strings to avoid literal "null" text in fields
                etName.setText(getSafeValue(snapshot, "fullName"));
                etSpecialization.setText(getSafeValue(snapshot, "speciality"));
                etEmail.setText(getSafeValue(snapshot, "email"));
                etPhone.setText(getSafeValue(snapshot, "phone"));
                etExperience.setText(getSafeValue(snapshot, "experience"));
                etFees.setText(getSafeValue(snapshot, "consultationFees"));

                String gender = snapshot.child("gender").getValue(String.class);
                if (gender != null) updateGenderUI(gender);

                currentCloudinaryUrl = snapshot.child("profileImageUrl").getValue(String.class);
                if (currentCloudinaryUrl != null && !currentCloudinaryUrl.isEmpty()) {
                    Glide.with(DoctorEditProfileActivity.this)
                            .load(currentCloudinaryUrl)
                            .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.ic_doctor))
                            .into(profileImageView);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { dismissProgress(); }
        });
    }

    private void validateAndSave() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Name required");
            return;
        }

        btnSave.setEnabled(false); // Prevent double-clicks during process
        if (imageUri != null) {
            uploadImageToCloudinary();
        } else {
            updateFirebaseData(currentCloudinaryUrl);
        }
    }

    private void uploadImageToCloudinary() {
        showProgress("Uploading Picture...");
        MediaManager.get().upload(imageUri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", "doctor_profiles")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String newUrl = (String) resultData.get("secure_url");
                        runOnUiThread(() -> updateFirebaseData(newUrl));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            dismissProgress();
                            Toast.makeText(DoctorEditProfileActivity.this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void updateFirebaseData(String profileUrl) {
        showProgress("Finalizing Profile...");
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", etName.getText().toString().trim());
        updates.put("speciality", etSpecialization.getText().toString().trim());
        updates.put("email", etEmail.getText().toString().trim());
        updates.put("phone", etPhone.getText().toString().trim());
        updates.put("experience", etExperience.getText().toString().trim());
        updates.put("consultationFees", etFees.getText().toString().trim());
        updates.put("gender", selectedGender);
        updates.put("profileImageUrl", profileUrl);

        doctorRef.updateChildren(updates).addOnCompleteListener(task -> {
            dismissProgress();
            if (task.isSuccessful()) {
                Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- GENDER UI LOGIC ---
    private void updateGenderUI(String gender) {
        selectedGender = gender;
        boolean isFemale = "Female".equalsIgnoreCase(gender);

        layoutFemale.setBackgroundResource(isFemale ? R.drawable.bg_gender_selected : R.drawable.bg_gender_unselected);
        txtFemale.setTextColor(ContextCompat.getColor(this, isFemale ? R.color.doc_primary : R.color.doc_text_hint));
        iconFemale.setColorFilter(ContextCompat.getColor(this, isFemale ? R.color.doc_primary : R.color.doc_text_hint));

        layoutMale.setBackgroundResource(!isFemale ? R.drawable.bg_gender_selected : R.drawable.bg_gender_unselected);
        txtMale.setTextColor(ContextCompat.getColor(this, !isFemale ? R.color.doc_primary : R.color.doc_text_hint));
        iconMale.setColorFilter(ContextCompat.getColor(this, !isFemale ? R.color.doc_primary : R.color.doc_text_hint));
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), IMAGE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).circleCrop().into(profileImageView);
        }
    }

    // --- HELPER UTILS ---
    private String getSafeValue(DataSnapshot snapshot, String key) {
        Object val = snapshot.child(key).getValue();
        return (val == null || val.toString().equals("null")) ? "" : val.toString();
    }

    private void showProgress(String msg) {
        if (progressDialog != null && !isFinishing()) {
            progressDialog.setMessage(msg);
            progressDialog.show();
        }
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        dismissProgress();
        super.onDestroy();
    }
}