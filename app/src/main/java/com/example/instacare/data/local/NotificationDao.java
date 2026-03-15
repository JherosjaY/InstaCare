package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificationDao {
    
    @Insert
    void insert(Notification notification);
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    List<Notification> getAllNotifications();
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    int getUnreadCount();
    
    @Query("UPDATE notifications SET isRead = 1")
    void markAllAsRead();
}
