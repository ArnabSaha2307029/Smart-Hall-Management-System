package com.example.smarthall.network;

import android.content.Context;
import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public static void sendNoticeTopicNotification(Context context, String noticeId, String title, String message, String poster, long timestamp) {
        executor.execute(() -> {
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
                String projectId = jsonObject.getString("project_id");

                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                GoogleCredentials credentials = GoogleCredentials.fromStream(bais)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
                credentials.refreshIfExpired();
                String accessToken = credentials.getAccessToken().getTokenValue();

                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", message);

                JSONObject data = new JSONObject();
                data.put("notice_id", noticeId != null ? noticeId : "");
                data.put("title", title);
                data.put("message", message);
                data.put("poster", poster != null ? poster : "Admin");
                data.put("timestamp", String.valueOf(timestamp));

                JSONObject androidNotification = new JSONObject();
                androidNotification.put("channel_id", "hall_notices_channel");
                androidNotification.put("notification_priority", "PRIORITY_MAX");
                androidNotification.put("default_sound", true);
                androidNotification.put("click_action", "OPEN_NOTICE_DETAIL");

                JSONObject androidObj = new JSONObject();
                androidObj.put("priority", "HIGH");
                androidObj.put("notification", androidNotification);

                JSONObject messageObj = new JSONObject();
                messageObj.put("topic", "hall_notices");
                messageObj.put("notification", notification);
                messageObj.put("data", data);
                messageObj.put("android", androidObj);

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
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("FCM_SENDER", "FCM HTTP v1 Response Code: " + response.code() + ", Body: " + responseBody);
                }
            } catch (Exception e) {
                Log.e("FCM_SENDER", "Failed to send FCM v1 notification", e);
            }
        });
    }
}
