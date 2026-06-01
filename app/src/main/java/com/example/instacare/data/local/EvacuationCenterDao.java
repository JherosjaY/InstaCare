package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EvacuationCenterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EvacuationCenter center);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EvacuationCenter> centers);

    @Delete
    void delete(EvacuationCenter center);

    @Query("SELECT * FROM evacuation_centers")
    List<EvacuationCenter> getAllCenters();

    @Query("SELECT * FROM evacuation_centers WHERE isActive = 1")
    List<EvacuationCenter> getActiveCenters();

    @Query("SELECT * FROM evacuation_centers WHERE id = :id LIMIT 1")
    EvacuationCenter getCenterById(String id);

    @Query("SELECT * FROM evacuation_centers WHERE source = 'local'")
    List<EvacuationCenter> getLocalCenters();

    @Query("DELETE FROM evacuation_centers WHERE source = 'api'")
    void deleteApiCenters();

    @Query("SELECT * FROM evacuation_centers WHERE barangay = :barangay")
    List<EvacuationCenter> getCentersByBarangaySync(String barangay);
}
