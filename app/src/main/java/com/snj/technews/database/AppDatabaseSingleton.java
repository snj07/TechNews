package com.snj.technews.database;


import android.arch.persistence.room.Room;
import android.content.Context;


public class AppDatabaseSingleton {
    private static AppDatabase appDatabase;
    private static final String DATABASE_NAME = "tech_news_db";
    private AppDatabaseSingleton() {
    }

    public static AppDatabase getInstance(Context context) {
        if (appDatabase == null) {
            appDatabase = Room.databaseBuilder(context,
                    AppDatabase.class, DATABASE_NAME).build();
        }
        return appDatabase;
    }
}
