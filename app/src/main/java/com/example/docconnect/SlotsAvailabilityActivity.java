package com.example.docconnect;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;
import java.util.Locale;

public class SlotsAvailabilityActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private LinearLayout containerStart, containerEnd, containerBreakStart, containerBreakEnd;
    private TextView tvStartValue, tvEndValue, tvBreakStartValue, tvBreakEndValue;
    private TextView tvSlot15, tvSlot20, tvSlot30;
    private SwitchMaterial switchBuffer;
    private TextView tvBuffer5, tvBuffer10, tvBuffer15;
    private Button btnNext;

    // Default Configuration Logic
    private int selectedSlotDuration = 20;
    private int selectedBufferTime = 5;
    private String startTime = "09:00 AM", endTime = "05:00 PM";
    private String breakStart = "01:00 PM", breakEnd = "02:00 PM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slots_availabilityxml);

        initializeViews();
        setupClickListeners();

        // Initial UI Sync
        updateSlotUI();
        updateBufferUI();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        switchBuffer = findViewById(R.id.switchBuffer);

        containerStart = findViewById(R.id.containerStart);
        containerEnd = findViewById(R.id.containerEnd);
        containerBreakStart = findViewById(R.id.containerBreakStart);
        containerBreakEnd = findViewById(R.id.containerBreakEnd);

        // Map to the second child (Index 1) of your LinearLayer containers
        tvStartValue = (TextView) containerStart.getChildAt(1);
        tvEndValue = (TextView) containerEnd.getChildAt(1);
        tvBreakStartValue = (TextView) containerBreakStart.getChildAt(1);
        tvBreakEndValue = (TextView) containerBreakEnd.getChildAt(1);

        tvSlot15 = findViewById(R.id.tvSlot15);
        tvSlot20 = findViewById(R.id.tvSlot20);
        tvSlot30 = findViewById(R.id.tvSlot30);
        tvBuffer5 = findViewById(R.id.tvBuffer5);
        tvBuffer10 = findViewById(R.id.tvBuffer10);
        tvBuffer15 = findViewById(R.id.tvBuffer15);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Time Picker Click Events
        containerStart.setOnClickListener(v -> showPicker(tvStartValue, t -> startTime = t));
        containerEnd.setOnClickListener(v -> showPicker(tvEndValue, t -> endTime = t));
        containerBreakStart.setOnClickListener(v -> showPicker(tvBreakStartValue, t -> breakStart = t));
        containerBreakEnd.setOnClickListener(v -> showPicker(tvBreakEndValue, t -> breakEnd = t));

        // Slot Selectors
        View.OnClickListener slotListener = v -> {
            int id = v.getId();
            if (id == R.id.tvSlot15) selectedSlotDuration = 15;
            else if (id == R.id.tvSlot20) selectedSlotDuration = 20;
            else if (id == R.id.tvSlot30) selectedSlotDuration = 30;
            updateSlotUI();
        };
        tvSlot15.setOnClickListener(slotListener);
        tvSlot20.setOnClickListener(slotListener);
        tvSlot30.setOnClickListener(slotListener);

        // Buffer Toggle & Selectors
        switchBuffer.setOnCheckedChangeListener((btn, isChecked) -> {
            float alpha = isChecked ? 1.0f : 0.4f;
            tvBuffer5.setAlpha(alpha);
            tvBuffer10.setAlpha(alpha);
            tvBuffer15.setAlpha(alpha);
            if (!isChecked) resetStyles(tvBuffer5, tvBuffer10, tvBuffer15);
            else updateBufferUI();
        });

        View.OnClickListener bufferListener = v -> {
            if (!switchBuffer.isChecked()) return;
            int id = v.getId();
            if (id == R.id.tvBuffer5) selectedBufferTime = 5;
            else if (id == R.id.tvBuffer10) selectedBufferTime = 10;
            else if (id == R.id.tvBuffer15) selectedBufferTime = 15;
            updateBufferUI();
        };
        tvBuffer5.setOnClickListener(bufferListener);
        tvBuffer10.setOnClickListener(bufferListener);
        tvBuffer15.setOnClickListener(bufferListener);

        // Final Navigation
        btnNext.setOnClickListener(v -> {
            ShiftConfiguration config = new ShiftConfiguration(
                    startTime, endTime, breakStart, breakEnd,
                    selectedSlotDuration, switchBuffer.isChecked(),
                    switchBuffer.isChecked() ? selectedBufferTime : 0
            );

            Intent intent = new Intent(this, SelectDatesActivity.class);
            intent.putExtra("SHIFT_CONFIG", config);
            startActivity(intent);
        });
    }

    private void showPicker(TextView target, OnTimeSelected listener) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, h, m) -> {
            String amPm = (h >= 12) ? "PM" : "AM";
            int h12 = (h > 12) ? h - 12 : (h == 0 ? 12 : h);
            String time = String.format(Locale.US, "%02d:%02d %s", h12, m, amPm);
            target.setText(time);
            listener.onSelected(time);
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }

    private void updateSlotUI() {
        resetStyles(tvSlot15, tvSlot20, tvSlot30);
        if (selectedSlotDuration == 15) highlight(tvSlot15);
        else if (selectedSlotDuration == 20) highlight(tvSlot20);
        else if (selectedSlotDuration == 30) highlight(tvSlot30);
    }

    private void updateBufferUI() {
        resetStyles(tvBuffer5, tvBuffer10, tvBuffer15);
        if (selectedBufferTime == 5) highlight(tvBuffer5);
        else if (selectedBufferTime == 10) highlight(tvBuffer10);
        else if (selectedBufferTime == 15) highlight(tvBuffer15);
    }

    private void highlight(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_segment_selected);
        tv.setTextColor(ContextCompat.getColor(this, R.color.doc_primary));
    }

    private void resetStyles(TextView... views) {
        for (TextView v : views) {
            v.setBackgroundResource(0);
            v.setTextColor(ContextCompat.getColor(this, R.color.doc_text_body));
        }
    }

    interface OnTimeSelected { void onSelected(String time); }
}