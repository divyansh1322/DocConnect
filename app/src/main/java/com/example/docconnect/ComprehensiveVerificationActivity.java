package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * COMPREHENSIVE VERIFICATION ACTIVITY
 * Cleaned and Bug-Fixed version.
 * Addresses MIUI system quirks and prevents image decoding crashes.
 */
public class ComprehensiveVerificationActivity extends AppCompatActivity {

    private static final String TAG = "VerifyActivity";

    // UI UI Components
    private ImageView ivProfile, ivLicenseImage;
    private TextView tvDoctorName, tvSpeciality, tvClinicName, tvClinicAddress, tvCouncil, tvRegNo, tvGender, tvDob, tvAdminBadge;
    private MaterialButton btnApprove, btnReject;

    // Firebase
    private DatabaseReference doctorRef;
    private String doctorId;
    private ValueEventListener doctorListener;

    // Guard flag to prevent Transition Mismatch errors and double-clicks
    private boolean isActionPending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Defensive check for Activity inflation (Fixes MIUI specific crashes)
        try {
            setContentView(R.layout.activity_comprehensive_verification);
        } catch (Exception e) {
            Log.e(TAG, "Inflation failed: " + e.getMessage());
            finish();
            return;
        }

        // 1. Get Intent Data with validation
        doctorId = getIntent().getStringExtra("doctorId");
        if (doctorId == null || doctorId.trim().isEmpty()) {
            Toast.makeText(this, "Error: Invalid Doctor ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Initialize Firebase Reference
        doctorRef = FirebaseDatabase.getInstance().getReference("doctors").child(doctorId);

        initViews();
        setupActions();
    }

    private void initViews() {
        ivProfile = findViewById(R.id.ivDoctorProfile);
        ivLicenseImage = findViewById(R.id.ivLicenseImage);
        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvSpeciality = findViewById(R.id.tvDoctorSpeciality);
        tvRegNo = findViewById(R.id.tvRegNo);
        tvClinicName = findViewById(R.id.tvDetailClinicName);
        tvClinicAddress = findViewById(R.id.tvDetailClinicAddress);
        tvCouncil = findViewById(R.id.tvDetailCouncil);
        tvGender = findViewById(R.id.tvDetailGender);
        tvDob = findViewById(R.id.tvDetailDob);
        tvAdminBadge = findViewById(R.id.tvAdminBadge);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);

        // Safe Back Button logic
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Load data in onStart so it refreshes if admin returns from LicenseDetailActivity
        loadDoctorData();
    }

    private void loadDoctorData() {
        doctorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Ensure Activity is still active before updating UI
                if (!snapshot.exists() || isFinishing() || isDestroyed()) return;

                try {
                    // Extract data with fallback values to prevent NullPointer on setText
                    String name = snapshot.child("fullName").getValue(String.class);
                    String spec = snapshot.child("speciality").getValue(String.class);
                    String clinic = snapshot.child("clinicName").getValue(String.class);
                    String addr = snapshot.child("clinicAddress").getValue(String.class);
                    String council = snapshot.child("council").getValue(String.class);
                    String regNo = snapshot.child("regNo").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String dob = snapshot.child("dob").getValue(String.class);

                    tvDoctorName.setText(name != null ? name : "N/A");
                    tvSpeciality.setText(spec != null ? spec : "N/A");
                    tvClinicName.setText(clinic != null ? clinic : "N/A");
                    tvClinicAddress.setText(addr != null ? addr : "N/A");
                    tvCouncil.setText(council != null ? council : "N/A");
                    tvGender.setText(gender != null ? gender : "N/A");
                    tvDob.setText(dob != null ? dob : "N/A");
                    tvRegNo.setText(regNo != null && !regNo.isEmpty() ? "Reg No. " + regNo : "Reg No. N/A");

                    // LOAD PROFILE IMAGE
                    Glide.with(getApplicationContext())
                            .load(snapshot.child("profileImageUrl").getValue(String.class))
                            .placeholder(R.drawable.ic_doctor)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .circleCrop()
                            .into(ivProfile);

                    // LOAD LICENSE IMAGE (Downsampled to fix SkJpegCodec memory errors)
                    String licenseUrl = snapshot.child("licenseImageUrl").getValue(String.class);
                    Glide.with(getApplicationContext())
                            .load(licenseUrl)
                            .placeholder(R.drawable.ic_verify)
                            .override(1000, 1000) // Fixes "Image decoding logging dropped" by reducing size
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(ivLicenseImage);

                    // License Click Detail
                    ivLicenseImage.setOnClickListener(v -> {
                        if (licenseUrl != null && !isActionPending) {
                            Intent intent = new Intent(ComprehensiveVerificationActivity.this, LicenseDetailActivity.class);
                            intent.putExtra("licenseUrl", licenseUrl);
                            startActivity(intent);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Data mapping failed: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Error: " + error.getMessage());
            }
        };
        doctorRef.addValueEventListener(doctorListener);
    }

    private void setupActions() {
        // We use "verified" and "rejected" as standard status strings
        btnApprove.setOnClickListener(v -> performUpdate("verified"));
        btnReject.setOnClickListener(v -> performUpdate("rejected"));
    }

    private void performUpdate(String newStatus) {
        if (isActionPending) return;
        isActionPending = true;

        // Disable buttons immediately to prevent the "Transition Mismatch" from your logs
        btnApprove.setEnabled(false);
        btnReject.setEnabled(false);

        doctorRef.child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (isFinishing()) return;

                    // Update Badge UI immediately
                    if ("verified".equals(newStatus)) {
                        tvAdminBadge.setText("VERIFIED");
                        tvAdminBadge.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
                    } else {
                        tvAdminBadge.setText("REJECTED");
                        tvAdminBadge.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
                    }

                    Toast.makeText(this, "Doctor " + newStatus + " successfully", Toast.LENGTH_SHORT).show();

                    // 1.2s delay so Admin sees the visual change before the screen closes
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1200);
                })
                .addOnFailureListener(e -> {
                    isActionPending = false;
                    btnApprove.setEnabled(true);
                    btnReject.setEnabled(true);
                    Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove listener to prevent memory leaks and background crashes
        if (doctorRef != null && doctorListener != null) {
            doctorRef.removeEventListener(doctorListener);
        }
    }
}