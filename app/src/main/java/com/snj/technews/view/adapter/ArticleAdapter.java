package com.snj.technews.view.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.BitmapRequestListener;
import com.snj.technews.ArticleDetailActivity;
import com.snj.technews.R;
import com.snj.technews.model.Article;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    private List<Article> articleList;
    private Context context;
    private final String TAG = getClass().getSimpleName();

    private final String ARTICLE = "article";

    public ArticleAdapter(Context context){
        this.context = context;
    }

    public void setArticleList(final List<Article> articleList) {
        if (this.articleList == null) {
            this.articleList = articleList;
            notifyItemRangeInserted(0, articleList.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return ArticleAdapter.this.articleList.size();
                }

                @Override
                public int getNewListSize() {
                    return articleList.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return ArticleAdapter.this.articleList.get(oldItemPosition).getId() ==
                            articleList.get(newItemPosition).getId();
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Article article = articleList.get(newItemPosition);

                    if(oldItemPosition>=articleList.size()){
                        return false;
                    }
                    Article old = articleList.get(oldItemPosition);
                    return article.getId() == old.getId()
                            && Objects.equals(article.getUrl(), article.getUrl());
                }
            });
            this.articleList = articleList;
            result.dispatchUpdatesTo(this);
        }
    }



    @Override
    public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.article_list_item,
                parent, false);

        return new ArticleViewHolder(itemView);
    }

    public String pareseDate(String dateText) {
        Date d = null;
        try {
            d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(dateText);
        } catch (ParseException e) {
            d = new Date();
        }

        Log.d(TAG,"Date Parsed!");
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);

    }

    @Override
    public void onBindViewHolder(final ArticleViewHolder holder, final int position) {
        Article article = articleList.get(position);
        holder.title.setText(article.getTitle());
        holder.publishedTime.setText(pareseDate(article.getPublishedAt()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ArticleDetailActivity.class);
                intent.putExtra(ARTICLE,articleList.get(position));
                ((Activity)context).startActivity(intent);
            }
        });
        AndroidNetworking.get(article.getUrlToImage())
                .setTag("imageRequestTag")
                .setPriority(Priority.MEDIUM)
                .setBitmapConfig(Bitmap.Config.ARGB_8888)
                .build()
                .getAsBitmap(new BitmapRequestListener() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        holder.imageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(ANError error) {
                        Log.d(TAG,"Error occurred in image fetching");
                    }
                });


    }

    @Override
    public int getItemCount() {
        return articleList == null ? 0 : articleList.size();
    }


    static class ArticleViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView publishedTime;
        private ImageView imageView;

        public ArticleViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            imageView = view.findViewById(R.id.imageView);
            publishedTime = view.findViewById(R.id.published_time);

        }


    }

}
