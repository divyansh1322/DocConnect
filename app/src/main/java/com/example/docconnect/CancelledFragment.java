package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * CancelledFragment: Professional Implementation with View Caching.
 * Features: Lifecycle-safe listeners, UI state management, and re-booking navigation.
 */
public class CancelledFragment extends Fragment {

    // --- 1. VIEW CACHING VARIABLES ---
    private View rootView;
    private boolean isLoaded = false;

    // UI Components
    private RecyclerView rvCancelled;
    private UpcomingAdapter adapter;
    private final ArrayList<AppointmentModel> cancelledList = new ArrayList<>();
    private TextView tvNoCancelled;

    // Firebase Logic
    private String userId;
    private DatabaseReference dbRef;
    private ValueEventListener cancelledListener;

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
        rootView = inflater.inflate(R.layout.fragment_cancelled, container, false);

        // Professional User ID Retrieval
        userId = FirebaseAuth.getInstance().getUid();

        // Initialize Views using rootView
        rvCancelled = rootView.findViewById(R.id.rvCancelled);
        tvNoCancelled = rootView.findViewById(R.id.tvNoCancelled);

        // UI Setup
        rvCancelled.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCancelled.setHasFixedSize(true);

        // Initialize Adapter with logic
        setupAdapter();

        // STEP 3: Load data only once
        if (!isLoaded && userId != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("UserBookings").child(userId);
            loadCancelledAppointments();
            isLoaded = true;
        }

        return rootView;
    }

    /**
     * Initializes the adapter with specific callbacks for button clicks.
     */
    private void setupAdapter() {
        adapter = new UpcomingAdapter(requireContext(), cancelledList,
                new UpcomingAdapter.OnAppointmentActionListener() {

                    @Override public void onCancelClick(AppointmentModel a, int p) {}
                    @Override public void onRescheduleClick(AppointmentModel a) {}
                    @Override public void onRateReviewClick(AppointmentModel a) {}

                    @Override
                    public void onReasonClick(AppointmentModel appointment) {
                        if (getActivity() != null) {
                            Intent intent = new Intent(requireContext(), CancellationReasonActivity.class);
                            intent.putExtra("bookingId", appointment.getBookingId());
                            intent.putExtra("doctorName", appointment.getDoctorName());
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onBookNowClick(AppointmentModel appointment) {
                        if (getActivity() != null) {
                            Intent intent = new Intent(requireContext(), BookAppointmentActivity.class);
                            intent.putExtra("id", appointment.getDoctorId());
                            intent.putExtra("doctorName", appointment.getDoctorName());
                            intent.putExtra("doctorSpecialty", appointment.getDoctorSpecialty()); // Fixed to match Model naming
                            intent.putExtra("doctorImage", appointment.getDoctorImage());
                            intent.putExtra("doctorFee", appointment.getDoctorFee());
                            intent.putExtra("clinicName", appointment.getClinicName());
                            intent.putExtra("clinicAddress", appointment.getClinicAddress());
                            startActivity(intent);
                        }
                    }
                });

        rvCancelled.setAdapter(adapter);
    }

    /**
     * Fetches and filters CANCELLED appointments with Lifecycle Guards.
     */
    private void loadCancelledAppointments() {
        cancelledListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Safeguard: Ensure fragment is still attached to the UI
                if (!isAdded() || getContext() == null) return;

                cancelledList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    AppointmentModel model = ds.getValue(AppointmentModel.class);
                    if (model != null && "CANCELLED".equalsIgnoreCase(model.getStatus())) {
                        cancelledList.add(model);
                    }
                }

                // Professional Visibility Toggling
                if (cancelledList.isEmpty()) {
                    rvCancelled.setVisibility(View.GONE);
                    if (tvNoCancelled != null) tvNoCancelled.setVisibility(View.VISIBLE);
                } else {
                    rvCancelled.setVisibility(View.VISIBLE);
                    if (tvNoCancelled != null) tvNoCancelled.setVisibility(View.GONE);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Potential logging or user feedback here
            }
        };
        dbRef.addValueEventListener(cancelledListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // PROFESSIONAL CLEANUP: Moved from onStop to onDestroy to keep listener
        // alive while tab-switching, only detaching when activity is destroyed.
        if (dbRef != null && cancelledListener != null) {
            dbRef.removeEventListener(cancelledListener);
        }
    }
}