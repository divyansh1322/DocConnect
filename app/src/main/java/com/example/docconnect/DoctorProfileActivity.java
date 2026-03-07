package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class DoctorProfileActivity extends AppCompatActivity {

    // --- UI Components ---
    private ImageView btnBack, btnShare, imgDoctor;
    private TextView tvName, tvSpeciality, tvFee, tvPatients, tvExperience,
            tvRating, tvAbout, tvLocation, tvClinicName, tvSeeAllReviews;
    private MaterialButton btnBook;

    // --- Review Card Components (From XML) ---
    private TextView tvReviewName, tvReviewDate, tvReviewText;

    // --- Data Variables ---
    private String doctorId;
    private String currentDoctorImageUrl;
    private String reviewCount = "0";
    private String averageRating = "0.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        initViews();
        receiveIntentData();
        setupClicks();

        if (doctorId != null) {
            fetchReviewsAndStats();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        imgDoctor = findViewById(R.id.imgDoctor);
        tvName = findViewById(R.id.tvName);
        tvSpeciality = findViewById(R.id.tvSpeciality);
        tvFee = findViewById(R.id.tvFee);
        tvSeeAllReviews = findViewById(R.id.tvSeeAllReviews);
        tvPatients = findViewById(R.id.tvPatients);
        tvExperience = findViewById(R.id.tvExperience);
        tvRating = findViewById(R.id.tvRating);
        tvAbout = findViewById(R.id.tvAboutD);
        tvLocation = findViewById(R.id.tvLocation);
        tvClinicName = findViewById(R.id.tvClinicName);
        btnBook = findViewById(R.id.btnBook);

        // Review Card IDs from your XML
        tvReviewName = findViewById(R.id.tvReviewName);
        tvReviewDate = findViewById(R.id.tvReviewDate);
        tvReviewText = findViewById(R.id.tvReviewText);
    }

    private void receiveIntentData() {
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "Profile data unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        doctorId = intent.getStringExtra("id");
        currentDoctorImageUrl = intent.getStringExtra("image");

        tvName.setText(safeString(intent.getStringExtra("name"), "Unknown Doctor"));
        tvSpeciality.setText(safeString(intent.getStringExtra("specialty"), "General Physician"));
        tvLocation.setText(safeString(intent.getStringExtra("clinicAddress"), "Address Not Available"));
        tvClinicName.setText(safeString(intent.getStringExtra("clinicName"), "Clinic Not Set"));
        tvAbout.setText(safeString(intent.getStringExtra("bio"), "No biography provided."));

        String fee = intent.getStringExtra("fee");
        tvFee.setText(fee != null ? fee + " Rs" : "N/A");

        String exp = intent.getStringExtra("experience");
        tvExperience.setText(exp != null ? exp + " Years" : "0 Years");

        String patients = intent.getStringExtra("patients");
        tvPatients.setText(patients != null ? patients : "0");

        if (!isFinishing()) {
            Glide.with(this)
                    .load(currentDoctorImageUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_doctor)
                            .error(R.drawable.ic_doctor)
                            .centerCrop())
                    .into(imgDoctor);
        }
    }

    /**
     * Calculates stats and retrieves the MOST RECENT review
     */
    private void fetchReviewsAndStats() {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("doctors")
                .child(doctorId)
                .child("reviews_ratings");

        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long totalReviews = snapshot.getChildrenCount();
                    double sumRatings = 0.0;
                    DataSnapshot lastReview = null;

                    for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                        Double ratingVal = reviewSnap.child("rating").getValue(Double.class);
                        if (ratingVal != null) {
                            sumRatings += ratingVal;
                        }
                        // Keep updating lastReview to get the final child (most recent)
                        lastReview = reviewSnap;
                    }

                    // Update Aggregates
                    double average = sumRatings / totalReviews;
                    reviewCount = String.valueOf(totalReviews);
                    averageRating = String.format(Locale.getDefault(), "%.1f", average);

                    tvRating.setText(averageRating);
                    tvSeeAllReviews.setText("See All (" + reviewCount + ")");

                    // Populate the Review Card with the last entry
                    if (lastReview != null) {
                        String pName = lastReview.child("patientName").getValue(String.class);
                        String pDate = lastReview.child("date").getValue(String.class);
                        String pFeedback = lastReview.child("feedback").getValue(String.class);

                        tvReviewName.setText(safeString(pName, "Anonymous Patient"));
                        tvReviewDate.setText(safeString(pDate, "Recently"));
                        tvReviewText.setText(safeString(pFeedback, "No comment provided."));
                    }
                } else {
                    tvRating.setText("0.0");
                    tvSeeAllReviews.setText("See All (0)");
                    tvReviewName.setText("No Reviews");
                    tvReviewText.setText("Be the first to review this doctor!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error: " + error.getMessage());
            }
        });
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        tvSeeAllReviews.setOnClickListener(v -> {
            if (doctorId == null) return;
            Intent reviewsIntent = new Intent(this, DoctorReviewsActivity.class);
            reviewsIntent.putExtra("doctorId", doctorId);
            reviewsIntent.putExtra("doctorName", tvName.getText().toString());
            startActivity(reviewsIntent);
        });

        btnBook.setOnClickListener(v -> {
            if (doctorId == null) return;
            Intent bookingIntent = new Intent(this, ChooseConsultationActivity.class);
            bookingIntent.putExtra("id", doctorId);
            bookingIntent.putExtra("doctorName", tvName.getText().toString());
            bookingIntent.putExtra("doctorSpecialty", tvSpeciality.getText().toString());
            bookingIntent.putExtra("doctorFee", tvFee.getText().toString());
            bookingIntent.putExtra("ratings", averageRating);
            bookingIntent.putExtra("doctorImage", currentDoctorImageUrl);
            bookingIntent.putExtra("clinicName", tvClinicName.getText().toString());
            bookingIntent.putExtra("clinicAddress", tvLocation.getText().toString());
            startActivity(bookingIntent);
        });

        btnShare.setOnClickListener(v -> {
            DoctorShareBottomSheet shareSheet = DoctorShareBottomSheet.newInstance(
                    tvName.getText().toString(),
                    tvSpeciality.getText().toString(),
                    currentDoctorImageUrl,
                    averageRating,
                    reviewCount
            );
            shareSheet.show(getSupportFragmentManager(), "DoctorShareSheet");
        });
    }

    private String safeString(String input, String defaultValue) {
        return (input == null || input.isEmpty() || input.equals("null")) ? defaultValue : input;
    }
}