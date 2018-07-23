package com.snj.technews.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import com.snj.technews.ArticleDetailActivity;
import com.snj.technews.R;

public class ArticleWidgetProvider extends AppWidgetProvider {

    private final String TAG = getClass().getSimpleName();
    @Override
    public void onUpdate(Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (final int appWidgetId : appWidgetIds) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_article_list_item);

            // Create an Intent to launch activity
            Intent intent = new Intent(context, ArticleDetailActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            rv.setOnClickPendingIntent(R.id.title, pendingIntent);

            Log.d(TAG, "onUpdate called: ");


            final Intent intent2= new Intent(context, WidgetService.class);
            intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent2.setData(Uri.parse(intent2.toUri(Intent.URI_INTENT_SCHEME)));
            rv = new RemoteViews(context.getPackageName(), R.layout.widget);
            rv.setRemoteAdapter(R.id.widget_article_list,
                    intent2);

            Intent clickIntentTemplate = new Intent(context, ArticleDetailActivity.class);

            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.widget_article_list, clickPendingIntentTemplate);
            rv.setEmptyView(R.id.article_list, R.id.widget_empty);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_article_list);
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, rv);

        }

    }


}
