package com.example.docconnect;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Optimized Fragment for Doctor Schedule.
 * Implements View Caching to prevent re-loading when switching bottom navigation tabs.
 */
public class DoctorScheduleFragment extends Fragment {

    // --- 1. VIEW CACHING VARIABLES ---
    private View rootView;
    private boolean isDataInitialized = false;

    // UI and Data components
    private RecyclerView rvSchedule;
    private ScheduleAdapter adapter;
    private final List<DoctorUpcomingModel> masterList = new ArrayList<>();
    private final List<DoctorUpcomingModel> displayList = new ArrayList<>();

    private TextView tvCount, tvDateHeader, filterUpcoming, filterCompleted;
    private ShimmerFrameLayout shimmerView;
    private View layoutEmptyState;

    // Logic and Firebase state
    private String currentDoctorId;
    private String selectedDate;
    private String activeFilter = "UPCOMING";

    // Date formatting utilities
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    // Auto Refresh Logic: Re-filters the list every minute to move past times to history
    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && "UPCOMING".equals(activeFilter)) {
                updateFilter("UPCOMING");
            }
            autoRefreshHandler.postDelayed(this, 60000);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // STEP 1: Check if rootView exists. If yes, return it to keep current state.
        if (rootView != null) {
            return rootView;
        }

        // STEP 2: Inflate the layout for the first time
        rootView = inflater.inflate(R.layout.fragment_doctor_schedule, container, false);

        initViews(rootView);

        currentDoctorId = FirebaseAuth.getInstance().getUid();

        // Initialize default date (Today)
        selectedDate = dbFormat.format(new Date());
        tvDateHeader.setText(displayFormat.format(new Date()));

        // Setup RecyclerView
        rvSchedule.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ScheduleAdapter(displayList);
        rvSchedule.setAdapter(adapter);

        setupListeners(rootView);

        // STEP 3: Only fetch from Firebase if this is the first time loading
        if (!isDataInitialized) {
            fetchData();
            isDataInitialized = true;
        }

        return rootView;
    }

    private void initViews(View v) {
        rvSchedule = v.findViewById(R.id.rv_schedule);
        tvCount = v.findViewById(R.id.tv_appointment_count);
        tvDateHeader = v.findViewById(R.id.tv_schedule_date_full);
        shimmerView = v.findViewById(R.id.shimmer_view_container);
        layoutEmptyState = v.findViewById(R.id.layout_empty_state);
        filterUpcoming = v.findViewById(R.id.filter_upcoming);
        filterCompleted = v.findViewById(R.id.filter_completed);
    }

    private void setupListeners(View v) {
        // Calendar button
        v.findViewById(R.id.btn_calendar_view).setOnClickListener(view -> showDatePicker());

        // Tab selection listeners
        filterUpcoming.setOnClickListener(view -> updateFilter("UPCOMING"));
        filterCompleted.setOnClickListener(view -> updateFilter("COMPLETED"));
    }

    private void showDatePicker() {
        if (getContext() == null) return;

        new DatePickerDialog(
                getContext(),
                (view, year, month, day) -> {
                    calendar.set(year, month, day);
                    selectedDate = dbFormat.format(calendar.getTime());
                    tvDateHeader.setText(displayFormat.format(calendar.getTime()));
                    fetchData(); // Fetch new data for the newly selected date
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void fetchData() {
        if (currentDoctorId == null) return;

        // Show Shimmer while data is coming
        showLoading(true);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("DoctorSchedule")
                .child(currentDoctorId);

        // Real-time listener: The UI will update instantly if a patient books/cancels
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                masterList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    DoctorUpcomingModel model = ds.getValue(DoctorUpcomingModel.class);

                    // Filter for the selected date only
                    if (model != null && selectedDate.equals(model.getDate())) {
                        model.setKey(ds.getKey());
                        masterList.add(model);
                    }
                }

                // Sort list by time
                Collections.sort(masterList, (a, b) -> a.getTime().compareTo(b.getTime()));

                showLoading(false);
                updateFilter(activeFilter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
            }
        });
    }

    private void updateFilter(String status) {
        if (!isAdded()) return;

        activeFilter = status;
        displayList.clear();

        // Toggle UI colors for tabs
        if ("UPCOMING".equals(status)) {
            setTabActive(filterUpcoming, filterCompleted);
        } else {
            setTabActive(filterCompleted, filterUpcoming);
        }

        String today = dbFormat.format(new Date());

        for (DoctorUpcomingModel m : masterList) {
            if (status.equalsIgnoreCase(m.getStatus())) {
                // If checking "Upcoming" for Today, hide items where time has already passed
                if ("UPCOMING".equals(status) && selectedDate.equals(today) && isTimePassed(m.getTime())) {
                    continue;
                }
                displayList.add(m);
            }
        }

        // Handle Empty State
        rvSchedule.setVisibility(displayList.isEmpty() ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);

        adapter.notifyDataSetChanged();
        tvCount.setText(String.format(Locale.getDefault(), "%02d", displayList.size()));
    }

    private boolean isTimePassed(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date apptTime = sdf.parse(time);
            Calendar now = Calendar.getInstance();
            Calendar appt = Calendar.getInstance();
            appt.setTime(apptTime);
            appt.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            return now.after(appt);
        } catch (Exception e) { return false; }
    }

    private void setTabActive(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.bg_filter_select);
        active.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        inactive.setBackgroundResource(R.drawable.bg_filter_unselected);
        inactive.setTextColor(ContextCompat.getColor(requireContext(), R.color.doc_text_primary));
    }

    private void showLoading(boolean show) {
        if (show) {
            shimmerView.setVisibility(View.VISIBLE);
            shimmerView.startShimmer();
            rvSchedule.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            shimmerView.stopShimmer();
            shimmerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start the pulse check for past times
        autoRefreshHandler.post(autoRefreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop timer to save battery
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }
}