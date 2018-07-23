package com.snj.technews.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.snj.technews.model.User;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM user")
    List<User> getAll();

    @Query("SELECT * FROM user WHERE uid = :uid")
    User findByUId(int uid);

    @Query("SELECT * FROM user WHERE email= :email")
    User findByEmailId(String email);
    @Insert
    void insertAll(User... users);

    @Delete
    void delete(User user);
}
