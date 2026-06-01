package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface BarangayZoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BarangayZone zone);

    @Insert
    void insertAll(List<BarangayZone> zones);

    @Delete
    void delete(BarangayZone zone);

    @Query("SELECT * FROM barangay_zones")
    List<BarangayZone> getAllZones();

    @Query("SELECT * FROM barangay_zones WHERE name = :name LIMIT 1")
    BarangayZone getZoneByName(String name);
}
