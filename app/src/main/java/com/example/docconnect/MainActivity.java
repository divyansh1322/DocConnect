package com.example.docconnect;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * MAIN ACTIVITY: The core controller for DocConnect.
 * Manages role-based navigation, background services, and permissions.
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1002;

    public BottomNavigationView bottomNavigationView;
    public String userRole;
    private boolean isActivityVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. UI THEME SETTINGS
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 2. VIEW INITIALIZATION
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Remove selection background tint for a cleaner look
        bottomNavigationView.setItemActiveIndicatorColor(ColorStateList.valueOf(Color.TRANSPARENT));
        bottomNavigationView.setItemRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));

        // 3. ROLE DATA FETCHING
        SharedPreferences prefs = getSharedPreferences("DocConnectData", MODE_PRIVATE);
        userRole = prefs.getString("selected_role", "user"); // Default to user if null

        // 4. NAVIGATION SETUP
        configureMenuForRole(userRole);
        setupNavigationListener();
        setupSmartBackNavigation();

        // 5. PERMISSIONS & SERVICES (Role-Specific)
        requestNotificationPermission();

        // CRITICAL: Only users get Step Tracking. Doctors/Admins do not.
        if ("user".equalsIgnoreCase(userRole)) {
            checkActivityRecognitionPermission();
        } else {
            // Explicitly stop service if role is not 'user' to save battery
            stopService(new Intent(this, StepService.class));
        }

        // 6. FIREBASE SYNC
        syncFCMTokenToDatabase();
        subscribeToRoleTopic(userRole);

        // 7. INITIAL FRAGMENT LOAD
        if (savedInstanceState == null) {
            bottomNavigationView.post(() -> {
                Intent intent = getIntent();
                if (intent != null && intent.hasExtra("navigate_to")) {
                    handleIntent(intent);
                } else {
                    loadDefaultHome();
                }
            });
        }
    }

    /**
     * Checks Activity Recognition for Step Tracking (Android 10+)
     */
    private void checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        ACTIVITY_RECOGNITION_REQUEST_CODE);
            } else {
                startStepService();
            }
        } else {
            startStepService();
        }
    }

    /**
     * Starts the Foreground StepService safely
     */
    private void startStepService() {
        // Double check role before starting to be 100% safe
        if (!"user".equalsIgnoreCase(userRole)) return;

        Intent serviceIntent = new Intent(this, StepService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStepService();
            } else {
                Toast.makeText(this, "Step tracking disabled: Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Role-aware back navigation logic
     */
    private void setupSmartBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int homeId = "doctor".equals(userRole) ? R.id.nav_doc_home : R.id.nav_home;
                if (bottomNavigationView.getSelectedItemId() != homeId) {
                    bottomNavigationView.setSelectedItemId(homeId);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void loadDefaultHome() {
        int homeId = "doctor".equals(userRole) ? R.id.nav_doc_home : R.id.nav_home;
        Fragment homeFrag = "doctor".equals(userRole) ? new DoctorHomeFragment() : new HomeFragment();
        bottomNavigationView.setSelectedItemId(homeId);
        loadFragment(homeFrag);
    }

    /**
     * Handles BottomNavigationView item selections based on User Role
     */
    private void setupNavigationListener() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (!isActivityVisible || isFinishing()) return false;

            int id = item.getItemId();
            Fragment selectedFragment = null;

            if ("user".equals(userRole)) {
                if (id == R.id.nav_home) selectedFragment = new HomeFragment();
                else if (id == R.id.nav_appointments) selectedFragment = new AppointmentsFragment();
                else if (id == R.id.nav_chats) selectedFragment = new ChatsFragment();
                else if (id == R.id.nav_profile) selectedFragment = new ProfileFragment();
            } else if ("doctor".equals(userRole)) {
                if (id == R.id.nav_doc_home) selectedFragment = new DoctorHomeFragment();
                else if (id == R.id.nav_doc_schedule) selectedFragment = new DoctorScheduleFragment();
                else if (id == R.id.nav_doc_chats) selectedFragment = new DoctorChatsFragment();
                else if (id == R.id.nav_doc_profile) selectedFragment = new DoctorProfileFragment();
            }

            return loadFragment(selectedFragment);
        });
    }

    /**
     * Utility to swap fragments with state-loss prevention
     */
    private boolean loadFragment(Fragment fragment) {
        if (fragment == null || isFinishing() || isDestroyed()) return false;
        if (getSupportFragmentManager().isStateSaved()) return false;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
        return true;
    }

    /**
     * Handles deep linking and notification taps
     */
    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("navigate_to")) {
            String target = intent.getStringExtra("navigate_to");
            if ("appointments".equals(target)) {
                bottomNavigationView.setSelectedItemId("doctor".equals(userRole) ? R.id.nav_doc_schedule : R.id.nav_appointments);
            } else if ("chats".equals(target)) {
                bottomNavigationView.setSelectedItemId("doctor".equals(userRole) ? R.id.nav_doc_chats : R.id.nav_chats);
            } else {
                loadDefaultHome();
            }
        } else {
            loadDefaultHome();
        }
    }

    /**
     * Dynamically swaps the menu XML based on the logged-in role
     */
    private void configureMenuForRole(String role) {
        bottomNavigationView.getMenu().clear();
        if ("doctor".equals(role)) {
            bottomNavigationView.inflateMenu(R.menu.doc_bottom_nav_menu);
        } else {
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu);
        }
    }

    /**
     * Syncs the unique FCM device token to the correct Firebase node
     */
    private void syncFCMTokenToDatabase() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) return;
            if (task.isSuccessful() && task.getResult() != null) {
                String token = task.getResult();
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null) {
                    String node = "user".equals(userRole) ? "users" : "doctors";
                    FirebaseDatabase.getInstance().getReference(node).child(uid).child("fcmToken").setValue(token);
                }
            }
        });
    }

    /**
     * Requests notification permissions for Android 13+
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void subscribeToRoleTopic(String role) {
        FirebaseMessaging.getInstance().subscribeToTopic(role);
    }
}