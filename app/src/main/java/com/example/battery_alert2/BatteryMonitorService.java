package com.example.battery_alert2;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class BatteryMonitorService extends Service {

    private BatteryReceiver batteryReceiver;
    private long lastAlertTime = 0L;
    private static final long ALERT_COOLDOWN = 30000;
    private static final String CHANNEL_ID = "BatteryAlertChannel";
    private static final int NOTIFICATION_ID = 1;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createForegroundNotification());
        setupBatteryMonitoring();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery Alert Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery Alert Active")
                .setContentText("Monitoring battery levels")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build();
    }

    private void setupBatteryMonitoring() {
        batteryReceiver = new BatteryReceiver(this::checkBatteryAlerts);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(batteryReceiver, filter);
    }

    private void checkBatteryAlerts(BatteryInfo batteryInfo) {
        SharedPreferences prefs = getSharedPreferences("BatteryAlert", Context.MODE_PRIVATE);
        int highLevel = prefs.getInt("highLevel", 90);
        int lowLevel = prefs.getInt("lowLevel", 20);
        boolean alertsEnabled = prefs.getBoolean("alertsEnabled", true);

        if (!alertsEnabled) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAlertTime < ALERT_COOLDOWN) return;

        boolean shouldAlert = false;
        String alertMessage = "";
        String alertType = "";

        // High battery alert (when charging and reaches high level)
        if (batteryInfo.isCharging() && batteryInfo.getLevel() >= highLevel) {
            shouldAlert = true;
            alertType = "HIGH";
            alertMessage = "🔌 Battery charged to " + batteryInfo.getLevel() + "%! " +
                    "Consider unplugging to preserve battery health.";
        }
        // Low battery alert (when NOT charging and drops to low level)
        else if (!batteryInfo.isCharging() && batteryInfo.getLevel() <= lowLevel) {
            shouldAlert = true;
            alertType = "LOW";
            alertMessage = "🪫 Battery critically low at " + batteryInfo.getLevel() + "%! " +
                    "Please charge your device immediately.";
        }

        if (shouldAlert) {
            // Log the alert for debugging
            android.util.Log.d("BatteryAlert",
                    "Alert triggered: " + alertType + " - Level: " + batteryInfo.getLevel() +
                            "% - Charging: " + batteryInfo.isCharging());

            showBatteryAlert(alertMessage, batteryInfo.getLevel());
            lastAlertTime = currentTime;

            // Update foreground notification to show last alert
            updateForegroundNotification(alertType, batteryInfo.getLevel());
        }
    }

    private void updateForegroundNotification(String alertType, int batteryLevel) {
        String title = "Battery Alert Active";
        String text = "Last alert: " + alertType + " at " + batteryLevel + "%";

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void showBatteryAlert(String message, int batteryLevel) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery Alert!")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();

        if (notificationManager != null) {
            notificationManager.notify(batteryLevel, notification);
        }


        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release(); // free resources after playing
                });
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }
    }
}