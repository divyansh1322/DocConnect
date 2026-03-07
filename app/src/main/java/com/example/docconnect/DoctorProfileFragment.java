package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

/**
 * DoctorProfileFragment: Optimized with View Caching.
 * Prevents re-fetching data when switching bottom navigation tabs.
 */
public class DoctorProfileFragment extends Fragment {

    // --- 1. VIEW CACHING VARIABLES ---
    private View rootView;
    private boolean isDataLoaded = false;

    // UI Components
    private ShapeableImageView imgProfile;
    private TextView tvName, tvRole, tvLicense;
    private TextView tvStatPatients, tvStatYears, tvStatRating;
    private ImageButton btnBack;
    private MaterialCardView cardPersonal, cardBio, cardWorkHours, cardLocation,
            cardHistory, cardReviews, cardSupport;
    private MaterialButton btnLogout;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference doctorRef;

    public DoctorProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // STEP 1: If rootView already exists, return it immediately to keep UI state
        if (rootView != null) {
            return rootView;
        }

        // STEP 2: Inflate the layout only if it's null (first time loading)
        rootView = inflater.inflate(R.layout.fragment_doctor_profile, container, false);

        // Initialize Firebase and UI
        mAuth = FirebaseAuth.getInstance();
        initViews(rootView);
        setupListeners();

        // STEP 3: Load data only once
        if (mAuth.getCurrentUser() != null && !isDataLoaded) {
            String userId = mAuth.getCurrentUser().getUid();
            doctorRef = FirebaseDatabase.getInstance().getReference("doctors").child(userId);
            loadDoctorData();
            isDataLoaded = true; // Prevents re-attaching listeners on every tab click
        }

        return rootView;
    }

    private void initViews(View view) {
        // Header
        imgProfile = view.findViewById(R.id.imgProfile);
        tvName = view.findViewById(R.id.tvName);
        tvRole = view.findViewById(R.id.tvRole);
        tvLicense = view.findViewById(R.id.tvLicense);
        btnBack = view.findViewById(R.id.btnBack);

        // Stats
        tvStatPatients = view.findViewById(R.id.tvStatPatients);
        tvStatYears = view.findViewById(R.id.tvStatYears);
        tvStatRating = view.findViewById(R.id.tvStatRating);

        // Cards
        cardPersonal = view.findViewById(R.id.cardPersonal);
        cardBio = view.findViewById(R.id.cardBio);
        cardWorkHours = view.findViewById(R.id.cardWorkHours);
        cardLocation = view.findViewById(R.id.cardLocation);
        cardHistory = view.findViewById(R.id.cardHistory);
        cardReviews = view.findViewById(R.id.cardReviews);
        cardSupport = view.findViewById(R.id.cardSupport);

        // Buttons
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    /**
     * Loads personal info and calculates rating using a persistent listener.
     */
    private void loadDoctorData() {
        doctorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Security check to ensure fragment is still attached to UI
                if (!isAdded() || getContext() == null) return;

                if (snapshot.exists()) {
                    // 1. Basic Info
                    tvName.setText(snapshot.child("fullName").getValue(String.class));
                    tvRole.setText(snapshot.child("speciality").getValue(String.class));
                    tvLicense.setText("License: " + snapshot.child("regNo").getValue(String.class));

                    // 2. Statistics
                    Long patients = snapshot.child("totalPatients").getValue(Long.class);
                    tvStatPatients.setText(patients != null ? String.valueOf(patients) : "0");

                    String exp = snapshot.child("experience").getValue(String.class);
                    tvStatYears.setText(exp != null ? exp + "+" : "0+");

                    // 3. Image loading
                    String url = snapshot.child("profileImageUrl").getValue(String.class);
                    if (url != null && !url.isEmpty()) {
                        Glide.with(requireContext()).load(url)
                                .placeholder(R.drawable.ic_doctor)
                                .into(imgProfile);
                    }

                    // 4. Rating Aggregation Logic
                    DataSnapshot ratingsNode = snapshot.child("reviews_ratings");
                    if (ratingsNode.exists()) {
                        double sum = 0;
                        int totalReviews = 0;

                        for (DataSnapshot review : ratingsNode.getChildren()) {
                            Object ratingObj = review.child("rating").getValue();
                            if (ratingObj instanceof Number) {
                                sum += ((Number) ratingObj).doubleValue();
                                totalReviews++;
                            }
                        }

                        if (totalReviews > 0) {
                            double average = sum / totalReviews;
                            tvStatRating.setText(String.format(Locale.getDefault(), "%.1f", average));
                        } else {
                            tvStatRating.setText("0.0");
                        }
                    } else {
                        tvStatRating.setText("0.0");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) Toast.makeText(getContext(), "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        // Back Button: Navigates back to Home
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        // Menu Navigation
        cardPersonal.setOnClickListener(v -> startActivity(new Intent(requireContext(), DoctorEditProfileActivity.class)));
        cardBio.setOnClickListener(v -> startActivity(new Intent(requireContext(), DoctorQualificationActivity.class)));

        cardWorkHours.setOnClickListener(v -> {
            DoctorWorkHoursBottomSheet bottomSheet = new DoctorWorkHoursBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "WorkHoursSheet");
        });

        cardLocation.setOnClickListener(v -> startActivity(new Intent(requireContext(), DoctorClinicLocationActivity.class)));
        cardHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), PastConsultationsActivity.class)));

        cardReviews.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DoctorReviewsActivity.class);
            intent.putExtra("doctor_id", mAuth.getCurrentUser().getUid());
            startActivity(intent);
        });

        cardSupport.setOnClickListener(v -> startActivity(new Intent(requireContext(), DoctorSupportActivity.class)));

        // Sign Out with Material Confirmation
        btnLogout.setOnClickListener(v -> showSignOutDialog());
    }

    private void showSignOutDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.RoundedAlertDialog)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(requireContext(), "Logout Successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(requireContext(), RoleSelectionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    if (getActivity() != null) getActivity().finish();
                }).show();
    }
}