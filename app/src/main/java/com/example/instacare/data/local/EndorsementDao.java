package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EndorsementDao {
    @Insert
    void insert(Endorsement endorsement);

    @Query("SELECT * FROM endorsements WHERE username = :username ORDER BY createdAt DESC")
    List<Endorsement> getAllByUsername(String username);

    @Query("SELECT * FROM endorsements ORDER BY createdAt DESC")
    List<Endorsement> getAll();

    @Query("SELECT * FROM endorsements WHERE status = :status ORDER BY createdAt DESC")
    List<Endorsement> getAllByStatus(String status);

    @Query("SELECT * FROM endorsements WHERE status IN ('APPROVED', 'DECLINED') ORDER BY updatedAt DESC")
    List<Endorsement> getAllByHistory();

    @Query("UPDATE endorsements SET status = :status, adminRemarks = :remarks, updatedAt = :updatedAt WHERE id = :id")
    void updateStatus(int id, String status, String remarks, long updatedAt);

    @Query("SELECT COUNT(*) FROM endorsements WHERE status = :status")
    int getCountByStatus(String status);

    @Query("DELETE FROM endorsements")
    void deleteAll();
}
