package com.example.docconnect;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * DoctorQualificationActivity: A2Z Optimized Performance Version.
 * Fixes: UI Hanging, Transition Mismatches, and Window Leakage.
 */
public class DoctorQualificationActivity extends AppCompatActivity {

    private static final String TAG = "DoctorQual";

    // --- UI COMPONENTS ---
    private ImageView btnBack;
    private ShapeableImageView imgProfile;
    private TextView tvDocName, tvDocRole;
    private EditText etBio, etExperience, etLicense, etQualifications;
    private MaterialButton btnSave;
    private ProgressDialog progressDialog;

    // --- FIREBASE & STATE ---
    private DatabaseReference doctorRef;
    private String currentUserId;
    private boolean isDataLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_qualification);

        // 1. SESSION & DB INITIALIZATION
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        doctorRef = FirebaseDatabase.getInstance().getReference("doctors").child(currentUserId);

        // 2. SETUP UI
        initViews();
        setupListeners();

        //

        // 3. PERFORMANCE SYNC (A2Z FIX)
        // Using postDelayed with a 300ms window ensures the hardware renderer finishes
        // the "Activity Open" transition before we touch the database.
        getWindow().getDecorView().postDelayed(() -> {
            if (!isFinishing()) {
                loadDoctorData();
            }
        }, 300);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgProfile = findViewById(R.id.imgProfile);
        tvDocName = findViewById(R.id.tvDocName);
        tvDocRole = findViewById(R.id.tvDocRole);
        etBio = findViewById(R.id.etBio);
        etExperience = findViewById(R.id.etExperience);
        etLicense = findViewById(R.id.etLicense);
        etQualifications = findViewById(R.id.etQualifications);
        btnSave = findViewById(R.id.btnSave);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Credentials...");
        progressDialog.setCancelable(false);
    }

    private void loadDoctorData() {
        if (isFinishing() || isDataLoading) return;

        isDataLoading = true;
        showProgress();

        doctorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isDataLoading = false;
                dismissProgress();

                if (!snapshot.exists() || isFinishing()) return;

                //

                // Extract with String sanitization
                String name = getSafeString(snapshot, "fullName", "Doctor");
                String role = getSafeString(snapshot, "speciality", "General Physician");
                String bio = getSafeString(snapshot, "bio", "");
                String exp = getSafeString(snapshot, "experienceSummary", "");
                String lic = getSafeString(snapshot, "licenseNumber", "");
                String qual = getSafeString(snapshot, "qualifications", "");
                String imageUrl = getSafeString(snapshot, "profileImageUrl", "");

                // Update UI - Wrapped in lifecycle check
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    tvDocName.setText(name);
                    tvDocRole.setText(role.toUpperCase());
                    etBio.setText(bio);
                    etExperience.setText(exp);
                    etLicense.setText(lic);
                    etQualifications.setText(qual);

                    if (!imageUrl.isEmpty()) {
                        Glide.with(DoctorQualificationActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_doctor)
                                .circleCrop()
                                .into(imgProfile);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isDataLoading = false;
                dismissProgress();
                Log.e(TAG, "Database Error: " + error.getMessage());
            }
        });
    }

    private void saveDoctorData() {
        String license = etLicense.getText().toString().trim();

        if (TextUtils.isEmpty(license)) {
            etLicense.setError("Medical License Required");
            return;
        }

        showProgress();
        btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("bio", etBio.getText().toString().trim());
        updates.put("experienceSummary", etExperience.getText().toString().trim());
        updates.put("licenseNumber", license);
        updates.put("qualifications", etQualifications.getText().toString().trim());

        // Perform Atomic Update
        doctorRef.updateChildren(updates).addOnCompleteListener(task -> {
            dismissProgress();
            if (task.isSuccessful() && !isFinishing()) {
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveDoctorData());
    }

    // --- LIFECYCLE SAFE HELPERS ---

    private String getSafeString(DataSnapshot snapshot, String key, String defaultValue) {
        Object value = snapshot.child(key).getValue();
        if (value == null || value.toString().equalsIgnoreCase("null")) {
            return defaultValue;
        }
        return value.toString().trim();
    }

    private void showProgress() {
        if (!isFinishing() && progressDialog != null && !progressDialog.isShowing()) {
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
        dismissProgress(); // Crucial to prevent WindowLeakedException
        super.onDestroy();
    }
}