package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class DoctorHomeFragment extends Fragment {

    private View rootView;
    private boolean isDataInitialized = false;

    private RecyclerView rvUpcoming;
    private DoctorUpcomingAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvDoctorName, tvClinicName, tvGreeting;
    private TextView tvActiveName, tvActiveDetails, tvPendingCount, tvCompletedCount, tvAppointmentCountSummary;
    private MaterialButton btnUpdateActive, btnViewHistory;
    private ImageView ivDoctorProfile;
    private View layoutActivePatientData, layoutNoActivePatient;
    private LinearLayout layoutEmptyUpcoming;
    private FrameLayout btnNotifications;

    private DatabaseReference scheduleDbRef, doctorDbRef;
    private String currentDoctorId;
    private final List<DoctorUpcomingModel> rawList = new ArrayList<>();
    private int completedTodayCount = 0;
    private DoctorUpcomingModel activePatient = null;
    private String lastActivePatientId = "";

    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded()) {
                processAppointmentsAndRefreshUI();
                timeHandler.postDelayed(this, 10000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (rootView != null) return rootView;

        rootView = inflater.inflate(R.layout.fragment_doctor_home, container, false);
        currentDoctorId = FirebaseAuth.getInstance().getUid();
        if (currentDoctorId == null) return rootView;

        initViews(rootView);
        initFirebase();
        setupRecyclerView();
        setupListeners();

        if (!isDataInitialized) {
            loadInitialData();
            isDataInitialized = true;
        }

        return rootView;
    }

    private void initViews(View v) {
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        ivDoctorProfile = v.findViewById(R.id.ivDoctorProfile);
        tvDoctorName = v.findViewById(R.id.tvDoctorName);
        tvClinicName = v.findViewById(R.id.tvClinicName);
        tvGreeting = v.findViewById(R.id.tvGreeting);
        tvActiveName = v.findViewById(R.id.tvActivePatientName);
        tvActiveDetails = v.findViewById(R.id.tvActivePatientDetails);
        tvPendingCount = v.findViewById(R.id.tvPendingCount);
        tvCompletedCount = v.findViewById(R.id.tvCompletedCount);
        tvAppointmentCountSummary = v.findViewById(R.id.tvAppointmentCountSummary);
        btnUpdateActive = v.findViewById(R.id.btnCompleteActive);
        btnViewHistory = v.findViewById(R.id.btnViewHistory);
        rvUpcoming = v.findViewById(R.id.rvUpcoming);
        layoutActivePatientData = v.findViewById(R.id.layoutActivePatientData);
        layoutNoActivePatient = v.findViewById(R.id.layoutNoActivePatient);
        layoutEmptyUpcoming = v.findViewById(R.id.layoutEmptyUpcoming);
        btnNotifications = v.findViewById(R.id.btnNotifications);
    }

    private void initFirebase() {
        scheduleDbRef = FirebaseDatabase.getInstance().getReference("DoctorSchedule").child(currentDoctorId);
        doctorDbRef = FirebaseDatabase.getInstance().getReference("doctors").child(currentDoctorId);
    }

    private void setupRecyclerView() {
        rvUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DoctorUpcomingAdapter(new ArrayList<>());
        rvUpcoming.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadInitialData);
        btnNotifications.setOnClickListener(v -> startActivity(new Intent(getContext(), DoctorNotificationsActivity.class)));

        btnUpdateActive.setOnClickListener(v -> {
            if (activePatient != null) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Update Session")
                        .setMessage("Mark this session as:")
                        .setPositiveButton("Completed", (d, w) -> updateStatusEverywhere(activePatient, "COMPLETED"))
                        .setNeutralButton("Missed", (d, w) -> updateStatusEverywhere(activePatient, "MISSED"))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        btnViewHistory.setOnClickListener(v -> {
            if (activePatient != null && activePatient.getPatientId() != null) {
                Intent intent = new Intent(getContext(), DoctorActivePatientActivity.class);
                intent.putExtra("doctorId", currentDoctorId);
                intent.putExtra("patientId", activePatient.getPatientId());
                startActivity(intent);
            }
        });
    }

    private void loadInitialData() {
        if (!isAdded()) return;
        updateGreeting();
        fetchDoctorProfile();
        fetchAppointments();
    }

    private void fetchDoctorProfile() {
        doctorDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (!isAdded() || !s.exists()) return;
                tvDoctorName.setText("Dr. " + s.child("fullName").getValue(String.class));
                tvClinicName.setText(s.child("clinicName").getValue(String.class));
                String imgUrl = s.child("profileImageUrl").getValue(String.class);
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    Glide.with(requireContext()).load(imgUrl).circleCrop().into(ivDoctorProfile);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void fetchAppointments() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        scheduleDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                rawList.clear();
                completedTodayCount = 0;
                for (DataSnapshot d : snapshot.getChildren()) {
                    DoctorUpcomingModel m = d.getValue(DoctorUpcomingModel.class);
                    if (m != null && today.equals(m.getDate())) {
                        m.setKey(d.getKey());
                        if ("COMPLETED".equalsIgnoreCase(m.getStatus())) completedTodayCount++;
                        else if ("UPCOMING".equalsIgnoreCase(m.getStatus())) rawList.add(m);
                    }
                }
                Collections.sort(rawList, (a, b) -> a.getTime().compareTo(b.getTime()));
                processAppointmentsAndRefreshUI();
                swipeRefresh.setRefreshing(false);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { swipeRefresh.setRefreshing(false); }
        });
    }

    private void processAppointmentsAndRefreshUI() {
        if (!isAdded()) return;
        activePatient = null;
        List<DoctorUpcomingModel> upcomingFiltered = new ArrayList<>();
        for (DoctorUpcomingModel p : rawList) {
            if (activePatient == null && hasStarted(p.getTime())) activePatient = p;
            else if (!hasStarted(p.getTime())) upcomingFiltered.add(p);
        }
        updateDashboardUI(upcomingFiltered);
    }

    private void updateDashboardUI(List<DoctorUpcomingModel> upcoming) {
        int totalLeft = upcoming.size() + (activePatient != null ? 1 : 0);
        tvAppointmentCountSummary.setText("You have " + totalLeft + " appointment" + (totalLeft == 1 ? "" : "s") + " left.");

        if (activePatient != null) {
            layoutActivePatientData.setVisibility(View.VISIBLE);
            layoutNoActivePatient.setVisibility(View.GONE);
            tvActiveName.setText(activePatient.getPatientName());
            tvActiveDetails.setText(activePatient.getPatientAge() + " yrs | " + activePatient.getPatientGender());
            if (!activePatient.getKey().equals(lastActivePatientId)) {
                triggerPulseAnimation(layoutActivePatientData);
                lastActivePatientId = activePatient.getKey();
            }
        } else {
            layoutActivePatientData.setVisibility(View.GONE);
            layoutNoActivePatient.setVisibility(View.VISIBLE);
        }

        tvPendingCount.setText(String.format(Locale.getDefault(), "%02d", upcoming.size()));
        tvCompletedCount.setText(String.format(Locale.getDefault(), "%02d", completedTodayCount));
        rvUpcoming.setVisibility(upcoming.isEmpty() ? View.GONE : View.VISIBLE);
        layoutEmptyUpcoming.setVisibility(upcoming.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.updateList(upcoming);
    }

    /**
     * CORE LOGIC: Multi-path update + Counter increment
     */
    private void updateStatusEverywhere(DoctorUpcomingModel m, String status) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("/DoctorSchedule/" + currentDoctorId + "/" + m.getKey() + "/status", status);
        if (m.getPatientId() != null) {
            updateMap.put("/UserBookings/" + m.getPatientId() + "/" + m.getKey() + "/status", status);
        }

        root.updateChildren(updateMap).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Session Marked as " + status, Toast.LENGTH_SHORT).show();
            if ("COMPLETED".equalsIgnoreCase(status)) {
                incrementTotalPatientsCount();
            }
        });
    }

    /**
     * INCREMENT LOGIC: Increments totalPatients under the 'doctors' node atomically.
     */
    private void incrementTotalPatientsCount() {
        DatabaseReference totalRef = FirebaseDatabase.getInstance().getReference("doctors")
                .child(currentDoctorId).child("totalPatients");

        totalRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer count = currentData.getValue(Integer.class);
                if (count == null) currentData.setValue(0);
                else currentData.setValue(count + 1);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (!committed) Log.e("FirebaseCount", "Transaction failed");
            }
        });
    }

    private boolean hasStarted(String timeStr) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Calendar now = Calendar.getInstance();
            Date date = df.parse(timeStr);
            if (date == null) return false;
            Calendar appt = Calendar.getInstance();
            appt.setTime(date);
            appt.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE));
            return !now.before(appt);
        } catch (Exception e) { return false; }
    }

    private void triggerPulseAnimation(View view) {
        Animation pulse = AnimationUtils.loadAnimation(getContext(), R.anim.pulse);
        view.startAnimation(pulse);
    }

    private void updateGreeting() {
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        tvGreeting.setText(hr < 12 ? "Good Morning," : hr < 17 ? "Good Afternoon," : "Good Evening,");
    }

    @Override public void onResume() { super.onResume(); timeHandler.post(timeRunnable); }
    @Override public void onPause() { super.onPause(); timeHandler.removeCallbacks(timeRunnable); }
}