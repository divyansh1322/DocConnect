package com.example.docconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashScreenActivity extends AppCompatActivity {

    // A static variable survives Activity destruction/recreation.
    // This ensures we only attempt to enable persistence ONCE per app session.
    private static boolean isPersistenceEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. SAFE PERSISTENCE INITIALIZATION
        // This must happen BEFORE any other Firebase call.
        if (!isPersistenceEnabled) {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                isPersistenceEnabled = true; // Mark as done so it never runs again
            } catch (Exception e) {
                // If it fails (usually because a reference was already created), we catch it
                // so the app doesn't crash.
            }
        }

        setContentView(R.layout.activity_splash_screen);

        // 2. UI INITIALIZATION & ANIMATION
        setupAnimations();

        // 3. LOGIC DELAY
        // 3000ms allows the staggered animations to play out beautifully
        new Handler().postDelayed(this::checkUserStatus, 3000);
    }

    private void setupAnimations() {
        ImageView logo = findViewById(R.id.logo_card);
        TextView title = findViewById(R.id.tv_app_name);
        TextView tagline = findViewById(R.id.tv_tagline);

        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in_animation);
        Animation titleAnim = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in_animation);
        Animation taglineAnim = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in_animation);

        logo.startAnimation(logoAnim);

        titleAnim.setStartOffset(300);
        title.startAnimation(titleAnim);

        taglineAnim.setStartOffset(600);
        tagline.startAnimation(taglineAnim);
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // If no user is logged in, send them straight to role selection
        if (currentUser == null) {
            navigateTo(RoleSelectionActivity.class);
            return;
        }

        String uid = currentUser.getUid();

        // --- THE OFFLINE-READY HINT ---
        // We check SharedPreferences first. If we already know the user is a doctor,
        // we skip the Admin/User checks which would "hang" if the user is offline.
        SharedPreferences prefs = getSharedPreferences("DocConnectData", MODE_PRIVATE);
        String savedRole = prefs.getString("selected_role", "unknown");

        switch (savedRole) {
            case "doctor":
                checkDoctorPath(uid);
                break;
            case "user":
                checkPatientPath(uid);
                break;
            case "super_admin":
                checkAdminPath(uid);
                break;
            default:
                // If no role is saved (first time), start the full check waterfall
                startFullWaterfallCheck(uid);
                break;
        }
    }

    /**
     * TIER 1: ADMIN CHECK
     */
    private void checkAdminPath(String uid) {
        FirebaseDatabase.getInstance().getReference("admins").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            saveRoleLocally("super_admin");
                            navigateTo(AdminDashboardActivity.class);
                        } else {
                            // Fallback if the role in SharedPreferences was wrong or revoked
                            startFullWaterfallCheck(uid);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { errorOut(); }
                });
    }

    /**
     * TIER 2: PATIENT (USER) CHECK
     */
    private void checkPatientPath(String uid) {
        FirebaseDatabase.getInstance().getReference("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            saveRoleLocally("user");
                            checkProfileCompletion(snapshot, ProfileCreationActivity.class);
                        } else {
                            // If not found, move to Doctor check
                            checkDoctorPath(uid);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { errorOut(); }
                });
    }

    /**
     * TIER 3: DOCTOR CHECK
     */
    private void checkDoctorPath(String uid) {
        FirebaseDatabase.getInstance().getReference("doctors").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            saveRoleLocally("doctor");
                            checkProfileCompletion(snapshot, DoctorProfileCreationActivity.class);
                        } else {
                            // If authenticated but NO record exists anywhere, log them out
                            FirebaseAuth.getInstance().signOut();
                            navigateTo(RoleSelectionActivity.class);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { errorOut(); }
                });
    }

    /**
     * Starts the check from the top if we don't know the user's role yet.
     */
    private void startFullWaterfallCheck(String uid) {
        // Start with Admin, then it flows to User, then Doctor
        checkAdminPath(uid);
    }

    private void checkProfileCompletion(DataSnapshot snapshot, Class<?> profileActivity) {
        Boolean completed = snapshot.child("isProfileCompleted").getValue(Boolean.class);
        if (Boolean.TRUE.equals(completed)) {
            navigateTo(MainActivity.class);
        } else {
            Toast.makeText(this, "Complete your profile", Toast.LENGTH_SHORT).show();
            navigateTo(profileActivity);
        }
    }

    private void saveRoleLocally(String role) {
        SharedPreferences prefs = getSharedPreferences("DocConnectData", MODE_PRIVATE);
        prefs.edit().putString("selected_role", role).apply();
    }

    private void errorOut() {
        Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show();
        navigateTo(RoleSelectionActivity.class);
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        // Clear activity stack so user cannot go back to Splash
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}