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

/**
 * UPDATED SPLASH SCREEN
 * Includes staggered animations for a premium UI feel.
 * Logic: Checks Auth -> Checks Admin -> Checks User -> Checks Doctor -> Handles Redirection.
 */
public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // 1. Initialize UI Elements for Animation
        ImageView logo = findViewById(R.id.logo_card);
        TextView title = findViewById(R.id.tv_app_name);
        TextView tagline = findViewById(R.id.tv_tagline);

        // 2. Load and Apply Staggered Animations
        // We load unique instances to apply different offsets
        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in_animation);
        Animation titleAnim = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in_animation);
        Animation taglineAnim = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in_animation);

        // Logo starts immediately
        logo.startAnimation(logoAnim);

        // Title pops in 300ms after the logo starts
        titleAnim.setStartOffset(300);
        title.startAnimation(titleAnim);

        // Tagline pops in 600ms after the logo starts
        taglineAnim.setStartOffset(600);
        tagline.startAnimation(taglineAnim);

        // 3. Logic Delay
        // Increased to 3000ms (3s) to allow the staggered animation to complete beautifully
        new Handler().postDelayed(this::checkUserStatus, 3000);
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            navigateTo(RoleSelectionActivity.class);
            return;
        }

        String uid = currentUser.getUid();

        // TIER 1: ADMIN CHECK
        FirebaseDatabase.getInstance().getReference("admins")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String role = snapshot.child("role").getValue(String.class);
                            String status = snapshot.child("status").getValue(String.class);

                            if ("super_admin".equals(role) && (status == null || status.equals("active"))) {
                                navigateTo(AdminDashboardActivity.class);
                                return;
                            }
                        }
                        // Not an admin, check for Patient (User)
                        checkUser(uid);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        errorOut();
                    }
                });
    }

    private void checkUser(String uid) {
        FirebaseDatabase.getInstance().getReference("users")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // User found, save role and check completion
                            saveRoleLocally("user");
                            checkProfile(snapshot, ProfileCreationActivity.class);
                        } else {
                            // Not a patient, check if they are a doctor
                            checkDoctor(uid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        errorOut();
                    }
                });
    }

    private void checkDoctor(String uid) {
        FirebaseDatabase.getInstance().getReference("doctors")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Doctor found, save role and check completion
                            saveRoleLocally("doctor");
                            checkProfile(snapshot, DoctorProfileCreationActivity.class);
                        } else {
                            // Auth exists but record missing in DB
                            FirebaseAuth.getInstance().signOut();
                            navigateTo(RoleSelectionActivity.class);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        errorOut();
                    }
                });
    }

    private void checkProfile(DataSnapshot snapshot, Class<?> profileActivity) {
        Boolean completed = snapshot.child("isProfileCompleted").getValue(Boolean.class);
        if (Boolean.TRUE.equals(completed)) {
            navigateTo(MainActivity.class);
        } else {
            Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
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
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Professional fade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}