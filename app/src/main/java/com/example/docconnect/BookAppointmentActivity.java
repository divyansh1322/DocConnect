package com.example.docconnect;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * BOOK APPOINTMENT ACTIVITY: Handles secure consultation scheduling.
 * Logic: Uses Firebase Transactions to prevent overbooking and
 * Atomic Multi-path updates to keep Doctor and Patient nodes in sync.
 */
public class BookAppointmentActivity extends AppCompatActivity {

    private static final String TAG = "DocConnect_Book";

    // Data passed strictly via Intent
    private String doctorId, doctorName, doctorSpecialty, doctorFee, doctorRating, doctorImage, clinicName, clinicAddress, consultationMedium;
    private String userName = "", userImage = "", userGender = "", userAge = "";

    // State Flags
    private boolean isRescheduling = false;
    private String oldBookingId = "";

    // UI Elements
    private TextView tvDoctorName, tvDoctorSpecialty, tvDoctorFee, tvDoctorRating, tvCalenderMonth;
    private ImageView imgDoctorBook;
    private LinearLayout layoutEmptySlots;
    private RecyclerView rvDates, rvTimeSlots;
    private MaterialButton btnBook;

    private final List<String> dateList = new ArrayList<>();
    private final Map<String, List<SlotsModel>> slotMap = new HashMap<>();
    private DateAdapter dateAdapter;
    private SlotAdapter slotAdapter;
    private String selectedDate = "";
    private SlotsModel selectedSlot;

    private DatabaseReference db;
    private FirebaseAuth auth;
    private String originalButtonText = "Book Appointment";
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        db = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerViews();
        receiveIntentData(); // All data strictly from Intent
        generateNextFiveDates();
        fetchUserProfile(); // Sync patient metadata
        fetchSlotsFromFirebase();
        updateUserFcmToken();

        btnBook.setOnClickListener(v -> attemptSecureBooking());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvDoctorSpecialty = findViewById(R.id.tvDoctorSpecialty);
        tvDoctorFee = findViewById(R.id.tvDoctorFeeBook);
        tvDoctorRating = findViewById(R.id.tvDoctorRating);
        imgDoctorBook = findViewById(R.id.imgDoctorBook);
        rvDates = findViewById(R.id.rvDates);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        layoutEmptySlots = findViewById(R.id.layoutEmptySlots);
        btnBook = findViewById(R.id.btnBook);
        tvCalenderMonth = findViewById(R.id.tvCalenderMonth);

