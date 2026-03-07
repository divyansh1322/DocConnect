package com.example.docconnect;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "DocConnect_FCM";
    private static final String CHANNEL_ID = "DocConnect_Notifications";
    private static final String CHANNEL_NAME = "DocConnect Alerts";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // 1. Extract Data from FCM Payload
        Map<String, String> data = remoteMessage.getData();
        String title = "";
        String body = "";
        String imageUrl = data.get("image_url");
        String navigateTo = data.get("navigate_to");
        String saveFlag = data.get("save_to_history");

        // Custom Role/Type for your different doctor system
        String notifType = data.get("notif_type"); // e.g. "doctor_booking"

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else {
            title = data.get("title");
            body = data.get("body");
        }

        // 2. Show the System Banner
        showNotification(title, body, navigateTo, imageUrl, notifType);

        // 3. Save to History (Using role-based database path)
        if (!"false".equals(saveFlag)) {
            saveNotificationToHistory(title, body, imageUrl, notifType);
        }
    }

    private void showNotification(String title, String message, String navigateTo, String imageUrl, String notifType) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (navigateTo != null) {
            intent.putExtra("navigate_to", navigateTo);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Logic for Different Doctor Sounds/Icons
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        int icon = R.drawable.ic_notification; // Your standard icon

        // If you send 'doctor_alert' in Console, you can change behavior here
        if ("doctor_alert".equals(notifType)) {
            // Option to set a different sound or priority for doctors
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Handle Image Previews
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Bitmap bitmap = getBitmapFromUrl(imageUrl);
            if (bitmap != null) {
                builder.setLargeIcon(bitmap);
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon((Bitmap) null)); // Fix for ambiguity error
            }
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void saveNotificationToHistory(String title, String message, String imageUrl, String type) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Database path: notifications -> userId -> uniqueID
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications").child(uid);
        String notifId = ref.push().getKey();

        // Current Time (e.g., 10:30 AM)
        String timeStamp = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        /**
         * UPDATED MODEL CALL:
         * We pass 'false' for the 'read' parameter because a
         * brand new notification has not been seen yet.
         */
        NotificationModel model = new NotificationModel(
                title,
                message,
                timeStamp,
                imageUrl,
                type,
                false  // This is the 'read' argument
        );

        if (notifId != null) {
            ref.child(notifId).setValue(model)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save notif: " + e.getMessage()));
        }
    }

    private Bitmap getBitmapFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Image fail: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateTokenInDatabase(token);
    }

    private void updateTokenInDatabase(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            SharedPreferences prefs = getSharedPreferences("DocConnectData", MODE_PRIVATE);
            String userRole = prefs.getString("selected_role", "user");

            // Syncs to your existing doctors or users nodes
            String node = "user".equals(userRole) ? "users" : "doctors";
            FirebaseDatabase.getInstance().getReference(node)
                    .child(uid).child("fcmToken").setValue(token);
        }
    }
}