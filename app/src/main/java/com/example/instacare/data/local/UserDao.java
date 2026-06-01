package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface UserDao {
    @Insert
    long registerUser(User user);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    User loginByUsername(String username, String password);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User checkUsernameExists(String username);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User checkEmailExists(String email);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);
    
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    User getUserById(int uid);

    @Query("UPDATE users SET isVerified = 1 WHERE uid = :uid")
    void verifyUserById(int uid);

    @Query("UPDATE users SET fullName = :fullName, email = :email, phone = :phone, address = :address WHERE username = :username")
    void updateProfile(String username, String fullName, String email, String phone, String address);

    @Query("UPDATE users SET fullName = :fullName, email = :email, phone = :phone, address = :address WHERE uid = :uid")
    void updateProfileById(int uid, String fullName, String email, String phone, String address);

    @Update
    void update(User user);

    @Query("DELETE FROM users WHERE email = :email")
    void deleteUserByEmail(String email);

    @Query("DELETE FROM users WHERE uid = :uid")
    void deleteUserById(int uid);

    // Admin queries
    @Query("SELECT * FROM users ORDER BY uid DESC")
    List<User> getAllUsers();

    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();

    @Query("UPDATE users SET isSuspended = 1 WHERE uid = :uid")
    void suspendUserById(int uid);

    @Query("UPDATE users SET isSuspended = 0 WHERE uid = :uid")
    void unsuspendUserById(int uid);

    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%'")
    List<User> searchUsers(String query);
}
