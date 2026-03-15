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

    @Query("SELECT * FROM emergency_contacts WHERE username = :username")
    List<EmergencyContact> getContactsByUser(String username);

    @Query("SELECT * FROM emergency_contacts")
    List<EmergencyContact> getAllContacts();

    @Query("SELECT COUNT(*) FROM emergency_contacts WHERE username = :username")
    int getCountByUser(String username);

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    int getCount();

    @Query("DELETE FROM emergency_contacts WHERE username = :username")
    void deleteAllByUser(String username);

    @Query("DELETE FROM emergency_contacts")
    void deleteAll();
}
