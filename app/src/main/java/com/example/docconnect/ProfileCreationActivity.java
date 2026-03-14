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

public class ProfileCreationActivity extends AppCompatActivity {
    private TextInputEditText etFullName, etEmail, etPhone, etAge, etPurpose;
    private AutoCompleteTextView acGender;
    private MaterialButton btnContinue;
    private ImageView btnBack;

    private DatabaseReference userRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_creation);

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("users");

        initViews();
        setupGenderDropdown();

        btnBack.setOnClickListener(v -> onBackPressed());
        btnContinue.setOnClickListener(v -> saveProfile());
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Please complete setup first.", Toast.LENGTH_SHORT).show();
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

    private void setupGenderDropdown() {
        String[] genderOptions = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genderOptions);
        acGender.setAdapter(adapter);
    }

    private void saveProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String purpose = etPurpose.getText().toString().trim();
        String gender = acGender.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etFullName.setError("Required"); return; }
        if (TextUtils.isEmpty(phone) || phone.length() != 10) { etPhone.setError("Invalid Phone"); return; }
        if (TextUtils.isEmpty(gender)) { acGender.setError("Required"); return; }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("gender", gender);
        userMap.put("age", age);
        userMap.put("purpose", purpose);
        userMap.put("isProfileCompleted", true); // Update flag to TRUE

        // updateChildren ensures joinedDate and status are NOT DELETED
        userRef.child(uid).updateChildren(userMap)
                .addOnSuccessListener(unused -> {
                    getSharedPreferences("DocConnectData", MODE_PRIVATE).edit().putString("selected_role", "user").apply();
                    Intent intent = new Intent(ProfileCreationActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}