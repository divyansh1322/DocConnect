package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeneratedSlotsPreviewActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView recyclerSlots;
    private TextView tvTitle;
    private AppCompatButton btnUpload;
    private ImageView btnBack;

    // Data State
    private ShiftConfiguration config;
    private long[] selectedDates;
    private final List<DoctorSlotModel> allGeneratedSlots = new ArrayList<>();
    private DoctorSlotAdapter adapter;

    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generated_slots_preview);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        recyclerSlots = findViewById(R.id.recyclerSlots);
        tvTitle = findViewById(R.id.tvTitle);
        btnUpload = findViewById(R.id.btnUploadFirebase);
        btnBack = findViewById(R.id.btnBack);

        // Initialize Adapter with the Delete Listener
        adapter = new DoctorSlotAdapter(allGeneratedSlots, new DoctorSlotAdapter.OnSlotDeleteListener() {
            @Override
            public void onDeleteRequested(int position, DoctorSlotModel slot) {
                handleSlotDeletion(position, slot);
            }
        });

        recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        recyclerSlots.setAdapter(adapter);

        if (getIntent().hasExtra("SHIFT_CONFIG") && getIntent().hasExtra("SELECTED_DATES")) {
            config = (ShiftConfiguration) getIntent().getSerializableExtra("SHIFT_CONFIG");
            selectedDates = getIntent().getLongArrayExtra("SELECTED_DATES");
            generateAllSlots();
        } else {
            Toast.makeText(this, "Configuration error", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBack.setOnClickListener(v -> finish());
        btnUpload.setOnClickListener(v -> uploadSlotsSafely());
    }

    /**
     * Handles immediate UI removal and provides an Undo option.
     */
    private void handleSlotDeletion(int position, DoctorSlotModel deletedSlot) {
        // 1. Remove from the local list and notify adapter immediately
        allGeneratedSlots.remove(position);
        adapter.notifyItemRemoved(position);
        updateTitleCount();

        // 2. Show Snackbar with Undo action
        Snackbar snackbar = Snackbar.make(recyclerSlots, "Slot removed", Snackbar.LENGTH_LONG);
        snackbar.setAction("UNDO", v -> {
            // Restore the item
            allGeneratedSlots.add(position, deletedSlot);
            adapter.notifyItemInserted(position);
            updateTitleCount();
            recyclerSlots.scrollToPosition(position);
        });

        snackbar.show();
    }

    private void updateTitleCount() {
        tvTitle.setText("Review " + allGeneratedSlots.size() + " Slots");
    }

    // ================= SLOT GENERATION LOGIC =================

    private void generateAllSlots() {
        allGeneratedSlots.clear();

        int[] start = parseTime(config.startTime);
        int[] end = parseTime(config.endTime);
        int[] breakStart = parseTime(config.breakStartTime);
        int[] breakEnd = parseTime(config.breakEndTime);

        long slotMillis = config.slotDurationMinutes * 60 * 1000L;
        long bufferMillis = config.isBufferEnabled
                ? config.bufferDurationMinutes * 60 * 1000L
                : 0;

        for (long dateMillis : selectedDates) {
            long dayStart = mergeTime(dateMillis, start[0], start[1]);
            long dayEnd = mergeTime(dateMillis, end[0], end[1]);
            long breakStartMillis = mergeTime(dateMillis, breakStart[0], breakStart[1]);
            long breakEndMillis = mergeTime(dateMillis, breakEnd[0], breakEnd[1]);

            long pointer = dayStart;

            while (pointer + slotMillis <= dayEnd) {
                long slotEnd = pointer + slotMillis;

                if (pointer < breakEndMillis && slotEnd > breakStartMillis) {
                    pointer = Math.max(slotEnd, breakEndMillis);
                    continue;
                }

                String timeLabel = formatTime(pointer) + " - " + formatTime(slotEnd);
                String dateLabel = new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(dateMillis);
                String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dateMillis);

                DoctorSlotModel slot = new DoctorSlotModel(
                        dateLabel,
                        timeLabel,
                        pointer,
                        slotEnd,
                        dateKey
                );

                slot.setStatus("AVAILABLE");
                allGeneratedSlots.add(slot);
                pointer = slotEnd + bufferMillis;
            }
        }

        adapter.notifyDataSetChanged();
        updateTitleCount();
    }

    // ================= FIREBASE SYNC =================

    private void uploadSlotsSafely() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (allGeneratedSlots.isEmpty()) {
            Toast.makeText(this, "No slots to upload", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpload.setEnabled(false);
        btnUpload.setText("Uploading...");

        String doctorId = mAuth.getCurrentUser().getUid();
        DatabaseReference slotsRef = mDatabase.child("doctors")
                .child(doctorId)
                .child("allslots");

        slotsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                int skippedCount = 0;

                for (DoctorSlotModel slot : allGeneratedSlots) {
                    String slotId = formatTimeForId(slot.getStartMillis())
                            + "_" + formatTimeForId(slot.getEndMillis());

                    if (snapshot.child(slot.getDateKey()).child(slotId).exists()) {
                        skippedCount++;
                        continue;
                    }

                    updates.put(slot.getDateKey() + "/" + slotId, slot);
                }

                if (updates.isEmpty()) {
                    resetUploadButton("Confirm & Upload");
                    Toast.makeText(GeneratedSlotsPreviewActivity.this,
                            "No new slots ( " + skippedCount + " already exist)",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                final int finalSkipped = skippedCount;
                slotsRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(GeneratedSlotsPreviewActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toast.makeText(GeneratedSlotsPreviewActivity.this,
                                "Uploaded successfully (" + finalSkipped + " skipped)",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        resetUploadButton("Retry Upload");
                        Toast.makeText(GeneratedSlotsPreviewActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                resetUploadButton("Confirm & Upload");
            }
        });
    }

    private void resetUploadButton(String text) {
        btnUpload.setEnabled(true);
        btnUpload.setText(text);
    }

    // ================= UTILITIES =================

    private int[] parseTime(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            Date d = sdf.parse(time);
            Calendar c = Calendar.getInstance();
            if (d != null) {
                c.setTime(d);
                return new int[]{c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)};
            }
        } catch (Exception ignored) {}
        return new int[]{9, 0};
    }

    private long mergeTime(long dateMillis, int hour, int min) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dateMillis);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("hh:mm a", Locale.US).format(new Date(millis));
    }

    private String formatTimeForId(long millis) {
        return new SimpleDateFormat("HHmm", Locale.US).format(new Date(millis));
    }
}