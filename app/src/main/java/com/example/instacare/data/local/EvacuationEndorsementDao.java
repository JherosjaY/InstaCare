package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

/**
 * Tier 3 — Feature E: DAO for EvacuationEndorsement.
 * Mirrors EndorsementDao.java but for the evacuation workflow.
 */
@Dao
public interface EvacuationEndorsementDao {

    @Insert
    long insert(EvacuationEndorsement endorsement);

    // ── User Queries ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM evacuation_endorsements WHERE userId = :userId ORDER BY createdAt DESC")
    List<EvacuationEndorsement> getAllByUserId(int userId);

    @Query("SELECT * FROM evacuation_endorsements WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    List<EvacuationEndorsement> getByUserIdAndStatus(int userId, String status);

    @Query("SELECT * FROM evacuation_endorsements WHERE id = :id")
    EvacuationEndorsement getById(int id);

    // ── Admin / Barangay Queries ───────────────────────────────────────────────

    @Query("SELECT * FROM evacuation_endorsements ORDER BY createdAt DESC")
    List<EvacuationEndorsement> getAll();

    @Query("SELECT * FROM evacuation_endorsements WHERE status = :status ORDER BY createdAt DESC")
    List<EvacuationEndorsement> getAllByStatus(String status);

    @Query("SELECT * FROM evacuation_endorsements WHERE status IN ('ASSIGNED','ARRIVED','RESOLVED') ORDER BY updatedAt DESC")
    List<EvacuationEndorsement> getAllProcessed();

    @Query("SELECT COUNT(*) FROM evacuation_endorsements WHERE status = :status")
    int getCountByStatus(String status);

    // ── Barangay-zone scoped queries (mirrors EndorsementDao pattern) ─────────

    @Query("SELECT * FROM evacuation_endorsements WHERE barangayZone = :zone ORDER BY createdAt DESC")
    List<EvacuationEndorsement> getAllByBarangay(String zone);

    @Query("SELECT * FROM evacuation_endorsements WHERE status = :status AND barangayZone = :zone ORDER BY createdAt DESC")
    List<EvacuationEndorsement> getAllByStatusAndBarangay(String status, String zone);

    @Query("SELECT COUNT(*) FROM evacuation_endorsements WHERE status = :status AND barangayZone = :zone")
    int getCountByStatusAndBarangay(String status, String zone);

    // ── Status Updates ────────────────────────────────────────────────────────

    @Query("UPDATE evacuation_endorsements SET status = :status, adminRemarks = :remarks, assignedCenterId = :centerId, assignedCenterName = :centerName, updatedAt = :updatedAt WHERE id = :id")
    void updateStatus(int id, String status, String remarks, String centerId, String centerName, long updatedAt);

    @Query("UPDATE evacuation_endorsements SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    void updateStatusOnly(int id, String status, long updatedAt);

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @Query("DELETE FROM evacuation_endorsements WHERE userId = :userId")
    void deleteByUserId(int userId);

    @Query("DELETE FROM evacuation_endorsements")
    void deleteAll();
}
