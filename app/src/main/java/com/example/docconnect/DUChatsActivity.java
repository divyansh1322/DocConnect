package com.example.docconnect;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * DUChatsActivity facilitates the real-time interaction between a Doctor and a Patient.
 * It handles message exchange, automatic session expiration (MISSED status),
 * and consultation completion.
 */
public class DUChatsActivity extends AppCompatActivity {

    private static final String TAG = "DU_CHATS_DEBUG";

    // --- UI Components ---
    private RecyclerView chatRecyclerView;
    private EditText etMessage;
    private FloatingActionButton btnSend;
    private ImageButton btnBack;
    private ShapeableImageView patientImage;
    private TextView tvPatientName, tvBookingId, tvStatusBadge;
    private MaterialButton btnCompleteAppointment, btnBackToHome;
    private MaterialCardView doctorInputContainer;
    private LinearLayout doctorCompletedView;

    // --- Logic & Firebase Variables ---
    private DUChatAdapter adapter;
    private ArrayList<DoctorChatModel.Message> messageList;
    private DatabaseReference chatRef, scheduleRef, rootRef;
    private String bookingId, currentUserId, patientId, doctorId;
    private DoctorChatModel doctorModel;
    private boolean isSessionActive = false;

    // Timer Logic for automatic session cleanup
    private final Handler autoMissedHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_du_chats);

        // 1. Recover Chat Data passed from the previous activity via Intent
        doctorModel = (DoctorChatModel) getIntent().getSerializableExtra("CHAT_DATA");

        if (doctorModel == null) {
            Log.e(TAG, "Intent Error: CHAT_DATA is null");
            Toast.makeText(this, "Session Data Error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize unique identifiers
        bookingId = doctorModel.getBookingId();
        patientId = doctorModel.getPatientId();
        doctorId = doctorModel.getDoctorId();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // 2. Initialize Firebase references
        rootRef = FirebaseDatabase.getInstance().getReference();
        chatRef = rootRef.child("Chats").child(bookingId); // Path for messages
        scheduleRef = rootRef.child("DoctorSchedule").child(currentUserId).child(bookingId); // Path for status updates

        initViews();
        setupHeader();
        setupRecyclerView();

        // 3. Start Data Listeners & Expiry Automation
        listenForMessages();        // Real-time chat messages
        checkAppointmentStatus();   // Observe if session is ACTIVE, COMPLETED, or MISSED
        startAutoMissedTimer();     // Start background countdown for session expiry

        // 4. Set UI Click Actions
        btnSend.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> finish());
        btnBackToHome.setOnClickListener(v -> finish());
        btnCompleteAppointment.setOnClickListener(v -> completeConsultation());
    }

    /**
     * Links XML layout components to Java variables.
     */
    private void initViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        patientImage = findViewById(R.id.patientImage);
        tvPatientName = findViewById(R.id.tvPatientName);
        tvBookingId = findViewById(R.id.tvBookingId);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        btnCompleteAppointment = findViewById(R.id.btnCompleteAppointment);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        doctorInputContainer = findViewById(R.id.doctorInputContainer);
        doctorCompletedView = findViewById(R.id.doctorCompletedView);
    }

    /**
     * Displays patient information and profile image in the top toolbar.
     */
    private void setupHeader() {
        tvPatientName.setText(doctorModel.getPatientName());
        tvBookingId.setText("ID: " + bookingId);

        if (doctorModel.getPatientImage() != null && !doctorModel.getPatientImage().isEmpty()) {
            Glide.with(this)
                    .load(doctorModel.getPatientImage())
                    .centerCrop()
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(patientImage);
        }
    }

    /**
     * Initializes the RecyclerView with a ChatAdapter and ensures it scrolls to the bottom.
     */
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        adapter = new DUChatAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Ensures newer messages appear at the bottom
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(adapter);
    }

    // --- AUTOMATION LOGIC (SESSION EXPIRY) ---

    /**
     * Calculates the time remaining for the appointment and schedules a 'Missed' check.
     */
    private void startAutoMissedTimer() {
        long expiryTime = getEndTimeMillis(doctorModel.getTime());
        long currentTime = System.currentTimeMillis();
        long delay = expiryTime - currentTime;

        if (delay > 0) {
            // Schedule the check for the future
            autoMissedHandler.postDelayed(this::checkAndMarkMissed, delay);
        } else {
            // Already past the slot time; check immediately
            checkAndMarkMissed();
        }
    }

    /**
     * Parses the time range string (e.g., "12:00am-12:50") to determine the exact millisecond expiry.
     * Includes a 5-minute grace period.
     */
    private long getEndTimeMillis(String slotRange) {
        try {
            String[] parts = slotRange.split("-");
            if (parts.length < 2) return 0;

            String startTimePart = parts[0].trim();
            String endTimePart = parts[1].trim();

            // Combine end time with am/pm context from start time
            String amPm = startTimePart.substring(startTimePart.length() - 2).toLowerCase();
            String fullEndTime = endTimePart + " " + amPm;

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf.parse(fullEndTime);

            Calendar calendar = Calendar.getInstance();
            Calendar timeCal = Calendar.getInstance();
            if (date != null) timeCal.setTime(date);

            calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, 0);

            // Returns End Time + 5 Minutes Grace Period
            return calendar.getTimeInMillis() + (5L * 60 * 1000);
        } catch (Exception e) {
            Log.e(TAG, "Time parsing failed: " + slotRange, e);
            return 0;
        }
    }

    /**
     * Checks current status and triggers MISSED update if the appointment was never completed.
     */
    private void checkAndMarkMissed() {
        scheduleRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                // Transition to MISSED only if it hasn't been COMPLETED yet
                if ("ACTIVE".equalsIgnoreCase(status) || "UPCOMING".equalsIgnoreCase(status)) {
                    markAsMissed();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Updates Firebase nodes for both Doctor and User to reflect a MISSED status.
     */
    private void markAsMissed() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/DoctorSchedule/" + currentUserId + "/" + bookingId + "/status", "MISSED");
        updates.put("/UserBookings/" + patientId + "/" + bookingId + "/status", "MISSED");

        rootRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Status set to MISSED automatically.");
        });
    }

    // --- CORE FUNCTIONALITY ---

    /**
     * Listens for changes in appointment status to toggle UI (Hide/Show chat box).
     */
    private void checkAppointmentStatus() {
        scheduleRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if (status == null) return;

                isSessionActive = "ACTIVE".equalsIgnoreCase(status);
                tvStatusBadge.setText(status.toUpperCase());

                if (isSessionActive) {
                    // Show message input and completion button
                    doctorInputContainer.setVisibility(View.VISIBLE);
                    btnCompleteAppointment.setVisibility(View.VISIBLE);
                    doctorCompletedView.setVisibility(View.GONE);
                } else {
                    // Disable interaction if session is COMPLETED, CANCELLED, or MISSED
                    doctorInputContainer.setVisibility(View.GONE);
                    btnCompleteAppointment.setVisibility(View.GONE);
                    doctorCompletedView.setVisibility(View.VISIBLE);
                    hideKeyboard();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Fetches all messages for the current booking ID and updates the RecyclerView.
     */
    private void listenForMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    DoctorChatModel.Message message = ds.getValue(DoctorChatModel.Message.class);
                    if (message != null) messageList.add(message);
                }
                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Pushes a new message to the Firebase database.
     */
    private void sendMessage() {
        if (!isSessionActive) {
            Toast.makeText(this, "Session is not active", Toast.LENGTH_SHORT).show();
            return;
        }

        String msgText = etMessage.getText().toString().trim();
        if (msgText.isEmpty()) return;

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("message", msgText);
        messageMap.put("senderId", currentUserId);
        messageMap.put("patientId", patientId);
        messageMap.put("doctorId", doctorId);
        messageMap.put("timestamp", ServerValue.TIMESTAMP); // Use Server-side time for accuracy
        messageMap.put("type", "text");

        chatRef.push().setValue(messageMap).addOnSuccessListener(aVoid -> {
            etMessage.setText(""); // Clear input on success
        });
    }

    /**
     * Marks the consultation as successful and increments the doctor's total patient count.
     */
    private void completeConsultation() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/DoctorSchedule/" + currentUserId + "/" + bookingId + "/status", "COMPLETED");
        updates.put("/UserBookings/" + patientId + "/" + bookingId + "/status", "COMPLETED");
        updates.put("/doctors/" + currentUserId + "/totalPatients", ServerValue.increment(1));

        rootRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Consultation Marked as Completed", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error completing consultation", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Utility to close the on-screen keyboard when a session ends.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Cleanup resources and stop the timer to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoMissedHandler.removeCallbacksAndMessages(null);
    }
}