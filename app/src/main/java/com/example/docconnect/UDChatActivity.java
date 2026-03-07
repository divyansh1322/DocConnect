package com.example.docconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * UDChatActivity: Master Implementation for DocConnect.
 * * HANDLES: Real-time Messaging, Session Status Logic, and Doctor Stats.
 * * FEATURES: Atomic Transaction Counter, Auto-Scroll, and Lifecycle-Safe listeners.
 */
public class UDChatActivity extends AppCompatActivity {

    private static final String TAG = "UDChatActivity";

    // --- UI COMPONENTS ---
    private LinearLayout inputContainer, tvEndedMessageLayout;
    private TextView tvDoctorName, tvSpecialty, tvStatusBadge, tvEndedMessage;
    private ShapeableImageView doctorImage;
    private EditText etMessage;
    private View btnSend;
    private ImageView btnBack;

    // --- RECYCLERVIEW ---
    private RecyclerView rvChats;
    private UDChatAdapter adapter;
    private ArrayList<ChatModel.Message> chatList = new ArrayList<>();

    // --- FIREBASE & DATA ---
    private ChatModel model;
    private DatabaseReference chatRef, bookingRef, doctorDbRef;
    private ValueEventListener chatListener, statusListener;
    private String currentUserId;

    // Safety flag to prevent double-counting if the listener triggers twice
    private boolean hasIncremented = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ud_chat);

        // 1. Session & Intent Validation
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            model = (ChatModel) getIntent().getSerializableExtra("CHAT_DATA");
        } catch (Exception e) {
            Log.e(TAG, "Serialization Error: " + e.getMessage());
        }

        if (model == null || model.getBookingId() == null) {
            Toast.makeText(this, "Data Error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Initialize UI Components
        initViews();
        setupHeader();

        // 3. Setup Firebase Paths & Listeners
        setupFirebase();

        // 4. Manual UI State Check
        handleSessionState();

        // 5. Global Listeners
        btnSend.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        // Layout Containers
        inputContainer = findViewById(R.id.inputContainer);
        tvEndedMessageLayout = findViewById(R.id.tvEndedMessageLayout);

        // Header Components
        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvSpecialty = findViewById(R.id.tvSpecialty);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        doctorImage = findViewById(R.id.doctorImage);
        btnBack = findViewById(R.id.btnBack);

        // Chat Components
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tvEndedMessage = findViewById(R.id.tvEndedMessage);

        // RecyclerView Logic
        rvChats = findViewById(R.id.rvChats);
        adapter = new UDChatAdapter(chatList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Logic: Keep latest chats at the bottom
        rvChats.setLayoutManager(layoutManager);
        rvChats.setAdapter(adapter);
    }

    private void setupHeader() {
        tvDoctorName.setText(model.getDoctorName() != null ? model.getDoctorName() : "Doctor");
        tvSpecialty.setText(model.getDoctorSpecialty() != null ? model.getDoctorSpecialty() : "Specialist");

        if (!isFinishing()) {
            Glide.with(this)
                    .load(model.getDoctorImage())
                    .placeholder(R.drawable.ic_doctor)
                    .circleCrop()
                    .into(doctorImage);
        }
    }

    private void setupFirebase() {
        // Path to Chat Messages
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(model.getBookingId());

        // Path to specific Booking Status
        bookingRef = FirebaseDatabase.getInstance().getReference("UserBookings")
                .child(currentUserId).child(model.getBookingId());

        // Path to Doctor's node for the Counter logic
        doctorDbRef = FirebaseDatabase.getInstance().getReference("doctors").child(model.getDoctorId());

        // Listener: Real-time Chat Sync
        chatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatModel.Message chat = ds.getValue(ChatModel.Message.class);
                    if (chat != null) chatList.add(chat);
                }
                adapter.notifyDataSetChanged();
                if (!chatList.isEmpty()) {
                    rvChats.smoothScrollToPosition(chatList.size() - 1);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        chatRef.addValueEventListener(chatListener);

        // Listener: Session Status (Detects when doctor finishes session)
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                String status = s.getValue(String.class);
                if (status != null) {
                    model.setStatus(status);

                    // ATOMIC TRIGGER: If doctor marks COMPLETED, increment thier total
                    if (status.equalsIgnoreCase("COMPLETED") && !hasIncremented) {
                        incrementLifetimeTotal();
                        hasIncremented = true;
                    }

                    handleSessionState();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        bookingRef.child("status").addValueEventListener(statusListener);
    }

    /**
     * incrementLifetimeTotal: Safely increments doctor's patient count using Transactions.
     */
    private void incrementLifetimeTotal() {
        DatabaseReference totalRef = doctorDbRef.child("totalPatients");
        totalRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer count = currentData.getValue(Integer.class);
                if (count == null) currentData.setValue(1);
                else currentData.setValue(count + 1);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(@Nullable DatabaseError e, boolean b, @Nullable DataSnapshot s) {
                if (e != null) Log.e(TAG, "Transaction Failed: " + e.getMessage());
            }
        });
    }

    /**
     * handleSessionState: Controls the Visibility of the Chat Input vs 'Ended' Notice.
     */
    private void handleSessionState() {
        String status = (model.getStatus() != null) ? model.getStatus().toUpperCase() : "UPCOMING";
        tvStatusBadge.setText(status);

        if (status.equals("ACTIVE")) {
            inputContainer.setVisibility(View.VISIBLE);
            if (tvEndedMessageLayout != null) tvEndedMessageLayout.setVisibility(View.GONE);
        } else {
            inputContainer.setVisibility(View.GONE);
            if (tvEndedMessageLayout != null) {
                tvEndedMessageLayout.setVisibility(View.VISIBLE);

                if (status.equals("COMPLETED")) {
                    tvEndedMessage.setText("Consultation Ended. Session Completed.");
                } else if (status.equals("UPCOMING")) {
                    tvEndedMessage.setText("Chat will open when the session starts.");
                } else {
                    tvEndedMessage.setText("This session is no longer active.");
                }
            }
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        btnSend.setEnabled(false); // Guard

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("message", text);
        messageMap.put("senderId", currentUserId);
        messageMap.put("timestamp", ServerValue.TIMESTAMP);
        messageMap.put("type", "text");

        chatRef.push().setValue(messageMap).addOnCompleteListener(task -> {
            btnSend.setEnabled(true);
            if (task.isSuccessful()) {
                etMessage.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Lifecycle Cleanup: Prevent leaks
        if (chatRef != null && chatListener != null) chatRef.removeEventListener(chatListener);
        if (bookingRef != null && statusListener != null) bookingRef.removeEventListener(statusListener);
        super.onDestroy();
    }
}