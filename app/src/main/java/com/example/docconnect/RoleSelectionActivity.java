package com.example.docconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * RoleSelectionActivity: Handles first-time persona selection.
 * Updated to ensure SharedPreferences sync with Firebase nodes.
 */
public class RoleSelectionActivity extends AppCompatActivity {

    private MaterialCardView cvUserRole, cvDoctorRole;
    private Button btnContinue;
    private String selectedRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. AUTO-SKIP LOGIC
        // If the user lands here but is already authenticated, route them correctly
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            routeLoggedInUser(currentUser.getUid());
            return;
        }

        setContentView(R.layout.activity_role_selection);

        // 2. Initialize UI
        cvUserRole = findViewById(R.id.cvUserRole);
        cvDoctorRole = findViewById(R.id.cvDoctorRole);
        btnContinue = findViewById(R.id.btnContinue);

        cvUserRole.setOnClickListener(v -> selectRole("user"));
        cvDoctorRole.setOnClickListener(v -> selectRole("doctor"));

        btnContinue.setOnClickListener(v -> {
            if (selectedRole.isEmpty()) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            // Persistence: Critical for MainActivity menu inflation
            saveRoleLocally(selectedRole);

            // Routing
            Intent intent = selectedRole.equals("user") ?
                    new Intent(this, LoginActivity.class) :
                    new Intent(this, DoctorLoginActivity.class);

            startActivity(intent);
        });
    }

    /**
     * Helper to persist role choice.
     */
    private void saveRoleLocally(String role) {
        SharedPreferences prefs = getSharedPreferences("DocConnectData", MODE_PRIVATE);
        prefs.edit().putString("selected_role", role).apply();
    }

    /**
     * Ensures returning users land on the right dashboard with the right role saved.
     */
    private void routeLoggedInUser(String uid) {
        FirebaseDatabase.getInstance().getReference("admins").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            startActivity(new Intent(RoleSelectionActivity.this, AdminDashboardActivity.class));
                            finish();
                        } else {
                            detectRoleAndNavigate(uid);
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) { }
                });
    }

    /**
     * Logic Fix: Detects role from DB and saves it before going to MainActivity.
     */
    private void detectRoleAndNavigate(String uid) {
        FirebaseDatabase.getInstance().getReference("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            saveRoleLocally("user");
                        } else {
                            // Assume doctor if not in user node (standard DocConnect logic)
                            saveRoleLocally("doctor");
                        }
                        startActivity(new Intent(RoleSelectionActivity.this, MainActivity.class));
                        finish();
                    }
                    @Override public void onCancelled(DatabaseError error) { }
                });
    }

    private void selectRole(String role) {
        selectedRole = role;
        int colorSelected = ContextCompat.getColor(this, R.color.doc_primary_dark);
        int colorUnselected = ContextCompat.getColor(this, R.color.doc_divider);

        if (role.equals("user")) {
            applyCardStyle(cvUserRole, colorSelected, 8, 8f);
            applyCardStyle(cvDoctorRole, colorUnselected, 2, 0f);
        } else {
            applyCardStyle(cvDoctorRole, colorSelected, 8, 8f);
            applyCardStyle(cvUserRole, colorUnselected, 2, 0f);
        }
    }

    private void applyCardStyle(MaterialCardView card, int color, int width, float elevation) {
        card.setStrokeColor(color);
        card.setStrokeWidth(width);
        card.setCardElevation(elevation);
    }
}