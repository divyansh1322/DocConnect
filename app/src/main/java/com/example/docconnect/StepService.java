package com.example.docconnect;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

/**
 * STEP SERVICE: The 24/7 background engine.
 * Tracks steps, updates the notification live, and handles daily resets.
 */
public class StepService extends Service implements SensorEventListener {

    private static final String TAG = "StepService";
    private static final String CHANNEL_ID = "step_channel";
    private static final int NOTIFICATION_ID = 1;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences sharedPreferences;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        // 1. WAKELOCK
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DocConnect::StepTrackerLock");
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L /*10 minutes timeout for safety*/);
            }
        }
        Log.d(TAG, "Service Created: WakeLock acquired.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 2. CREATE CHANNEL
        createNotificationChannel();

        // 3. START FOREGROUND SAFELY
        startForegroundSafely();

        // 4. REGISTER SENSORS
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sharedPreferences = getSharedPreferences("StepPrefs", MODE_PRIVATE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepSensor != null) {
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }

        return START_STICKY;
    }

    private void startForegroundSafely() {
        Notification notification = buildStepNotification(sharedPreferences != null ? sharedPreferences.getInt("last_steps", 0) : 0);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Check if we actually have the permission before calling startForeground
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
                } else {
                    Log.e(TAG, "Cannot start FGS: ACTIVITY_RECOGNITION permission missing.");
                    stopSelf();
                }
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service: " + e.getMessage());
            // On API 31+, this catches ForegroundServiceStartNotAllowedException
            stopSelf();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalStepsSinceBoot = event.values[0];
            calculateDailySteps(totalStepsSinceBoot);
        }
    }

    private void calculateDailySteps(float totalStepsSinceBoot) {
        if (sharedPreferences == null) sharedPreferences = getSharedPreferences("StepPrefs", MODE_PRIVATE);

        String todayDate = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
        String lastSavedDate = sharedPreferences.getString("last_date", "");
        float sensorOffset = sharedPreferences.getFloat("sensor_offset", -1f);
        int dailySteps = 0;

        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!todayDate.equals(lastSavedDate)) {
            editor.putString("last_date", todayDate);
            editor.putFloat("sensor_offset", totalStepsSinceBoot);
            editor.putInt("last_steps", 0);
            sensorOffset = totalStepsSinceBoot;
        }

        if (sensorOffset == -1f) {
            editor.putFloat("sensor_offset", totalStepsSinceBoot);
            dailySteps = 0;
        } else {
            dailySteps = (int) (totalStepsSinceBoot - sensorOffset);
            if (dailySteps < 0) {
                editor.putFloat("sensor_offset", totalStepsSinceBoot);
                dailySteps = 0;
            }
            editor.putInt("last_steps", dailySteps);
        }
        editor.apply();

        // Broadcast update to Activity
        Intent uiIntent = new Intent("STEP_UPDATE");
        uiIntent.putExtra("LIVE_STEPS", dailySteps);
        sendBroadcast(uiIntent);

        updateNotification(dailySteps);
    }

    private Notification buildStepNotification(int steps) {
        // Build the intent to open MainActivity when clicking notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, notificationIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Steps Today: " + steps)
                .setContentText("DocConnect is tracking your movement")
                .setSmallIcon(R.mipmap.ic_launcher) // Fallback icon
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    private void updateNotification(int steps) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            // Only update if notification permission is granted (Android 13+)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                manager.notify(NOTIFICATION_ID, buildStepNotification(steps));
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Health Tracker", NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy();
        Log.d(TAG, "Service Destroyed.");
    }
}