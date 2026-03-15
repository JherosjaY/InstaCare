package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    public String username;
    public String fullName;
    public String email;
    public String password;
    public String phone;
    public boolean isVerified = false;
    public boolean isSuspended = false; // Admin can suspend accounts

    public User(String username, String email, String password, String phone) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }
}
