package com.example.docconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

/**
 * AdminUserDetailsActivity
 * ------------------------
 * Manages administrative view of a specific user.
 * Features: Profile data (including exact joinedDate), Appointment stats,
 * Complaint tracking, and Real-time Blocking/Unblocking.
 */
public class AdminUserDetailsActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvName, tvEmail, tvPhone, tvJoinDate, tvStatusBadge;
    private TextView tvApptCount, tvComplaintCount, tvCancelRate;
    private ShapeableImageView imgProfile;
    private MaterialButton btnBlockToggle, btnViewAppointments, btnViewComplaints;
    private ImageView btnBack;

    // Firebase References
    private DatabaseReference mDatabase;
    private String selectedUserId;

    // Listener Handles (Stored for clean removal in onDestroy)
    private ValueEventListener userListener, statsListener, complaintsListener;
    private DatabaseReference userRef, statsRef, complaintsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_profile);

        // Initialize Firebase Global Reference
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Retrieve UID passed from the previous list activity
        selectedUserId = getIntent().getStringExtra("userId");

        // Safety validation to prevent crashes if ID is missing
        if (selectedUserId == null) {
            Toast.makeText(this, "Error: User ID not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();

        // Start Real-time Data Sync
        loadUserData();       // Fetches Profile, Status, and exact joinedDate
        loadBookingStats();   // Fetches Completed & Cancelled Counts
        loadComplaintStats(); // Fetches Report Issue Count

        // Click Listeners
        btnBlockToggle.setOnClickListener(v -> toggleUserStatus());
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Bind XML IDs to Java Objects.
     * Note: IDs remain exactly as they appear in your layout.
     */
    private void initViews() {
        tvName = findViewById(R.id.user_name_text);
        tvStatusBadge = findViewById(R.id.status_badge);
        imgProfile = findViewById(R.id.img_profile);
        btnBack = findViewById(R.id.btn_back);

        tvEmail = findViewById(R.id.email_val);
        tvPhone = findViewById(R.id.phone_val);
        tvJoinDate = findViewById(R.id.join_val);

        tvApptCount = findViewById(R.id.stat_appt_val);
        tvComplaintCount = findViewById(R.id.stat_issue_val);
        tvCancelRate = findViewById(R.id.stat_cancel_val);

        btnBlockToggle = findViewById(R.id.btn_block_user_action);
        btnViewAppointments = findViewById(R.id.btn_view_appts);
        btnViewComplaints = findViewById(R.id.btn_view_complaints);
    }

    /**
     * [PROFILE LOGIC]
     * Syncs general info from the 'users' node including the exact joinedDate.
     */
    private void loadUserData() {
        userRef = mDatabase.child("users").child(selectedUserId);
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists()) {
                    // Update Text Fields with null checks
                    tvName.setText(snapshot.child("fullName").getValue(String.class));
                    tvEmail.setText(snapshot.child("email").getValue(String.class));
                    tvPhone.setText(snapshot.child("phone").getValue(String.class));

                    // GET EXACT joinedDate saved during registration
                    String dateJoined = snapshot.child("joinedDate").getValue(String.class);
                    tvJoinDate.setText(dateJoined != null ? dateJoined : "Not Available");

                    // Load Profile Image using Glide
                    String photoUrl = snapshot.child("profilePhotoUrl").getValue(String.class);
                    Glide.with(getApplicationContext())
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(imgProfile);

                    // Update Status Badge (Active/Blocked)
                    String status = snapshot.child("status").getValue(String.class);
                    updateStatusUI(status);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Profile sync failed", error.toException());
            }
        };
        userRef.addValueEventListener(userListener);
    }

    /**
     * [BOOKING LOGIC]
     * Aggregates counts for COMPLETED and CANCELLED statuses.
     */
    private void loadBookingStats() {
        statsRef = mDatabase.child("UserBookings").child(selectedUserId);
        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                int completed = 0;
                int cancelled = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    if (status != null) {
                        if (status.equalsIgnoreCase("COMPLETED")) {
                            completed++;
                        } else if (status.equalsIgnoreCase("CANCELLED")) {
                            cancelled++;
                        }
                    }
                }

                tvApptCount.setText(String.format(Locale.getDefault(), "%02d", completed));
                tvCancelRate.setText(String.format(Locale.getDefault(), "%02d", cancelled));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Booking stats sync failed", error.toException());
            }
        };
        statsRef.addValueEventListener(statsListener);
    }

    /**
     * [COMPLAINT LOGIC]
     * Counts entries in 'users/{userId}/report_issue'.
     */
    private void loadComplaintStats() {
        complaintsRef = mDatabase.child("users").child(selectedUserId).child("report_issue");
        complaintsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                long count = 0;
                if (snapshot.exists()) {
                    count = snapshot.getChildrenCount();
                }
                tvComplaintCount.setText(String.format(Locale.getDefault(), "%02d", count));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        complaintsRef.addValueEventListener(complaintsListener);
    }

    /**
     * Dynamically adjusts UI colors and button text based on 'status' key.
     */
    private void updateStatusUI(String status) {
        if (status == null) status = "Active";
        tvStatusBadge.setText(status.toUpperCase());

        if (status.equalsIgnoreCase("Blocked")) {
            tvStatusBadge.setTextColor(Color.parseColor("#E74C3C")); // Red text
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_blocked);
            btnBlockToggle.setText("Unblock User");
            btnBlockToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2ECC71"))); // Green Button
        } else {
            tvStatusBadge.setTextColor(Color.parseColor("#2ECC71")); // Green text
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active);
            btnBlockToggle.setText("Block User");
            btnBlockToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E74C3C"))); // Red Button
        }
    }

    /**
     * Updates Firebase 'status' node for real-time moderation.
     */
    private void toggleUserStatus() {
        String currentStatus = tvStatusBadge.getText().toString();
        String newStatus = currentStatus.equalsIgnoreCase("BLOCKED") ? "Active" : "Blocked";

        mDatabase.child("users").child(selectedUserId).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User is now " + newStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show());
    }

    /**
     * Cleanup listeners to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
        if (statsRef != null && statsListener != null) {
            statsRef.removeEventListener(statsListener);
        }
        if (complaintsRef != null && complaintsListener != null) {
            complaintsRef.removeEventListener(complaintsListener);
        }
    }
}