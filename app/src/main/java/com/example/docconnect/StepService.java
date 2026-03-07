package com.example.docconnect;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

/**
 * STEP SERVICE: Runs 24/7 in the background.
 * Optimized for real-time UI "ticking" and daily resets.
 */
public class StepService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences sharedPreferences;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        // 1. WAKELOCK: Keeps CPU active even if the screen is off.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DocConnect::StepTracker");
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes safety timeout*/);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, "step_channel")
                .setContentTitle("DocConnect Step Tracker")
                .setContentText("Counting your steps in real-time...")
                .setSmallIcon(R.drawable.ic_footstep)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // Android 14+ FGS Type Health requirement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        } else {
            startForeground(1, notification);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sharedPreferences = getSharedPreferences("StepPrefs", MODE_PRIVATE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            // SENSOR_DELAY_UI is the key for real-time "instant" updates
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // event.values[0] is the total steps since the phone's last REBOOT
            float totalStepsSinceBoot = event.values[0];

            // We call the logic method to calculate daily steps and broadcast them
            processAndBroadcastSteps(totalStepsSinceBoot);
        }
    }

    /**
     * CORE LOGIC: Manages midnight resets and triggers the instant UI update.
     */
    private void processAndBroadcastSteps(float totalStepsSinceBoot) {
        String todayDate = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
        String lastSavedDate = sharedPreferences.getString("last_date", "");
        float midnightOffset = sharedPreferences.getFloat("sensor_offset", -1);
        int dailySteps = 0;

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Check for Midnight/New Day reset
        if (!todayDate.equals(lastSavedDate)) {
            editor.putString("last_date", todayDate);
            editor.putFloat("sensor_offset", totalStepsSinceBoot);
            editor.putInt("last_steps", 0);
            midnightOffset = totalStepsSinceBoot; // Update local variable for immediate use
        }

        if (midnightOffset == -1) {
            editor.putFloat("sensor_offset", totalStepsSinceBoot);
            dailySteps = 0;
        } else {
            // Actual Calculation
            dailySteps = (int) (totalStepsSinceBoot - midnightOffset);
            editor.putInt("last_steps", dailySteps);
        }
        editor.apply();

        // --- THE INSTANT BROADCAST ---
        // Ensure HomeFragment uses intent.getIntExtra("LIVE_STEPS", 0)
        Intent intent = new Intent("STEP_UPDATE");
        intent.putExtra("LIVE_STEPS", dailySteps);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "step_channel", "Health Tracking", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }
}