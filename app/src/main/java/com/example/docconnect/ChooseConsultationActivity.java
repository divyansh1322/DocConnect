package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class ChooseConsultationActivity extends AppCompatActivity {

    // UI Components - Cast as MaterialCardView for stroke control
    private MaterialCardView cardChat, cardWalkIn;
    private MaterialButton btnContinue;
    private ImageView btnBack;

    // State variable
    private String selectedConsultationType = "CHAT";

    // Doctor Data
    private String doctorId, doctorName, doctorSpecialty, doctorFee, doctorImage, doctorRatings, clinicAddress, clinicName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_consultation);

        receiveAndValidateIntent();
        initViews();

        if (savedInstanceState != null) {
            selectedConsultationType = savedInstanceState.getString("selected_type", "CHAT");
        }

        // Initialize the UI state
        updateSelection(selectedConsultationType);
    }

    private void receiveAndValidateIntent() {
        Intent incomingIntent = getIntent();
        if (incomingIntent == null) {
            handleMissingData();
            return;
        }

        doctorId = incomingIntent.getStringExtra("id");
        doctorName = incomingIntent.getStringExtra("doctorName");
        doctorSpecialty = incomingIntent.getStringExtra("doctorSpecialty");
        doctorFee = incomingIntent.getStringExtra("doctorFee");
        doctorImage = incomingIntent.getStringExtra("doctorImage");
        doctorRatings = incomingIntent.getStringExtra("ratings");
        clinicName = incomingIntent.getStringExtra("clinicName");
        clinicAddress = incomingIntent.getStringExtra("clinicAddress");

        if (doctorId == null || doctorId.isEmpty()) {
            handleMissingData();
        }
    }

    private void handleMissingData() {
        Toast.makeText(this, "Doctor information missing", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        cardChat = findViewById(R.id.cardChat);
        cardWalkIn = findViewById(R.id.cardWalkIn);
        btnContinue = findViewById(R.id.btnContinue);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        cardChat.setOnClickListener(v -> updateSelection("CHAT"));
        cardWalkIn.setOnClickListener(v -> updateSelection("WALKIN"));

        btnContinue.setOnClickListener(v -> proceedToBooking());
    }

    private void updateSelection(String type) {
        selectedConsultationType = type;

        if ("CHAT".equals(type)) {
            applySelectedStyle(cardChat, R.color.doc_primary);
            applyUnselectedStyle(cardWalkIn);
        } else {
            applySelectedStyle(cardWalkIn, R.color.doc_success);
            applyUnselectedStyle(cardChat);
        }
    }

    /**
     * Applies stroke and elevation using MaterialCardView methods.
     * No XML drawables required.
     */
    private void applySelectedStyle(MaterialCardView card, int colorResId) {
        if (card == null) return;

        // Set stroke width (typically 2dp to 3dp converted to pixels)
        card.setStrokeWidth(6);
        card.setStrokeColor(ContextCompat.getColor(this, colorResId));
        card.setCardElevation(12f);

        // Optional: MaterialCardView has a built-in checked state
        card.setChecked(true);
    }

    private void applyUnselectedStyle(MaterialCardView card) {
        if (card == null) return;

        // Remove stroke by setting width to 0
        card.setStrokeWidth(0);
        card.setCardElevation(2f);
        card.setChecked(false);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selected_type", selectedConsultationType);
    }

    private void proceedToBooking() {
        Intent nextIntent = new Intent(this, BookAppointmentActivity.class);
        nextIntent.putExtra("CONSULTATION_MEDIUM", selectedConsultationType);
        nextIntent.putExtra("id", doctorId);
        nextIntent.putExtra("doctorName", doctorName);
        nextIntent.putExtra("doctorSpecialty", doctorSpecialty);
        nextIntent.putExtra("doctorFee", doctorFee);
        nextIntent.putExtra("doctorImage", doctorImage);
        nextIntent.putExtra("ratings", doctorRatings);
        nextIntent.putExtra("clinicName", clinicName);
        nextIntent.putExtra("clinicAddress", clinicAddress);
        startActivity(nextIntent);
    }
}