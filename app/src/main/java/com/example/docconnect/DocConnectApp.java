package com.example.docconnect;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class DocConnectApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ✅ 1. GLOBAL THEME LOCK (Forces Light Mode for the entire app)
        // This stays active even if the user changes system settings later
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // ✅ 2. GLOBAL FONT LOCK
        // We register a callback that "watches" every activity in your app.
        // As soon as any activity is created, we force its font scale to 100%
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                applyGlobalFontScale(activity);
            }

            @Override public void onActivityStarted(@NonNull Activity activity) {}
            @Override public void onActivityResumed(@NonNull Activity activity) {}
            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });

        // ✅ 3. CLOUDINARY CONFIG (Original Logic)
        initCloudinary();
    }

    private void applyGlobalFontScale(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        // Check if the font is currently scaled (not 1.0)
        if (configuration.fontScale != 1.0f) {
            configuration.fontScale = 1.0f; // Reset to 100%

            // This updates the resources of the specific activity being created
            context.getResources().updateConfiguration(configuration,
                    context.getResources().getDisplayMetrics());
        }
    }

    private void initCloudinary() {
        // Config for Cloudinary 3.0.2
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dps6a4fvu");
        config.put("secure", true);

        // Initialize once here to avoid crashes
        MediaManager.init(this, config);
    }
}