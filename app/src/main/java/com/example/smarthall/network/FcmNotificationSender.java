package com.example.smarthall.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.auth.oauth2.GoogleCredentials;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FcmNotificationSender {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static void sendFcmRawMessage(Context context, JSONObject messageObj, String logTag) {
        executor.execute(() -> {
            String accessToken;
            String projectId;
            try {
                InputStream inputStream = context.getAssets().open("service-account.json");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                inputStream.close();
                byte[] bytes = baos.toByteArray();

                String jsonString = new String(bytes, StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(jsonString);
                projectId = jsonObject.getString("project_id");

                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                GoogleCredentials credentials = GoogleCredentials.fromStream(bais)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
                credentials.refreshIfExpired();
                accessToken = credentials.getAccessToken().getTokenValue();
            } catch (Exception e) {
                Log.e("StudentDashboardError", "Operation failed: Failed to obtain OAuth2 token for FCM: " + e.getMessage(), e);
                return;
            }

            try {
                JSONObject payload = new JSONObject();
                payload.put("message", messageObj);

                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.get("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(payload.toString(), mediaType);

                Request request = new Request.Builder()
                        .url("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json; UTF-8")
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    int code = response.code();
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.d("FCM_HTTP", logTag + " FCM HTTP response code: " + code + " | body: " + errorBody);
                    if (!response.isSuccessful()) {
                        Log.e("StudentDashboardError", "Operation failed: FCM delivery failed with code: " + code + " | body: " + errorBody);
                    }
                }
            } catch (Exception e) {
                Log.e("StudentDashboardError", "Operation failed: Failed to send FCM notification: " + e.getMessage(), e);
            }
        });
    }

    private static void sendFcmMessage(Context context, String topic, JSONObject data, String logTag) {
        try {
            String cleanTopic = (topic != null ? topic : "").trim().replaceAll("\\s+", "");
            Log.d("FCM_HTTP", "Sending FCM to topic: " + cleanTopic);

            JSONObject androidObj = new JSONObject();
            androidObj.put("priority", "HIGH");

            JSONObject messageObj = new JSONObject();
            messageObj.put("topic", cleanTopic);
            messageObj.put("data", data);
            messageObj.put("android", androidObj);

            sendFcmRawMessage(context, messageObj, logTag);
        } catch (Exception e) {
            Log.e("StudentDashboardError", "Operation failed: Failed to assemble FCM message: " + e.getMessage(), e);
        }
    }

    public static void sendSosAlertNotification(Context context, String alertId, String senderName, String senderRoll, String roomNo, String message, long timestamp) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "sos_alert");
            data.put("alertId", alertId != null ? alertId.trim() : "");
            data.put("senderName", senderName != null ? senderName.trim() : "");
            data.put("senderRoll", senderRoll != null ? senderRoll.trim() : "");
            data.put("roomNo", roomNo != null ? roomNo.trim() : "");
            data.put("message", message != null ? message.trim() : "");
            data.put("timestamp", String.valueOf(timestamp));

            JSONObject androidNotification = new JSONObject();
            androidNotification.put("title", "EMERGENCY SOS: Room " + (roomNo != null ? roomNo.trim() : ""));
            androidNotification.put("body", (senderName != null ? senderName.trim() : "Student")
                    + " (" + (senderRoll != null ? senderRoll.trim() : "") + ") needs immediate assistance!");
            androidNotification.put("channel_id", "sos_emergency_alert_channel");
            androidNotification.put("notification_priority", "PRIORITY_MAX");
            androidNotification.put("default_vibrate_timings", true);
            androidNotification.put("default_sound", true);

            JSONObject androidObj = new JSONObject();
            androidObj.put("priority", "HIGH");
            androidObj.put("notification", androidNotification);

            JSONObject messageObj = new JSONObject();
            messageObj.put("topic", "hall_sos_alerts");
            messageObj.put("data", data);
            messageObj.put("android", androidObj);

            sendFcmRawMessage(context, messageObj, "FCM_SOS");
        } catch (Exception e) {
            Log.e("FCM_SOS", "Failed to construct SOS alert payload", e);
        }
    }

    public static void sendNoticeTopicNotification(Context context, String noticeId, String title, String message, String poster, long timestamp) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "notice");
            data.put("notice_id", noticeId != null ? noticeId.trim() : "");
            data.put("title", title != null ? title.trim() : "");
            data.put("message", message != null ? message : "");
            data.put("poster", poster != null ? poster.trim() : "Admin");
            data.put("timestamp", String.valueOf(timestamp));
            sendFcmMessage(context, "hall_notices", data, "FCM_NOTICE");
        } catch (Exception e) {
            Log.e("FCM_NOTICE", "Failed to construct notice payload", e);
        }
    }

    public static void sendChatNotification(Context context, String topic, String title, String message, String senderRole, String studentRoll) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "chat");
            data.put("sender", senderRole != null ? senderRole.trim() : "");
            data.put("message", message != null ? message : "");
            data.put("title", title != null ? title.trim() : "");
            data.put("student_roll", studentRoll != null ? studentRoll.trim() : "");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));
            sendFcmMessage(context, topic, data, "FCM_CHAT");
        } catch (Exception e) {
            Log.e("FCM_CHAT", "Failed to construct chat payload", e);
        }
    }

    public static void sendComplaintNotificationToAdmin(Context context, String complaintId, String title, String description, String studentRoll, String studentPhone) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "complaint");
            data.put("action", "new_complaint");
            data.put("complaint_id", complaintId != null ? complaintId.trim() : "");
            data.put("title", title != null ? title.trim() : "");
            data.put("description", description != null ? description.trim() : "");
            data.put("student_roll", studentRoll != null ? studentRoll.trim() : "");
            data.put("student_phone", studentPhone != null ? studentPhone.trim() : "");
            data.put("sender", "student");
            data.put("status", "pending");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));
            sendFcmMessage(context, "admin_inbox", data, "FCM_COMPLAINT");
        } catch (Exception e) {
            Log.e("FCM_COMPLAINT", "Failed to construct complaint payload", e);
        }
    }

    public static void sendComplaintResolvedNotificationToStudent(Context context, String studentRoll, String complaintId, String title) {
        try {
            String cleanRoll = studentRoll != null ? studentRoll.trim().replaceAll("\\s+", "") : "";
            JSONObject data = new JSONObject();
            data.put("type", "complaint");
            data.put("action", "complaint_resolved");
            data.put("complaint_id", complaintId != null ? complaintId.trim() : "");
            data.put("title", title != null ? title.trim() : "");
            data.put("student_roll", cleanRoll);
            data.put("sender", "admin");
            data.put("status", "resolved");
            data.put("message", "Your complaint has been marked as resolved: " + (title != null ? title.trim() : ""));
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));
            sendFcmMessage(context, "student_chat_" + cleanRoll, data, "FCM_COMPLAINT_RESOLVED");
        } catch (Exception e) {
            Log.e("FCM_COMPLAINT_RESOLVED", "Failed to construct complaint resolved payload", e);
        }
    }
}
