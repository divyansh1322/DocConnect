package com.example.docconnect;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * BMI TRACKER ACTIVITY
 * Calculates Body Mass Index (BMI) in real-time using SeekBars and saves historical
 * data to the user's specific Firebase node for long-term tracking.
 */
public class BMITrackerActivity extends AppCompatActivity {

    // UI Components
    private TextView tvBmiScore, tvStatusTitle, tvStatusDesc, tvHeightVal, tvWeightVal;
    private SeekBar seekBarHeight, seekBarWeight;
    private ImageView imgStatusIcon;
    private FrameLayout layoutStatusIconBg;
    private MaterialButton btnSave;

    // Formatting: Ensures strictly one decimal place (e.g., 22.4)
    private final DecimalFormat df = new DecimalFormat("0.0");

    // Variables (Defaults)
    private int currentHeightCm = 175;
    private float currentWeightKg = 68.0f;
    private float calculatedBmi = 0;
    private String currentStatus = "Normal";

    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi_tracker);

        // Initialize Firebase References
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();

        // Setup Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupSeekBars();

        // 1. Initial Calculation (Static default)
        calculateBmi();

        // 2. DATA SYNC: Fetch the most recent saved data to restore the user's last state
        fetchLastBmiData();

        // Setup Save Button
        btnSave.setOnClickListener(v -> saveToFirebase());
    }

    private void initViews() {
        tvBmiScore = findViewById(R.id.tvBmiScore);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusDesc = findViewById(R.id.tvStatusDesc);
        tvHeightVal = findViewById(R.id.tvHeightVal);
        tvWeightVal = findViewById(R.id.tvWeightVal);

        seekBarHeight = findViewById(R.id.seekBarHeight);
        seekBarWeight = findViewById(R.id.seekBarWeight);

        imgStatusIcon = findViewById(R.id.imgStatusIcon);
        layoutStatusIconBg = findViewById(R.id.layoutStatusIconBg);
        btnSave = findViewById(R.id.btnSave);
    }

    /**
     * Listeners for height and weight SeekBars.
     * Updates the UI text and triggers recalculation instantly on every slider movement.
     */
    private void setupSeekBars() {
        seekBarHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 50) progress = 50; // Safety: Minimum height limit
                currentHeightCm = progress;
                tvHeightVal.setText(String.valueOf(currentHeightCm));
                calculateBmi();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarWeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 20) progress = 20; // Safety: Minimum weight limit
                currentWeightKg = progress;
                tvWeightVal.setText(String.valueOf((int)currentWeightKg));
                calculateBmi();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * FETCH LOGIC: Retrieves only the latest BMI record for the logged-in user.
     * This ensures the sliders are set to the user's actual last-known measurements.
     */
    private void fetchLastBmiData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            // Path: /users/{uid}/BMI_track/ -> Ordered by key, limited to last 1
            mDatabase.child("users").child(uid).child("BMI_track")
                    .orderByKey().limitToLast(1)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    Long height = child.child("height").getValue(Long.class);
                                    Object weightObj = child.child("weight").getValue();

                                    if (height != null) currentHeightCm = height.intValue();
                                    if (weightObj != null) currentWeightKg = Float.parseFloat(weightObj.toString());

                                    // Restore UI component states
                                    seekBarHeight.setProgress(currentHeightCm);
                                    seekBarWeight.setProgress((int) currentWeightKg);
                                    tvHeightVal.setText(String.valueOf(currentHeightCm));
                                    tvWeightVal.setText(String.valueOf((int)currentWeightKg));
                                    calculateBmi();
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    /**
     * CALCULATION LOGIC: BMI = Weight(kg) / (Height(m) * Height(m))
     */
    private void calculateBmi() {
        float heightM = currentHeightCm / 100f; // Convert cm to meters
        if (heightM > 0) {
            calculatedBmi = currentWeightKg / (heightM * heightM);
        }
        tvBmiScore.setText(df.format(calculatedBmi));
        updateStatusUI(calculatedBmi);
    }

    /**
     * UI FEEDBACK LOGIC: Updates the color theme and description based on WHO BMI categories.
     *

     [Image of BMI category chart]

     */
    private void updateStatusUI(float bmi) {
        int colorRes;
        int bgRes;
        String desc;

        if (bmi < 18.5) {
            currentStatus = "Underweight";
            colorRes = Color.parseColor("#FACC15"); // Yellow
            bgRes = Color.parseColor("#FEFCE8");
            desc = "You are underweight. Consider consulting a nutritionist.";
        } else if (bmi >= 18.5 && bmi < 24.9) {
            currentStatus = "Normal Weight";
            colorRes = Color.parseColor("#10B981"); // Green
            bgRes = Color.parseColor("#E6FFFA");
            desc = "Your BMI is in the healthy range.";
        } else if (bmi >= 25 && bmi < 29.9) {
            currentStatus = "Overweight";
            colorRes = Color.parseColor("#F97316"); // Orange
            bgRes = Color.parseColor("#FFF7ED");
            desc = "You are slightly overweight. Exercise can help.";
        } else {
            currentStatus = "Obese";
            colorRes = Color.parseColor("#EF4444"); // Red
            bgRes = Color.parseColor("#FEF2F2");
            desc = "Your BMI indicates obesity. Please consult a doctor.";
        }

        tvStatusTitle.setText(currentStatus);
        tvStatusDesc.setText(desc);
        tvStatusTitle.setTextColor(colorRes);
        imgStatusIcon.setColorFilter(colorRes);
        layoutStatusIconBg.setBackgroundTintList(ColorStateList.valueOf(bgRes));
    }

    /**
     * PERSISTENCE LOGIC: Pushes the measurement record to Firebase with a timestamp.
     */
    private void saveToFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            // Generate unique push key for this history entry
            String recordKey = mDatabase.child("users").child(uid).child("BMI_track").push().getKey();

            Map<String, Object> bmiMap = new HashMap<>();
            // Clean rounding for the database entry
            bmiMap.put("bmi_score", Double.parseDouble(df.format(calculatedBmi)));
            bmiMap.put("height", currentHeightCm);
            bmiMap.put("weight", currentWeightKg);
            bmiMap.put("status", currentStatus);
            bmiMap.put("timestamp", System.currentTimeMillis());

            if (recordKey != null) {
                mDatabase.child("users").child(uid).child("BMI_track").child(recordKey)
                        .setValue(bmiMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Measurements Saved!", Toast.LENGTH_SHORT).show();
                            // Return to Home cleanly and clear activity stack
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}