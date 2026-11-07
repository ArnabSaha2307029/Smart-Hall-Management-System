package com.example.smarthall.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.smarthall.R;
import com.example.smarthall.SmartHallApp;
import com.example.smarthall.ui.sos.EmergencyAlertActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EmergencySosManager {

    private static final String RTDB_URL = "https://smart-hall-management-sy-748d2-default-rtdb.firebaseio.com";
    public static final String SOS_CHANNEL_ID = "sos_emergency_alert_channel";
    private static final long TEN_MINUTES_MS = 10 * 60 * 1000L;

    private static final Set<String> handledSosIds = Collections.synchronizedSet(new HashSet<>());

    public static DatabaseReference getSosReference() {
        return FirebaseDatabase.getInstance(RTDB_URL).getReference("emergency_sos");
    }

    public static void createEmergencyNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context != null) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        SOS_CHANNEL_ID,
                        "Emergency Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Full-screen emergency SOS alerts");
                channel.enableLights(true);
                channel.setLightColor(0xFFB71C1C);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 800, 400, 800, 400, 800});
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

    public static ValueEventListener registerSosListener(Context context, String currentUserRoll) {
        createEmergencyNotificationChannel(context);
        DatabaseReference sosRef = getSosReference();

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                for (DataSnapshot child : snapshot.getChildren()) {
                    processSosSnapshot(context, child, currentUserRoll, now);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EmergencySosManager", "SOS listener cancelled: " + error.getMessage());
            }
        };

        sosRef.addValueEventListener(listener);
        return listener;
    }

    public static void processSosSnapshot(Context context, DataSnapshot child, String currentUserRoll, long now) {
        if (child == null || context == null) {
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

        String senderRoll = child.child("senderRoll").getValue(String.class);
        if (!TextUtils.isEmpty(currentUserRoll) && !TextUtils.isEmpty(senderRoll)
                && senderRoll.trim().equalsIgnoreCase(currentUserRoll.trim())) {
            return;
        }

        Long timestamp = child.child("timestamp").getValue(Long.class);
        if (timestamp == null) {
            timestamp = now;
        }
        if (now - timestamp > TEN_MINUTES_MS) {
            return;
        }

        if (handledSosIds.contains(sosId)) {
            return;
        }
        handledSosIds.add(sosId);

        String location = child.child("location").getValue(String.class);
        String incident = child.child("incident").getValue(String.class);
        String senderName = child.child("senderName").getValue(String.class);

        triggerAlert(context, location, incident, senderName, senderRoll, timestamp);
    }

    public static void triggerAlert(Context context, String location, String incident,
                                    String senderName, String senderRoll, long timestamp) {
        if (context == null) {
            return;
        }

        createEmergencyNotificationChannel(context);

        Intent alertIntent = new Intent(context, EmergencyAlertActivity.class);
        alertIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alertIntent.putExtra("location", location);
        alertIntent.putExtra("incident", incident);
        alertIntent.putExtra("senderName", senderName);
        alertIntent.putExtra("senderRoll", senderRoll);
        alertIntent.putExtra("timestamp", timestamp);

        if (SmartHallApp.isAppInForeground()) {
            context.startActivity(alertIntent);
        } else {
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    (int) (System.currentTimeMillis() & 0xfffffff),
                    alertIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SOS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle("EMERGENCY ALERT: " + (location != null ? location : "Immediate Attention"))
                    .setContentText("Reported by " + (senderRoll != null ? senderRoll : "Student") + ": " + (incident != null ? incident : "Emergency"))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setContentIntent(fullScreenPendingIntent)
                    .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify((int) (System.currentTimeMillis() & 0xfffffff), builder.build());
            }
        }
    }
}
