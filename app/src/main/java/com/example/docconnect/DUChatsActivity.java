package com.example.docconnect;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * DUChatsActivity facilitates the real-time interaction between a Doctor and a Patient.
 * Updated: Auto-missed logic removed. Manual completion and messaging remain active.
 */
public class DUChatsActivity extends AppCompatActivity {

    private static final String TAG = "DU_CHATS_DEBUG";

    // --- UI Components ---
    private RecyclerView chatRecyclerView;
    private EditText etMessage;
    private FloatingActionButton btnSend; // Kept as FAB per your original code
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
        chatRef = rootRef.child("Chats").child(bookingId);
        scheduleRef = rootRef.child("DoctorSchedule").child(currentUserId).child(bookingId);

        initViews();
        setupHeader();
        setupRecyclerView();

        // 3. Start Data Listeners
        listenForMessages();        // Real-time chat messages
        checkAppointmentStatus();   // Observe if session is ACTIVE or COMPLETED

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
     * Initializes the RecyclerView with a ChatAdapter.
     */
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        adapter = new DUChatAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(adapter);
    }

    /**
     * Listens for changes in appointment status to toggle UI.
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
                    doctorInputContainer.setVisibility(View.VISIBLE);
                    btnCompleteAppointment.setVisibility(View.VISIBLE);
                    doctorCompletedView.setVisibility(View.GONE);
                } else {
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
     * Fetches all messages for the current booking ID.
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
        messageMap.put("timestamp", ServerValue.TIMESTAMP);
        messageMap.put("type", "text");

        chatRef.push().setValue(messageMap).addOnSuccessListener(aVoid -> {
            etMessage.setText("");
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
     * Utility to close the on-screen keyboard.
     */
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
        // Handler and timer logic removed.
    }
}