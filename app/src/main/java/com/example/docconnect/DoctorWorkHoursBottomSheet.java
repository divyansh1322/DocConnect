package com.example.docconnect;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * BottomSheet fragment allowing doctors to configure their clinic's working hours.
 * Supports toggling general availability, selecting day ranges, and picking shift times.
 */
public class DoctorWorkHoursBottomSheet extends BottomSheetDialogFragment {

    // UI Components
    private ImageView btnBack;
    private SwitchCompat switchAvailability;
    private TextView tvAvailabilityStatus;
    private LinearLayout containerScheduleDetails;

    private TextView tvStartTime, tvEndTime;
    private MaterialCardView btnStartTime, btnEndTime, cardWeekdays, cardWeekend;
    private MaterialButton btnSaveSchedule;

    // Logic State Variables
    private boolean isAvailable = true;
    private String selectedDays = "Mon-Fri";

    // Firebase References
    private FirebaseAuth mAuth;
    private DatabaseReference doctorRef;

    // Callback interface for parent activity/fragment synchronization
    private OnScheduleSavedListener listener;

    public interface OnScheduleSavedListener {
        void onScheduleSaved();
    }

    public void setOnScheduleSavedListener(OnScheduleSavedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_doctor_work_hours, container, false);
    }

    /**
     * Standard BottomSheet initialization:
     * Sets background to transparent and forces the sheet to open in fully expanded mode.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String currentUserId = mAuth.getCurrentUser().getUid();
            // Connect to specific doctor node in Firebase Realtime Database
            doctorRef = FirebaseDatabase.getInstance()
                    .getReference("doctors")
                    .child(currentUserId);
        }

        initViews(view);
        setupListeners();
        loadScheduleData(); // Pre-fill with existing settings from the database
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        switchAvailability = view.findViewById(R.id.switchAvailability);
        tvAvailabilityStatus = view.findViewById(R.id.tvAvailabilityStatus);
        containerScheduleDetails = view.findViewById(R.id.containerScheduleDetails);
        tvStartTime = view.findViewById(R.id.tvStartTime);
        tvEndTime = view.findViewById(R.id.tvEndTime);
        btnStartTime = view.findViewById(R.id.btnStartTime);
        btnEndTime = view.findViewById(R.id.btnEndTime);
        cardWeekdays = view.findViewById(R.id.cardWeekdays);
        cardWeekend = view.findViewById(R.id.cardWeekend);
        btnSaveSchedule = view.findViewById(R.id.btnSaveSchedule);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> dismiss());

        // Toggle master availability (Enables/Disables the entire form)
        switchAvailability.setOnCheckedChangeListener((buttonView, checked) -> {
            isAvailable = checked;
            updateUIState(checked);
        });

        // Day range selection
        cardWeekdays.setOnClickListener(v -> updateDaysUI("Mon-Fri"));
        cardWeekend.setOnClickListener(v -> updateDaysUI("Sat-Sun"));

        // Time pickers
        btnStartTime.setOnClickListener(v -> showTimePicker(tvStartTime));
        btnEndTime.setOnClickListener(v -> showTimePicker(tvEndTime));

        btnSaveSchedule.setOnClickListener(v -> saveScheduleToFirebase());
    }

    /**
     * Manages visual state for the day-selection cards (Mon-Fri vs Sat-Sun).
     */
    private void updateDaysUI(String days) {
        if (!isAvailable || !isAdded()) return;
        selectedDays = days;

        int primary = ContextCompat.getColor(requireContext(), R.color.doc_primary);
        int surface = ContextCompat.getColor(requireContext(), R.color.doc_surface);
        int divider = ContextCompat.getColor(requireContext(), R.color.doc_divider);
        int primaryLight = ContextCompat.getColor(requireContext(), R.color.doc_primary_light);

        if ("Mon-Fri".equals(days)) {
            cardWeekdays.setStrokeColor(primary);
            cardWeekdays.setCardBackgroundColor(primaryLight);
            cardWeekend.setStrokeColor(divider);
            cardWeekend.setCardBackgroundColor(surface);
        } else {
            cardWeekend.setStrokeColor(primary);
            cardWeekend.setCardBackgroundColor(primaryLight);
            cardWeekdays.setStrokeColor(divider);
            cardWeekdays.setCardBackgroundColor(surface);
        }
    }

    /**
     * Disables/Enables input views based on the availability switch.
     * Uses alpha transparency to visually grey out the form when closed.
     */
    private void updateUIState(boolean enabled) {
        if (!isAdded()) return;
        containerScheduleDetails.setAlpha(enabled ? 1f : 0.4f);
        btnStartTime.setEnabled(enabled);
        btnEndTime.setEnabled(enabled);
        cardWeekdays.setEnabled(enabled);
        cardWeekend.setEnabled(enabled);

        if (enabled) {
            tvAvailabilityStatus.setText("Open for Appointments");
            tvAvailabilityStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.doc_primary));
        } else {
            tvAvailabilityStatus.setText("Clinic is Temporarily Closed");
            tvAvailabilityStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.doc_error));
        }
    }

    /**
     * Standard TimePickerDialog formatted into 12-hour AM/PM string for medical display.
     */
    private void showTimePicker(TextView targetView) {
        if (!isAvailable || !isAdded()) return;
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            String amPm = hourOfDay >= 12 ? "PM" : "AM";
            int hour12 = hourOfDay % 12;
            if (hour12 == 0) hour12 = 12;
            String time = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);
            targetView.setText(time);
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
    }

    /**
     * One-time fetch to retrieve existing work hours from Firebase.
     */
    private void loadScheduleData() {
        if (doctorRef == null) return;

        doctorRef.child("workHoursSchedule").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                String start = snapshot.child("startTime").getValue(String.class);
                String end = snapshot.child("endTime").getValue(String.class);
                String days = snapshot.child("days").getValue(String.class);
                Boolean available = snapshot.child("isAvailable").getValue(Boolean.class);

                if (start != null) tvStartTime.setText(start);
                if (end != null) tvEndTime.setText(end);
                if (days != null) updateDaysUI(days);

                if (available != null) {
                    switchAvailability.setChecked(available);
                    updateUIState(available);
                    isAvailable = available;
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /**
     * Performs an atomic update to save schedule data under the 'workHoursSchedule' child node.
     * This avoids overwriting critical fields like 'fullName' or 'speciality'.
     */
    private void saveScheduleToFirebase() {
        if (doctorRef == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Map individual schedule properties
        Map<String, Object> workHoursMap = new HashMap<>();
        workHoursMap.put("isAvailable", isAvailable);
        workHoursMap.put("days", selectedDays);
        workHoursMap.put("startTime", tvStartTime.getText().toString());
        workHoursMap.put("endTime", tvEndTime.getText().toString());

        // 2. Prepare multi-path update map
        Map<String, Object> finalUpdate = new HashMap<>();
        finalUpdate.put("workHoursSchedule", workHoursMap); // Nested node update

        //

        // 3. Commit the partial update
        doctorRef.updateChildren(finalUpdate)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Work hours saved!", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onScheduleSaved();
                    dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}