package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Fragment that displays a list of the user's past medical appointments marked as 'COMPLETED'.
 * Optimized with View Caching to prevent re-loading when switching ViewPager tabs.
 */
public class CompletedFragment extends Fragment {

    // --- 1. VIEW CACHING VARIABLES ---
    private View rootView;
    private boolean isLoaded = false;

    // UI and Data components
    private RecyclerView rvCompleted;
    private UpcomingAdapter adapter;
    private final ArrayList<AppointmentModel> completedList = new ArrayList<>();
    private String userId;

    // View state components
    private TextView tvNoCompleted;
    private ProgressBar pbLoading;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // STEP 1: Check if rootView already exists. Return it to maintain UI state.
        if (rootView != null) {
            return rootView;
        }

        // STEP 2: Inflate the UI layout for the first time
        rootView = inflater.inflate(R.layout.fragment_completed, container, false);

        // Retrieve current user's unique ID
        userId = FirebaseAuth.getInstance().getUid();

        // Bind Views: Connect Java objects to XML layout IDs using rootView
        rvCompleted = rootView.findViewById(R.id.rvCompleted);
        tvNoCompleted = rootView.findViewById(R.id.tvNoCompleted);
        pbLoading = rootView.findViewById(R.id.pbLoading);

        // Configure the RecyclerView
        rvCompleted.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize Adapter with logic
        setupAdapter();

        // STEP 3: Load data only once
        if (!isLoaded && userId != null) {
            loadCompletedAppointments();
            isLoaded = true;
        }

        return rootView;
    }

    /**
     * Set up the adapter with its specific action listener logic.
     */
    private void setupAdapter() {
        adapter = new UpcomingAdapter(requireContext(), completedList,
                new UpcomingAdapter.OnAppointmentActionListener() {
                    @Override public void onCancelClick(AppointmentModel a, int p) {}
                    @Override public void onRescheduleClick(AppointmentModel a) {}
                    @Override public void onReasonClick(AppointmentModel a) {}
                    @Override public void onBookNowClick(AppointmentModel a) {}

                    @Override
                    public void onRateReviewClick(AppointmentModel appointment) {
                        Intent intent = new Intent(getContext(), RateReviewActivity.class);
                        intent.putExtra("bookingId", appointment.getBookingId());
                        intent.putExtra("doctorSpecialty", appointment.getDoctorSpecialty());
                        intent.putExtra("doctorImage", appointment.getDoctorImage());
                        intent.putExtra("doctorId", appointment.getDoctorId());
                        intent.putExtra("doctorName", appointment.getDoctorName());

                        String currentDate = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
                        intent.putExtra("visitDate", currentDate);
                        startActivity(intent);
                    }
                });

        rvCompleted.setAdapter(adapter);
    }

    /**
     * Connects to Firebase Realtime Database to fetch bookings filtered by the 'COMPLETED' status.
     */
    private void loadCompletedAppointments() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);

        FirebaseDatabase.getInstance()
                .getReference("UserBookings")
                .child(userId)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return; // Fragment safety check

                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);

                        completedList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            AppointmentModel model = ds.getValue(AppointmentModel.class);
                            if (model != null && "COMPLETED".equalsIgnoreCase(model.getStatus())) {
                                completedList.add(model);
                            }
                        }

                        // Toggle UI: Show the list or the empty state message
                        if (completedList.isEmpty()) {
                            rvCompleted.setVisibility(View.GONE);
                            if (tvNoCompleted != null) tvNoCompleted.setVisibility(View.VISIBLE);
                        } else {
                            rvCompleted.setVisibility(View.VISIBLE);
                            if (tvNoCompleted != null) tvNoCompleted.setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    }
                });
    }
}