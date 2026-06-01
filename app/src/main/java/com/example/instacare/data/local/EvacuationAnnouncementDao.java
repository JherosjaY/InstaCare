package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface EvacuationAnnouncementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(EvacuationAnnouncement announcement);

    @Update
    void update(EvacuationAnnouncement announcement);

    @Delete
    void delete(EvacuationAnnouncement announcement);

    /** All active announcements, newest first */
    @Query("SELECT * FROM evacuation_announcements WHERE isActive = 1 ORDER BY timestamp DESC")
    List<EvacuationAnnouncement> getAllActive();

    /** Active announcements for a specific center + broadcasts (centerId IS NULL) */
    @Query("SELECT * FROM evacuation_announcements WHERE isActive = 1 AND (centerId = :centerId OR centerId IS NULL) ORDER BY timestamp DESC")
    List<EvacuationAnnouncement> getForCenter(String centerId);

    /** Count of unread/active urgent announcements */
    @Query("SELECT COUNT(*) FROM evacuation_announcements WHERE isActive = 1 AND priority = 'URGENT'")
    int getUrgentCount();

    @Query("UPDATE evacuation_announcements SET isActive = 0 WHERE id = :id")
    void archive(int id);
}
