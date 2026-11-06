package com.example.smarthall.ui.sos;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smarthall.databinding.ActivityEmergencyAlertBinding;

public class EmergencyAlertActivity extends AppCompatActivity {

    private ActivityEmergencyAlertBinding binding;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF8B0000);
        }

        binding = ActivityEmergencyAlertBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        populateSosDetails(getIntent());
        startAlarmAudio();
        startVibration();

        binding.btnDismissSos.setOnClickListener(v -> {
            stopAlert();
            finish();
        });
    }

    private void populateSosDetails(Intent intent) {
        if (intent == null) {
            return;
        }
        String location = intent.getStringExtra("sosLocation");
        if (location == null || location.isEmpty()) {
            location = intent.getStringExtra("location");
        }
        String incident = intent.getStringExtra("sosIncident");
        if (incident == null || incident.isEmpty()) {
            incident = intent.getStringExtra("incident");
        }
        String senderRoll = intent.getStringExtra("senderRoll");

        binding.tvSosLocation.setText(location != null && !location.isEmpty() ? location : "Location not specified");
        binding.tvSosIncident.setText(incident != null && !incident.isEmpty() ? incident : "Not specified");
        binding.tvSosReporter.setText("Reported by: " + (senderRoll != null && !senderRoll.isEmpty() ? senderRoll : "Unknown"));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        populateSosDetails(intent);
    }

    private void startAlarmAudio() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }

            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e("EmergencyAlert", "Error playing alarm sound", e);
        }
    }

    private void startVibration() {
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 800, 400, 800, 400};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            Log.e("EmergencyAlert", "Error starting vibrator", e);
        }
    }

    private void stopAlert() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e("EmergencyAlert", "Error stopping MediaPlayer", e);
            }
            mediaPlayer = null;
        }

        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception e) {
                Log.e("EmergencyAlert", "Error cancelling Vibrator", e);
            }
            vibrator = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopAlert();
        super.onDestroy();
    }
}
