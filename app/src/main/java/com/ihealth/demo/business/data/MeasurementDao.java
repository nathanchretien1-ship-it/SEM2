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
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    List<MeasurementEntity> getAllMeasurements();

    // Fetch measurements within a specific time range
    @Query("SELECT * FROM measurements WHERE timestamp >= :timestamp ORDER BY timestamp DESC")
    List<MeasurementEntity> getMeasurementsSince(long timestamp);

    // Delete older measurements (e.g., to clear history older than 7 days)
    @Query("DELETE FROM measurements WHERE timestamp < :timestamp")
    void deleteOlderThan(long timestamp);
}