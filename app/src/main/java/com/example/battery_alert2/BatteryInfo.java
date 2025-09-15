package com.example.battery_alert2;

public class BatteryInfo {
    private int level;
    private int voltage;
    private int temperature;
    private String health;
    private boolean isCharging;
    private String chargingType;

    public BatteryInfo(int level, int voltage, int temperature, String health,
                       boolean isCharging, String chargingType) {
        this.level = level;
        this.voltage = voltage;
        this.temperature = temperature;
        this.health = health;
        this.isCharging = isCharging;
        this.chargingType = chargingType;
    }

    // Getters
    public int getLevel() { return level; }
    public int getVoltage() { return voltage; }
    public int getTemperature() { return temperature; }
    public String getHealth() { return health; }
    public boolean isCharging() { return isCharging; }
    public String getChargingType() { return chargingType; }
}