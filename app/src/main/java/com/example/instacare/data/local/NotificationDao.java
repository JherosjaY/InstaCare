package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NotificationDao {
    
    @Insert
    void insert(Notification notification);
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    List<Notification> getAllNotifications();

    @Query("SELECT * FROM notifications WHERE (userId = :userId OR userId = 0) ORDER BY timestamp DESC")
    List<Notification> getNotificationsForUser(int userId);
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    int getUnreadCount();

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0 AND (userId = :userId OR userId = 0)")
    int getUnreadCountForUser(int userId);
    
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId OR userId = 0")
    void markAllAsRead(int userId);

    @Query("SELECT * FROM notifications WHERE isRead = 0 AND (userId = :userId OR userId = 0)")
    List<Notification> getUnreadNotificationsForUser(int userId);

    @Query("DELETE FROM notifications WHERE userId = :userId")
    void deleteByUserId(int userId);

    @androidx.room.Delete
    void delete(Notification notification);

    @androidx.room.Update
    void update(Notification notification);
}
