package com.snj.technews.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import com.snj.technews.model.Article;
import com.snj.technews.network.NewsRepository;

import java.util.List;

public class ArticleListViewModel extends ViewModel {

    private  LiveData<List<Article>> articleObservable;
    private String source;

    public ArticleListViewModel(String source) {
        this.source = source;

    }

    public LiveData<List<Article>> getProjectListObservable() {
        articleObservable = NewsRepository.getInstance().fetchArticles(source);
        return articleObservable;
    }

}
