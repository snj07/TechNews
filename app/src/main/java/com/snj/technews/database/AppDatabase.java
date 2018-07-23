package com.snj.technews.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.snj.technews.dao.ArticleDao;
import com.snj.technews.dao.UserDao;
import com.snj.technews.model.Article;
import com.snj.technews.model.User;

@Database(entities = {User.class, Article.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract ArticleDao articleDao();
}
