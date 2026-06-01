package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EmergencyAlertDao {
    @Insert
    long insert(EmergencyAlert alert);

    @Query("SELECT * FROM emergency_alerts ORDER BY timestamp DESC")
    List<EmergencyAlert> getAllAlerts();

    @Query("UPDATE emergency_alerts SET status = :status WHERE id = :id")
    void updateStatus(int id, String status);

    @Query("SELECT COUNT(*) FROM emergency_alerts")
    int getAlertCount();

    @Query("DELETE FROM emergency_alerts WHERE userId = :userId")
    void deleteByUserId(int userId);

    @Query("SELECT id FROM emergency_alerts WHERE userId = :userId")
    List<Integer> getAlertIdsByUser(int userId);

    @Query("SELECT * FROM emergency_alerts WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    EmergencyAlert getLatestAlertForUser(int userId);

    @Query("SELECT * FROM emergency_alerts WHERE barangayZone = :barangay ORDER BY timestamp DESC")
    List<EmergencyAlert> getAlertsByBarangay(String barangay);
}
