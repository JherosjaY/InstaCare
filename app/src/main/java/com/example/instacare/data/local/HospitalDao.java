package com.example.instacare.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HospitalDao {
    @Query("SELECT * FROM hospitals")
    LiveData<List<Hospital>> getAllHospitals();

    @Query("SELECT * FROM hospitals")
    List<Hospital> getAllHospitalsDirect();

    @Query("SELECT * FROM hospitals WHERE type = :type")
    LiveData<List<Hospital>> getHospitalsByType(String type);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Hospital> hospitals);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Hospital hospital);

    @Delete
    void delete(Hospital hospital);

    @Query("DELETE FROM hospitals WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT COUNT(*) FROM hospitals")
    int getCount();
}
