package com.example.docconnect;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DoctorChatsFragment extends Fragment implements DoctorChatAdapter.OnChatClickListener {

    private View rootView;
    private boolean isDataInitialized = false;

    // UI Components
    private RecyclerView rvChatConsultations;
    private DoctorChatAdapter adapter;
    private List<DoctorChatModel> chatList;
    private TextView tvDoctorName, tvSessionStatus, tvAllPatientsHeader;
    private ShapeableImageView imgDoctor;
    private ImageButton btnBack, btnCalendar;
    private LinearLayout layoutEmptyState;
    private ProgressBar loader;

    // Firebase
    private DatabaseReference rootRef, scheduleRef;
    private FirebaseAuth mAuth;
    private ValueEventListener consultationsListener;

    // Date Utilities
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());

    private String selectedDate;
    private static final int GRACE_PERIOD_MINUTES = 5;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView != null) return rootView;

        rootView = inflater.inflate(R.layout.fragment_doctor_chats, container, false);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();
        selectedDate = dateFormat.format(calendar.getTime());

        initViews(rootView);

        rvChatConsultations.setLayoutManager(new LinearLayoutManager(getContext()));
        chatList = new ArrayList<>();
        adapter = new DoctorChatAdapter(chatList, getContext(), this);
        rvChatConsultations.setAdapter(adapter);

        setupClickListeners();

        if (!isDataInitialized) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded()) {
                    fetchDoctorProfile();
                    startRealTimeConsultationListener();
                    isDataInitialized = true;
                }
            }, 300);
        }

        return rootView;
    }

    private void initViews(View v) {
        rvChatConsultations = v.findViewById(R.id.rvChatConsultations);
        tvDoctorName = v.findViewById(R.id.tvDoctorName);
        imgDoctor = v.findViewById(R.id.imgDoctor);
        btnBack = v.findViewById(R.id.btnBack);
        btnCalendar = v.findViewById(R.id.btnCalendar);
        tvSessionStatus = v.findViewById(R.id.tvSessionStatus);
        tvAllPatientsHeader = v.findViewById(R.id.tvAllPatientsHeader); // New Binding
        layoutEmptyState = v.findViewById(R.id.layoutEmptyState);
        loader = v.findViewById(R.id.loader);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        btnCalendar.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDate = dateFormat.format(calendar.getTime());
                    startRealTimeConsultationListener();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void fetchDoctorProfile() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        rootRef.child("doctors").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String url = snapshot.child("profileImageUrl").getValue(String.class);
                    if (name != null) tvDoctorName.setText(name);

                    if (getContext() != null && url != null) {
                        Glide.with(requireContext()).load(url).centerCrop()
                                .placeholder(R.drawable.ic_doctor).into(imgDoctor);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void startRealTimeConsultationListener() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        if (scheduleRef != null && consultationsListener != null) {
            scheduleRef.removeEventListener(consultationsListener);
        }

        scheduleRef = rootRef.child("DoctorSchedule").child(uid);
        loader.setVisibility(View.VISIBLE);

        consultationsListener = scheduleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                if (isAdded()) loader.setVisibility(View.GONE);

                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        DoctorChatModel model = data.getValue(DoctorChatModel.class);
                        if (model != null) {
                            model.setBookingId(data.getKey());

                            if ("UPCOMING".equalsIgnoreCase(model.getStatus()) &&
                                    isSlotExpired(model.getDate(), model.getTime())) {
                                markAsMissedInFirebase(model);
                                continue;
                            }

                            if ("CHAT".equalsIgnoreCase(model.getConsultationMedium()) &&
                                    selectedDate.equals(model.getDate())) {
                                chatList.add(model);
                            }
                        }
                    }

                    Collections.sort(chatList, (c1, c2) -> {
                        try {
                            Date date1 = dateTimeParser.parse(c1.getDate() + " " + extractStartTime(c1.getTime()));
                            Date date2 = dateTimeParser.parse(c2.getDate() + " " + extractStartTime(c2.getTime()));
                            return date2.compareTo(date1);
                        } catch (ParseException e) { return 0; }
                    });
                }
                updateUI();
                adapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {
                if (isAdded()) loader.setVisibility(View.GONE);
            }
        });
    }

    private boolean isSlotExpired(String dateStr, String timeRange) {
        try {
            String endTimePart = timeRange.contains("-") ? timeRange.split("-")[1].trim() : timeRange.trim();
            Date slotEndTime = dateTimeParser.parse(dateStr + " " + endTimePart);
            if (slotEndTime == null) return false;

            Calendar deadline = Calendar.getInstance();
            deadline.setTime(slotEndTime);
            deadline.add(Calendar.MINUTE, GRACE_PERIOD_MINUTES);
            return Calendar.getInstance().after(deadline);
        } catch (Exception e) { return false; }
    }

    private String extractStartTime(String timeRange) {
        return timeRange.contains("-") ? timeRange.split("-")[0].trim() : timeRange.trim();
    }

    private void markAsMissedInFirebase(DoctorChatModel model) {
        String doctorId = mAuth.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("/DoctorSchedule/" + doctorId + "/" + model.getBookingId() + "/status", "MISSED");
        updates.put("/UserBookings/" + model.getPatientId() + "/" + model.getBookingId() + "/status", "MISSED");
        rootRef.updateChildren(updates);
    }

    private void updateUI() {
        if (!isAdded()) return;

        if (chatList.isEmpty()) {
            // HIDE RecyclerView and "All Patients" Header
            rvChatConsultations.setVisibility(View.GONE);
            tvAllPatientsHeader.setVisibility(View.GONE);

            // SHOW Empty State
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvSessionStatus.setText("No Chats (" + selectedDate + ")");
        } else {
            // SHOW RecyclerView and Header
            rvChatConsultations.setVisibility(View.VISIBLE);
            tvAllPatientsHeader.setVisibility(View.VISIBLE);

            // HIDE Empty State
            layoutEmptyState.setVisibility(View.GONE);
            tvSessionStatus.setText("⚡ " + chatList.size() + " Chats on " + selectedDate);
        }
    }

    @Override
    public void onStartConsultation(DoctorChatModel model) {
        if (model == null) return;
        model.setStatus("ACTIVE");
        adapter.notifyDataSetChanged();

        String doctorId = mAuth.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("/DoctorSchedule/" + doctorId + "/" + model.getBookingId() + "/status", "ACTIVE");
        updates.put("/UserBookings/" + model.getPatientId() + "/" + model.getBookingId() + "/status", "ACTIVE");

        rootRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(getContext(), DUChatsActivity.class);
                    intent.putExtra("CHAT_DATA", model);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(getContext(), "Error starting session", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDetailsClick(DoctorChatModel model) {
        if (model == null) return;
        Intent intent = new Intent(getContext(), DUChatsActivity.class);
        intent.putExtra("CHAT_DATA", model);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduleRef != null && consultationsListener != null) {
            scheduleRef.removeEventListener(consultationsListener);
        }
    }
}