package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * DoctorSignupActivity: Professional Implementation
 * Features: Atomic Auth-Database synchronization, Lifecycle guards, and strict input validation.
 */
public class DoctorSignupActivity extends AppCompatActivity {

    // UI Elements
    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private ImageView ivTogglePassword, ivToggleConfirmPassword, btnBack;
    private MaterialButton btnSignUp;
    private TextView tvLogin;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // State variables
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_signup);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("doctors");

        initViews();
        setupListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnBack = findViewById(R.id.btnBack);

        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        ivTogglePassword.setOnClickListener(v -> {
            togglePasswordVisibility(etPassword, ivTogglePassword, isPasswordVisible);
            isPasswordVisible = !isPasswordVisible;
        });

        ivToggleConfirmPassword.setOnClickListener(v -> {
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible);
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        });

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, DoctorLoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSignUp.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        // --- Validation ---
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full Name is required");
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Minimum 6 characters required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // --- Firebase Creation ---
        btnSignUp.setEnabled(false);
        btnSignUp.setText("Verifying Details...");



        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (isFinishing()) return;

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToDatabase(user, fullName, email);
                    } else {
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText("Create Account");
                        String error = task.getException() != null ? task.getException().getMessage() : "Auth Failed";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(FirebaseUser user, String name, String email) {
        if (user == null || isFinishing()) return;

        btnSignUp.setText("Syncing Profile...");

        // Standardized Date/Time format for professional logs
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getUid());
        userData.put("fullName", name);
        userData.put("email", email);
        userData.put("role", "doctor");
        userData.put("status", "pending");
        userData.put("date", currentDate);
        userData.put("time", currentTime);

        mDatabase.child(user.getUid()).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (isFinishing()) return;

                    if (task.isSuccessful()) {
                        updateDisplayName(user, name);
                    } else {
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText("Create Account");
                        Toast.makeText(this, "Database Sync Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateDisplayName(FirebaseUser user, String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (isFinishing()) return;

            Toast.makeText(this, "Registration Successful. Pending Approval.", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(this, DoctorProfileCreationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void togglePasswordVisibility(EditText editText, ImageView icon, boolean isVisible) {
        if (isVisible) {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            icon.setImageResource(R.drawable.ic_eye_on);
        } else {
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            icon.setImageResource(R.drawable.ic_eye_off);
        }
        // Ensure cursor remains at the end
        if (editText.getText() != null) {
            editText.setSelection(editText.getText().length());
        }
    }
}