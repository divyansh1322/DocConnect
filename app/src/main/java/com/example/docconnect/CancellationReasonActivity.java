package com.example.docconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CancellationReasonActivity: Professional Implementation
 * Features: Selection state management, anti-duplicate submission, and lifecycle-safe Firebase writes.
 */
public class CancellationReasonActivity extends AppCompatActivity {

    // Selection Variables
    private String selectedReason = "";
    private View layoutSymptoms, layoutFinancial, layoutFamily, layoutOther;
    private List<View> reasonLayouts;

    // UI Components
    private ImageView btnBack;
    private TextInputEditText etComments;
    private MaterialButton btnConfirm;

    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancellation_reason);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 2. Initialize Views
        etComments = findViewById(R.id.etComments);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnBack = findViewById(R.id.btnBack);

        // 3. Setup Selection Logic
        setupReasonSelection();

        // 4. Handle Button Listeners
        btnConfirm.setOnClickListener(v -> submitCancellation());

        // Restoration Logic: Professional apps remember user selection on rotation
        if (savedInstanceState != null) {
            selectedReason = savedInstanceState.getString("selected_reason", "");
            restoreSelectionUI(selectedReason);
        }

        // Setup Back Button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        } else {
            View toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setOnClickListener(v -> finish());
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selected_reason", selectedReason);
    }

    /**
     * Finds the layout views for reasons and assigns their click listeners.
     */
    private void setupReasonSelection() {
        layoutSymptoms = findViewById(R.id.layout_symptoms);
        layoutFinancial = findViewById(R.id.layout_financial);
        layoutFamily = findViewById(R.id.layout_family);
        layoutOther = findViewById(R.id.layout_other);

        reasonLayouts = Arrays.asList(layoutSymptoms, layoutFinancial, layoutFamily, layoutOther);

        // Ensure listeners are only set if views exist
        if (layoutSymptoms != null) layoutSymptoms.setOnClickListener(v -> handleSelection(v, "Symptoms resolved"));
        if (layoutFinancial != null) layoutFinancial.setOnClickListener(v -> handleSelection(v, "Financial Issue"));
        if (layoutFamily != null) layoutFamily.setOnClickListener(v -> handleSelection(v, "Family Emergency"));
        if (layoutOther != null) layoutOther.setOnClickListener(v -> handleSelection(v, "Other reasons"));
    }

    /**
     * Updates the UI to highlight the selected reason.
     */
    private void handleSelection(View selectedView, String reason) {
        if (selectedView == null) return;
        selectedReason = reason;

        for (View view : reasonLayouts) {
            if (view != null) {
                if (view == selectedView) {
                    view.setBackgroundResource(R.drawable.bg_selected_radio_button);
                    view.setSelected(true);
                } else {
                    view.setBackgroundResource(R.drawable.bg_card_unselected);
                    view.setSelected(false);
                }
            }
        }
    }

    /**
     * Professional Helper: Restores UI state from saved string (used for rotation).
     */
    private void restoreSelectionUI(String reason) {
        if (reason == null || reason.isEmpty()) return;

        if (reason.equals("Symptoms resolved")) handleSelection(layoutSymptoms, reason);
        else if (reason.equals("Financial Issue")) handleSelection(layoutFinancial, reason);
        else if (reason.equals("Family Emergency")) handleSelection(layoutFamily, reason);
        else if (reason.equals("Other reasons")) handleSelection(layoutOther, reason);
    }

    /**
     * Validates data and pushes the cancellation reason to Firebase.
     */

    private void submitCancellation() {
        String userId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : null;
        String comments = (etComments.getText() != null) ? etComments.getText().toString().trim() : "";

        if (userId == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedReason.isEmpty()) {
            Toast.makeText(this, "Please select a reason.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare Data
        Map<String, Object> cancelData = new HashMap<>();
        cancelData.put("reason", selectedReason);
        cancelData.put("comments", comments);
        cancelData.put("timestamp", System.currentTimeMillis());

        // DEBOUNCING: Prevent duplicate network calls
        btnConfirm.setEnabled(false);

        mDatabase.child("users")
                .child(userId)
                .child("cancellationsReasons")
                .push()
                .setValue(cancelData)
                .addOnSuccessListener(aVoid -> {
                    if (!isFinishing()) {
                        Toast.makeText(this, "Cancellation recorded.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        btnConfirm.setEnabled(true);
                        Toast.makeText(this, "Submission failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}