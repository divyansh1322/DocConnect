package com.example.docconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * LOGIN ACTIVITY (User/Patient Side)
 * 1. Handles authentication via Firebase.
 * 2. Checks roles (Admin vs User) across database nodes.
 * 3. Navigates to ForgotPasswordActivity for password resets.
 */
public class LoginActivity extends AppCompatActivity {

    // UI Elements
    private EditText emailEt, passwordEt;
    private MaterialButton signInBtn;
    private TextView signUpTv, forgotPasswordTv;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Instances
        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupListeners();
    }

    /**
     * Bind XML IDs to Java Objects
     */
    private void initViews() {
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        signInBtn = findViewById(R.id.signInBtn);
        signUpTv = findViewById(R.id.signUpTv);
        forgotPasswordTv = findViewById(R.id.forgotPasswordTv);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Define Click Actions
     */
    private void setupListeners() {
        // Trigger Login Process
        signInBtn.setOnClickListener(v -> validateData());

        // Open Signup Screen
        signUpTv.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        // Open Dedicated Forgot Password Screen
        forgotPasswordTv.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Input Validation before calling Firebase
     */
    private void validateData() {
        String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEt.setError("Email is required");
            emailEt.requestFocus();
        } else if (TextUtils.isEmpty(password)) {
            passwordEt.setError("Password is required");
            passwordEt.requestFocus();
        } else {
            loginUser(email, password);
        }
    }

    /**
     * Authenticate with Firebase Auth
     */
    private void loginUser(String email, String password) {
        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        String uid = authResult.getUser().getUid();
                        // Priority 1: Check if this user is an Admin
                        checkAdminRole(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Step 1: Check the 'admins' node in Realtime Database
     */
    private void checkAdminRole(String uid) {
        rootRef.child("admins").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    // Admins must be 'active' to enter
                    if (status == null || "active".equals(status)) {
                        saveRoleAndNavigate("admin", AdminDashboardActivity.class);
                    } else {
                        handleAccessDenied("Your admin account is currently disabled.");
                    }
                } else {
                    // If not an admin, check the 'users' node
                    checkUserRole(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "DB Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Step 2: Check the 'users' node in Realtime Database
     */
    private void checkUserRole(String uid) {
        rootRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    if ("blocked".equals(status)) {
                        handleAccessDenied("Your account has been blocked by admin.");
                    } else {
                        // Check if profile setup is complete
                        Boolean isCompleted = snapshot.child("isProfileCompleted").getValue(Boolean.class);
                        saveRoleAndNavigate("user", Boolean.TRUE.equals(isCompleted) ? MainActivity.class : ProfileCreationActivity.class);
                    }
                } else {
                    // Email exists in Auth but not in DB nodes
                    handleAccessDenied("Account record not found. Please register again.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "DB Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Persistent Session Management: Saves role for SplashScreen and Auto-login
     */
    private void saveRoleAndNavigate(String role, Class<?> targetActivity) {
        SharedPreferences prefs = getSharedPreferences("DocConnectData", MODE_PRIVATE);
        prefs.edit().putString("selected_role", role).apply();

        Intent intent = new Intent(LoginActivity.this, targetActivity);
        // Clear activity history so user cannot go back to login screen
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Security: Log out and clear data if access is unauthorized
     */
    private void handleAccessDenied(String message) {
        showLoading(false);
        mAuth.signOut();
        getSharedPreferences("DocConnectData", MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * UI Helper: Toggles loading state and disables interaction
     */
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        signInBtn.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);

        // Prevent interaction during network calls
        emailEt.setEnabled(!isLoading);
        passwordEt.setEnabled(!isLoading);
        forgotPasswordTv.setEnabled(!isLoading);
        signUpTv.setEnabled(!isLoading);
    }
}