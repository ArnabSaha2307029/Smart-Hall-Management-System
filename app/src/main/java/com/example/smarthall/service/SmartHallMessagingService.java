package com.example.smarthall.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.example.smarthall.R;
import com.example.smarthall.ui.admin.AdminDashboardActivity;
import com.example.smarthall.ui.chat.ChatActivity;
import com.example.smarthall.ui.complaint.ComplaintsListActivity;
import com.example.smarthall.ui.student.NoticeDetailActivity;
import com.example.smarthall.ui.student.StudentDashboardActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class SmartHallMessagingService extends FirebaseMessagingService {

    public static final String NOTICE_CHANNEL_ID = "hall_notices_channel";
    public static final String CHAT_CHANNEL_ID = "hall_chat_channel";
    private static final String CHANNEL_ID = "hall_notices_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("FCM_INGRESS", "Message arrived! Data payload: " + remoteMessage.getData().toString());
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String currentUserRole = prefs.getString("user_role", "");

        Map<String, String> data = remoteMessage.getData();
        if (data == null || data.isEmpty()) {
            return;
        }

        String type = data.get("type");
        if ("complaint".equals(type)) {
            String sender = remoteMessage.getData().getOrDefault("sender", "");
            if (!currentUserRole.isEmpty() && currentUserRole.equalsIgnoreCase(sender)) {
                Log.d("FCM_FILTER", "Ignored self-sent complaint message");
                return;
            }
            handleComplaintMessage(data);
        } else if ("chat".equals(type)) {
            String sender = remoteMessage.getData().getOrDefault("sender", "");
            if (!currentUserRole.isEmpty() && currentUserRole.equalsIgnoreCase(sender)) {
                Log.d("FCM_FILTER", "Ignored self-sent chat message");
                return;
            }
            handleChatMessage(data);
        } else if (type == null || type.isEmpty() || "notice".equals(type)) {
            if ("admin".equalsIgnoreCase(currentUserRole)) {
                Log.d("FCM_FILTER", "Admin ignored own notice");
                return;
            }
            handleNoticeMessage(data);
        }
    }

    private void handleComplaintMessage(Map<String, String> data) {
        String complaintId = data.get("complaint_id");
        String title = data.get("title");
        String action = data.get("action");
        String studentRoll = data.get("student_roll");

        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String localRole = prefs.getString("user_role", "");
        boolean isAdmin = "admin".equalsIgnoreCase(localRole);

        String notifTitle;
        String notifBody;
        if ("complaint_resolved".equalsIgnoreCase(action)) {
            notifTitle = "Complaint Resolved";
            notifBody = title != null && !title.isEmpty() ? ("Resolved: " + title) : "Your complaint has been marked as resolved.";
        } else {
            notifTitle = "New Complaint Submitted";
            notifBody = (studentRoll != null && !studentRoll.isEmpty() ? ("Roll " + studentRoll + ": ") : "") + (title != null ? title : "");
        }

        createNotificationChannels();

        Intent intent = new Intent(this, ComplaintsListActivity.class);
        intent.putExtra("complaint_id", complaintId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        if (isAdmin) {
            stackBuilder.addNextIntent(new Intent(this, AdminDashboardActivity.class));
        } else {
            stackBuilder.addNextIntent(new Intent(this, StudentDashboardActivity.class));
        }
        stackBuilder.addNextIntent(intent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                (int) System.currentTimeMillis(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(notifTitle)
                .setContentText(notifBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notifBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void handleChatMessage(Map<String, String> data) {
        String studentRoll = data.get("student_roll");
        String senderRole = data.get("sender");
        String message = data.get("message");
        String title = data.get("title");

        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String localRole = prefs.getString("user_role", null);
        boolean isAdmin = "admin".equalsIgnoreCase(localRole) || "student".equalsIgnoreCase(senderRole);

        String sender = title;
        if (sender == null || sender.isEmpty()) {
            if ("student".equalsIgnoreCase(senderRole)) {
                sender = studentRoll != null && !studentRoll.isEmpty() ? ("Student " + studentRoll) : "Student";
            } else if ("admin".equalsIgnoreCase(senderRole)) {
                sender = "Admin";
            } else if (senderRole != null && !senderRole.isEmpty()) {
                sender = senderRole;
            } else {
                sender = isAdmin ? ("Student " + (studentRoll != null ? studentRoll : "")) : "Admin";
            }
        }
        if (message == null) {
            message = "";
        }

        createNotificationChannels();

        Intent intent = new Intent(this, ChatActivity.class);
        intent.setAction("OPEN_CHAT");
        intent.putExtra("student_roll", studentRoll);
        intent.putExtra("chatting_with_role", isAdmin ? "student" : "admin");
        intent.putExtra(ChatActivity.EXTRA_PRIMARY_COLOR, com.example.smarthall.util.HallThemeManager.getActivePrimaryColor(this));
        intent.putExtra(ChatActivity.EXTRA_PRIMARY_DARK_COLOR, com.example.smarthall.util.HallThemeManager.getActivePrimaryDarkColor(this));

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        if (isAdmin) {
            stackBuilder.addNextIntent(new Intent(this, AdminDashboardActivity.class));
        } else {
            stackBuilder.addNextIntent(new Intent(this, StudentDashboardActivity.class));
        }
        stackBuilder.addNextIntent(intent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                (int) System.currentTimeMillis(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHAT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(sender)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 250, 250, 250})
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            int notificationId = (int) System.currentTimeMillis();
            nm.notify(notificationId, builder.build());
        }
    }

    private void handleNoticeMessage(Map<String, String> data) {
        String noticeId = data.get("notice_id");
        String title = data.get("title");
        String message = data.get("message");
        String poster = data.get("poster");
        long timestamp = 0;
        String timestampStr = data.get("timestamp");
        if (timestampStr != null && !timestampStr.isEmpty()) {
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException ignored) {
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

        createNotificationChannels();

        Intent intent = new Intent(this, NoticeDetailActivity.class);
        intent.setAction("OPEN_NOTICE_DETAIL");
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            int notificationId = (int) System.currentTimeMillis();
            nm.notify(notificationId, builder.build());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "FCM Device Token refreshed: " + token);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel noticeChannel = new NotificationChannel(
                        NOTICE_CHANNEL_ID,
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
}
