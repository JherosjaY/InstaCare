package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ChatMessageDao {
    @Insert
    void insert(ChatMessage message);

    @Query("SELECT * FROM chat_messages WHERE endorsementId = :endorsementId AND conversationRole = :role ORDER BY timestamp ASC")
    List<ChatMessage> getMessagesForEndorsement(int endorsementId, String role);

    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :userId AND isRead = 0")
    int getUnreadCount(int userId);

    @Query("SELECT COUNT(*) FROM chat_messages WHERE endorsementId = :endorsementId AND receiverId = :userId AND conversationRole = :role AND isRead = 0")
    int getUnreadCountForEndorsement(int endorsementId, int userId, String role);

    @Query("UPDATE chat_messages SET isRead = 1 WHERE endorsementId = :endorsementId AND receiverId = :userId AND conversationRole = :role")
    void markAsRead(int endorsementId, int userId, String role);

    @Query("DELETE FROM chat_messages WHERE senderId = :userId OR receiverId = :userId")
    void deleteByUserId(int userId);
}
