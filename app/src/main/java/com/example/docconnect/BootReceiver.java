package com.example.docconnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * BOOT RECEIVER: The "Sentry" of the app.
 * This class waits for the system "BOOT_COMPLETED" signal.
 * It ensures that even if the user restarts their phone, tracking starts immediately
 * without them ever having to manually open the DocConnect app.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // We check for three types of boot signals to cover different phone manufacturers
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.d(TAG, "System Boot Detected: Starting DocConnect Step Tracker...");

            // Prepare the intent to launch the StepService
            Intent serviceIntent = new Intent(context, StepService.class);

            try {
                // Android 8.0 (Oreo) and above requires startForegroundService
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to auto-start StepService: " + e.getMessage());
            }
        }
    }
}