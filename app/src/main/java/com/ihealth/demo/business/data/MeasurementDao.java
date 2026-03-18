package com.ihealth.demo.business.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MeasurementDao {

    @Insert
    long insert(MeasurementEntity measurement);

    // Fetch all measurements ordered by timestamp descending (newest first)
    @Query("SELECT * FROM measurements WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    List<MeasurementEntity> getAllMeasurements(String userEmail);

    // Fetch measurements within a specific time range
    @Query("SELECT * FROM measurements WHERE userEmail = :userEmail AND timestamp >= :timestamp ORDER BY timestamp DESC")
    List<MeasurementEntity> getMeasurementsSince(String userEmail, long timestamp);

    // Fetch unsent measurements
    @Query("SELECT * FROM measurements WHERE userEmail = :userEmail AND isSentToServer = 0")
    List<MeasurementEntity> getUnsentMeasurements(String userEmail);

    // Update a measurement's sync status
    @Query("UPDATE measurements SET isSentToServer = 1 WHERE id = :id")
    void markAsSent(int id);

    // Delete older measurements (e.g., to clear history older than 7 days)
    @Query("DELETE FROM measurements WHERE timestamp < :timestamp")
    void deleteOlderThan(long timestamp);

    @Query("DELETE FROM measurements WHERE userEmail = :userEmail AND isSentToServer = 1")
    void deleteSentMeasurements(String userEmail);

    @Query("DELETE FROM measurements WHERE userEmail = :userEmail")
    void deleteAllMeasurements(String userEmail);
}