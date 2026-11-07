package com.example.smarthall.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.smarthall.R;
import com.example.smarthall.ui.auth.LoginActivity;
import com.example.smarthall.ui.sos.EmergencyAlertActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EmergencySosForegroundService extends Service {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";
    private static final String SERVICE_CHANNEL_ID = "sos_foreground_service_channel";
    private static final String ALERT_CHANNEL_ID = "emergency_alerts_channel";
    private static final int SERVICE_NOTIFICATION_ID = 1001;
    private static final long TEN_MINUTES_MS = 600000L;

    private DatabaseReference sosRef;
    private ValueEventListener sosListener;
    private final Set<String> handledSosIds = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void onCreate() {
        super.onCreate();
        createServiceNotificationChannel();
        createAlertNotificationChannel();
        startForegroundServiceNotification();
        attachSosListener();
    }

    private void startForegroundServiceNotification() {
        Intent launchIntent = new Intent(this, LoginActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("SmartHall Safety Active")
                .setContentText("Monitoring emergency hall alerts")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(contentPendingIntent);

        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(SERVICE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification);
        }
    }

    private void createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        SERVICE_CHANNEL_ID,
                        "SOS Monitoring Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Silent background monitoring for emergency SOS events");
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void createAlertNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        ALERT_CHANNEL_ID,
                        "Emergency Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("High-priority emergency alerts and alarm");
                channel.enableLights(true);
                channel.setLightColor(0xFF8B0000);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 800, 400, 800, 400});
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                channel.setBypassDnd(true);
                Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri != null) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build();
                    channel.setSound(alarmUri, audioAttributes);
                }
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void attachSosListener() {
        if (sosRef != null && sosListener != null) {
            return;
        }
        sosRef = FirebaseDatabase.getInstance(RTDB_URL).getReference("emergency_sos");
        sosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                for (DataSnapshot child : snapshot.getChildren()) {
                    handleSosRecord(child, now);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EmergencySosService", "Firebase SOS listener error: " + error.getMessage());
            }
        };
        sosRef.addValueEventListener(sosListener);
    }

    private void handleSosRecord(DataSnapshot child, long now) {
        if (child == null) {
            return;
        }

        String sosId = child.child("sosId").getValue(String.class);
        if (TextUtils.isEmpty(sosId)) {
            sosId = child.getKey();
        }
        if (TextUtils.isEmpty(sosId)) {
            return;
        }

        String status = child.child("status").getValue(String.class);
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            return;
        }

        Long timestamp = child.child("timestamp").getValue(Long.class);
        if (timestamp == null) {
            timestamp = now;
        }
        if (now - timestamp >= TEN_MINUTES_MS) {
            return;
        }

        String senderRoll = child.child("senderRoll").getValue(String.class);
        String currentUserRoll = getCurrentUserRoll();

        if (!TextUtils.isEmpty(senderRoll) && !TextUtils.isEmpty(currentUserRoll)
                && senderRoll.trim().equalsIgnoreCase(currentUserRoll.trim())) {
            return;
        }

        if (handledSosIds.contains(sosId)) {
            return;
        }
        handledSosIds.add(sosId);

        String location = child.child("location").getValue(String.class);
        String incident = child.child("incident").getValue(String.class);

        triggerAlert(location != null ? location : "Unknown Location",
                incident != null ? incident : "Not specified",
                senderRoll != null ? senderRoll : "Unknown");
    }

    private String getCurrentUserRoll() {
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String roll = prefs.getString("roll", "");
        if (TextUtils.isEmpty(roll)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SharedPreferences studentPrefs = getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                roll = studentPrefs.getString("student_roll", "");
            }
        }
        return roll;
    }

    private void triggerAlert(String location, String incident, String senderRoll) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    "SmartHall:SosEmergencyWakeLock"
            );
            wakeLock.acquire(15000);
        }

        Intent intent = new Intent();
        intent.setClass(this, EmergencyAlertActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("sosLocation", location);
        intent.putExtra("location", location);
        intent.putExtra("sosIncident", incident);
        intent.putExtra("incident", incident);
        intent.putExtra("senderRoll", senderRoll);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("EMERGENCY SOS")
                .setContentText("Location: " + location)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) System.currentTimeMillis(), builder.build());
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e("EmergencySosService", "Failed to start EmergencyAlertActivity directly", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundServiceNotification();
        if (sosRef == null || sosListener == null) {
            attachSosListener();
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (sosRef == null || sosListener == null) {
            attachSosListener();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (sosRef != null && sosListener != null) {
            sosRef.removeEventListener(sosListener);
            sosListener = null;
        }
        super.onDestroy();
    }
}