        btnBook.setEnabled(false);
        btnBook.setAlpha(0.6f);
    }

    private void setupRecyclerViews() {
        dateAdapter = new DateAdapter(dateList, this::onDateSelected);
        rvDates.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDates.setAdapter(dateAdapter);

        slotAdapter = new SlotAdapter(new ArrayList<>(), slot -> {
            selectedSlot = slot;
            btnBook.setEnabled(true);
            btnBook.setAlpha(1.0f);
        });
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        rvTimeSlots.setAdapter(slotAdapter);
    }

    private void receiveIntentData() {
        Intent i = getIntent();

        // Doctor Info - Prioritize specific intent extras
        doctorId = i.getStringExtra("doctorId");
        if (doctorId == null) doctorId = i.getStringExtra("id");
        doctorName = i.getStringExtra("doctorName");
        doctorSpecialty = i.getStringExtra("doctorSpecialty");
        doctorFee = i.getStringExtra("doctorFee");
        doctorImage = i.getStringExtra("doctorImage");
        doctorRating = i.getStringExtra("ratings");
        if (doctorRating == null) doctorRating = "4.5";

        // Clinic & Medium Info
        clinicName = i.getStringExtra("clinicName");
        clinicAddress = i.getStringExtra("clinicAddress");
        consultationMedium = i.getStringExtra("CONSULTATION_MEDIUM");
        if (consultationMedium == null) consultationMedium = "Chat";

        // Reschedule Check
        isRescheduling = i.getBooleanExtra("isRescheduling", false);
        oldBookingId = i.getStringExtra("oldBookingId");

        if (isRescheduling) {
            originalButtonText = "Confirm Reschedule";
            btnBook.setText(originalButtonText);
        }

        // UI Hydration
        tvDoctorName.setText(doctorName);
        tvDoctorSpecialty.setText(doctorSpecialty);
        tvDoctorFee.setText(String.format("%s Rs", doctorFee));
        tvDoctorRating.setText(doctorRating);

        if (!isFinishing()) {
            Glide.with(this)
                    .load(doctorImage)
                    .centerCrop()
                    .placeholder(R.drawable.ic_doctor)
                    .into(imgDoctorBook);
        }
    }

    /**
     * CONCURRENCY LOGIC: uses Firebase runTransaction to ensure slot remains AVAILABLE
     * during the booking attempt. This prevents race conditions.
     */
    private void attemptSecureBooking() {
        if (userName == null || userName.isEmpty()) {
            fetchUserProfile();
            Toast.makeText(this, "Fetching profile... try again", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();
        btnBook.setEnabled(false);

        // Define slot path based on timing
        String slotId = formatTime(selectedSlot.getStartMillis()) + "_" + formatTime(selectedSlot.getEndMillis());
        DatabaseReference slotRef = db.child("doctors").child(doctorId).child("allslots").child(selectedDate).child(slotId);

        slotRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                SlotsModel slot = data.getValue(SlotsModel.class);
                // Abort if slot was taken while user was on this screen
                if (slot == null || !"AVAILABLE".equalsIgnoreCase(slot.getStatus())) return Transaction.abort();

                data.child("status").setValue("BOOKED");
                data.child("bookedBy").setValue(auth.getUid());
                return Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError e, boolean committed, DataSnapshot s) {
                if (committed) executeAtomicUpdate(slotId);
                else {
                    hideLoading();
                    restoreButtonAndShow("Slot already taken by another patient.");
                }
            }
        });
    }

    /**
     * ATOMIC MASTER UPDATE: Writes to multiple paths simultaneously.
     * Ensures Doctor and Patient see the same data at the same time.
     */
    private void executeAtomicUpdate(String slotId) {
        String patientId = auth.getUid();
        String bookingId = db.child("UserBookings").child(patientId).push().getKey();
        if (bookingId == null || patientId == null) return;

        Map<String, Object> appointment = new HashMap<>();
        appointment.put("bookingId", bookingId);
        appointment.put("patientId", patientId);
        appointment.put("patientName", userName);
        appointment.put("patientImage", userImage);
        appointment.put("patientGender", userGender);
        appointment.put("patientAge", userAge);

        appointment.put("doctorId", doctorId);
        appointment.put("doctorName", doctorName);
        appointment.put("doctorSpecialty", doctorSpecialty);
        appointment.put("doctorFee", doctorFee);
        appointment.put("doctorImage", doctorImage);

        appointment.put("date", selectedDate);
        appointment.put("time", selectedSlot.getTimeDisplay());
        appointment.put("clinicName", clinicName );
        appointment.put("clinicAddress", clinicAddress );
        appointment.put("consultationMedium", consultationMedium);
        appointment.put("status", "UPCOMING");
        appointment.put("timestamp", ServerValue.TIMESTAMP);
        appointment.put("isRated", false); // Important for HomeFragment alerts
        appointment.put("isAlertShown", false); // Logic for popup persistence

        Map<String, Object> masterUpdate = new HashMap<>();
        masterUpdate.put("/UserBookings/" + patientId + "/" + bookingId, appointment);
        masterUpdate.put("/DoctorSchedule/" + doctorId + "/" + bookingId, appointment);

        // Logic for handling Rescheduling
        if (isRescheduling && oldBookingId != null) {
            masterUpdate.put("/UserBookings/" + patientId + "/" + oldBookingId + "/status", "RESCHEDULED_OUT");
            masterUpdate.put("/UserBookings/" + patientId + "/" + oldBookingId + "/newBookingId", bookingId);
            masterUpdate.put("/DoctorSchedule/" + doctorId + "/" + oldBookingId + "/status", "RESCHEDULED_OUT");
            masterUpdate.put("/DoctorSchedule/" + doctorId + "/" + oldBookingId + "/newBookingId", bookingId);
            appointment.put("rescheduledFrom", oldBookingId);
        }

        db.updateChildren(masterUpdate).addOnCompleteListener(task -> {
            hideLoading();
            if (task.isSuccessful()) {
                navigateToSuccess(bookingId);
            } else {
                // Rollback slot status if the master update fails (Atomic Fallback)
                db.child("doctors").child(doctorId).child("allslots").child(selectedDate).child(slotId).child("status").setValue("AVAILABLE");
                restoreButtonAndShow("Booking failed. Please try again.");
            }
        });
    }

    private void navigateToSuccess(String bookingId) {
        Intent intent = new Intent(this, AppointmentSuccessActivity.class);
        intent.putExtra("bookingId", bookingId);
        intent.putExtra("doctorName", doctorName);
        intent.putExtra("doctorSpecialty", doctorSpecialty);
        intent.putExtra("doctorImage", doctorImage);
        intent.putExtra("clinicName", clinicName);
        intent.putExtra("clinicAddress", clinicAddress);
        intent.putExtra("doctorFee", doctorFee);
        intent.putExtra("date", selectedDate);
        intent.putExtra("time", selectedSlot.getTimeDisplay());
        intent.putExtra("CONSULTATION_MEDIUM", consultationMedium);
        startActivity(intent);
        finish();
    }

    // --- FIREBASE DATA SYNC ---

    private void fetchUserProfile() {
        String uid = auth.getUid();
        if (uid == null) return;
        db.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists() && !isFinishing()) {
                    userName = s.child("fullName").getValue(String.class);
                    userImage = s.child("profilePhotoUrl").getValue(String.class);
                    userGender = s.child("gender").getValue(String.class);
                    Object ageObj = s.child("age").getValue();
                    userAge = (ageObj != null) ? String.valueOf(ageObj) : "N/A";
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void fetchSlotsFromFirebase() {
        if (doctorId == null) return;
        db.child("doctors").child(doctorId).child("allslots").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                slotMap.clear();
                long now = System.currentTimeMillis();
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    List<SlotsModel> slots = new ArrayList<>();
                    for (DataSnapshot slotSnap : dateSnap.getChildren()) {
                        SlotsModel slot = slotSnap.getValue(SlotsModel.class);
                        // Filter for available and future slots only
                        if (slot != null && "AVAILABLE".equalsIgnoreCase(slot.getStatus()) && slot.getStartMillis() > now) {
                            slots.add(slot);
                        }
                    }
                    if (!slots.isEmpty()) {
                        slots.sort(Comparator.comparingLong(SlotsModel::getStartMillis));
                        slotMap.put(dateSnap.getKey(), slots);
                    }
                }
                if (!selectedDate.isEmpty()) updateSlotUI(slotMap.get(selectedDate));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- UI HELPERS ---

    private void onDateSelected(String date) {
        selectedDate = date;
        selectedSlot = null;
        updateSlotUI(slotMap.get(date));
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(date);
            if (d != null) tvCalenderMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d));
        } catch (Exception e) { Log.e(TAG, "Date formatting error"); }
    }

    private void updateSlotUI(List<SlotsModel> slots) {
        if (slots == null || slots.isEmpty()) {
            rvTimeSlots.setVisibility(View.GONE);
            layoutEmptySlots.setVisibility(View.VISIBLE);
            slotAdapter.updateList(new ArrayList<>());
        } else {
            rvTimeSlots.setVisibility(View.VISIBLE);
            layoutEmptySlots.setVisibility(View.GONE);
            slotAdapter.updateList(slots);
        }
        btnBook.setEnabled(false);
        btnBook.setAlpha(0.6f);
    }

    private void generateNextFiveDates() {
        dateList.clear();
        Calendar c = Calendar.getInstance();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (int i = 0; i < 5; i++) {
            dateList.add(keyFormat.format(c.getTime()));
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        dateAdapter.notifyDataSetChanged();
        if (!dateList.isEmpty()) onDateSelected(dateList.get(0));
    }

    private void showLoading() {
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.dialog_layout);
        if (loadingDialog.getWindow() != null) loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    private void restoreButtonAndShow(String msg) {
        btnBook.setEnabled(true);
        btnBook.setAlpha(1.0f);
        btnBook.setText(originalButtonText);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("HHmm", Locale.getDefault()).format(new Date(millis));
    }

    private void updateUserFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && auth.getUid() != null) {
                db.child("users").child(auth.getUid()).child("fcmToken").setValue(task.getResult());
            }
        });
    }
}