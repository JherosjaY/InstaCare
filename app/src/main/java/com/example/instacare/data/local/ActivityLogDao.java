package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ActivityLogDao {
    @Insert
    void insert(ActivityLog log);

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    List<ActivityLog> getAllLogsSync();

    @Query("SELECT * FROM activity_logs WHERE type = :type ORDER BY timestamp DESC")
    List<ActivityLog> getLogsByTypeSync(String type);

    @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestamp DESC")
    List<ActivityLog> getLogsByUserSync(int userId);

    @Query("SELECT * FROM activity_logs WHERE barangayZone = :barangay ORDER BY timestamp DESC")
    List<ActivityLog> getLogsByBarangaySync(String barangay);

    @Query("SELECT * FROM activity_logs WHERE barangayZone = :zone AND type = :type ORDER BY timestamp DESC")
    List<ActivityLog> getLogsByZoneAndTypeSync(String zone, String type);

    @Query("DELETE FROM activity_logs WHERE userId = :userId")
    void deleteByUserId(int userId);

    @Query("DELETE FROM activity_logs")
    void deleteAll();
}
