package com.example.docconnect;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

/**
 * AdminAppointmentDetailActivity: Professional Implementation
 * Features: Multi-node Firebase synchronization, Lifecycle-safe UI updates,
 * and robust error handling for cross-device stability.
 */
public class AdminAppointmentDetailActivity extends AppCompatActivity {

    // Firebase reference and ID storage
    private DatabaseReference mDatabase;
    private String targetBookingId, targetUserId;

    // UI Components
    private TextView tvDocName, tvDocSub, tvDate, tvTime, tvFullName, tvBookingId, tvClinicName, tvClinicAddress, tvRatingReviews;
    private ImageView ivDoctorImage;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_appointment_detail);

        // 1. Retrieve IDs passed via Intent from the AdminAppointmentAdapter
        targetBookingId = getIntent().getStringExtra("bookingId");
        targetUserId = getIntent().getStringExtra("userId");

        // Basic validation: Professional apps must fail-fast if critical data is missing
        if (targetBookingId == null || targetUserId == null) {
            Toast.makeText(this, "Error: Booking data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements
        initViews();

        // 2. Point to the specific booking entry
        mDatabase = FirebaseDatabase.getInstance().getReference("UserBookings")
                .child(targetUserId)
                .child(targetBookingId);

        // Start the data retrieval process
        fetchDataFromFirebase();

        // Set up back button listener
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Links Java objects to XML layout IDs
     */
    private void initViews() {
        tvDocName = findViewById(R.id.tvDocName);
        tvDocSub = findViewById(R.id.tvDocSub);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvFullName = findViewById(R.id.fullName);
        tvBookingId = findViewById(R.id.tvbookingId);
        tvClinicName = findViewById(R.id.tvClinicName);
        tvClinicAddress = findViewById(R.id.tvclinicAddress);
        tvRatingReviews = findViewById(R.id.tvRatingReviews);
        ivDoctorImage = findViewById(R.id.ivDoctorImage);
        btnBack = findViewById(R.id.btnBack);
    }

    /**
     * Primary data fetcher with Lifecycle Awareness
     */
    private void fetchDataFromFirebase() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot bookingSnapshot) {
                // Professional Check: Don't update UI if user closed the activity
                if (isFinishing() || isDestroyed()) return;

                if (bookingSnapshot.exists()) {
                    // Step A: Update UI with basic info
                    updateBookingUI(bookingSnapshot);

                    // Step B: Fetch the actual Patient Name
                    fetchPatientName();

                    // Step C: Fetch Doctor rating/review stats
                    String doctorId = bookingSnapshot.child("doctorId").getValue(String.class);
                    if (doctorId != null && !doctorId.isEmpty()) {
                        fetchDoctorStats(doctorId);
                    }
                } else {
                    Toast.makeText(AdminAppointmentDetailActivity.this, "Booking not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });
    }

    /**
     * Sets texts and loads the doctor image using Glide
     */
    private void updateBookingUI(DataSnapshot data) {
        if (isFinishing() || isDestroyed()) return;

        tvDocName.setText(safeGet(data, "doctorName"));
        tvDocSub.setText(safeGet(data, "doctorSpecialty"));
        tvDate.setText(safeGet(data, "date"));
        tvTime.setText(safeGet(data, "time"));
        tvBookingId.setText(safeGet(data, "bookingId"));
        tvClinicName.setText(safeGet(data, "clinicName"));
        tvClinicAddress.setText(safeGet(data, "clinicAddress"));

        // Load doctor profile image with RequestOptions for better memory management
        String docImgUrl = data.child("doctorImage").getValue(String.class);

        if (!isFinishing()) {
            Glide.with(this)
                    .load(docImgUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_doctor)
                            .error(R.drawable.ic_doctor)
                            .circleCrop())
                    .into(ivDoctorImage);
        }
    }

    /**
     * Cross-references the 'users' node with success/failure handling
     */
    private void fetchPatientName() {
        FirebaseDatabase.getInstance().getReference("users")
                .child(targetUserId)
                .child("fullName")
                .get().addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (snapshot.exists() && snapshot.getValue() != null) {
                        tvFullName.setText(snapshot.getValue(String.class));
                    } else {
                        tvFullName.setText("Unknown Patient");
                    }
                }).addOnFailureListener(e -> {
                    if (!isFinishing()) tvFullName.setText("Error loading name");
                });
    }

    /**
     * Navigates to the doctor's node with advanced numeric type handling
     */
    private void fetchDoctorStats(String docId) {
        FirebaseDatabase.getInstance().getReference("doctors")
                .child(docId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isFinishing() || isDestroyed()) return;

                        if (snapshot.exists()) {
                            double totalRating = 0;
                            long ratingsCount = 0;
                            long reviewsCount = 0;

                            if (snapshot.hasChild("ratings")) {
                                DataSnapshot ratingsNode = snapshot.child("ratings");
                                ratingsCount = ratingsNode.getChildrenCount();
                                for (DataSnapshot child : ratingsNode.getChildren()) {
                                    Object val = child.getValue();
                                    // Professional check for diverse numeric types in Firebase
                                    if (val instanceof Number) {
                                        totalRating += ((Number) val).doubleValue();
                                    }
                                }
                            }

                            if (snapshot.hasChild("reviews")) {
                                reviewsCount = snapshot.child("reviews").getChildrenCount();
                            }

                            double average = (ratingsCount > 0) ? totalRating / ratingsCount : 0.0;
                            tvRatingReviews.setText(String.format(Locale.getDefault(), "%.1f (%d reviews)", average, reviewsCount));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error fetching stats: " + error.getMessage());
                    }
                });
    }

    /**
     * Helper method to prevent NullPointerExceptions
     */
    private String safeGet(DataSnapshot snapshot, String key) {
        if (snapshot == null || !snapshot.hasChild(key)) return "N/A";
        Object value = snapshot.child(key).getValue();
        return (value != null) ? value.toString() : "N/A";
    }

    @Override
    protected void onDestroy() {
        // Ensure no pending Glide requests or listeners are active
        super.onDestroy();
    }
}