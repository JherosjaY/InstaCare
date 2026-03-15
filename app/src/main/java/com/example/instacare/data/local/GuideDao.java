package com.example.instacare.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GuideDao {
    @Query("SELECT * FROM guides")
    LiveData<List<Guide>> getAllGuides();

    @Query("SELECT * FROM guides")
    List<Guide> getAllGuidesDirect();

    @Query("SELECT * FROM guides WHERE category = :category")
    LiveData<List<Guide>> getGuidesByCategory(String category);

    @Query("SELECT * FROM guides WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    LiveData<List<Guide>> searchGuides(String query);

    @Query("UPDATE guides SET isBookmarked = :status WHERE id = :id")
    void updateBookmarkStatus(String id, boolean status);

    @Query("SELECT * FROM guides WHERE isBookmarked = 1")
    LiveData<List<Guide>> getBookmarkedGuides();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Guide> guides);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Guide guide);

    @Query("DELETE FROM guides WHERE id = :id")
    void deleteById(String id);
}
