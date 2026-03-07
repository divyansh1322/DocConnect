package com.example.docconnect;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * UPCOMING FRAGMENT - DOC-CONNECT
 * This fragment manages the "Upcoming" appointments tab.
 */
public class UpcomingFragment extends Fragment implements SearchableFragment {

    // --- 1. UI & STATE VARIABLES ---
    private View rootView;                // For View Caching (prevents reload on tab switch)
    private boolean isLoaded = false;     // Tracks if the first data load is done
    private RecyclerView rvUpcoming;
    private UpcomingAdapter adapter;
    private ArrayList<AppointmentModel> upcomingList = new ArrayList<>();
    private TextView tvNoData;

    // --- 2. FIREBASE VARIABLES ---
    private DatabaseReference rootRef, userBookingsRef;
    private ValueEventListener bookingsListener;
    private String userId;
    private String currentSearchQuery = "";

    // 5-minute buffer before an appointment moves from "Upcoming" to "Missed"
    private static final int GRACE_PERIOD_MINUTES = 5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // VIEW CACHING: Keeps the list ready even if user switches bottom navigation tabs
        if (rootView != null) return rootView;

        rootView = inflater.inflate(R.layout.fragment_upcoming, container, false);
        userId = FirebaseAuth.getInstance().getUid();

        // UI Binding
        rvUpcoming = rootView.findViewById(R.id.rvUpcoming);
        tvNoData = rootView.findViewById(R.id.tvNoUpcoming);
        rvUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Firebase Initialization
        rootRef = FirebaseDatabase.getInstance().getReference();
        if (userId != null) {
            userBookingsRef = rootRef.child("UserBookings").child(userId);
        }

        setupAdapter();

        // Start listening to data only once
        if (!isLoaded && userId != null) {
            attachFirebaseListener();
            isLoaded = true;
        }

