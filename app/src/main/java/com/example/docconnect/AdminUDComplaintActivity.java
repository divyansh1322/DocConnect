package com.example.docconnect;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.*;

/**
 * AdminUDComplaintActivity: A2Z Management for Admin.
 * Handles: Viewing details, resolving tickets, and fetching profile emails dynamically.
 */
public class AdminUDComplaintActivity extends AppCompatActivity {

    private String ticketId, userId, userType, nodeKey, currentEvidenceUrl;
    private DatabaseReference ticketRef;

    private TextView tvTargetName, tvDescription, tvUserEmail, tvToolbarTitle;
    private ImageView ivUserImage, ivEvidencePreview;
    private Chip chipStatus;
    private MaterialButton btnResolve, btnDelete;
    private View btnViewFullSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ud_complaint);

        // 1. Get Intent Extras
        ticketId = getIntent().getStringExtra("ticketId");
        userId = getIntent().getStringExtra("userId");
        userType = getIntent().getStringExtra("userType"); // "users" or "doctors"
        nodeKey = getIntent().getStringExtra("nodeKey");   // "report_issue" or "support_tickets"

        initViews();

        if (userId != null && ticketId != null) {
            // Path: /users/UID/report_issue/TicketID OR /doctors/UID/support_tickets/TicketID
            ticketRef = FirebaseDatabase.getInstance().getReference(userType)
                    .child(userId).child(nodeKey).child(ticketId);
            loadData();
        } else {
            Toast.makeText(this, "Error: Data path missing", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupButtons();
    }

    private void initViews() {
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvTargetName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvDescription = findViewById(R.id.tvIssueDescription);
        ivUserImage = findViewById(R.id.ivUserImage);
        ivEvidencePreview = findViewById(R.id.ivEvidencePreview);
        chipStatus = findViewById(R.id.chipStatus);
        btnResolve = findViewById(R.id.btnResolve);
        btnDelete = findViewById(R.id.btnDelete);
        btnViewFullSize = findViewById(R.id.btnViewFullSize);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Dynamic Title based on type
        if (userType != null) {
            String title = userType.equalsIgnoreCase("doctors") ? "Doctor Complaint" : "User Complaint";
            tvToolbarTitle.setText(title);
        }
    }

    private void loadData() {
        ticketRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || isFinishing()) return;

                // Basic Ticket Info
                String name = snapshot.child("fullName").getValue(String.class);
                String desc = snapshot.child("description").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                String profileImg = snapshot.child("profileImageUrl").getValue(String.class);
                currentEvidenceUrl = snapshot.child("evidenceUrl").getValue(String.class);

                // UI Assignment
                tvTargetName.setText(name != null ? name : "Unknown Name");
                tvDescription.setText(desc != null ? desc : "No description provided.");

                // Email Handling Logic
                String email = snapshot.child("email").getValue(String.class);
                if (email == null || email.isEmpty()) {
                    fetchEmailFromUserProfile(); // Go deeper if not in ticket
                } else {
                    tvUserEmail.setText(email);
                }

                // Status Logic
                if (status != null) {
                    chipStatus.setText(status.toUpperCase());
                    if (status.equalsIgnoreCase("resolved")) {
                        btnResolve.setEnabled(false);
                        btnResolve.setText("Resolved");
                        btnResolve.setAlpha(0.6f);
                    }
                }

                // Image Loading
                Glide.with(AdminUDComplaintActivity.this)
                        .load(profileImg).placeholder(R.drawable.ic_person)
                        .circleCrop().into(ivUserImage);

                if (currentEvidenceUrl != null && !currentEvidenceUrl.isEmpty()) {
                    ivEvidencePreview.setVisibility(View.VISIBLE);
                    btnViewFullSize.setVisibility(View.VISIBLE);
                    Glide.with(AdminUDComplaintActivity.this).load(currentEvidenceUrl).into(ivEvidencePreview);
                } else {
                    ivEvidencePreview.setVisibility(View.GONE);
                    btnViewFullSize.setVisibility(View.GONE);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Fetches the email from the actual user/doctor profile if it's not saved in the ticket.
     */
    private void fetchEmailFromUserProfile() {
        FirebaseDatabase.getInstance().getReference(userType).child(userId).child("email")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            tvUserEmail.setText(snapshot.getValue(String.class));
                        } else {
                            tvUserEmail.setText("Email not found");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupButtons() {
        // Resolve: Updates status to 'resolved'
        btnResolve.setOnClickListener(v -> {
            ticketRef.child("status").setValue("resolved")
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Marked as Resolved", Toast.LENGTH_SHORT).show());
        });

        // Delete (Reject): Completely removes the complaint ticket
        btnDelete.setOnClickListener(v -> {
            ticketRef.removeValue().addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Complaint Removed", Toast.LENGTH_SHORT).show();
                finish();
            });
        });

        // Full Size Preview
        btnViewFullSize.setOnClickListener(v -> {
            if (currentEvidenceUrl != null) showImagePreview(currentEvidenceUrl);
        });
    }

    private void showImagePreview(String url) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.activity_license_detail); // Using your existing preview layout
        ImageView ivFull = dialog.findViewById(R.id.photoView);
        View close = dialog.findViewById(R.id.btnClose);

        if (close != null) close.setOnClickListener(view -> dialog.dismiss());
        Glide.with(this).load(url).into(ivFull);
        dialog.show();
    }
}