package com.snj.technews.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Binder;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.snj.technews.ArticleDetailActivity;
import com.snj.technews.R;
import com.snj.technews.database.AppDatabase;
import com.snj.technews.database.AppDatabaseSingleton;
import com.snj.technews.model.Article;
import com.snj.technews.utils.ImageUtils;
import com.snj.technews.utils.SharedPrefConfigUtils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class WidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {


    private List<Article> articleList;
    private Context context;
    private AppDatabase appDatabase;
    private int mAppWidgetId;


    public WidgetViewsFactory(Context applicationContext, Intent intent) {
        this.context = applicationContext;
        this.articleList = new ArrayList<>();
        appDatabase = AppDatabaseSingleton.getInstance(context);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                articleList = appDatabase.articleDao().getArticlesByUId(SharedPrefConfigUtils.getUserId(context));
                Intent intent2 = new Intent(context, ArticleWidgetProvider.class);
                intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
                // since it seems the onUpdate() is only fired on that:
                int[] ids = AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(new ComponentName(context, ArticleWidgetProvider.class));
                intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_article_list);
                context.sendBroadcast(intent2);
                return null;
            }
        }.execute();
        Log.d("widget_list_size", articleList.size() + "");
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }


    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {

        final long identityToken = Binder.clearCallingIdentity();


        Binder.restoreCallingIdentity(identityToken);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                List<Article> articleListUpdated = appDatabase.articleDao().getArticlesByUId(SharedPrefConfigUtils.getUserId(context));
                if(articleListUpdated.size()!=articleList.size()){
                    articleList = articleListUpdated;
                    Intent intent2 = new Intent(context, ArticleWidgetProvider.class);
                    intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
                    // since it seems the onUpdate() is only fired on that:
                    int[] ids = AppWidgetManager.getInstance(context)
                            .getAppWidgetIds(new ComponentName(context, ArticleWidgetProvider.class));
                    intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_article_list);
                    context.sendBroadcast(intent2);
//                    Binder.restoreCallingIdentity(identityToken);
                }
                return null;
            }
        }.execute();

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return articleList.size();
    }


    @Override
    public RemoteViews getViewAt(final int position) {

        if (position == AdapterView.INVALID_POSITION ||
                articleList == null || articleList.size()<=position) {
            return null;
        }

        final RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget_article_list_item);

        Intent intent = new Intent(context, ArticleDetailActivity.class);
        intent.putExtra("article", articleList.get(position));
        //don't use pending intent here
        // PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickFillInIntent(R.id.widget_item, intent);

        views.setTextViewText(R.id.title, articleList.get(position).getTitle());
//        views.setTextViewText(R.id.published_time, articleList.get(position).getPublishedAt());

        try {
            Bitmap b = Picasso.get().load(articleList.get(position).getUrlToImage()).resize(ImageUtils.dipToPixels(context, 110.0f), ImageUtils.dipToPixels(context, 80.0f)).get();
            views.setImageViewBitmap(R.id.imageView, b);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return views;


    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(context.getPackageName(), R.layout.widget_article_list_item);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /*@Override
    public void onDataSetChanged() {
        synchronized (articleList) {
            articleList.clear();
            Iterables.addAll(articleList, DataSource.getRSSItems(urls));
        }
    }*/

}