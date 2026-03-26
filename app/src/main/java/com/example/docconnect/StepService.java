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
import android.util.Log;

import androidx.core.app.NotificationCompat;

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

        // 1. WAKELOCK: Keeps CPU alive so sensors work when the screen is OFF.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DocConnect::StepTrackerLock");
            wakeLock.acquire();
        }

        Log.d(TAG, "Service Created: WakeLock acquired.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 2. CREATE NOTIFICATION CHANNEL (For Android 8.0+)
        createNotificationChannel();

        // 3. INITIAL NOTIFICATION: Required to start as a Foreground Service.
        Notification notification = buildStepNotification(0);

        // 4. ANDROID 14+ COMPLIANCE: Must specify FOREGROUND_SERVICE_TYPE_HEALTH.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // 5. REGISTER SENSORS
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sharedPreferences = getSharedPreferences("StepPrefs", MODE_PRIVATE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepSensor != null) {
                // SENSOR_DELAY_UI ensures real-time "ticking" on the screen.
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }

        // 6. START_STICKY: Tells Android to restart this service if it gets killed for memory.
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // event.values[0] is the total steps since the phone was last REBOOTED.
            float totalStepsSinceBoot = event.values[0];
            calculateDailySteps(totalStepsSinceBoot);
        }
    }

    /**
     * CORE LOGIC: Manages the daily offset and broadcasts the result.
     */
    private void calculateDailySteps(float totalStepsSinceBoot) {
        String todayDate = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
        String lastSavedDate = sharedPreferences.getString("last_date", "");
        float sensorOffset = sharedPreferences.getFloat("sensor_offset", -1f);
        int dailySteps = 0;

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // CHECK FOR MIDNIGHT/NEW DAY
        if (!todayDate.equals(lastSavedDate)) {
            editor.putString("last_date", todayDate);
            // We save the current sensor value as the "start point" for today.
            editor.putFloat("sensor_offset", totalStepsSinceBoot);
            editor.putInt("last_steps", 0);
            sensorOffset = totalStepsSinceBoot;
        }

        // CALCULATE ACTUAL STEPS
        if (sensorOffset == -1f) {
            editor.putFloat("sensor_offset", totalStepsSinceBoot);
            dailySteps = 0;
        } else {
            dailySteps = (int) (totalStepsSinceBoot - sensorOffset);
            // Handle case where phone reboots (sensor resets to 0)
            if (dailySteps < 0) {
                editor.putFloat("sensor_offset", totalStepsSinceBoot);
                dailySteps = 0;
            }
            editor.putInt("last_steps", dailySteps);
        }
        editor.apply();

        // UPDATE UI: Send broadcast to HomeFragment
        Intent uiIntent = new Intent("STEP_UPDATE");
        uiIntent.putExtra("LIVE_STEPS", dailySteps);
        sendBroadcast(uiIntent);

        // UPDATE NOTIFICATION: Update the status bar text live
        updateNotification(dailySteps);
    }

    /**
     * Builds the notification object with the current step count.
     */
    private Notification buildStepNotification(int steps) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Today's Steps: " + steps)
                .setContentText("DocConnect is tracking your health.")
                .setSmallIcon(R.drawable.ic_footstep) // Ensure this icon exists!
                .setOngoing(true)
                .setOnlyAlertOnce(true) // Stops the phone from beeping on every step update
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void updateNotification(int steps) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildStepNotification(steps));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Health Tracker", NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows real-time step count in the notification bar.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        // CLEANUP: Release WakeLock and stop sensor listeners
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        Log.d(TAG, "Service Destroyed: Resources released.");
    }
}