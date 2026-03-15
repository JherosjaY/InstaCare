package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "emergency_contacts")
public class EmergencyContact {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String phoneNumber;
    public String relation; // e.g., "Parent", "Sibling", "Friend"
    public String username; // Per-user isolation (Stable identifier)

    public EmergencyContact(String name, String phoneNumber, String relation) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relation = relation;
    }
}
