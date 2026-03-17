package com.ihealth.demo.business.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MeasurementDao {

    @Insert
    void insert(MeasurementEntity measurement);

    // Fetch all measurements ordered by timestamp descending (newest first)
    @Query("SELECT * FROM measurements WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    List<MeasurementEntity> getAllMeasurements(String userEmail);

    // Fetch measurements within a specific time range
    @Query("SELECT * FROM measurements WHERE userEmail = :userEmail AND timestamp >= :timestamp ORDER BY timestamp DESC")
    List<MeasurementEntity> getMeasurementsSince(String userEmail, long timestamp);

    // Delete older measurements (e.g., to clear history older than 7 days)
    @Query("DELETE FROM measurements WHERE timestamp < :timestamp")
    void deleteOlderThan(long timestamp);
}