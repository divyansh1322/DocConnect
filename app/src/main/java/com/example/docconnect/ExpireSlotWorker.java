package com.example.docconnect;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * PRODUCTION WORKER: Handles background Firebase updates when a slot expires.
 * Runs even if the app is killed or the phone is rebooted.
 */
public class ExpireSlotWorker extends Worker {

    public ExpireSlotWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // 1. Extract data passed from the Fragment
        String bookingId = getInputData().getString("booking_id");
        String patientId = getInputData().getString("patient_id");
        String doctorId = FirebaseAuth.getInstance().getUid();

        // Safety check: If data is missing, don't retry (Failure)
        if (bookingId == null || patientId == null || doctorId == null) {
            return Result.failure();
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // 2. Prepare the Atomic Update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("/DoctorSchedule/" + doctorId + "/" + bookingId + "/status", "MISSED");
        updates.put("/UserBookings/" + patientId + "/" + bookingId + "/status", "MISSED");

        try {
            // 3. BLOCKING CALL: WorkManager runs on a background thread.
            // We MUST wait for Firebase to finish before returning success.
            // Timeout after 30 seconds if internet is totally stuck.
            Tasks.await(rootRef.updateChildren(updates), 30, TimeUnit.SECONDS);
            return Result.success();

        } catch (Exception e) {
            // 4. RETRY: If network fails, WorkManager will automatically
            // try again later based on Android's backoff policy.
            return Result.retry();
        }
    }
}