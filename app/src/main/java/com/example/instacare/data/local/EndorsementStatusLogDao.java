package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EndorsementStatusLogDao {
    @Insert
    void insert(EndorsementStatusLog log);

    @Query("SELECT * FROM endorsement_status_log WHERE endorsementId = :endorsementId ORDER BY timestamp DESC")
    List<EndorsementStatusLog> getLogsForEndorsement(int endorsementId);
}
