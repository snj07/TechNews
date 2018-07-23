package com.snj.technews;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.BitmapRequestListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.snj.technews.database.AppDatabase;
import com.snj.technews.database.AppDatabaseSingleton;
import com.snj.technews.model.Article;
import com.snj.technews.utils.SharedPrefConfigUtils;
import com.snj.technews.widget.ArticleWidgetProvider;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ArticleDetailActivity extends AppCompatActivity {


    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.article_image)
    ImageView articleImage;
    @BindView(R.id.author)
    TextView author;
    @BindView(R.id.share_button)
    ImageButton shareBtn;
    @BindView(R.id.btn_open_article)
    ImageButton openArticleBtn;
    @BindView(R.id.btn_speak)
    ImageButton speakBtn;
    @BindView(R.id.like_button)
    LikeButton likeButton;
    @BindView(R.id.description)
    TextView description;

    private long articleId;
    private AppDatabase appDatabase;
    Thread insertThread;
    private TextToSpeech tts;
    private InterstitialAd mInterstitialAd;
    private String deviceId;
    private final String TAG = getClass().getSimpleName();
    private final String ARTICLE = "article";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        ButterKnife.bind(this);
        final Article article = getIntent().getParcelableExtra(ARTICLE);
        articleId = -1;
        appDatabase = AppDatabaseSingleton.getInstance(this);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {

                    int result = tts.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", getResources().getString(R.string.language_not_supported));
                    } else {
                        speakBtn.setEnabled(true);
                        //speakOut(article.getDescription());
                    }

                } else {
                    Log.e("TTS", "Initialization Failed!");
                }
            }
        });
        tts.setPitch(0.6f);

        description.setText(article.getDescription());
        title.setText(article.getTitle());
        if (article.getAuthor() != null && !article.getAuthor().isEmpty() && !article.getAuthor().equals("null"))
            author.setText(getResources().getText(R.string.by) +" "+ article.getAuthor());
        AndroidNetworking.get(article.getUrlToImage())
                .setTag("imageRequestTag")
                .setPriority(Priority.MEDIUM)
                .setBitmapConfig(Bitmap.Config.ARGB_8888)
                .build()
                .getAsBitmap(new BitmapRequestListener() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        articleImage.setImageBitmap(bitmap);

                    }

                    @Override
                    public void onError(ANError error) {
                        // handle error
                    }
                });

        if (article.getId() > 0) {
            likeButton.setLiked(true);
        }
        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                insertThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        article.setUser_id(getSharedPreferences(SharedPrefConfigUtils.MY_PREFERENCES, Context.MODE_PRIVATE).getLong(SharedPrefConfigUtils.USER_ID, 1));
                        articleId = appDatabase.articleDao().insert(article);
                        updateWidget();
                    }
                });
                insertThread.start();
                updateWidget();
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                if (insertThread == null || (insertThread != null && !insertThread.isAlive())) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (article.getId() == 0)
                                article.setId(articleId);
                            appDatabase.articleDao().delete(article);
                            updateWidget();
                        }
                    }).start();
                }

            }
        });

        openArticleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl())));
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent();
                intent2.setAction(Intent.ACTION_SEND);
                intent2.setType("text/plain");
                intent2.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.read_article_colon) + article.getTitle() + "\n "+getResources().getString(R.string.link_colon) + article.getUrl());
                startActivity(Intent.createChooser(intent2, "Share via"));
            }
        });

        speakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakOut(article.getDescription());
            }
        });

        String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceId = MD5(androidId).toUpperCase();


        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.full_screen_ads));

        if (BuildConfig.DEBUG) {

            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR).addTestDevice(deviceId).build();
            mInterstitialAd.loadAd(adRequest);
            mInterstitialAd.setAdListener(new AdListener() {
                public void onAdLoaded() {

                }
            });

        } else {
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build();
            mInterstitialAd.loadAd(adRequest);
            mInterstitialAd.setAdListener(new AdListener() {
                public void onAdLoaded() {

                }
            });

        }

    }

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        showInterstitial();
        super.onBackPressed();
    }

    private void showInterstitial() {


        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    @Override
    protected void onDestroy() {
        //Close the Text to Speech Library
        if(tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTS Destroyed");
        }
        super.onDestroy();
    }
    public void speakOut(String description) {
        tts.speak(description, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void updateWidget() {

        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, ArticleWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_article_list);
    }
}
