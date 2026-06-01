package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AlertNotificationDao {
    @Insert
    void insert(AlertNotification notification);

    @Query("SELECT * FROM alert_notifications WHERE alertId = :alertId")
    List<AlertNotification> getNotificationsForAlert(int alertId);

    @Query("DELETE FROM alert_notifications WHERE alertId = :alertId")
    void deleteByAlertId(int alertId);
}
