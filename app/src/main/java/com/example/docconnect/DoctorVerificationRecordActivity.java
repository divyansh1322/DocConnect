package com.example.docconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Administrative activity used to track, verify, and manage doctor registrations.
 * Displays a list of all doctors and provides high-level statistics (Pending, Approved, Rejected).
 */
public class DoctorVerificationRecordActivity extends AppCompatActivity {

    // UI Components for the list and statistics dashboard
    private RecyclerView rvDoctors;
    private LinearLayout layoutEmptyState;
    private TextView tvPendingTotal, tvApprovedTotal, tvRejectedTotal, tvToolbarTitle;
    private ImageView ivbtnBack;

    // Data handling
    private List<VerificationDoctorModel> doctorList;
    private VerificationDoctorAdapter adapter;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_verification_record);

        // 1. Initialize Views
        rvDoctors = findViewById(R.id.rvDoctors);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvPendingTotal = findViewById(R.id.tvPendingTotal);
        tvApprovedTotal = findViewById(R.id.tvApprovedTotal);
        tvRejectedTotal = findViewById(R.id.tvRejectedTotal);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        ivbtnBack = findViewById(R.id.ivbtnBack);

        // 2. Setup RecyclerView with a functional interface listener for status updates
        doctorList = new ArrayList<>();
        // The adapter is passed a method reference to handle button clicks inside the list items
        adapter = new VerificationDoctorAdapter(doctorList, this::updateDoctorStatus);

        rvDoctors.setLayoutManager(new LinearLayoutManager(this));
        rvDoctors.setAdapter(adapter);

        // 3. Database Reference targeting the root "doctors" node
        dbRef = FirebaseDatabase.getInstance().getReference("doctors");

        // 4. Load Data: Attach the real-time listener
        loadDoctorsAndStats();

        ivbtnBack.setOnClickListener(v -> finish());
    }

    /**
     * Updates a doctor's verification status in Firebase.
     * This is triggered from the Adapter when an admin clicks 'Approve' or 'Reject'.
     */
    private void updateDoctorStatus(String doctorId, String newStatus) {
        if (doctorId == null) return;

        // Path: doctors -> {doctorId} -> status
        dbRef.child(doctorId).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Doctor " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Fetches all doctor nodes and calculates counts for the statistics header.
     * Uses ValueEventListener for real-time dashboard updates.
     */
    private void loadDoctorsAndStats() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Temporary counters for the dashboard
                int pendingCount = 0;
                int approvedCount = 0;
                int rejectedCount = 0;

                doctorList.clear();

                // Iterate through every doctor in the database
                for (DataSnapshot data : snapshot.getChildren()) {
                    VerificationDoctorModel doc = data.getValue(VerificationDoctorModel.class);
                    if (doc != null) {
                        // Set the ID manually from the Firebase key
                        doc.setId(data.getKey());
                        String status = doc.getStatus();

                        // Add every doctor to the master list for display
                        doctorList.add(doc);

                        // Logic for incrementing statistics based on status string
                        if (status == null || "pending".equalsIgnoreCase(status)) {
                            pendingCount++;
                        } else if ("approved".equalsIgnoreCase(status) || "verified".equalsIgnoreCase(status)) {
                            approvedCount++;
                        } else if ("rejected".equalsIgnoreCase(status)) {
                            rejectedCount++;
                        }
                    }
                }

                // Push calculated stats to the UI
                updateUI(pendingCount, approvedCount, rejectedCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database read errors here
            }
        });
    }

    /**
     * Refresh the UI components: Toggle empty states, update text counts, and notify adapter.
     */
    private void updateUI(int pending, int approved, int rejected) {
        if (doctorList.isEmpty()) {
            rvDoctors.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvDoctors.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }

        // Notify adapter that data changed to refresh the UI list items
        adapter.notifyDataSetChanged();

        // Update the numeric badges in the dashboard header
        tvPendingTotal.setText(String.valueOf(pending));
        tvApprovedTotal.setText(String.valueOf(approved));
        tvRejectedTotal.setText(String.valueOf(rejected));

        // Update toolbar title with a cumulative total
        tvToolbarTitle.setText("Doctor Records (" + (pending + approved + rejected) + " Total)");
    }
}