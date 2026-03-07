package com.example.docconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorNotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationModel> list = new ArrayList<>();
    private LinearLayout layoutCaughtUp;
    private DatabaseReference dbRef;
    private ValueEventListener notificationListener;
    private ImageView btnBack;
    private TextView tvMarkRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_notifications);

        initViews();
        setupRecyclerView();

        btnBack.setOnClickListener(v -> finish());
        tvMarkRead.setOnClickListener(v -> markAllAsRead());

        fetchNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutCaughtUp = findViewById(R.id.layoutCaughtUp);
        btnBack = findViewById(R.id.btnBack);
        tvMarkRead = findViewById(R.id.tvMarkRead);
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setHasFixedSize(true);
        adapter = new NotificationAdapter(list);
        rvNotifications.setAdapter(adapter);
    }

    private void fetchNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("notifications").child(uid);

        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                list.clear();
                boolean hasAnyUnread = false;

                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        NotificationModel model = data.getValue(NotificationModel.class);
                        if (model != null) {
                            list.add(0, model); // Add newest to top
                            if (!model.isRead()) { // Logic: Check if unread
                                hasAnyUnread = true;
                            }
                        }
                    }
                    updateUIState(false, hasAnyUnread);
                } else {
                    // Logic: Database is empty
                    updateUIState(true, false);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        dbRef.addValueEventListener(notificationListener);
    }

    /**
     * Rules applied:
     * 1. Empty DB -> Show "Caught Up", Hide List, Hide MarkRead
     * 2. Not Empty + Unread exists -> Hide "Caught Up", Show List, Show MarkRead
     * 3. Not Empty + All Read -> Hide "Caught Up", Show List, Hide MarkRead
     */
    private void updateUIState(boolean isDbEmpty, boolean hasUnread) {
        if (isDbEmpty) {
            rvNotifications.setVisibility(View.GONE);
            layoutCaughtUp.setVisibility(View.VISIBLE);
            tvMarkRead.setVisibility(View.GONE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            layoutCaughtUp.setVisibility(View.GONE);
            // Dynamic Visibility based on your unread logic
            tvMarkRead.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        }
    }

    private void markAllAsRead() {
        if (dbRef == null || list.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    // Update field "read" to true for all
                    updates.put(child.getKey() + "/read", true);
                }
                dbRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    Toast.makeText(DoctorNotificationsActivity.this, "Marked as read", Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dbRef != null && notificationListener != null) {
            dbRef.removeEventListener(notificationListener);
        }
    }
}