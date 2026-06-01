package com.example.instacare.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface DisasterGuideDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DisasterGuide guide);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DisasterGuide> guides);

    @Update
    void update(DisasterGuide guide);

    @Delete
    void delete(DisasterGuide guide);

    @Query("DELETE FROM disaster_guides WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM disaster_guides")
    LiveData<List<DisasterGuide>> getAllDisasterGuides();

    @Query("SELECT * FROM disaster_guides")
    List<DisasterGuide> getAllDisasterGuidesDirect();

    @Query("SELECT * FROM disaster_guides WHERE id = :id LIMIT 1")
    DisasterGuide getGuideByIdDirect(String id);

    @Query("SELECT * FROM disaster_guides WHERE disasterType = :type")
    LiveData<List<DisasterGuide>> getGuidesByType(String type);

    @Query("UPDATE disaster_guides SET isBookmarked = :status WHERE id = :id")
    void updateBookmarkStatus(String id, boolean status);
}
