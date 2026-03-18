package com.example.docconnect;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * DUChatsActivity: Production-level Chat Activity.
 * Handles real-time messaging and uses WorkManager to handle session expiration.
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

    // --- Time Sync ---
    private long serverTimeOffset = 0;
    private final SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_du_chats);

        // 1. Recover Chat Data
        doctorModel = (DoctorChatModel) getIntent().getSerializableExtra("CHAT_DATA");

        if (doctorModel == null) {
            Log.e(TAG, "Intent Error: CHAT_DATA is null");
            Toast.makeText(this, "Session Data Error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bookingId = doctorModel.getBookingId();
        patientId = doctorModel.getPatientId();
        doctorId = doctorModel.getDoctorId();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // 2. Initialize Firebase
        rootRef = FirebaseDatabase.getInstance().getReference();
        chatRef = rootRef.child("Chats").child(bookingId);
        scheduleRef = rootRef.child("DoctorSchedule").child(currentUserId).child(bookingId);

        initViews();
        setupHeader();
        setupRecyclerView();

        // 3. Start Data Listeners
        fetchServerTimeOffset();    // Needed for WorkManager timing
        listenForMessages();
        checkAppointmentStatus();

        // 4. Set UI Click Actions
        btnSend.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> finish());
        btnBackToHome.setOnClickListener(v -> finish());
        btnCompleteAppointment.setOnClickListener(v -> completeConsultation());
    }

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

    private void setupHeader() {
        tvPatientName.setText(doctorModel.getPatientName());
        tvBookingId.setText("ID: " + bookingId);

        if (doctorModel.getPatientImage() != null && !doctorModel.getPatientImage().isEmpty()) {
            Glide.with(this).load(doctorModel.getPatientImage()).centerCrop()
                    .placeholder(R.drawable.ic_person_placeholder).into(patientImage);
        }
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        adapter = new DUChatAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(adapter);
    }

    /**
     * SYNC TIME: Gets server offset to ensure WorkManager starts at the right moment.
     */
    private void fetchServerTimeOffset() {
        rootRef.child(".info/serverTimeOffset").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    serverTimeOffset = snapshot.getValue(Long.class);
                    // Once we have the time, ensure the expiry worker is scheduled
                    scheduleExpiryWorker();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    /**
     * WORKMANAGER INTEGRATION: Schedules the background task to mark MISSED.
     */
    private void scheduleExpiryWorker() {
        try {
            String endPart = doctorModel.getTime().contains("-") ?
                    doctorModel.getTime().split("-")[1].trim() : doctorModel.getTime().trim();
            Date endTime = dateTimeParser.parse(doctorModel.getDate() + " " + endPart);

            if (endTime == null) return;

            long serverNow = System.currentTimeMillis() + serverTimeOffset;
            long delay = (endTime.getTime() + (5 * 60 * 1000)) - serverNow; // 5 min grace

            if (delay > 0) {
                Data inputData = new Data.Builder()
                        .putString("booking_id", bookingId)
                        .putString("patient_id", patientId)
                        .build();

                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExpireSlotWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .setInputData(inputData)
                        .build();

                // Unique work ensures only one timer exists for this booking
                WorkManager.getInstance(this).enqueueUniqueWork(bookingId, ExistingWorkPolicy.KEEP, request);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void checkAppointmentStatus() {
        scheduleRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if (status == null) return;

                isSessionActive = "ACTIVE".equalsIgnoreCase(status);
                tvStatusBadge.setText(status.toUpperCase());

                if (isSessionActive) {
                    doctorInputContainer.setVisibility(View.VISIBLE);
                    btnCompleteAppointment.setVisibility(View.VISIBLE);
                    doctorCompletedView.setVisibility(View.GONE);
                } else {
                    // Session is MISSED or COMPLETED
                    doctorInputContainer.setVisibility(View.GONE);
                    btnCompleteAppointment.setVisibility(View.GONE);
                    doctorCompletedView.setVisibility(View.VISIBLE);
                    hideKeyboard();

                    // If the background worker marked it missed, cancel the worker here as well
                    WorkManager.getInstance(DUChatsActivity.this).cancelUniqueWork(bookingId);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

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
                if (!messageList.isEmpty()) chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

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
        messageMap.put("timestamp", ServerValue.TIMESTAMP);
        messageMap.put("type", "text");

        chatRef.push().setValue(messageMap).addOnSuccessListener(aVoid -> etMessage.setText(""));
    }

    private void completeConsultation() {
        // Cancel the WorkManager task because the doctor completed it manually
        WorkManager.getInstance(this).cancelUniqueWork(bookingId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("/DoctorSchedule/" + currentUserId + "/" + bookingId + "/status", "COMPLETED");
        updates.put("/UserBookings/" + patientId + "/" + bookingId + "/status", "COMPLETED");
        updates.put("/doctors/" + currentUserId + "/totalPatients", ServerValue.increment(1));

        rootRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Consultation Completed Successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}