package com.example.instacare.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void registerUser(User user);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    User loginByUsername(String username, String password);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User checkUsernameExists(String username);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User checkEmailExists(String email);
    
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);

    @Query("UPDATE users SET isVerified = 1 WHERE email = :email")
    void verifyUser(String email);

    @Query("UPDATE users SET fullName = :fullName, email = :email, phone = :phone WHERE username = :username")
    void updateProfile(String username, String fullName, String email, String phone);

    @Query("DELETE FROM users WHERE email = :email")
    void deleteUserByEmail(String email);

    // Admin queries
    @Query("SELECT * FROM users ORDER BY uid DESC")
    List<User> getAllUsers();

    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();

    @Query("UPDATE users SET isSuspended = 1 WHERE email = :email")
    void suspendUser(String email);

    @Query("UPDATE users SET isSuspended = 0 WHERE email = :email")
    void unsuspendUser(String email);

    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%'")
    List<User> searchUsers(String query);
}
