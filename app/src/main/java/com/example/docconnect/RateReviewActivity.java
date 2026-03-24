package com.example.docconnect;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RateReviewActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private Slider communicationSlider, punctualitySlider, bedsideMannerSlider;
    private TextView tvCommStatus, tvPunctualityStatus, tvBedsideStatus;
    private EditText etFeedback;
    private MaterialButton btnSubmit;
    private ImageView btnBack;
    private ShapeableImageView ivDoctorProfile;
    private TextView tvDoctorName, tvVisitDetails;
    private ProgressDialog progressDialog;

    private DatabaseReference mDatabase;
    private String doctorId, bookingId, patientId;
    private String currentUserName = "Anonymous User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_review);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        patientId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        doctorId = getIntent().getStringExtra("doctorId");
        bookingId = getIntent().getStringExtra("bookingId");

        initViews();
        getWindow().getDecorView().postDelayed(this::startLoadingSequence, 250);
    }

    private void initViews() {
        ivDoctorProfile = findViewById(R.id.ivDoctorProfile);
        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvVisitDetails = findViewById(R.id.tvVisitDetails);
        btnBack = findViewById(R.id.btnBack);
        ratingBar = findViewById(R.id.ratingBar);
        etFeedback = findViewById(R.id.etFeedback);
        btnSubmit = findViewById(R.id.btnSubmitReview);
        communicationSlider = findViewById(R.id.communicationSlider);
        punctualitySlider = findViewById(R.id.punctualitySlider);
        bedsideMannerSlider = findViewById(R.id.bedsideMannerSlider);
        tvCommStatus = findViewById(R.id.tvCommStatus);
        tvPunctualityStatus = findViewById(R.id.tvPunctualityStatus);
        tvBedsideStatus = findViewById(R.id.tvBedsideStatus);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Saving your experience...");
    }

    private void startLoadingSequence() {
        if (isFinishing()) return;

        tvDoctorName.setText(getIntent().getStringExtra("doctorName"));
        String specialty = getIntent().getStringExtra("doctorSpecialty");
        String date = getIntent().getStringExtra("visitDate");
        tvVisitDetails.setText(String.format("%s • %s", specialty, date));

        Glide.with(this)
                .load(getIntent().getStringExtra("doctorImage"))
                .placeholder(R.drawable.ic_doctor)
                .into(ivDoctorProfile);


        fetchCurrentUserName();
        setupSliderListeners();

        btnSubmit.setOnClickListener(v -> validateAndSubmit());
        btnBack.setOnClickListener(v -> finish());
    }

    private void checkIfAlreadyRated() {
        if (bookingId == null) return;
        mDatabase.child("UserBookings").child(patientId).child(bookingId).child("rated")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                            Toast.makeText(RateReviewActivity.this, "Already reviewed.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchCurrentUserName() {
        mDatabase.child("users").child(patientId).child("fullName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) currentUserName = snapshot.getValue(String.class);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void setupSliderListeners() {
        communicationSlider.addOnChangeListener((s, v, f) -> tvCommStatus.setText(getStatusLabel((int) v)));
        punctualitySlider.addOnChangeListener((s, v, f) -> tvPunctualityStatus.setText(getPunctualityLabel((int) v)));
        bedsideMannerSlider.addOnChangeListener((s, v, f) -> tvBedsideStatus.setText(getStatusLabel((int) v)));
    }

    private String getStatusLabel(int value) {
        String[] labels = {"", "Poor", "Fair", "Good", "Very Good", "Excellent"};
        return labels[value < labels.length ? value : 0];
    }

    private String getPunctualityLabel(int value) {
        String[] labels = {"", "Late", "On-Time", "Professional"};
        return labels[value < labels.length ? value : 0];
    }

    private void validateAndSubmit() {
        if (ratingBar.getRating() == 0) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSubmit.setEnabled(false);
        processAtomicSubmission(ratingBar.getRating());
    }

    /**
     * UPDATED: Saves all data into a single 'reviews_ratings' node
     * for easier average calculation in the Profile Fragment.
     */
    private void processAtomicSubmission(float stars) {
        if (isFinishing()) return;
        progressDialog.show();

        // 1. Path setup under the specific doctor
        DatabaseReference doctorNodeRef = mDatabase.child("doctors").child(doctorId).child("reviews_ratings");
        String feedbackId = doctorNodeRef.push().getKey();
        String readableDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        checkIfAlreadyRated();
        if (feedbackId == null) {
            btnSubmit.setEnabled(true);
            progressDialog.dismiss();
            return;
        }

        // 2. Prepare the Single Combined Data Object
        Map<String, Object> unifiedReviewData = new HashMap<>();
        unifiedReviewData.put("patientName", currentUserName);
        unifiedReviewData.put("patientId", patientId);
        unifiedReviewData.put("feedback", etFeedback.getText().toString().trim());
        unifiedReviewData.put("rating", stars); // Crucial for average calculation
        unifiedReviewData.put("commScore", communicationSlider.getValue());
        unifiedReviewData.put("punctScore", punctualitySlider.getValue());
        unifiedReviewData.put("bedsideScore", bedsideMannerSlider.getValue());
        unifiedReviewData.put("date", readableDate);
        unifiedReviewData.put("timestamp", ServerValue.TIMESTAMP);

        // 3. Prepare Atomic Update Map
        Map<String, Object> updates = new HashMap<>();

        // Save review data under doctors/{uid}/reviews_ratings/{pushID}
        updates.put("/doctors/" + doctorId + "/reviews_ratings/" + feedbackId, unifiedReviewData);

        // Update booking metadata
        updates.put("/UserBookings/" + patientId + "/" + bookingId + "/rated", true);
        updates.put("/UserBookings/" + patientId + "/" + bookingId + "/popupShown", true);

        // 4. Execute all updates simultaneously
        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            if (isFinishing()) return;
            progressDialog.dismiss();

            if (task.isSuccessful()) {
                Toast.makeText(this, "Experience saved!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                btnSubmit.setEnabled(true);
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Save failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        super.onDestroy();
    }
}