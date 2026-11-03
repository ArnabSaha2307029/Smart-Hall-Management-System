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

    public static void sendNoticeTopicNotification(Context context, String noticeId, String title, String message, String poster, long timestamp) {
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
                Log.e("FCM_AUTH", "Failed to obtain OAuth2 token", e);
                if (context != null) {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "OAuth2 Token Failed: " + errorMsg, Toast.LENGTH_LONG).show()
                    );
                }
                return;
            }

            try {
                String topic = "hall_notices";
                String cleanTopic = topic.trim().replaceAll("\\s+", "");
                Log.d("FCM_HTTP", "Sending FCM to topic: " + cleanTopic);

                JSONObject data = new JSONObject();
                data.put("type", "notice");
                data.put("notice_id", noticeId != null ? noticeId.trim() : "");
                data.put("title", title != null ? title.trim() : "");
                data.put("message", message != null ? message : "");
                data.put("poster", poster != null ? poster.trim() : "Admin");
                data.put("timestamp", String.valueOf(timestamp));

                JSONObject androidObj = new JSONObject();
                androidObj.put("priority", "HIGH");

                JSONObject messageObj = new JSONObject();
                messageObj.put("topic", cleanTopic);
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
                    int code = response.code();
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.d("FCM_HTTP", "FCM HTTP response code: " + code + " | body: " + errorBody);
                    if (!response.isSuccessful()) {
                        Log.e("FCM_HTTP", "FCM delivery failed with code: " + code + " | body: " + errorBody);
                        if (context != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(context, "FCM Error " + code + ": " + errorBody, Toast.LENGTH_LONG).show()
                            );
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("FCM_HTTP", "Failed to send FCM notice notification", e);
                if (context != null) {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Network Error: " + errorMsg, Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    public static void sendChatNotification(Context context, String topic, String title, String message, String senderRole, String studentRoll) {
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
                Log.e("FCM_AUTH", "Failed to obtain OAuth2 token", e);
                if (context != null) {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "OAuth2 Token Failed: " + errorMsg, Toast.LENGTH_LONG).show()
                    );
                }
                return;
            }

            try {
                String cleanTopic = (topic != null ? topic : "").trim().replaceAll("\\s+", "");
                Log.d("FCM_HTTP", "Sending FCM to topic: " + cleanTopic);

                JSONObject data = new JSONObject();
                data.put("type", "chat");
                data.put("sender", senderRole != null ? senderRole.trim() : "");
                data.put("message", message != null ? message : "");
                data.put("title", title != null ? title.trim() : "");
                data.put("student_roll", studentRoll != null ? studentRoll.trim() : "");
                data.put("timestamp", String.valueOf(System.currentTimeMillis()));

                JSONObject androidObj = new JSONObject();
                androidObj.put("priority", "HIGH");

                JSONObject messageObj = new JSONObject();
                messageObj.put("topic", cleanTopic);
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
                    int code = response.code();
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.d("FCM_HTTP", "FCM HTTP response code: " + code + " | body: " + errorBody);
                    if (!response.isSuccessful()) {
                        Log.e("FCM_HTTP", "FCM delivery failed with code: " + code + " | body: " + errorBody);
                        if (context != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(context, "FCM Error " + code + ": " + errorBody, Toast.LENGTH_LONG).show()
                            );
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("FCM_HTTP", "Failed to send FCM chat notification", e);
                if (context != null) {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Network Error: " + errorMsg, Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }
}