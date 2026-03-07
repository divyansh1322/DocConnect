package com.example.docconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Mandatory Profile Setup Activity for Patients.
 * Forces the user to provide essential details (Age, Gender, Phone) immediately
 * after signup to ensure medical records are accurate.
 */
public class ProfileCreationActivity extends AppCompatActivity {

    // --- UI Elements ---
    private TextInputEditText etFullName, etEmail, etPhone, etAge, etPurpose;
    private AutoCompleteTextView acGender;
    private MaterialButton btnContinue;
    private ImageView btnBack;

    // --- Firebase Logic ---
    private DatabaseReference userRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_creation);

        // Firebase Initialization
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("users");

        initViews();
        setupGenderDropdown();

        // STRICT MODE: Ensures the UI back button triggers the blocked logic
        btnBack.setOnClickListener(v -> onBackPressed());

        btnContinue.setOnClickListener(v -> saveProfile());
    }

    /**
     * STRICT MODE: Overriding Hardware Back Button.
     * Prevents users from exiting the setup process via the system back key/gesture.
     */
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "You must complete your profile to continue.", Toast.LENGTH_SHORT).show();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAge = findViewById(R.id.etAge);
        etPurpose = findViewById(R.id.etPurpose);
        acGender = findViewById(R.id.acGender);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
    }

    /**
     * Configures the material dropdown for Gender selection.
     */
    private void setupGenderDropdown() {
        String[] genderOptions = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                genderOptions
        );
        acGender.setAdapter(adapter);
    }

    /**
     * Validates inputs and performs a batch write to Firebase.
     * Sets the critical 'isProfileCompleted' flag to TRUE on success.
     */
    private void saveProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = currentUser.getUid();

        // Data extraction
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String purpose = etPurpose.getText().toString().trim();
        String gender = acGender.getText().toString().trim();

        // --- Data Validation ---
        if (TextUtils.isEmpty(name)) { etFullName.setError("Name required"); return; }
        if (TextUtils.isEmpty(email)) { etEmail.setError("Email required"); return; }
        if (TextUtils.isEmpty(phone) || phone.length() != 10) { etPhone.setError("Enter valid 10-digit phone"); return; }
        if (TextUtils.isEmpty(gender)) { acGender.setError("Select gender"); return; }
        if (TextUtils.isEmpty(age)) { etAge.setError("Age required"); return; }

        // --- Data Packing ---
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("gender", gender);
        userMap.put("age", age);
        userMap.put("purpose", purpose);
        userMap.put("role", "user");

        /**
         * CRITICAL FLAG:
         * This boolean is used by the Splash Screen or Login logic to verify
         * if the user should be allowed into the MainActivity.
         */
        userMap.put("isProfileCompleted", true);

        // Firebase Write
        userRef.child(uid).setValue(userMap)
                .addOnSuccessListener(unused -> {

                    // Persistent State: Cache role locally for instant Splash Screen routing
                    SharedPreferences prefs = getSharedPreferences("DocConnectData", MODE_PRIVATE);
                    prefs.edit().putString("userRole", "user").apply();

                    Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();

                    // Navigate to Main Dashboard and clear the backstack
                    Intent intent = new Intent(ProfileCreationActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}