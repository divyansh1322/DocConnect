package com.example.docconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RatingBar;
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
import java.util.Locale;

/**
 * DoctorReviewsActivity: Professional Transition-Safe Implementation.
 * * HANDLES: Summarized rating header and dynamic reviews list.
 * * FIXES: "Transition Mismatch" by delaying heavy data parsing until window is stable.
 * * FIXES: "MIUI NullPointer" by ensuring UI initialization completes before data sync.
 */
public class DoctorReviewsActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView rvReviews;
    private DoctorReviewsAdapter adapter;
    private List<DoctorReviewsModel> reviewsList;
    private ProgressBar progressBar;

    // Header Components (Matches your XML IDs exactly)
    private TextView tvHeading, tvAverageRating, tvTotalReviews, tvEmptyState;
    private RatingBar ratingBarSummary;
    private ImageButton btnBack;
    private View layoutEmptyState;

    // Firebase
    private DatabaseReference databaseReference;
    private ValueEventListener reviewsListener;
    private String currentDoctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_reviews);

        initViews();
        handleIntentData();

        if (currentDoctorId != null) {
            setupRecyclerView();

            // Path logic maintained exactly as requested
            databaseReference = FirebaseDatabase.getInstance().getReference("doctors")
                    .child(currentDoctorId)
                    .child("reviews_ratings");

            // --- THE A2Z TRANSITION FIX ---
            // We delay the start of Firebase work by 300ms.
            // This prevents the "TransitionRecord Mismatch" red error in Logcat
            // and stops the Xiaomi/MIUI system-level NullPointerException.
            getWindow().getDecorView().postDelayed(() -> {
                if (!isFinishing()) {
                    fetchReviews();
                }
            }, 300);
        }
    }

    private void initViews() {
        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        tvHeading = findViewById(R.id.tvHeading);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        ratingBarSummary = findViewById(R.id.ratingBar);
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void handleIntentData() {
        if (getIntent() != null) {
            currentDoctorId = getIntent().getStringExtra("doctorId");
            if (currentDoctorId == null) currentDoctorId = getIntent().getStringExtra("doctor_id");
            if (currentDoctorId == null) currentDoctorId = getIntent().getStringExtra("id");

            String doctorName = getIntent().getStringExtra("doctorName");
            if (tvHeading != null && doctorName != null) {
                tvHeading.setText(doctorName + "'s Reviews");
            }
        }

        if (currentDoctorId == null) {
            Toast.makeText(this, "Error: Doctor ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        reviewsList = new ArrayList<>();
        adapter = new DoctorReviewsAdapter(reviewsList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(adapter);
        rvReviews.setHasFixedSize(true);
    }

    private void fetchReviews() {
        if (isFinishing()) return;

        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        reviewsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;

                reviewsList.clear();
                float totalRatingSum = 0;

                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        try {
                            DoctorReviewsModel review = dataSnapshot.getValue(DoctorReviewsModel.class);
                            if (review != null) {
                                reviewsList.add(review);
                                totalRatingSum += review.getRating();
                            }
                        } catch (Exception e) {
                            Log.e("FirebaseData", "Error parsing review", e);
                        }
                    }

                    if (!reviewsList.isEmpty()) {
                        updateRatingHeader(totalRatingSum, reviewsList.size());
                        adapter.notifyDataSetChanged();
                        rvReviews.setVisibility(View.VISIBLE);
                        layoutEmptyState.setVisibility(View.GONE);
                    } else {
                        showNoDataState();
                    }
                } else {
                    showNoDataState();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Log.e("FirebaseData", "Fetch cancelled: " + error.getMessage());
            }
        };
        databaseReference.addValueEventListener(reviewsListener);
    }

    private void updateRatingHeader(float sum, int count) {
        float average = sum / count;
        tvAverageRating.setText(String.format(Locale.US, "%.1f", average));
        ratingBarSummary.setRating(average);
        tvTotalReviews.setText("Based on " + count + " reviews");
    }

    private void showNoDataState() {
        rvReviews.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        tvAverageRating.setText("0.0");
        ratingBarSummary.setRating(0);
        tvTotalReviews.setText("No reviews yet");
    }

    @Override
    protected void onDestroy() {
        // Lifecycle cleanup: Essential to stop listeners from updating a closed activity
        if (databaseReference != null && reviewsListener != null) {
            databaseReference.removeEventListener(reviewsListener);
        }
        super.onDestroy();
    }
}