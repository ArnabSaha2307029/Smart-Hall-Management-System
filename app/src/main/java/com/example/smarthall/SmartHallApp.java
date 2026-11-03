package com.example.smarthall;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;

public class SmartHallApp extends Application {

    public static final String CHANNEL_ID = "hall_notices_channel";
    public static final String CHAT_CHANNEL_ID = "hall_chat_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        logDeviceToken();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel noticeChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Hall Announcements",
                        NotificationManager.IMPORTANCE_HIGH);
                noticeChannel.enableLights(true);
                noticeChannel.enableVibration(true);
                noticeChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(noticeChannel);

                NotificationChannel chatChannel = new NotificationChannel(
                        CHAT_CHANNEL_ID,
                        "Hall Chat Messages",
                        NotificationManager.IMPORTANCE_HIGH);
                chatChannel.enableLights(true);
                chatChannel.setLightColor(Color.RED);
                chatChannel.enableVibration(true);
                chatChannel.setVibrationPattern(new long[]{0, 250, 250, 250});
                chatChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                chatChannel.setBypassDnd(true);
                nm.createNotificationChannel(chatChannel);
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
