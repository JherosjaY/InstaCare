package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EmergencyContactDao {
    @Insert
    void insert(EmergencyContact contact);

    @Delete
    void delete(EmergencyContact contact);

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId")
    List<EmergencyContact> getContactsByUser(int userId);

    @Query("SELECT * FROM emergency_contacts")
    List<EmergencyContact> getAllContacts();

    @Query("SELECT COUNT(*) FROM emergency_contacts WHERE userId = :userId")
    int getCountByUser(int userId);

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    int getCount();

    @Query("DELETE FROM emergency_contacts WHERE userId = :userId")
    void deleteAllByUser(int userId);

    @Query("DELETE FROM emergency_contacts WHERE id = :contactId")
    void deleteById(int contactId);

    @Query("DELETE FROM emergency_contacts")
    void deleteAll();
}
