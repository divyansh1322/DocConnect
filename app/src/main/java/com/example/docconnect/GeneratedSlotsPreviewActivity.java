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

/**
 * This Activity allows Doctors to preview generated time slots based on their shift configuration.
 * It handles logic for: 1. Calculating time slots, 2. Manual deletion, 3. Firebase Uploading.
 */
public class GeneratedSlotsPreviewActivity extends AppCompatActivity {

    // --- UI Components ---
    private RecyclerView recyclerSlots; // List to display the generated slots
    private TextView tvTitle;           // Shows the count of slots (e.g., "Review 10 Slots")
    private AppCompatButton btnUpload;  // The button to save everything to Firebase
    private ImageView btnBack;          // Navigation back button

    // --- Data State ---
    private ShiftConfiguration config;  // Holds the user's settings (start time, end time, etc.)
    private long[] selectedDates;       // Array of dates (in milliseconds) selected by the doctor
    private final List<DoctorSlotModel> allGeneratedSlots = new ArrayList<>(); // The master list of slots
    private DoctorSlotAdapter adapter;  // Bridge between the data list and the RecyclerView UI

    // --- Firebase ---
    private DatabaseReference mDatabase; // Reference to Google Firebase Realtime Database
    private FirebaseAuth mAuth;         // Used to get the current Doctor's Unique ID (UID)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generated_slots_preview);

        // 1. Initialize Firebase instances
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // 2. Link Java variables to XML layout IDs
        recyclerSlots = findViewById(R.id.recyclerSlots);
        tvTitle = findViewById(R.id.tvTitle);
        btnUpload = findViewById(R.id.btnUploadFirebase);
        btnBack = findViewById(R.id.btnBack);

        // 3. Setup the Adapter with a "Delete Listener"
        // This interface handles what happens when a user clicks the delete icon on a specific slot
        adapter = new DoctorSlotAdapter(allGeneratedSlots, new DoctorSlotAdapter.OnSlotDeleteListener() {
            @Override
            public void onDeleteRequested(int position, DoctorSlotModel slot) {
                handleSlotDeletion(position, slot);
            }
        });

        // 4. Configure RecyclerView (Vertical list layout)
        recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        recyclerSlots.setAdapter(adapter);

        // 5. Retrieve Data sent from the previous Activity (Shift setup screen)
        if (getIntent().hasExtra("SHIFT_CONFIG") && getIntent().hasExtra("SELECTED_DATES")) {
            config = (ShiftConfiguration) getIntent().getSerializableExtra("SHIFT_CONFIG");
            selectedDates = getIntent().getLongArrayExtra("SELECTED_DATES");

            // Start the math to generate individual time slots
            generateAllSlots();
        } else {
            // If data is missing, close the screen to prevent crashes
            Toast.makeText(this, "Configuration error", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 6. Setup Click Listeners
        btnBack.setOnClickListener(v -> finish());
        btnUpload.setOnClickListener(v -> uploadSlotsSafely());
    }

    /**
     * Removes a slot from the preview list.
     * Provides an "UNDO" option using a Snackbar in case of accidental clicks.
     */
    private void handleSlotDeletion(int position, DoctorSlotModel deletedSlot) {
        // Remove from list and update UI
        allGeneratedSlots.remove(position);
        adapter.notifyItemRemoved(position);
        updateTitleCount();

        // Show popup with Undo button
        Snackbar snackbar = Snackbar.make(recyclerSlots, "Slot removed", Snackbar.LENGTH_LONG);
        snackbar.setAction("UNDO", v -> {
            // If user clicks UNDO, put the item back exactly where it was
            allGeneratedSlots.add(position, deletedSlot);
            adapter.notifyItemInserted(position);
            updateTitleCount();
            recyclerSlots.scrollToPosition(position);
        });
        snackbar.show();
    }

    /**
     * Updates the text at the top of the screen to show how many slots are ready.
     */
    private void updateTitleCount() {
        tvTitle.setText("Review " + allGeneratedSlots.size() + " Slots");
    }

    // ================= SLOT GENERATION LOGIC (THE BRAINS) =================

    /**
     * Loops through every selected date and creates time intervals based on user config.
     */
    private void generateAllSlots() {
        allGeneratedSlots.clear(); // Clear list before starting

        // Convert String times (e.g., "09:00 AM") into Hour/Minute integers
        int[] start = parseTime(config.startTime);
        int[] end = parseTime(config.endTime);
        int[] breakStart = parseTime(config.breakStartTime);
        int[] breakEnd = parseTime(config.breakEndTime);

        // Convert durations into Milliseconds for easier math
        long slotMillis = config.slotDurationMinutes * 60 * 1000L;
        long bufferMillis = config.isBufferEnabled
                ? config.bufferDurationMinutes * 60 * 1000L
                : 0;

        // OUTER LOOP: For every date selected (e.g., Monday, Tuesday)
        for (long dateMillis : selectedDates) {

            // Combine the specific Date with the Start/End times
            long dayStart = mergeTime(dateMillis, start[0], start[1]);
            long dayEnd = mergeTime(dateMillis, end[0], end[1]);
            long breakStartMillis = mergeTime(dateMillis, breakStart[0], breakStart[1]);
            long breakEndMillis = mergeTime(dateMillis, breakEnd[0], breakEnd[1]);

            // The 'pointer' moves forward as we create slots
            long pointer = dayStart;

            // INNER LOOP: Keep creating slots until we hit the end of the shift
            while (pointer + slotMillis <= dayEnd) {
                long slotEnd = pointer + slotMillis;

                // CHECK: If this slot overlaps with the Break time, skip it
                if (pointer < breakEndMillis && slotEnd > breakStartMillis) {
                    pointer = Math.max(slotEnd, breakEndMillis);
                    continue;
                }

                // Prepare labels for the UI
                String timeLabel = formatTime(pointer) + " - " + formatTime(slotEnd);
                String dateLabel = new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(dateMillis);
                String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dateMillis);

                // Create the Slot Object
                DoctorSlotModel slot = new DoctorSlotModel(
                        dateLabel,
                        timeLabel,
                        pointer,
                        slotEnd,
                        dateKey
                );

                slot.setStatus("AVAILABLE"); // Initial state
                allGeneratedSlots.add(slot);

                // Move the pointer forward (Slot length + Buffer gap)
                pointer = slotEnd + bufferMillis;
            }
        }

        adapter.notifyDataSetChanged(); // Refresh the list on screen
        updateTitleCount();
    }

