package com.snj.technews.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.snj.technews.model.Article;
import com.snj.technews.model.User;

import java.util.List;

@Dao
public interface ArticleDao {
    @Query("SELECT * FROM article")
    List<Article> getAll();

    @Query("SELECT * FROM article WHERE user_id = :uid")
    List<Article> getArticlesByUId(long uid);


    @Insert
    long insert(Article article);

    @Delete
    void delete(Article article);
}
