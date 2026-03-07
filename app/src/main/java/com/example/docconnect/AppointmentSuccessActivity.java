package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * APPOINTMENT SUCCESS ACTIVITY
 * ----------------------------
 * Displays the confirmation receipt after a successful Firebase transaction.
 * Logic: Clears the Activity Task Stack to prevent returning to the booking flow.
 */
public class AppointmentSuccessActivity extends AppCompatActivity {

    private TextView tvDocName, tvSubtitle, tvBookingRef, tvDate, tvTime, tvFee, tvClinicName, tvClinicAddress;
    private ImageView imgDoctor;
    private MaterialButton btnDone;
    private MaterialCardView mainCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_success);

        initViews();
        handleBackPress();

        // 1. VISUAL POLISH: POP-IN ENTRY
        // Ensure R.anim.pop_in exists for a smooth scale-up effect
        Animation popIn = AnimationUtils.loadAnimation(this, R.anim.pop_in);
        mainCard.startAnimation(popIn);

        // 2. DATA BINDING FROM INTENT
        Intent intent = getIntent();
        if (intent != null) {
            bindData(intent);
        }

        // 3. LOGIC: NAVIGATE HOME & CLEAR STACK
        btnDone.setOnClickListener(v -> navigateToHome());
    }

    private void initViews() {
        tvDocName = findViewById(R.id.tvDocName);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvBookingRef = findViewById(R.id.tvBookingRef);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvFee = findViewById(R.id.tvFee);
        tvClinicName = findViewById(R.id.tvClinicName);
        tvClinicAddress = findViewById(R.id.tvClinicAddress);
        imgDoctor = findViewById(R.id.imgDoctor);
        btnDone = findViewById(R.id.btnDone);
        mainCard = findViewById(R.id.mainCard);
    }

    private void bindData(Intent intent) {
        // Format Booking ID (Short code for receipt UI)
        String bId = intent.getStringExtra("bookingId");
        if (bId != null && !bId.isEmpty()) {
            int maxLength = Math.min(bId.length(), 6);
            tvBookingRef.setText("#" + bId.substring(0, maxLength).toUpperCase());
        } else {
            tvBookingRef.setText("#DC-SUCC");
        }

        tvDocName.setText(intent.getStringExtra("doctorName"));
        tvSubtitle.setText(intent.getStringExtra("doctorSpecialty"));

        String fee = intent.getStringExtra("doctorFee");
        tvFee.setText(fee != null ? "Rs. " + fee : "N/A");

        tvClinicName.setText(intent.getStringExtra("clinicName"));
        tvClinicAddress.setText(intent.getStringExtra("clinicAddress"));
        tvTime.setText(intent.getStringExtra("time"));

        // Format raw date: "2026-02-20" -> "Fri, 20 Feb 2026"
        String rawDate = intent.getStringExtra("date");
        tvDate.setText(formatDate(rawDate));

        if (!isFinishing()) {
            Glide.with(this)
                    .load(intent.getStringExtra("doctorImage"))
                    .placeholder(R.drawable.ic_doctor)
                    .circleCrop()
                    .into(imgDoctor);
        }
    }

    /**
     * Logic: Uses Flags to clear the Task Stack.
     * This prevents the user from clicking 'Back' and seeing the booking screen again.
     */
    private void navigateToHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
        finish();
    }

    /**
     * Overrides hardware back button to follow the same "Clear Stack" logic.
     */
    private void handleBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToHome();
            }
        });
    }

    private String formatDate(String raw) {
        if (raw == null) return "Date TBD";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw);
            return new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(d);
        } catch (Exception e) {
            return raw;
        }
    }
}