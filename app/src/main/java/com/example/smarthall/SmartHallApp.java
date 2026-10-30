package com.example.smarthall;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;

public class SmartHallApp extends Application {

    public static final String CHANNEL_ID = "hall_notices_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        logDeviceToken();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Hall Announcements",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void logDeviceToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Log.d("FCM", "FCM Device Token: " + task.getResult());
            } else {
                Log.w("FCM", "Failed to retrieve FCM Device Token", task.getException());
            }
        });
    }
}
