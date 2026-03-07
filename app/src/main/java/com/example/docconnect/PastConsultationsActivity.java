package com.example.docconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PastConsultationsActivity: Professional History Management for DocConnect.
 * * HANDLES: Filtering COMPLETED appointments with real-time TabLayout integration.
 * * FIXES: "Transition Mismatch" by delaying heavy Firebase operations until animation ends.
 * * FIXES: "MIUI NullPointer" by ensuring UI initialization is complete before data sync.
 */
public class PastConsultationsActivity extends AppCompatActivity {

    private static final String TAG = "PastConsultations";

    // --- UI COMPONENTS ---
    private RecyclerView rvPast;
    private PastConsultantAdapter adapter;
    private TabLayout tabLayout;
    private TextView tvEmptyState;
    private View btnBack;

    // --- DATA LISTS ---
    // Master list holds all "COMPLETED" records from the DB
    private final List<DoctorUpcomingModel> masterCompletedList = new ArrayList<>();
    // Display list is a filtered subset (All vs Recent) shown in the RecyclerView
    private final List<DoctorUpcomingModel> displayList = new ArrayList<>();

    // --- FIREBASE ---
    private DatabaseReference scheduleDbRef;
    private ValueEventListener historyListener;
    private String currentDoctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_consultations);

        // 1. SESSION VALIDATION: Exit if the doctor is not logged in
        currentDoctorId = FirebaseAuth.getInstance().getUid();
        if (currentDoctorId == null) {
            Toast.makeText(this, "Session expired. Please log in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();

        // 2. SAFE INITIALIZATION (A2Z FIX):
        // Delaying the heavy Firebase call by 200ms allows the Android 'TransitionChain'
        // to finish opening the window, preventing the 'Mismatch current collecting' log error.
        getWindow().getDecorView().postDelayed(this::fetchCompletedConsultations, 200);
    }

    /**
     * Initializes XML references and configures RecyclerView.
     */
    private void initViews() {
        rvPast = findViewById(R.id.rvPastConsultations);
        tabLayout = findViewById(R.id.tabLayoutHistory);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnBack = findViewById(R.id.btnBack);

        rvPast.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PastConsultantAdapter(displayList);
        rvPast.setAdapter(adapter);

        scheduleDbRef = FirebaseDatabase.getInstance()
                .getReference("DoctorSchedule")
                .child(currentDoctorId);
    }

    /**
     * Sets up navigation and Tab filtering logic.
     */
    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyFilter(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Retrieves data from Firebase and filters for COMPLETED status.
     */
    private void fetchCompletedConsultations() {
        if (isFinishing()) return;

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Safeguard against late Firebase updates after activity is closed
                if (isFinishing()) return;

                masterCompletedList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    DoctorUpcomingModel model = ds.getValue(DoctorUpcomingModel.class);

                    // History only tracks COMPLETED or ENDED sessions
                    if (model != null && "COMPLETED".equalsIgnoreCase(model.getStatus())) {
                        model.setKey(ds.getKey());
                        masterCompletedList.add(model);
                    }
                }

                // SORTING: Newest Date first
                Collections.sort(masterCompletedList, (o1, o2) -> {
                    if (o1.getDate() == null || o2.getDate() == null) return 0;
                    return o2.getDate().compareTo(o1.getDate());
                });

                // Refresh the UI with the currently selected filter
                applyFilter(tabLayout.getSelectedTabPosition());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Fetch Error: " + error.getMessage());
            }
        };

        scheduleDbRef.addValueEventListener(historyListener);
    }

    /**
     * filters the master list based on the user's tab selection.
     * @param position 0 = All History, 1 = Recent 15
     */
    private void applyFilter(int position) {
        displayList.clear();

        if (position == 0) {
            displayList.addAll(masterCompletedList);
        } else {
            // Show only the top 15 most recent entries
            int limit = Math.min(masterCompletedList.size(), 15);
            for (int i = 0; i < limit; i++) {
                displayList.add(masterCompletedList.get(i));
            }
        }

        // --- EMPTY STATE UI MANAGEMENT ---
        if (displayList.isEmpty()) {
            rvPast.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvPast.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        // CLEANUP: Essential to prevent memory leaks and 'Activity Chain' errors
        if (scheduleDbRef != null && historyListener != null) {
            scheduleDbRef.removeEventListener(historyListener);
        }
        super.onDestroy();
    }
}