package com.example.smarthall.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.example.smarthall.R;
import com.example.smarthall.ui.student.NoticeDetailActivity;
import com.example.smarthall.ui.student.StudentDashboardActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class SmartHallMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "hall_notices_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String noticeId = null;
        String title = null;
        String message = null;
        String poster = null;
        long timestamp = 0;

        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            noticeId = data.get("notice_id");
            title = data.get("title");
            message = data.get("message");
            poster = data.get("poster");
            String timestampStr = data.get("timestamp");
            if (timestampStr != null && !timestampStr.isEmpty()) {
                try {
                    timestamp = Long.parseLong(timestampStr);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (title == null) {
            title = "Hall Announcement";
        }
        if (message == null) {
            message = "";
        }
        if (poster == null) {
            poster = "Admin";
        }

        createNotificationChannel();

        Intent intent = new Intent(this, NoticeDetailActivity.class);
        intent.putExtra("notice_id", noticeId);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("poster", poster);
        intent.putExtra("timestamp", timestamp);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, StudentDashboardActivity.class));
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                (int) System.currentTimeMillis(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            nm.notify(notificationId, builder.build());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "FCM Device Token refreshed: " + token);
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
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
