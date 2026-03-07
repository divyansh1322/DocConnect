package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity handling the user registration process for DocConnect.
 * Integrates Firebase Authentication and saves the 'joinedDate' in Realtime Database.
 */
public class SignupActivity extends AppCompatActivity {

    // UI Widgets
    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private ImageView ivTogglePassword, ivToggleConfirmPassword, btnBack;
    private Button btnSignUp;
    private RelativeLayout loadingOverlay;
    private TextView tvLogin;

    // Firebase instances
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // State variables for password visibility toggles
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Database reference to the "users" node
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        // UI Initialization and click listener setup
        initViews();
        setupListeners();
    }

    /**
     * Links XML components to Java objects.
     */
    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnBack = findViewById(R.id.btnBack);
        btnSignUp = findViewById(R.id.btnSignUp);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvLogin = findViewById(R.id.tvLogin);
    }

    /**
     * Sets up click interactions for navigation, password toggles, and registration.
     */
    private void setupListeners() {
        // Back button and Login text both navigate the user back
        btnBack.setOnClickListener(v -> finish());
        tvLogin.setOnClickListener(v -> finish());

        // Toggle visibility for primary password field
        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            togglePasswordVisibility(etPassword, ivTogglePassword, isPasswordVisible);
        });

        // Toggle visibility for confirm password field
        ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible);
        });

        // Trigger the registration logic
        btnSignUp.setOnClickListener(v -> registerUser());
    }

    /**
     * Changes the TransformationMethod of an EditText to show or hide password text.
     */
    private void togglePasswordVisibility(EditText editText, ImageView icon, boolean isVisible) {
        if (isVisible) {
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            icon.setImageResource(R.drawable.ic_eye_on);
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            icon.setImageResource(R.drawable.ic_eye_off);
        }
        editText.setSelection(editText.getText().length());
    }

    /**
     * Validates input fields and attempts to create a new user in Firebase Auth.
     */
    private void registerUser() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Full name required");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserToDatabase(mAuth.getCurrentUser(), name, email);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Creates a user profile entry in the Realtime Database using the Auth UID.
     * Includes the current date as the 'joinedDate'.
     */
    private void saveUserToDatabase(FirebaseUser user, String name, String email) {
        if (user == null) return;

        // Fetch current date in readable format (e.g., Feb 25, 2026)
        String currentDate = DateFormat.getDateInstance().format(new Date());

        // Construct a map of user properties
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", name);
        userData.put("email", email);
        userData.put("id", user.getUid());
        userData.put("role", "user");
        userData.put("status", "active");
        userData.put("joinedDate", currentDate); // Setting the current date here

        // Store data under users/{uid}
        mDatabase.child(user.getUid()).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateProfileAndNavigate(user, name);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Updates the FirebaseUser object's Display Name and navigates to the profile creation screen.
     */
    private void updateProfileAndNavigate(FirebaseUser user, String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name).build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            setLoading(false);
            Intent intent = new Intent(SignupActivity.this, ProfileCreationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Manages the visibility of the loading overlay.
     */
    private void setLoading(boolean isLoading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        btnSignUp.setEnabled(!isLoading);
    }
}