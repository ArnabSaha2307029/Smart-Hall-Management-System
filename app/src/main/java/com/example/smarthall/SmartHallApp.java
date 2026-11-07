package com.example.smarthall;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;

public class SmartHallApp extends Application {

    public static final String CHANNEL_ID = "hall_notices_channel";
    public static final String CHAT_CHANNEL_ID = "hall_chat_channel";
    public static final String SOS_CHANNEL_ID = "sos_emergency_alert_channel";
    public static final String LEGACY_SOS_CHANNEL_ID = "emergency_alerts_channel";

    private static int runningActivities = 0;

    public static boolean isAppInForeground() {
        return runningActivities > 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                runningActivities++;
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                runningActivities--;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
        createNotificationChannels();
        logDeviceToken();
        subscribeToSosTopic();
        startSosServiceIfLoggedIn();

        android.content.SharedPreferences prefs = getSharedPreferences("smart_hall_prefs", MODE_PRIVATE);
        if (prefs.contains("night_mode_preference")) {
            int mode = prefs.getInt("night_mode_preference", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    private void startSosServiceIfLoggedIn() {
        android.content.SharedPreferences userPrefs = getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE);
        boolean hasUserSession = userPrefs.contains("user_role") || userPrefs.contains("roll");
        if (hasUserSession) {
            android.content.Intent serviceIntent = new android.content.Intent(this, com.example.smarthall.services.EmergencySosForegroundService.class);
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
        }
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

                NotificationChannel sosChannel = new NotificationChannel(
                        SOS_CHANNEL_ID,
                        "Emergency Alerts",
                        NotificationManager.IMPORTANCE_HIGH);
                sosChannel.setDescription("High priority full-screen emergency SOS alerts");
                sosChannel.enableLights(true);
                sosChannel.setLightColor(Color.RED);
                sosChannel.enableVibration(true);
                sosChannel.setVibrationPattern(new long[]{0, 800, 400, 800, 400, 800});
                sosChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                sosChannel.setBypassDnd(true);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();
                Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri != null) {
                    sosChannel.setSound(alarmUri, audioAttributes);
                }
                nm.createNotificationChannel(sosChannel);

                NotificationChannel legacySosChannel = new NotificationChannel(
                        LEGACY_SOS_CHANNEL_ID,
                        "Emergency Alerts (Legacy)",
                        NotificationManager.IMPORTANCE_HIGH);
                legacySosChannel.enableLights(true);
                legacySosChannel.setLightColor(Color.RED);
                legacySosChannel.enableVibration(true);
                legacySosChannel.setVibrationPattern(new long[]{0, 800, 400, 800, 400, 800});
                legacySosChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                legacySosChannel.setBypassDnd(true);
                if (alarmUri != null) {
                    legacySosChannel.setSound(alarmUri, audioAttributes);
                }
                nm.createNotificationChannel(legacySosChannel);
            }
        }
    }

    private void subscribeToSosTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("hall_sos_alerts")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM_SUB", "Successfully subscribed to hall_sos_alerts topic");
                    } else {
                        Log.w("FCM_SUB", "Failed to subscribe to hall_sos_alerts topic", task.getException());
                    }
                });
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
