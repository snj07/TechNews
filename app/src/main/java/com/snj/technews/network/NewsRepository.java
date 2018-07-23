package com.snj.technews.network;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.ParsedRequestListener;
import com.snj.technews.model.Article;
import com.snj.technews.network.model.NewsResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsRepository {


    //'https://newsapi.org/v2/top-headlines?' +
    //          'sources=bbc-news&' +
    //          'apiKey=API_KEY';

    private String API = "API_KEY";
//    private String country ="us";
    private static NewsRepository newsRepository;
    private String URL = "https://newsapi.org/v2/top-headlines";

    public LiveData<List<Article>> fetchArticles(String source)
    {
        Map<String, String> params = new HashMap<>();
        params.put("sources", source);
       // params.put("country", country);
        params.put("apiKey", API);

        final MutableLiveData<List<Article>> responseData = new MutableLiveData<>();

        AndroidNetworking.get(URL)
                .addQueryParameter(params)
                .addHeaders("token", "1234")
                .setTag("test")
                .setPriority(Priority.LOW)
                .build()
                .getAsObject(NewsResponse.class, new ParsedRequestListener<NewsResponse>() {
                    @Override
                    public void onResponse(NewsResponse response) {
                        responseData.setValue(response.getArticles());
                        Log.d("response : ",response.toString()+"");
                        Log.d("data : ",response.getArticles().size()+"");
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.d("error : ",anError.getErrorDetail()+"");

                        responseData.setValue(null);
                    }
                });
        return responseData;
    }

    public synchronized static NewsRepository getInstance() {

        if (newsRepository == null) {
                newsRepository = new NewsRepository();

        }
        return newsRepository;
    }
}
