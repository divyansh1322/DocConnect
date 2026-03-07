package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ADMIN DASHBOARD - DocConnect Professional
 * Logic: Real-time aggregation of Doctors, Users, Bookings, and Complaints.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    // --- UI ELEMENTS ---
    private TextView tvAppointments, tvCancelled, tvMissed;
    private TextView tvPendingDoctorCount, tvComplaintCount, tvFlaggedCount;
    private TextView tvRole;
    private MaterialCardView cardPendingDoctors, cardComplaintCountBox, cardFlaggedDoctors;
    private MaterialCardView cardVerify, cardUsers, cardAppointments, cardComplaints;
    private MaterialButton btnLogout;

    // --- FIREBASE ---
    private DatabaseReference dbBookings, dbDoctors, dbUsers, dbAdmins;
    private FirebaseAuth mAuth;
    private ValueEventListener roleListener, statsListener, bookingsListener;

    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        setupFirebase();
        initializeViews();
        setupClickListeners();

        if (mAuth.getCurrentUser() != null) {
            fetchAdminRole();
            fetchTodaySnapshot();
            fetchDoctorAndComplaintStats();
        } else {
            forceLogout();
        }
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        dbBookings = FirebaseDatabase.getInstance().getReference("UserBookings");
        dbDoctors = FirebaseDatabase.getInstance().getReference("doctors");
        dbUsers = FirebaseDatabase.getInstance().getReference("users");
        dbAdmins = FirebaseDatabase.getInstance().getReference("admins");
    }

    private void initializeViews() {
        tvRole = findViewById(R.id.tvRole);
        btnLogout = findViewById(R.id.btnLogout);
        tvPendingDoctorCount = findViewById(R.id.tvPendingDoctorCount);
        tvComplaintCount = findViewById(R.id.tvComplaintCount);
        tvFlaggedCount = findViewById(R.id.tvFlaggedCount);
        cardPendingDoctors = findViewById(R.id.cardPendingDoctors);
        cardComplaintCountBox = findViewById(R.id.cardComplaintCountBox);
        cardFlaggedDoctors = findViewById(R.id.cardFlaggedDoctors);
        tvAppointments = findViewById(R.id.tvCountAppointments);
        tvCancelled = findViewById(R.id.tvCountCancelled);
        tvMissed = findViewById(R.id.tvMissedUsers);
        cardVerify = findViewById(R.id.cardDoctorVerification);
        cardUsers = findViewById(R.id.cardUserManagement);
        cardAppointments = findViewById(R.id.cardAppointments);
        cardComplaints = findViewById(R.id.cardComplaints);
    }

    private void setupClickListeners() {
        cardPendingDoctors.setOnClickListener(v -> navigateTo(DoctorVerificationRecordActivity.class));
        cardFlaggedDoctors.setOnClickListener(v -> navigateTo(DoctorVerificationRecordActivity.class));
        cardComplaintCountBox.setOnClickListener(v -> navigateTo(ComplaintsRecordActivity.class));
        cardVerify.setOnClickListener(v -> navigateTo(DoctorVerificationRecordActivity.class));
        cardUsers.setOnClickListener(v -> navigateTo(UserManagementRecordActivity.class));
        cardAppointments.setOnClickListener(v -> navigateTo(AppointmentsRecordActivity.class));
        cardComplaints.setOnClickListener(v -> navigateTo(ComplaintsRecordActivity.class));

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    // -------------------- LOGOUT LOGIC --------------------

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this, R.style.RoundedAlertDialog)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to exit the Admin Dashboard?")
                .setPositiveButton("Logout", (d, w) -> forceLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void forceLogout() {
        // Since this is an Activity, we check !isFinishing()
        if (!isFinishing()) {
            // 1. Show Toast
            Toast.makeText(this, "Logout Successfully", Toast.LENGTH_SHORT).show();

            // 2. Firebase Sign out
            mAuth.signOut();

            // 3. Navigation
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // 4. Close Activity
            finish();
        }
    }

    // -------------------- DATA FETCHING --------------------

    private void fetchAdminRole() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        roleListener = dbAdmins.child(uid).child("role")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isFinishing()) return;
                        if (!snapshot.exists()) {
                            forceLogout();
                            return;
                        }
                        String role = snapshot.getValue(String.class);
                        if (role != null) {
                            tvRole.setText(" ROLE: " + role.toUpperCase(Locale.US));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchDoctorAndComplaintStats() {
        statsListener = dbDoctors.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                int pending = 0, rejected = 0;
                long newDoctorTickets = 0;

                for (DataSnapshot doc : snapshot.getChildren()) {
                    String status = doc.child("status").getValue(String.class);
                    if ("pending".equalsIgnoreCase(status)) pending++;
                    if ("rejected".equalsIgnoreCase(status)) rejected++;

                    if (doc.hasChild("support_ticket")) {
                        String ticketStatus = doc.child("support_ticket").child("status").getValue(String.class);
                        if ("new".equalsIgnoreCase(ticketStatus)) newDoctorTickets++;
                    }
                }

                updateText(tvPendingDoctorCount, String.valueOf(pending));
                updateText(tvFlaggedCount, String.valueOf(rejected));

                final long finalDoctorTickets = newDoctorTickets;
                dbUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        if (isFinishing()) return;
                        long newUserTickets = 0;
                        for (DataSnapshot user : userSnapshot.getChildren()) {
                            if (user.hasChild("report_issue")) {
                                String issueStatus = user.child("report_issue").child("status").getValue(String.class);
                                if ("new".equalsIgnoreCase(issueStatus)) newUserTickets++;
                            }
                        }
                        updateText(tvComplaintCount, String.valueOf(finalDoctorTickets + newUserTickets));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchTodaySnapshot() {
        bookingsListener = dbBookings.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                int today = 0, cancelled = 0, missed = 0;

                for (DataSnapshot user : snapshot.getChildren()) {
                    for (DataSnapshot booking : user.getChildren()) {
                        String date = booking.child("date").getValue(String.class);
                        String status = booking.child("status").getValue(String.class);

                        if (date == null || status == null || !todayDate.equals(date)) continue;

                        switch (status.toUpperCase(Locale.US)) {
                            case "UPCOMING":
                            case "COMPLETED": today++; break;
                            case "CANCELLED": cancelled++; break;
                            case "MISSED": missed++; break;
                        }
                    }
                }
                updateText(tvAppointments, String.valueOf(today));
                updateText(tvCancelled, String.valueOf(cancelled));
                updateText(tvMissed, String.valueOf(missed));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateText(TextView view, String value) {
        if (view != null && !isFinishing()) {
            view.setText(value);
        }
    }

    private void navigateTo(Class<?> target) {
        startActivity(new Intent(this, target));
    }

    @Override
    protected void onDestroy() {
        if (dbAdmins != null && roleListener != null) dbAdmins.removeEventListener(roleListener);
        if (dbDoctors != null && statsListener != null) dbDoctors.removeEventListener(statsListener);
        if (dbBookings != null && bookingsListener != null) dbBookings.removeEventListener(bookingsListener);
        super.onDestroy();
    }
}