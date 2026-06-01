package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AssistantMessageDao {
    @Insert
    void insert(AssistantMessage message);

    @Update
    void update(AssistantMessage message);

    @Query("SELECT * FROM assistant_messages WHERE userId = :userId ORDER BY timestamp ASC")
    List<AssistantMessage> getMessagesForUser(int userId);

    @androidx.room.Delete
    void delete(AssistantMessage message);

    @Query("DELETE FROM assistant_messages WHERE userId = :userId")
    void deleteAllMessagesForUser(int userId);
}
