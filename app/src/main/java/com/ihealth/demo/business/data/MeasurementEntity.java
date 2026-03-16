package com.ihealth.demo.business.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "measurements")
public class MeasurementEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String deviceType;
    public Integer bpm;
    public Integer spo2;
    public Double temperature;

    // Timestamp in milliseconds to easily sort and filter for the last 24h/week
    public long timestamp;

    public MeasurementEntity(String deviceType, Integer bpm, Integer spo2, Double temperature, long timestamp) {
        this.deviceType = deviceType;
        this.bpm = bpm;
        this.spo2 = spo2;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }
}