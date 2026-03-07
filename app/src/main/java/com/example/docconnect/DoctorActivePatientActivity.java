package com.example.docconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DoctorActivePatientActivity: A-Z implementation for viewing patient history.
 * Logic: Pulls image/name from DoctorSchedule (per DB screenshot) and contact info from users node.
 */
public class DoctorActivePatientActivity extends AppCompatActivity {

    // --- UI HOOKS ---
    private TextView tvName, tvPhone, tvEmail, tvAge, tvGender;
    private ImageView imgProfile;
    private ImageButton btnBack;
    private RecyclerView rvHistory;
    private LinearLayout layoutEmpty;

    // --- FIREBASE & DATA ---
    private DatabaseReference rootRef;
    private String doctorId, patientId;
    private PatientHistoryAdapter adapter;
    private final List<PatientHistoryModel> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_active_patient);

        // 1. Retrieve IDs passed from the ScheduleAdapter
        doctorId = getIntent().getStringExtra("doctorId");
        patientId = getIntent().getStringExtra("patientId");

        // Safety check to prevent app crash if IDs are missing
        if (doctorId == null || patientId == null) {
            Toast.makeText(this, "Data Sync Error: Missing IDs", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();

        rootRef = FirebaseDatabase.getInstance().getReference();

        // 2. Fetch Data
        fetchPatientDataFromSchedule();
        fetchSecondaryUserDetails();

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvName = findViewById(R.id.user_name_text);
        tvPhone = findViewById(R.id.phone_val);
        tvEmail = findViewById(R.id.email_val);
        tvAge = findViewById(R.id.join_val);
        tvGender = findViewById(R.id.status_badge);
        imgProfile = findViewById(R.id.img_profile);
        btnBack = findViewById(R.id.btn_back);
        rvHistory = findViewById(R.id.rv_user_appointments);
        layoutEmpty = findViewById(R.id.layout_empty_appointments);
    }

    private void setupRecyclerView() {
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientHistoryAdapter(historyList);
        rvHistory.setAdapter(adapter);
    }

    /**
     * Logic: Pulls name and image from DoctorSchedule node (Matches your Screenshot).
     */
    private void fetchPatientDataFromSchedule() {
        // We query the doctor's specific schedule to find this patient's records
        rootRef.child("DoctorSchedule").child(doctorId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();
                boolean profileSet = false;

                for (DataSnapshot d : snapshot.getChildren()) {
                    PatientHistoryModel model = d.getValue(PatientHistoryModel.class);
                    if (model != null && patientId.equals(model.getPatientId())) {
                        model.setKey(d.getKey());
                        historyList.add(model);

                        // Use the FIRST matching record to set the Profile Image and Name
                        if (!profileSet) {
                            tvName.setText(d.child("patientName").getValue(String.class));
                            String url = d.child("patientImage").getValue(String.class); // Fix: Key from screenshot

                            if (url != null && !url.isEmpty() && !isFinishing()) {
                                Glide.with(DoctorActivePatientActivity.this)
                                        .load(url)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_doctor)
                                        .into(imgProfile);
                            }
                            profileSet = true;
                        }
                    }
                }

                // Sort history: Most recent date first
                Collections.sort(historyList, (a, b) -> b.getDate().compareTo(a.getDate()));

                // Toggle visibility based on data availability
                if (historyList.isEmpty()) {
                    rvHistory.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvHistory.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(DoctorActivePatientActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Secondary Fetch: Pulls contact info (Phone/Email/Age) from the 'users' node.
     */
    private void fetchSecondaryUserDetails() {
        rootRef.child("users").child(patientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists()) {
                    tvPhone.setText(s.child("phone").getValue(String.class));
                    tvEmail.setText(s.child("email").getValue(String.class));
                    tvAge.setText(s.child("age").getValue(String.class));

                    String gender = s.child("gender").getValue(String.class);
                    if (gender != null) tvGender.setText(gender.toUpperCase());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }
}