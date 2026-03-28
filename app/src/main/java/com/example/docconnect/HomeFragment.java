package com.example.docconnect;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * HOME FRAGMENT: The production-ready dashboard.
 * Optimized for role-based logic, real-time sync, and memory safety.
 */
public class HomeFragment extends Fragment {

    // --- VIEW CACHING & STATE ---
    private View rootView;
    private boolean isDataInitialized = false;
    private String userRole = "";
    private boolean isReceiverRegistered = false; // Flag to prevent unregistering errors

    // --- UI ELEMENTS ---
    private TextView tvStepCount, tvUserName, tvBmiScore, tvStatLabel, tvGreeting;
    private ProgressBar progressSteps;
    private SeekBar viewBmiVisual;
    private CardView bmiCard, cardArticleImmunity, cardArticleHeart;
    private ImageView ivProfileImage;
    private TextView tvSeeAllSpecialist, tvSeeAllArticles, tvSeeAllDoctors;

    // --- PERSISTENCE ---
    private SharedPreferences stepPrefs;
    private SharedPreferences rolePrefs;
    private static final String PREF_NAME = "StepPrefs";
    private static final String ROLE_PREF_NAME = "DocConnectData";

    // --- FIREBASE ---
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;

    /**
     * BROADCAST RECEIVER: Listens for step updates from StepService.
     */
    private final BroadcastReceiver stepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("LIVE_STEPS")) {
                int liveSteps = intent.getIntExtra("LIVE_STEPS", 0);
                updateStepUIWithLiveValue(liveSteps);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // VIEW CACHING: Prevents UI flickering and redundant inflation
        if (rootView != null) return rootView;

        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize SharedPreferences
        stepPrefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        rolePrefs = requireActivity().getSharedPreferences(ROLE_PREF_NAME, Context.MODE_PRIVATE);

        // Fetch Role immediately to drive logic
        userRole = rolePrefs.getString("selected_role", "user");

        // Firebase Auth & Database Reference
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) currentUserId = currentUser.getUid();

        // 1. Initialize all UI Views
        initViews(rootView);

        // 2. Role-Based Service Initiation
        if ("user".equalsIgnoreCase(userRole)) {
            startStepService();
        }

        // 3. Setup Listeners and Static Data
        setupClickListeners(rootView);
        setupRecyclers(rootView);

        // 4. Initial Data Pull
        if (!isDataInitialized) {
            refreshAllData();
            isDataInitialized = true;
        }

        return rootView;
    }

    private void initViews(View view) {
        tvStepCount = view.findViewById(R.id.tvStepCount);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        progressSteps = view.findViewById(R.id.progressSteps);
        ivProfileImage = view.findViewById(R.id.imgProfile);
        bmiCard = view.findViewById(R.id.bmiCard);
        tvBmiScore = view.findViewById(R.id.tvBmiScore);
        tvStatLabel = view.findViewById(R.id.tvStatLabel);
        viewBmiVisual = view.findViewById(R.id.viewBmiVisual);
        cardArticleImmunity = view.findViewById(R.id.cardArticleImmunity);
        cardArticleHeart = view.findViewById(R.id.cardArticleHeart);
        tvSeeAllSpecialist = view.findViewById(R.id.tvSeeAllSpecialist);
        tvSeeAllArticles = view.findViewById(R.id.tvSeeAllArticles);
        tvSeeAllDoctors = view.findViewById(R.id.tvSeeAllDoctors);

        if (progressSteps != null) progressSteps.setMax(6000);
        if (viewBmiVisual != null) viewBmiVisual.setEnabled(false);
    }

    /**
     * Starts StepService only after permission check and role verification.
     */
    private void startStepService() {
        if (!"user".equalsIgnoreCase(userRole)) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 100);
                return;
            }
        }

        Intent serviceIntent = new Intent(requireContext(), StepService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent);
            } else {
                requireContext().startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Service Start Error: " + e.getMessage());
        }
    }

    private void updateStepUI() {
        int steps = stepPrefs.getInt("last_steps", 0);
        updateStepUIWithLiveValue(steps);
    }

    private void updateStepUIWithLiveValue(int steps) {
        if (tvStepCount != null) tvStepCount.setText(String.valueOf(steps));
        if (progressSteps != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressSteps.setProgress(steps, true);
            } else {
                progressSteps.setProgress(steps);
            }
        }
    }

    private void setupClickListeners(View view) {
        if (bmiCard != null)
            bmiCard.setOnClickListener(v -> startActivity(new Intent(requireContext(), BMITrackerActivity.class)));
        if (tvSeeAllSpecialist != null)
            tvSeeAllSpecialist.setOnClickListener(v -> startActivity(new Intent(requireContext(), SpecialityActivity.class)));
        if (tvSeeAllArticles != null)
            tvSeeAllArticles.setOnClickListener(v -> startActivity(new Intent(requireContext(), ArticleActivity.class)));
        if (tvSeeAllDoctors != null)
            tvSeeAllDoctors.setOnClickListener(v -> startActivity(new Intent(requireContext(), TopDoctorsActivity.class)));

        View btnNotify = view.findViewById(R.id.btnNotification);
        if (btnNotify != null)
            btnNotify.setOnClickListener(v -> startActivity(new Intent(requireContext(), NotificationsActivity.class)));

        View searchBar = view.findViewById(R.id.searchContainer);
        if (searchBar != null) searchBar.setOnClickListener(v -> navigateToSearch("", true));

        setupCategoryClick(view.findViewById(R.id.layoutDentist), "Orthodontics");
        setupCategoryClick(view.findViewById(R.id.layoutNeurology), "Neurologist");
        setupCategoryClick(view.findViewById(R.id.layoutOrthopedic), "Orthopedic");
        setupCategoryClick(view.findViewById(R.id.layoutPediatric), "Cardiologist");
        setupCategoryClick(view.findViewById(R.id.layoutNutrition), "Nutritionist");
        setupCategoryClick(view.findViewById(R.id.layoutEye), "ENT");

        if (cardArticleImmunity != null)
            cardArticleImmunity.setOnClickListener(v -> openWebArticle("https://www.healthline.com/nutrition/how-to-boost-immune-system"));
        if (cardArticleHeart != null)
            cardArticleHeart.setOnClickListener(v -> openWebArticle("https://www.heart.org/en/healthy-living/healthy-eating"));
    }

    private void setupCategoryClick(View view, String query) {
        if (view != null) view.setOnClickListener(v -> navigateToSearch(query, false));
    }

    private void refreshAllData() {
        if (!isAdded() || currentUserId == null) return;
        setDynamicGreeting();
        loadUserData();
        loadLatestBmiData();
        checkAppointmentStatusForPopups();
        if ("user".equalsIgnoreCase(userRole)) {
            updateStepUI();
        }
    }

    private void loadUserData() {
        mDatabase.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name != null && name.contains(" ")) name = name.split(" ")[0];
                    if (tvUserName != null) tvUserName.setText(name);

                    String imgUrl = snapshot.child("profilePhotoUrl").getValue(String.class);
                    if (imgUrl != null && !imgUrl.isEmpty() && ivProfileImage != null) {
                        Glide.with(HomeFragment.this).load(imgUrl).placeholder(R.drawable.ic_person).circleCrop().into(ivProfileImage);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLatestBmiData() {
        mDatabase.child("users").child(currentUserId).child("BMI_track").orderByKey().limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && isAdded()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String scoreStr = String.valueOf(child.child("bmi_score").getValue());
                                if (tvBmiScore != null) tvBmiScore.setText(scoreStr);
                                if (tvStatLabel != null)
                                    tvStatLabel.setText(child.child("status").getValue(String.class));

                                try {
                                    float val = Float.parseFloat(scoreStr);
                                    int progress = (int) ((val - 15) * 4);
                                    if (viewBmiVisual != null)
                                        viewBmiVisual.setProgress(Math.max(0, Math.min(100, progress)));
                                } catch (Exception e) {
                                    if (viewBmiVisual != null) viewBmiVisual.setProgress(50);
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void checkAppointmentStatusForPopups() {
        mDatabase.child("UserBookings").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AppointmentModel appointment = ds.getValue(AppointmentModel.class);
                    if (appointment == null || appointment.isPopupShown()) continue;

                    String status = appointment.getStatus();
                    if ("COMPLETED".equalsIgnoreCase(status)) {
                        showCompletionDialog(appointment);
                        break;
                    } else if ("MISSED".equalsIgnoreCase(status)) {
                        showMissedDialog(appointment);
                        break;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showCompletionDialog(AppointmentModel appointment) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.RoundedAlertDialog)
                .setIcon(R.drawable.ic_check_circle).setTitle("Visit Completed!")
                .setMessage("Rate your session with " + appointment.getDoctorName() + "?")
                .setPositiveButton("Rate Now", (dialog, which) -> {
                    markPopupAsShown(appointment.getBookingId());
                    Intent intent = new Intent(requireContext(), RateReviewActivity.class);
                    intent.putExtra("bookingId", appointment.getBookingId());
                    intent.putExtra("doctorId", appointment.getDoctorId());
                    intent.putExtra("doctorName", appointment.getDoctorName());
                    intent.putExtra("doctorSpecialty", appointment.getDoctorSpecialty());
                    intent.putExtra("visitDate", appointment.getDate());
                    intent.putExtra("doctorImage", appointment.getDoctorImage());
                    startActivity(intent);
                })
                .setNegativeButton("Dismiss", (dialog, which) -> markPopupAsShown(appointment.getBookingId()))
                .show();
    }

    private void showMissedDialog(AppointmentModel appointment) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.RoundedAlertDialog)
                .setIcon(R.drawable.ic_missed).setTitle("Missed Appointment")
                .setMessage("You missed your slot with " + appointment.getDoctorName())
                .setPositiveButton("OK", (dialog, which) -> markPopupAsShown(appointment.getBookingId()))
                .show();
    }

    private void markPopupAsShown(String bId) {
        mDatabase.child("UserBookings").child(currentUserId).child(bId).child("popupShown").setValue(true);
    }

    private void setDynamicGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (tvGreeting == null) return;
        if (hour < 12) tvGreeting.setText("Good Morning,");
        else if (hour < 17) tvGreeting.setText("Good Afternoon,");
        else tvGreeting.setText("Good Evening,");
    }

    private void navigateToSearch(String query, boolean skipLoading) {
        Intent intent = new Intent(requireContext(), SearchResultsActivity.class);
        intent.putExtra("SEARCH_QUERY", query);
        intent.putExtra("SKIP_LOADING", skipLoading);
        startActivity(intent);
    }

    private void openWebArticle(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Browser not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclers(View view) {
        RecyclerView rv = view.findViewById(R.id.recyclerSymptoms);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            List<SymptomModel> list = new ArrayList<>();
            list.add(new SymptomModel("Fever", R.drawable.ic_fever));
            list.add(new SymptomModel("Cough", R.drawable.ic_cough));
            list.add(new SymptomModel("Headache", R.drawable.ic_headache));
            list.add(new SymptomModel("Skin", R.drawable.ic_skin));
            list.add(new SymptomModel("Stomach", R.drawable.ic_stomach));
            rv.setAdapter(new SymptomAdapter(list, query -> navigateToSearch(query, false)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startStepService();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // PROFESSIONAL SYNC: Only register receiver if the role is 'user'
        if ("user".equalsIgnoreCase(userRole)) {
            IntentFilter filter = new IntentFilter("STEP_UPDATE");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireActivity().registerReceiver(stepReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                requireActivity().registerReceiver(stepReceiver, filter);
            }
            isReceiverRegistered = true;
        }
        refreshAllData();
    }

    @Override
    public void onPause() {
        super.onPause();
        // CLEANUP: Prevent memory leaks by unregistering the receiver safely
        if (isReceiverRegistered) {
            try {
                requireActivity().unregisterReceiver(stepReceiver);
                isReceiverRegistered = false;
            } catch (Exception e) {
                Log.e("HomeFragment", "Receiver error: " + e.getMessage());
            }
        }
    }
}