package com.example.docconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * EditMedicalActivity: Professional Health Records Management.
 * FIXES: UI Hanging via Postponed Transitions and Hardware Sync.
 * FEATURES: Atomic Updates, Field Validation, and Safe Data Mapping.
 */
public class EditMedicalActivity extends AppCompatActivity {

    private static final String TAG = "EditMedical";
    private TextInputEditText etHeight, etWeight, etAllergies, etConditions, etMedications, etSurgeries;
    private AutoCompleteTextView dropdownBloodGroup;
    private TextView btnSave;
    private ImageButton btnBack;

    private DatabaseReference userRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_medical);

        // 1. A2Z OPTIMIZATION: Tell Android to wait for data before showing the screen
        supportPostponeEnterTransition();

        // Security logic: check session
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        initViews();
        setupBloodGroupDropdown();

        // 2. PERFORMANCE SYNC:
        // Delay Firebase and UI population to let the hardware renderer settle.
        getWindow().getDecorView().postDelayed(() -> {
            if (!isFinishing()) {
                loadMedicalProfile();
            }
        }, 300);

        btnSave.setOnClickListener(v -> validateAndSave());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        dropdownBloodGroup = findViewById(R.id.actv_blood_group);
        etAllergies = findViewById(R.id.et_allergies);
        etConditions = findViewById(R.id.et_conditions);
        etMedications = findViewById(R.id.et_medications);
        etSurgeries = findViewById(R.id.et_surgeries);
        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupBloodGroupDropdown() {
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, bloodGroups);
        dropdownBloodGroup.setAdapter(adapter);
    }

    private void loadMedicalProfile() {
        userRef.child("medicalProfile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;

                if (snapshot.exists()) {
                    // Logic: Safe mapping with null fallbacks
                    etHeight.setText(getSafe(snapshot, "height"));
                    etWeight.setText(getSafe(snapshot, "weight"));

                    String bg = snapshot.child("bloodGroup").getValue(String.class);
                    if (bg != null && !bg.isEmpty()) {
                        dropdownBloodGroup.setText(bg, false);
                    }

                    etAllergies.setText(getSafe(snapshot, "allergies"));
                    etConditions.setText(getSafe(snapshot, "chronicConditions"));
                    etMedications.setText(getSafe(snapshot, "currentMedications"));
                    etSurgeries.setText(getSafe(snapshot, "pastSurgeries"));
                }

                // 3. START TRANSITION: Show the activity now that data is ready
                supportStartPostponedEnterTransition();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                supportStartPostponedEnterTransition();
            }
        });
    }

    private void validateAndSave() {
        String height = Objects.requireNonNull(etHeight.getText()).toString().trim();
        String weight = Objects.requireNonNull(etWeight.getText()).toString().trim();

        if (TextUtils.isEmpty(height) || TextUtils.isEmpty(weight)) {
            Toast.makeText(this, "Height and Weight are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setAlpha(0.5f);
        processAtomicSave(height, weight);
    }

    /**
     * ATOMIC SAVING LOGIC: Updates specific child nodes without rewriting the whole user object.
     */
    private void processAtomicSave(String h, String w) {
        Map<String, Object> medicalData = new HashMap<>();
        medicalData.put("height", h);
        medicalData.put("weight", w);
        medicalData.put("bloodGroup", dropdownBloodGroup.getText().toString());
        medicalData.put("allergies", etAllergies.getText().toString().trim());
        medicalData.put("chronicConditions", etConditions.getText().toString().trim());
        medicalData.put("currentMedications", etMedications.getText().toString().trim());
        medicalData.put("pastSurgeries", etSurgeries.getText().toString().trim());

        userRef.child("medicalProfile").updateChildren(medicalData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Health Records Updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setAlpha(1.0f);
                    Toast.makeText(this, "Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Helper to prevent "null" strings from appearing in EditTexts
    private String getSafe(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        return (value == null || value.toString().equals("null")) ? "" : value.toString();
    }
}