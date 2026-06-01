package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface EvacuationResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EvacuationResource resource);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EvacuationResource> resources);

    @Update
    void update(EvacuationResource resource);

    @Delete
    void delete(EvacuationResource resource);

    @Query("SELECT * FROM evacuation_resources WHERE centerId = :centerId ORDER BY resourceType ASC")
    List<EvacuationResource> getResourcesForCenter(String centerId);

    @Query("SELECT * FROM evacuation_resources WHERE centerId = :centerId AND isAvailable = 1")
    List<EvacuationResource> getAvailableResourcesForCenter(String centerId);

    @Query("DELETE FROM evacuation_resources WHERE centerId = :centerId")
    void deleteAllForCenter(String centerId);

    @Query("SELECT COUNT(*) FROM evacuation_resources WHERE centerId = :centerId AND isAvailable = 1")
    int getAvailableCountForCenter(String centerId);
}
