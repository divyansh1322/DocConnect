package com.example.docconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DoctorLoginActivity extends AppCompatActivity {

    private TextInputEditText emailEt, passwordEt;
    private MaterialButton signInBtn;
    private TextView signUpTv, forgotPasswordTv;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference doctorRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        doctorRef = FirebaseDatabase.getInstance().getReference("doctors");

        initViews();
        setupListeners();
    }

    private void initViews() {
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        signInBtn = findViewById(R.id.signInBtn);
        signUpTv = findViewById(R.id.signUpTv);
        forgotPasswordTv = findViewById(R.id.forgotPasswordTv);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        // Sign In logic
        signInBtn.setOnClickListener(v -> loginDoctor());

        // Navigate to Signup Activity
        signUpTv.setOnClickListener(v -> {
            Intent intent = new Intent(DoctorLoginActivity.this, DoctorSignupActivity.class);
            startActivity(intent);
        });

        // INTENT TO FORGOT PASSWORD ACTIVITY (Updated as per your request)
        forgotPasswordTv.setOnClickListener(v -> {
            Intent intent = new Intent(DoctorLoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void loginDoctor() {
        String email = emailEt.getText().toString().trim();
        String pass = passwordEt.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEt.setError("Email is required");
            emailEt.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            passwordEt.setError("Password is required");
            passwordEt.requestFocus();
            return;
        }

        setLoadingState(true);

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    if (result.getUser() != null) {
                        validateDoctorData(result.getUser().getUid());
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(this, "Login Failed: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void validateDoctorData(String uid) {
        doctorRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    handleAccessDenied("This account is not registered as a doctor.");
                    return;
                }

                String status = snapshot.child("status").getValue(String.class);

                // Check verification status (Matches your "Admin Approval" logic)
                if (status != null && status.equalsIgnoreCase("verified")) {

                    // Save "doctor" role to SharedPreferences for SplashScreen/Auto-login
                    saveRoleLocally("doctor");

                    // Check if profile is finished
                    Boolean isProfileCompleted = snapshot.child("isProfileCompleted").getValue(Boolean.class);

                    if (Boolean.TRUE.equals(isProfileCompleted)) {
                        navigateTo(MainActivity.class);
                    } else {
                        navigateTo(DoctorProfileCreationActivity.class);
                    }

                } else if (status != null && status.equalsIgnoreCase("pending")) {
                    handleAccessDenied("Your account is pending admin approval.");
                } else {
                    handleAccessDenied("Access denied. Contact support.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoadingState(false);
                Toast.makeText(DoctorLoginActivity.this, "DB Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveRoleLocally(String role) {
        SharedPreferences prefs = getSharedPreferences("DocConnectData", MODE_PRIVATE);
        prefs.edit().putString("selected_role", role).apply();
    }

    private void handleAccessDenied(String message) {
        setLoadingState(false);
        mAuth.signOut();
        // Clear preferences on denied access
        getSharedPreferences("DocConnectData", MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void navigateTo(Class<?> target) {
        setLoadingState(false);
        Intent intent = new Intent(DoctorLoginActivity.this, target);
        // Clear activity stack so user can't press back to login screen
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            signInBtn.setVisibility(View.INVISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            signInBtn.setVisibility(View.VISIBLE);
        }
        emailEt.setEnabled(!isLoading);
        passwordEt.setEnabled(!isLoading);
        forgotPasswordTv.setEnabled(!isLoading);
        signUpTv.setEnabled(!isLoading);
    }
}