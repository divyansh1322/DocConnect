package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Fragment responsible for displaying and managing the Patient's profile.
 * Optimized with View Caching to prevent re-fetching/flickering during tab switches.
 */
public class ProfileFragment extends Fragment {

    // --- 1. VIEW CACHING VARIABLES ---
    private View rootView;
    private boolean isDataLoaded = false;

    // UI Components: Profile Header
    private TextView tvName, tvEmail, btnEdit;
    private ShapeableImageView ivProfile;
    private ImageView btnBack;

    private String currentUserId;

    // Feature Navigation Cards
    private MaterialCardView cardAppointments, cardHealth, cardFamily, cardNotifications, cardHelp, cardAbout;

    // Logout trigger
    private MaterialButton cardLogout;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // STEP 1: Check if rootView exists. If yes, return it to keep current UI state.
        if (rootView != null) {
            return rootView;
        }

        // STEP 2: Inflate the layout for the first time
        rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        // 1. Initialize Firebase Session
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        // 2. Setup UI Components
        initViews(rootView);

        // 3. Setup Listeners
        setupClickListeners();

        // STEP 3: Load data only once
        if (!isDataLoaded) {
            loadUserProfile();
            isDataLoaded = true;
        }

        return rootView;
    }

    private void initViews(View view) {
        tvName = view.findViewById(R.id.tv_name);
        tvEmail = view.findViewById(R.id.tv_email);
        ivProfile = view.findViewById(R.id.iv_profile);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnBack = view.findViewById(R.id.btn_back);

        cardAppointments = view.findViewById(R.id.card_appointments);
        cardHealth = view.findViewById(R.id.card_health_records);
        cardFamily = view.findViewById(R.id.card_family);
        cardNotifications = view.findViewById(R.id.card_notifications);
        cardHelp = view.findViewById(R.id.card_help);
        cardAbout = view.findViewById(R.id.card_about);

        cardLogout = view.findViewById(R.id.card_logout);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            // Using addValueEventListener ensures profile changes (like a new photo)
            // reflect here automatically while the app is open.
            mDatabase.child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Safety check to ensure fragment is still attached
                    if (!isAdded() || getContext() == null) return;

                    if (snapshot.exists()) {
                        String name = snapshot.child("fullName").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        String imageUrl = snapshot.child("profilePhotoUrl").getValue(String.class);

                        if (name != null) tvName.setText(name);
                        if (email != null) tvEmail.setText(email);

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_doctor)
                                    .into(ivProfile);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void setupClickListeners() {
        // Edit Profile Navigation (External Activity)
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        // Back Button: Navigates to Home Tab in MainActivity
        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_home);
            }
        });

        // Internal Navigation to Appointments via Activity's Nav logic
        cardAppointments.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                if ("user".equals(activity.userRole)) {
                    activity.bottomNavigationView.setSelectedItemId(R.id.nav_appointments);
                } else {
                    activity.bottomNavigationView.setSelectedItemId(R.id.nav_doc_schedule);
                }
            }
        });

        // Sequential Module Navigation (External Activities)
        cardHealth.setOnClickListener(v -> openActivity(EditMedicalActivity.class));
        cardFamily.setOnClickListener(v -> openActivity(AddFamilyMemberActivity.class));
        cardNotifications.setOnClickListener(v -> openActivity(NotificationsActivity.class));
        cardHelp.setOnClickListener(v -> openActivity(HelpNSupportActivity.class));
        cardAbout.setOnClickListener(v -> openActivity(LegalAboutAppActivity.class));

        cardLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(requireContext(), activityClass);
        startActivity(intent);
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.RoundedAlertDialog)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of DocConnect?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Log Out", (dialog, which) -> {
                    // Sign out and clear local flag
                    mAuth.signOut();
                    isDataLoaded = false;

                    Toast.makeText(requireContext(), "Logout Successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), RoleSelectionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .show();
    }
}