package com.example.docconnect;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DoctorProfileCreationActivity extends AppCompatActivity {

    private ImageView ivProfileImage, ivLicensePreview;
    private ImageButton btnBack;
    private LinearLayout profilePlaceholder, licensePlaceholder;
    private TextInputEditText etFullName, etDob, etSpeciality, etClinicName, etClinicAddress, etRegNo, etCouncil;
    private AutoCompleteTextView actvGender;
    private MaterialCardView cvLicenseUpload, cvProfileContainer;
    private MaterialButton btnSubmit;
    private ProgressDialog progressDialog;

    private Uri profileImageUri, licenseImageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference doctorRef;
    private static final String UPLOAD_PRESET = "ds132213";

    private ActivityResultLauncher<Intent> profilePicker, licensePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_verification);

        mAuth = FirebaseAuth.getInstance();
        doctorRef = FirebaseDatabase.getInstance().getReference("doctors");

        initViews();
        setupGenderDropdown();
        setupImagePickers();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        ivLicensePreview = findViewById(R.id.ivLicensePreview);
        profilePlaceholder = findViewById(R.id.profilePlaceholder);
        licensePlaceholder = findViewById(R.id.licensePlaceholder);
        cvProfileContainer = findViewById(R.id.cvProfileContainer);

        etFullName = findViewById(R.id.etFullName);
        actvGender = findViewById(R.id.actvGender);
        etDob = findViewById(R.id.etDob);
        etRegNo = findViewById(R.id.etRegNo);
        etCouncil = findViewById(R.id.etCouncil);
        etSpeciality = findViewById(R.id.etSpeciality);
        etClinicName = findViewById(R.id.etClinicName);
        etClinicAddress = findViewById(R.id.etClinicAddress);

        cvLicenseUpload = findViewById(R.id.cvLicenseUpload);
        btnSubmit = findViewById(R.id.btnSubmitVerification);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void setupGenderDropdown() {
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        actvGender.setAdapter(adapter);
    }

    private void setupImagePickers() {
        profilePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                profileImageUri = result.getData().getData();
                profilePlaceholder.setVisibility(View.GONE);
                Glide.with(this).load(profileImageUri).circleCrop().into(ivProfileImage);
            }
        });

        licensePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                licenseImageUri = result.getData().getData();
                licensePlaceholder.setVisibility(View.GONE);
                ivLicensePreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(licenseImageUri).centerCrop().into(ivLicensePreview);
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        cvProfileContainer.setOnClickListener(v -> openGallery(profilePicker));
        cvLicenseUpload.setOnClickListener(v -> openGallery(licensePicker));
        etDob.setOnClickListener(v -> showDatePicker());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void openGallery(ActivityResultLauncher<Intent> launcher) {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcher.launch(i);
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) ->
                etDob.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void validateAndSubmit() {
        if (etFullName.getText().toString().trim().isEmpty() ||
                actvGender.getText().toString().trim().isEmpty() ||
                etDob.getText().toString().trim().isEmpty() ||
                etRegNo.getText().toString().trim().isEmpty() ||
                etCouncil.getText().toString().trim().isEmpty() ||
                etSpeciality.getText().toString().trim().isEmpty() ||
                etClinicName.getText().toString().trim().isEmpty() ||
                etClinicAddress.getText().toString().trim().isEmpty() ||
                profileImageUri == null || licenseImageUri == null) {

            Toast.makeText(this, "Please fill all fields and upload images", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImagesChained();
    }

    private void uploadImagesChained() {
        progressDialog.setMessage("Uploading Profile Photo...");
        progressDialog.show();
        btnSubmit.setEnabled(false);

        String uid = mAuth.getUid();

        MediaManager.get().upload(profileImageUri).unsigned(UPLOAD_PRESET).callback(new UploadCallback() {
            @Override
            public void onSuccess(String requestId, Map resultData) {
                String profileUrl = resultData.get("secure_url").toString();
                uploadLicense(uid, profileUrl);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                handleError("Profile Upload Failed: " + error.getDescription());
            }

            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void uploadLicense(String uid, String profileUrl) {
        progressDialog.setMessage("Uploading License...");

        MediaManager.get().upload(licenseImageUri).unsigned(UPLOAD_PRESET).callback(new UploadCallback() {
            @Override
            public void onSuccess(String requestId, Map resultData) {
                String licenseUrl = resultData.get("secure_url").toString();
                saveToFirebase(uid, profileUrl, licenseUrl);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                handleError("License Upload Failed: " + error.getDescription());
            }

            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveToFirebase(String uid, String pUrl, String lUrl) {
        progressDialog.setMessage("Finalizing Profile...");

        // ENSURE ALL VALUES ARE STRINGS (Prevents Long to String Crash)
        Map<String, Object> doc = new HashMap<>();
        doc.put("fullName", String.valueOf(etFullName.getText().toString().trim()));
        doc.put("gender", String.valueOf(actvGender.getText().toString().trim()));
        doc.put("dob", String.valueOf(etDob.getText().toString().trim()));
        doc.put("regNo", String.valueOf(etRegNo.getText().toString().trim()));
        doc.put("council", String.valueOf(etCouncil.getText().toString().trim()));
        doc.put("speciality", String.valueOf(etSpeciality.getText().toString().trim()));
        doc.put("clinicName", String.valueOf(etClinicName.getText().toString().trim()));
        doc.put("clinicAddress", String.valueOf(etClinicAddress.getText().toString().trim()));
        doc.put("profileImageUrl", pUrl);
        doc.put("licenseImageUrl", lUrl);
        doc.put("status", "Pending");
        doc.put("isProfileCompleted", true);

        doctorRef.child(uid).setValue(doc).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(this, "Application Submitted Successfully!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, DoctorVerificationWaitingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                handleError("Firebase Error: " + task.getException().getMessage());
            }
        });
    }

    private void handleError(String message) {
        progressDialog.dismiss();
        btnSubmit.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Please complete your profile to continue", Toast.LENGTH_SHORT).show();
    }
}