    // ================= FIREBASE SYNC =================

    /**
     * Uploads the list to Firebase under: doctors -> [UID] -> allslots -> [DateKey]
     */
    private void uploadSlotsSafely() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (allGeneratedSlots.isEmpty()) {
            Toast.makeText(this, "No slots to upload", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent double-clicks
        btnUpload.setEnabled(false);
        btnUpload.setText("Uploading...");

        String doctorId = mAuth.getCurrentUser().getUid();
        DatabaseReference slotsRef = mDatabase.child("doctors")
                .child(doctorId)
                .child("allslots");

        // First, check if slots already exist to avoid duplicates
        slotsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                int skippedCount = 0;

                for (DoctorSlotModel slot : allGeneratedSlots) {
                    // Create a unique ID for the slot based on its time (e.g., "0900_0930")
                    String slotId = formatTimeForId(slot.getStartMillis())
                            + "_" + formatTimeForId(slot.getEndMillis());

                    // If slot exists in DB already, don't overwrite it
                    if (snapshot.child(slot.getDateKey()).child(slotId).exists()) {
                        skippedCount++;
                        continue;
                    }

                    // Add to our batch update map
                    updates.put(slot.getDateKey() + "/" + slotId, slot);
                }

                if (updates.isEmpty()) {
                    resetUploadButton("Confirm & Upload");
                    Toast.makeText(GeneratedSlotsPreviewActivity.this,
                            "No new slots ( " + skippedCount + " already exist)",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // Perform all updates in one network call
                final int finalSkipped = skippedCount;
                slotsRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Success! Return to Main Screen
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

    // ================= UTILITIES (Formatters) =================

    /**
     * Converts "09:00 AM" String into int[]{9, 0}
     */
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
        return new int[]{9, 0}; // Default fallback
    }

    /**
     * Combines a date millisecond with specific hours and minutes.
     */
    private long mergeTime(long dateMillis, int hour, int min) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dateMillis);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /**
     * Converts millis back to readable "09:00 AM" for UI
     */
    private String formatTime(long millis) {
        return new SimpleDateFormat("hh:mm a", Locale.US).format(new Date(millis));
    }

    /**
     * Converts millis to "0900" for Firebase Key IDs
     */
    private String formatTimeForId(long millis) {
        return new SimpleDateFormat("HHmm", Locale.US).format(new Date(millis));
    }
}