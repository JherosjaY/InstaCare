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

    @Query("SELECT * FROM activity_logs WHERE userEmail = :email ORDER BY timestamp DESC")
    List<ActivityLog> getLogsByUserSync(String email);

    @Query("DELETE FROM activity_logs")
    void deleteAll();
}
