package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface EvacueeCheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(EvacueeCheckIn checkIn);

    @Update
    void update(EvacueeCheckIn checkIn);

    /** Get the current active check-in for a user (null if not checked in) */
    @Query("SELECT * FROM evacuee_checkins WHERE userId = :userId AND isActive = 1 LIMIT 1")
    EvacueeCheckIn getActiveCheckInForUser(int userId);

    /** Count active evacuees at a specific center */
    @Query("SELECT COUNT(*) FROM evacuee_checkins WHERE centerId = :centerId AND isActive = 1")
    int getActiveCountForCenter(String centerId);

    /** All active check-ins across all centers (for admin overview) */
    @Query("SELECT * FROM evacuee_checkins WHERE isActive = 1 ORDER BY checkInTime DESC")
    List<EvacueeCheckIn> getAllActiveCheckIns();

    /** Check-in history for a specific user */
    @Query("SELECT * FROM evacuee_checkins WHERE userId = :userId ORDER BY checkInTime DESC")
    List<EvacueeCheckIn> getHistoryForUser(int userId);

    /** Mark all check-ins older than 24h as inactive (auto-expiry) */
    @Query("UPDATE evacuee_checkins SET isActive = 0, checkOutTime = :now WHERE isActive = 1 AND checkInTime < :cutoff")
    void expireOldCheckIns(long now, long cutoff);

    /** Check out — mark a specific user's active check-in as done */
    @Query("UPDATE evacuee_checkins SET isActive = 0, checkOutTime = :now WHERE userId = :userId AND isActive = 1")
    void checkOut(int userId, long now);
}
