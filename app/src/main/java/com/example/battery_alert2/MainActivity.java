package com.example.battery_alert2;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private BatteryReceiver batteryReceiver;
    private TextView batteryInfoText;
    private TextView chargingStatusText;
    private SeekBar highLevelSeekBar;
    private SeekBar lowLevelSeekBar;
    private TextView highLevelText;
    private TextView lowLevelText;
    private Switch enableAlertsSwitch;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        requestPermissions();
        setupBatteryReceiver();
        setupSeekBars();
        startBatteryService();
    }

    private void initViews() {
        batteryInfoText = findViewById(R.id.batteryInfoText);
        chargingStatusText = findViewById(R.id.chargingStatusText);
        highLevelSeekBar = findViewById(R.id.highLevelSeekBar);
        lowLevelSeekBar = findViewById(R.id.lowLevelSeekBar);
        highLevelText = findViewById(R.id.highLevelText);
        lowLevelText = findViewById(R.id.lowLevelText);
        enableAlertsSwitch = findViewById(R.id.enableAlertsSwitch);

        // Load saved preferences
        SharedPreferences prefs = getSharedPreferences("BatteryAlert", MODE_PRIVATE);
        int highLevel = prefs.getInt("highLevel", 90);
        int lowLevel = prefs.getInt("lowLevel", 20);
        boolean alertsEnabled = prefs.getBoolean("alertsEnabled", true);

        highLevelSeekBar.setProgress(highLevel);
        lowLevelSeekBar.setProgress(lowLevel);
        enableAlertsSwitch.setChecked(alertsEnabled);

        updateSeekBarTexts();
    }

    private void setupSeekBars() {
        highLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress <= lowLevelSeekBar.getProgress()) {
                    seekBar.setProgress(lowLevelSeekBar.getProgress() + 1);
                }
                updateSeekBarTexts();
                savePreferences();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        lowLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= highLevelSeekBar.getProgress()) {
                    seekBar.setProgress(highLevelSeekBar.getProgress() - 1);
                }
                updateSeekBarTexts();
                savePreferences();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        enableAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> savePreferences());
    }

    private void updateSeekBarTexts() {
        highLevelText.setText("High Alert: " + highLevelSeekBar.getProgress() + "%");
        lowLevelText.setText("Low Alert: " + lowLevelSeekBar.getProgress() + "%");
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences("BatteryAlert", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("highLevel", highLevelSeekBar.getProgress());
        editor.putInt("lowLevel", lowLevelSeekBar.getProgress());
        editor.putBoolean("alertsEnabled", enableAlertsSwitch.isChecked());
        editor.apply();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void setupBatteryReceiver() {
        batteryReceiver = new BatteryReceiver(batteryInfo -> {
            runOnUiThread(() -> {
                String infoText = "Battery Level: " + batteryInfo.getLevel() + "%\n" +
                        "Voltage: " + batteryInfo.getVoltage() + "mV\n" +
                        "Temperature: " + (batteryInfo.getTemperature() / 10.0) + "°C\n" +
                        "Health: " + batteryInfo.getHealth();
                batteryInfoText.setText(infoText);

                String chargingText = batteryInfo.isCharging() ?
                        "🔋 Charging (" + batteryInfo.getChargingType() + ")" :
                        "🔌 Not Charging";
                chargingStatusText.setText(chargingText);
            });
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(batteryReceiver, filter);
    }

    private void startBatteryService() {
        Intent serviceIntent = new Intent(this, BatteryMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }
    }
}