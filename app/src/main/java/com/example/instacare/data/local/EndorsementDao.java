package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EndorsementDao {
    @Insert
    long insert(Endorsement endorsement);

    @Query("SELECT * FROM barangay_endorsements WHERE userId = :userId ORDER BY createdAt DESC")
    List<Endorsement> getAllByUserId(int userId);

    @Query("SELECT * FROM barangay_endorsements WHERE alertId = 0 ORDER BY createdAt DESC")
    List<Endorsement> getAllManual();

    @Query("SELECT * FROM barangay_endorsements WHERE status = :status AND alertId = 0 ORDER BY createdAt DESC")
    List<Endorsement> getAllManualByStatus(String status);

    @Query("SELECT * FROM barangay_endorsements WHERE status IN ('FORWARDED', 'RESOLVED') ORDER BY updatedAt DESC")
    List<Endorsement> getAllByHistory();

    @Query("SELECT * FROM barangay_endorsements WHERE status IN ('REVIEWED', 'FORWARDED', 'RESOLVED') AND alertId = 0 ORDER BY updatedAt DESC")
    List<Endorsement> getAllProcessed();

    @Query("UPDATE barangay_endorsements SET status = :status, adminRemarks = :remarks, updatedAt = :updatedAt WHERE id = :id")
    void updateStatus(int id, String status, String remarks, long updatedAt);

    @Query("SELECT COUNT(*) FROM barangay_endorsements WHERE status = :status")
    int getCountByStatus(String status);

    @Query("SELECT * FROM barangay_endorsements WHERE userId = :userId AND alertId = 0 ORDER BY createdAt DESC")
    List<Endorsement> getAllManualByUserId(int userId);

    @Query("SELECT * FROM barangay_endorsements WHERE alertId > 0 ORDER BY createdAt DESC")
    List<Endorsement> getEmergenciesForBarangay();

    @Query("SELECT * FROM barangay_endorsements WHERE id = :id")
    Endorsement getEndorsementById(int id);

    @Query("DELETE FROM barangay_endorsements WHERE userId = :userId")
    void deleteByUserId(int userId);

    @Query("DELETE FROM barangay_endorsements")
    void deleteAll();

    // Isolated Barangay Queries
    @Query("SELECT * FROM barangay_endorsements WHERE alertId = 0 AND barangayZone = :barangay ORDER BY createdAt DESC")
    List<Endorsement> getAllManualByBarangay(String barangay);

    @Query("SELECT * FROM barangay_endorsements WHERE status = :status AND alertId = 0 AND barangayZone = :barangay ORDER BY createdAt DESC")
    List<Endorsement> getAllManualByStatusByBarangay(String status, String barangay);

    @Query("SELECT * FROM barangay_endorsements WHERE status IN ('FORWARDED', 'RESOLVED') AND barangayZone = :barangay ORDER BY updatedAt DESC")
    List<Endorsement> getAllByHistoryByBarangay(String barangay);

    @Query("SELECT * FROM barangay_endorsements WHERE status IN ('REVIEWED', 'FORWARDED', 'RESOLVED') AND alertId = 0 AND barangayZone = :barangay ORDER BY updatedAt DESC")
    List<Endorsement> getAllProcessedByBarangay(String barangay);

    @Query("SELECT COUNT(*) FROM barangay_endorsements WHERE status = :status AND barangayZone = :barangay")
    int getCountByStatusByBarangay(String status, String barangay);
}
