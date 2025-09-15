package com.example.battery_alert2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class BatteryReceiver extends BroadcastReceiver {

    private BatteryUpdateListener listener;

    public BatteryReceiver(BatteryUpdateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

            int batteryPct = Math.round(level * 100 / (float) scale);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            String chargingType;
            switch (plugged) {
                case BatteryManager.BATTERY_PLUGGED_USB:
                    chargingType = "USB";
                    break;
                case BatteryManager.BATTERY_PLUGGED_AC:
                    chargingType = "AC";
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    chargingType = "Wireless";
                    break;
                default:
                    chargingType = "Not Charging";
                    break;
            }

            String healthString;
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    healthString = "Good";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    healthString = "Overheat";
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD:
                    healthString = "Dead";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                    healthString = "Over Voltage";
                    break;
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    healthString = "Failure";
                    break;
                case BatteryManager.BATTERY_HEALTH_COLD:
                    healthString = "Cold";
                    break;
                default:
                    healthString = "Unknown";
                    break;
            }

            BatteryInfo batteryInfo = new BatteryInfo(
                    batteryPct, voltage, temperature, healthString, isCharging, chargingType
            );

            if (listener != null) {
                listener.onBatteryUpdate(batteryInfo);
            }
        }
    }
}