package com.snj.technews.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import com.snj.technews.database.AppDatabase;
import com.snj.technews.database.AppDatabaseSingleton;
import com.snj.technews.model.Article;

import java.util.List;

public class ArticleListViewModelLocal extends ViewModel {

    private  MutableLiveData<List<Article>> articleObservable;
    private AppDatabase appDatabase;
    private Context context;
    long user_id;
    public ArticleListViewModelLocal(Context context, final long user_id) {
        this.context = context;
        articleObservable = new MutableLiveData<>();
        this.user_id= user_id;

    }

    public LiveData<List<Article>> getProjectListObservable() {


        appDatabase = AppDatabaseSingleton.getInstance(context);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //cant use setvalue from background
                articleObservable.postValue(appDatabase.articleDao().getArticlesByUId(user_id));
            }
        }).start();

        return articleObservable;
    }



}
