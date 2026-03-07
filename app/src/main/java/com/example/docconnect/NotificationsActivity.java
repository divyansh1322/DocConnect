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

/**
 * NotificationsActivity: Managed Hub for User Alerts.
 * Features: Real-time sync, Unread status detection, and Empty state handling.
 */
public class NotificationsActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationModel> list = new ArrayList<>();
    private LinearLayout layoutCaughtUp;
    private TextView tvMarkRead;
    private ImageView btnBack;

    // Firebase
    private DatabaseReference dbRef;
    private ValueEventListener notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // 1. Initialize all views from XML
        initViews();

        // 2. Setup RecyclerView for performance
        setupRecyclerView();

        // 3. Set Click Listeners
        btnBack.setOnClickListener(v -> finish());

        tvMarkRead.setOnClickListener(v -> {
            // Logic to mark everything as read in the database
            markAllNotificationsAsRead();
        });

        // 4. Start Listening to Firebase
        fetchNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutCaughtUp = findViewById(R.id.layoutCaughtUp);
        tvMarkRead = findViewById(R.id.tvMarkRead);
        btnBack = findViewById(R.id.btnBack);
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
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Reference: notifications -> {userId}
        dbRef = FirebaseDatabase.getInstance().getReference("notifications").child(uid);

        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                list.clear();
                boolean hasUnread = false;

                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        NotificationModel model = data.getValue(NotificationModel.class);
                        if (model != null) {
                            // Add to top of list (Index 0) for reverse chronological order
                            list.add(0, model);

                            // Logic check: If even ONE item is not read, we show the button
                            if (!model.isRead()) {
                                hasUnread = true;
                            }
                        }
                    }
                    // State: Notifications exist
                    toggleUIState(false, hasUnread);
                } else {
                    // State: No notifications in database
                    toggleUIState(true, false);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationsActivity.this, "Sync Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        // Use addValueEventListener for real-time updates while the activity is open
        dbRef.addValueEventListener(notificationListener);
    }

    /**
     * Manages Visibility of UI elements based on data state.
     */
    private void toggleUIState(boolean isListEmpty, boolean hasUnreadItems) {
        if (isListEmpty) {
            // No data in DB
            rvNotifications.setVisibility(View.GONE);
            layoutCaughtUp.setVisibility(View.VISIBLE);
            tvMarkRead.setVisibility(View.GONE);
        } else {
            // Data exists
            rvNotifications.setVisibility(View.VISIBLE);
            layoutCaughtUp.setVisibility(View.GONE);

            // Only show "Mark all as read" if there is at least one unread message
            tvMarkRead.setVisibility(hasUnreadItems ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Updates all child nodes under the user's notification path to read: true.
     */
    private void markAllNotificationsAsRead() {
        if (dbRef == null || list.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();

        // Single value event to get current keys once and update them
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    // This creates a map entry like: "notification_id_1/read" : true
                    updates.put(child.getKey() + "/read", true);
                }

                if (!updates.isEmpty()) {
                    dbRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                        Toast.makeText(NotificationsActivity.this, "All caught up!", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // PROFESSIONAL CLEANUP: Stop listening when the activity is not visible to save battery/data
        if (dbRef != null && notificationListener != null) {
            dbRef.removeEventListener(notificationListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Extra cleanup to prevent memory leaks
        if (dbRef != null && notificationListener != null) {
            dbRef.removeEventListener(notificationListener);
        }
    }
}