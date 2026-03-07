package com.example.docconnect;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * AppointmentsRecordActivity: Professional Ledger Implementation
 * A2Z Features: Multi-User Data Parsing, Real-time Doctor/Patient Search, Date Management.
 */
public class AppointmentsRecordActivity extends AppCompatActivity {

    private RecyclerView rvAppointments;
    private AdminAppointmentAdapter adapter;
    private List<AdminAppointmentModel> fullList = new ArrayList<>();
    private DatabaseReference dbRef;
    private ValueEventListener appointmentsListener;

    private TextInputEditText etSearch;
    private TextView tvDateLabel;
    private LinearLayout layoutNoData;
    private MaterialButton btnFilterStatus;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments_record);

        // 1. View Initialization
        initUI();

        // 2. Button Logic (Status Popup)
        setupFilterButtons();

        // 3. Firebase Setup
        dbRef = FirebaseDatabase.getInstance().getReference("UserBookings");

        // 4. Set Initial Date (Today)
        if (savedInstanceState != null) {
            selectedDate = savedInstanceState.getString("selected_date");
            tvDateLabel.setText(savedInstanceState.getString("date_label"));
        } else {
            Calendar cal = Calendar.getInstance();
            updateSelectedDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }

        // 5. Start Real-time listener
        loadAppointments();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selected_date", selectedDate);
        outState.putString("date_label", tvDateLabel.getText().toString());
    }

    private void initUI() {
        rvAppointments = findViewById(R.id.rvAppointments);
        etSearch = findViewById(R.id.etSearch);
        tvDateLabel = findViewById(R.id.tvDateLabel);
        layoutNoData = findViewById(R.id.layoutNoData);
        btnFilterStatus = findViewById(R.id.btnFilterStatus);
        ImageButton btnCalendar = findViewById(R.id.btnCalendar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup Recycler with LayoutManager
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminAppointmentAdapter(new ArrayList<>());
        rvAppointments.setAdapter(adapter);

        // --- CALENDAR DIALOG LOGIC ---
        btnCalendar.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                updateSelectedDate(year, month, dayOfMonth);
                applyFilters();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

            if (!isFinishing()) dpd.show();
        });

        // --- DUAL-NAME SEARCH LOGIC (Real-time) ---
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterButtons() {
        btnFilterStatus.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            String[] statuses = {"All Appointments", "Upcoming", "Completed", "Cancelled", "Missed"};
            for (String s : statuses) popup.getMenu().add(s);
            popup.setOnMenuItemClickListener(item -> {
                btnFilterStatus.setText(item.getTitle());
                applyFilters();
                return true;
            });
            popup.show();
        });
    }

    private void loadAppointments() {
        appointmentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;

                fullList.clear();
                // Iterating through UserIDs -> BookingIDs
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    for (DataSnapshot booking : userSnap.getChildren()) {
                        try {
                            AdminAppointmentModel model = booking.getValue(AdminAppointmentModel.class);
                            if (model != null) {
                                model.setUserId(userSnap.getKey());
                                model.setId(booking.getKey());
                                fullList.add(model);
                            }
                        } catch (Exception e) {
                            Log.e("Firebase", "Parsing error: " + e.getMessage());
                        }
                    }
                }
                applyFilters(); // Apply current filters to new data
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AppointmentsRecordActivity.this, "Sync Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        dbRef.addValueEventListener(appointmentsListener);
    }

    /**
     * MASTER FILTER LOGIC
     * Combines Date, Status, and Search Query (Doctor + Patient Names).
     */
    private void applyFilters() {
        if (isFinishing() || adapter == null) return;

        String query = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        String statusFilter = btnFilterStatus.getText().toString();

        List<AdminAppointmentModel> filteredList = new ArrayList<>();

        for (AdminAppointmentModel item : fullList) {
            // 1. Match selected Date
            boolean matchesDate = selectedDate.equals(item.getDate());

            // 2. Match Search Query (Searches both Doctor and Patient names)
            boolean matchesSearch = query.isEmpty() ||
                    (item.getDoctorName() != null && item.getDoctorName().toLowerCase().contains(query)) ||
                    (item.getPatientName() != null && item.getPatientName().toLowerCase().contains(query));

            // 3. Match Status Filter
            boolean matchesStatus = statusFilter.equals("Any Status") ||
                    (item.getStatus() != null && item.getStatus().equalsIgnoreCase(statusFilter));

            // Final inclusion check
            if (matchesDate && matchesSearch && matchesStatus) {
                filteredList.add(item);
            }
        }

        updateUIState(filteredList);
    }

    private void updateUIState(List<AdminAppointmentModel> filteredList) {
        if (filteredList.isEmpty()) {
            rvAppointments.setVisibility(View.GONE);
            layoutNoData.setVisibility(View.VISIBLE);
        } else {
            rvAppointments.setVisibility(View.VISIBLE);
            layoutNoData.setVisibility(View.GONE);
        }
        adapter.updateList(filteredList);
    }

    private void updateSelectedDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(cal.getTime());

        Calendar today = Calendar.getInstance();
        if (year == today.get(Calendar.YEAR) && month == today.get(Calendar.MONTH) && day == today.get(Calendar.DAY_OF_MONTH)) {
            tvDateLabel.setText("TODAY'S APPOINTMENTS");
        } else {
            tvDateLabel.setText("APPOINTMENTS FOR " + selectedDate);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // CLEANUP: Prevent memory leaks and background data usage
        if (dbRef != null && appointmentsListener != null) {
            dbRef.removeEventListener(appointmentsListener);
        }
    }
}