        return rootView;
    }

    /**
     * ADAPTER SETUP
     * Links the RecyclerView buttons to the logic in this Fragment.
     */
    private void setupAdapter() {
        adapter = new UpcomingAdapter(requireContext(), upcomingList, new UpcomingAdapter.OnAppointmentActionListener() {
            @Override
            public void onCancelClick(AppointmentModel appointment, int position) {
                showMaterialCancelDialog(appointment);
            }

            @Override
            public void onRescheduleClick(AppointmentModel appointment) {
                showMaterialRescheduleDialog(appointment);
            }

            @Override
            public void onRateReviewClick(AppointmentModel a) {
                Toast.makeText(getContext(), "Review available after completion", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReasonClick(AppointmentModel a) {
                showStatusInfo(a);
            }

            @Override
            public void onBookNowClick(AppointmentModel a) {
                onRescheduleClick(a);
            }
        });
        rvUpcoming.setAdapter(adapter);
    }

    /**
     * FIREBASE LISTENER
     * Logic: Real-time update + Auto-Expiry + Search Filtering.
     */
    private void attachFirebaseListener() {
        if (userBookingsRef == null) return;

        // CRITICAL: Always remove old listener before adding a new one (prevents memory leaks)
        if (bookingsListener != null) userBookingsRef.removeEventListener(bookingsListener);

        bookingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<AppointmentModel> filteredList = new ArrayList<>();
                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                    AppointmentModel appointment = bookingSnapshot.getValue(AppointmentModel.class);

                    if (appointment != null) {
                        // LOGIC: Automatically move past appointments to "MISSED" status
                        if ("UPCOMING".equalsIgnoreCase(appointment.getStatus()) &&
                                isSlotExpired(appointment.getDate(), appointment.getTime())) {
                            markAsMissedInFirebase(appointment);
                            continue; // Don't add to current "Upcoming" list
                        }

                        // LOGIC: Filter by Status and the Search Query
                        if ("UPCOMING".equalsIgnoreCase(appointment.getStatus())) {
                            if (matchesQuery(appointment, currentSearchQuery)) {
                                filteredList.add(appointment);
                            }
                        }
                    }
                }
                updateUI(filteredList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", error.getMessage());
            }
        };
        userBookingsRef.addValueEventListener(bookingsListener);
    }

    /**
     * CANCEL APPOINTMENT WITH UNDO
     * Logic: Shows Dialog -> Updates Firebase -> Shows Snackbar with Undo Button.
     */
    private void showMaterialCancelDialog(AppointmentModel appointment) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel Appointment")
                .setMessage("Cancel your visit with " + appointment.getDoctorName() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {

                    // 1. Move to Cancelled Status
                    updateAppointmentStatus(appointment, "CANCELLED");

                    // 2. Setup Snackbar for 3.5 seconds
                    Snackbar snackbar = Snackbar.make(rvUpcoming, "Appointment Cancelled", Snackbar.LENGTH_LONG);

                    // 3. UNDO Logic: If clicked, change status back to UPCOMING
                    snackbar.setAction("UNDO", v -> {
                        updateAppointmentStatus(appointment, "UPCOMING");
                        Toast.makeText(getContext(), "Appointment Restored!", Toast.LENGTH_SHORT).show();
                    });

                    snackbar.setActionTextColor(Color.YELLOW);
                    snackbar.show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * FIREBASE ATOMIC UPDATE
     * Logic: Updates both User and Doctor nodes simultaneously so data is never out of sync.
     */
    private void updateAppointmentStatus(AppointmentModel a, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/UserBookings/" + userId + "/" + a.getBookingId() + "/status", status);
        updates.put("/DoctorSchedule/" + a.getDoctorId() + "/" + a.getBookingId() + "/status", status);

        rootRef.updateChildren(updates).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * RESCHEDULE LOGIC
     * Logic: Completely removes old booking slot -> Sends data to Booking Activity.
     */
    private void showMaterialRescheduleDialog(AppointmentModel appointment) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reschedule")
                .setMessage("Your current slot will be removed. Pick a new time?")
                .setPositiveButton("Proceed", (dialog, which) -> {

                    // Delete old appointment from both records
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("/UserBookings/" + userId + "/" + appointment.getBookingId(), null);
                    updates.put("/DoctorSchedule/" + appointment.getDoctorId() + "/" + appointment.getBookingId(), null);

                    rootRef.updateChildren(updates).addOnSuccessListener(unused -> {
                        // Open Booking screen and pass doctor info
                        Intent intent = new Intent(getContext(), BookAppointmentActivity.class);
                        intent.putExtra("id", appointment.getDoctorId());
                        intent.putExtra("doctorName", appointment.getDoctorName());
                        intent.putExtra("doctorSpecialty", appointment.getDoctorSpecialty());
                        intent.putExtra("doctorFee", appointment.getDoctorFee());
                        intent.putExtra("doctorImage", appointment.getDoctorImage());
                        intent.putExtra("clinicName", appointment.getClinicName());
                        intent.putExtra("clinicAddress", appointment.getClinicAddress());
                        intent.putExtra("isRescheduling", true);
                        intent.putExtra("oldBookingId", appointment.getBookingId());
                        startActivity(intent);
                    });
                })
                .setNegativeButton("Back", null)
                .show();
    }

    // --- HELPER & UTILITY METHODS ---

    /**
     * Updates the RecyclerView or shows "No Data" message.
     */
    private void updateUI(ArrayList<AppointmentModel> newList) {
        if (!isAdded()) return;
        upcomingList.clear();
        upcomingList.addAll(newList);

        if (upcomingList.isEmpty()) {
            rvUpcoming.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
        } else {
            rvUpcoming.setVisibility(View.VISIBLE);
            tvNoData.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Time parsing logic to determine if a slot is in the past.
     */
    private boolean isSlotExpired(String dateStr, String timeRange) {
        try {
            // Split "10:00 AM - 10:30 AM" to get "10:30 AM"
            String[] parts = timeRange.split("-");
            String endTimePart = (parts.length > 1) ? parts[1].trim() : parts[0].trim();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            Date slotEndTime = sdf.parse(dateStr + " " + endTimePart);

            if (slotEndTime == null) return false;

            Calendar deadline = Calendar.getInstance();
            deadline.setTime(slotEndTime);
            deadline.add(Calendar.MINUTE, GRACE_PERIOD_MINUTES);

            return Calendar.getInstance().after(deadline);
        } catch (Exception e) {
            return false;
        }
    }

    private void markAsMissedInFirebase(AppointmentModel a) {
        updateAppointmentStatus(a, "MISSED");
    }

    private void showStatusInfo(AppointmentModel a) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Appointment Info")
                .setMessage("Doctor: " + a.getDoctorName() + "\nDate: " + a.getDate() + "\nTime: " + a.getTime())
                .setPositiveButton("OK", null)
                .show();
    }

    private boolean matchesQuery(AppointmentModel a, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        return a.getDoctorName() != null && a.getDoctorName().toLowerCase().contains(query.toLowerCase());
    }

    @Override
    public void filterList(String query) {
        this.currentSearchQuery = query;
        attachFirebaseListener(); // Re-filters data based on search text
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Memory safety: Kill the listener when Fragment is fully destroyed
        if (userBookingsRef != null && bookingsListener != null) {
            userBookingsRef.removeEventListener(bookingsListener);
        }
    }
}