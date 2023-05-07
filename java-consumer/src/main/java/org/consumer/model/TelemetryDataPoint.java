package org.consumer.model;

public class TelemetryDataPoint {
    public String deviceId;
    public float temperature;
    public long ts;
    public String door;

    public TelemetryDataPoint() {}
    public TelemetryDataPoint(String deviceId, float temperature, long ts, String door) {
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.ts = ts;
        this.door = door;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public void setDoor(String door) {
        this.door = door;
    }

}