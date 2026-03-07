package com.example.docconnect;

import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * UserManagementRecordActivity
 * Optimized to handle Real-time updates and prevent system-level crashes.
 */
public class UserManagementRecordActivity extends AppCompatActivity {

    private static final String TAG = "DocConnect_Admin";

    // UI Elements
    private RecyclerView rvUsers;
    private UserManageAdapter adapter;
    private List<UserManageModel> userList = new ArrayList<>();
    private List<UserManageModel> filteredList = new ArrayList<>();

    private TextView tvTotalUsersCount, tvActiveUsersCount;
    private ImageButton btnBack;
    private TextInputEditText searchInput;
    private MaterialButton btnAll, btnStatus;

    // Firebase References
    private DatabaseReference mUserDatabase, mBookingDatabase;
    private ValueEventListener userListener, bookingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. DEFENSIVE FIX: Wrap inflation in try-catch to stop MIUI-specific
        // SettingsCloudData crashes identified in Logcat.
        try {
            setContentView(R.layout.activity_user_management_recod);
        } catch (Exception e) {
            Log.e(TAG, "Activity Inflation Failed: " + e.getMessage());
            finish();
            return;
        }

        mUserDatabase = FirebaseDatabase.getInstance().getReference("users");
        mBookingDatabase = FirebaseDatabase.getInstance().getReference("UserBookings");

        initViews();
        setupRecyclerView();

        // Start Data Sync
        fetchTotalUsersDirect();
        fetchActiveUsersToday();

        setupSearch();
        setupFilterButtons();
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rvUsers);
        btnBack = findViewById(R.id.btnBack);
        searchInput = findViewById(R.id.searchInputField);
        btnAll = findViewById(R.id.btnAllUsers);
        btnStatus = findViewById(R.id.btnStatus);
        tvTotalUsersCount = findViewById(R.id.tvTotalUsersCount);
        tvActiveUsersCount = findViewById(R.id.tvActiveUsersCount);

        // 2. BUG FIX: Navigation Guard. finish() is safe, but we check for null
        // to prevent NPEs during rapid screen transitions.
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void fetchTotalUsersDirect() {
        userListener = mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 3. LIFECYCLE FIX: Do not update UI if user has already left the activity.
                if (isFinishing() || isDestroyed()) return;

                userList.clear();
                if (snapshot.exists()) {
                    long count = snapshot.getChildrenCount();

                    if (tvTotalUsersCount != null) {
                        tvTotalUsersCount.setText(String.valueOf(count));
                    }

                    for (DataSnapshot data : snapshot.getChildren()) {
                        // 4. DATA FIX: Use try-catch for individual mapping to
                        // prevent one "bad" user from crashing the whole list.
                        try {
                            UserManageModel user = data.getValue(UserManageModel.class);
                            if (user != null) {
                                user.setUserId(data.getKey());
                                userList.add(user);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Mapping error at UID: " + data.getKey());
                        }
                    }
                } else if (tvTotalUsersCount != null) {
                    tvTotalUsersCount.setText("0");
                }
                showAllUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "User Database Error: " + error.getMessage());
            }
        });
    }

    private void fetchActiveUsersToday() {
        bookingListener = mBookingDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                int activeCount = 0;
                // Standardized date format matching Firebase storage logic
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        boolean userActive = false;
                        for (DataSnapshot booking : userSnapshot.getChildren()) {
                            // 5. BUG FIX: Null-safe string conversion for booking dates.
                            Object dbDateObj = booking.child("date").getValue();
                            if (dbDateObj != null && todayDate.equals(dbDateObj.toString())) {
                                userActive = true;
                                break;
                            }
                        }
                        if (userActive) activeCount++;
                    }

                    if (tvActiveUsersCount != null) {
                        tvActiveUsersCount.setText(String.valueOf(activeCount));
                    }
                } else if (tvActiveUsersCount != null) {
                    tvActiveUsersCount.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Booking Database Error: " + error.getMessage());
            }
        });
    }

    private void setupRecyclerView() {
        if (rvUsers != null) {
            adapter = new UserManageAdapter(filteredList);
            rvUsers.setLayoutManager(new LinearLayoutManager(this));
            rvUsers.setAdapter(adapter);
        }
    }

    private void setupSearch() {
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    performSearch(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void performSearch(String query) {
        filteredList.clear();
        String cleanQuery = query.toLowerCase().trim();
        for (UserManageModel user : userList) {
            String name = (user.getFullName() != null) ? user.getFullName().toLowerCase() : "";
            if (name.contains(cleanQuery)) {
                filteredList.add(user);
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void setupFilterButtons() {
        if (btnAll != null) btnAll.setOnClickListener(v -> showAllUsers());
        if (btnStatus != null) {
            btnStatus.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, btnStatus);
                popup.getMenu().add("Active");
                popup.getMenu().add("Blocked");
                popup.setOnMenuItemClickListener(item -> {
                    filterByStatus(item.getTitle().toString());
                    return true;
                });
                popup.show();
            });
        }
    }

    private void showAllUsers() {
        filteredList.clear();
        filteredList.addAll(userList);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void filterByStatus(String status) {
        filteredList.clear();
        for (UserManageModel user : userList) {
            if (user.getStatus() != null && user.getStatus().equalsIgnoreCase(status)) {
                filteredList.add(user);
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 6. MEMORY FIX: Force remove listeners.
        // This stops background tasks from trying to access this activity after it's destroyed.
        if (mUserDatabase != null && userListener != null) {
            mUserDatabase.removeEventListener(userListener);
        }
        if (mBookingDatabase != null && bookingListener != null) {
            mBookingDatabase.removeEventListener(bookingListener);
        }
    }
}