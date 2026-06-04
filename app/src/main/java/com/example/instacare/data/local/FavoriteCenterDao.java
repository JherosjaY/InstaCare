package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FavoriteCenterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteCenter favorite);

    @Query("DELETE FROM favorite_centers WHERE centerName = :name")
    void deleteByName(String name);

    @Query("SELECT * FROM favorite_centers ORDER BY timestamp DESC")
    List<FavoriteCenter> getAll();

    @Query("SELECT COUNT(*) > 0 FROM favorite_centers WHERE centerName = :name")
    boolean isFavorite(String name);
}
