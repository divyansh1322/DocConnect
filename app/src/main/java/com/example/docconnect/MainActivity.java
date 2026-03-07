package com.example.docconnect;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

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
 * MAIN ACTIVITY: The logic controller for DocConnect.
 * Handles role-based UI, Fragment management, FCM notifications, and Smart Back-Navigation.
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    public BottomNavigationView bottomNavigationView;
    public String userRole;
    private boolean isActivityVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // FORCE LIGHT MODE - Add this BEFORE super.onCreate
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // 1. BIND VIEW & INITIAL STYLE
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Remove the Material 3 selection pill/indicator
        bottomNavigationView.setItemActiveIndicatorColor(ColorStateList.valueOf(Color.TRANSPARENT));
        // Remove the gray ripple/flash when clicking icons
        bottomNavigationView.setItemRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));

        // 2. FETCH PERSISTED USER ROLE
        SharedPreferences prefs = getSharedPreferences("DocConnectData", MODE_PRIVATE);
        userRole = prefs.getString("selected_role", "user");

        // 3. CONFIGURE ROLE-BASED UI
        configureMenuForRole(userRole);
        setupNavigationListener();

        // 4. SMART BACK-NAVIGATION LOGIC (Role-Aware)
        // This prevents the app from closing if the user isn't on the Home screen.
        setupSmartBackNavigation();

        // 5. BACKGROUND SERVICES
        requestNotificationPermission();
        syncFCMTokenToDatabase();
        subscribeToRoleTopic(userRole);

        // 6. STARTUP LOGIC (Deep Links & Default Fragment)
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
     * SMART BACK-NAVIGATION:
     * If user is on Appointments/Chats/Profile -> Pressing Back moves to Home.
     * If user is ALREADY on Home -> Pressing Back exits the app.
     */
    private void setupSmartBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Determine the "Home" ID based on the current user role
                int homeId = "doctor".equals(userRole) ? R.id.nav_doc_home : R.id.nav_home;

                // Check which tab is currently selected in the BottomNav
                if (bottomNavigationView.getSelectedItemId() != homeId) {
                    // If not on Home, jump back to the Home tab
                    bottomNavigationView.setSelectedItemId(homeId);
                } else {
                    // If already on Home, disable this interceptor and actually exit the app
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /**
     * LOGIC: Forces the correct starting fragment based on role.
     */
    private void loadDefaultHome() {
        int homeId = "doctor".equals(userRole) ? R.id.nav_doc_home : R.id.nav_home;
        Fragment homeFrag = "doctor".equals(userRole) ? new DoctorHomeFragment() : new HomeFragment();

        bottomNavigationView.setSelectedItemId(homeId);
        loadFragment(homeFrag);
    }

    /**
     * NAVIGATION LISTENER: Swaps fragments based on BottomNav selection.
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
            }
            else if ("doctor".equals(userRole)) {
                if (id == R.id.nav_doc_home) selectedFragment = new DoctorHomeFragment();
                else if (id == R.id.nav_doc_schedule) selectedFragment = new DoctorScheduleFragment();
                else if (id == R.id.nav_doc_chats) selectedFragment = new DoctorChatsFragment();
                else if (id == R.id.nav_doc_profile) selectedFragment = new DoctorProfileFragment();
            }

            return loadFragment(selectedFragment);
        });
    }

    /**
     * FRAGMENT LOADER: Performs the actual UI swap.
     */
    private boolean loadFragment(Fragment fragment) {
        if (fragment == null || isFinishing() || isDestroyed()) return false;
        if (getSupportFragmentManager().isStateSaved()) return false;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();

        return true;
    }

    /**
     * NOTIFICATION HANDLING: Redirects users to specific screens from a notification.
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
     * MENU INFLATER: Swaps menus so Doctors don't see Patient options and vice-versa.
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
     * FCM TOKEN SYNC: Updates the database with the device's unique notification ID.
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
     * PERMISSIONS: Requests notification access for Android 13+.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    // --- ACTIVITY LIFECYCLE ---

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