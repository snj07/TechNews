package com.snj.technews;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.snj.technews.utils.Constants;
import com.snj.technews.utils.ImageUtils;
import com.snj.technews.utils.SharedPrefConfigUtils;
import com.snj.technews.view.ui.ArticleListFragment;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

public class MainArticlesListActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseRemoteConfig firebaseRemoteConfig;

    private static final String UPDATE_CONTENTS_KEY = "update_contents_key";
    private static final String LATEST_VERSION_KEY = "latest_version_key";
    private static final int LATEST_VERSION = 2;
    private static final String UPDATE_CONTENTS = "Some content";
    private final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
        AndroidNetworking.initialize(getApplicationContext(), okHttpClient);
        setContentView(R.layout.activity_article_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);


        //Sets whether analytics collection is enabled for this app on this device.
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        final ImageView imageView = headerView.findViewById(R.id.imageView);
        SharedPreferences sp = getSharedPreferences(SharedPrefConfigUtils.MY_PREFERENCES, Context.MODE_PRIVATE);
        imageView.setImageResource(R.mipmap.ic_launcher_round);

        TextView nameTV = headerView.findViewById(R.id.nameHeaderTV);
        TextView emailTV = headerView.findViewById(R.id.emailHeaderTV);
        nameTV.setText(SharedPrefConfigUtils.getUserName(this.getApplicationContext()));
        emailTV.setText(SharedPrefConfigUtils.getUserEmailId(this.getApplicationContext()));


        new AsyncTask<Void, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(Void... voids) {
                try {
                    Log.d("image_url", SharedPrefConfigUtils.getSharedPreference(getApplicationContext()).getString(SharedPrefConfigUtils.USER_IMAGE, ""));
                    String url = SharedPrefConfigUtils.getSharedPreference(getApplicationContext()).getString(SharedPrefConfigUtils.USER_IMAGE, "");
                    if (TextUtils.isEmpty(url)) {
                        return null;
                    }
                    if (url.contains("facebook")) {
                        url += "?type=large";
                    }
                    Log.d("image_url_1", url);

                    return Picasso.get().load(url).resize(150, 150).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null)
                    imageView.setImageBitmap(ImageUtils.getRoundedShape(bitmap));
            }
        }.execute();

        //  Picasso.get().load( "http://graph.facebook.com/"+userID+"/picture?type=small").into(imageView)


        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            ArticleListFragment fragment = new ArticleListFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("isLocal", false);
            bundle.putString("source", Constants.TECHCRUNCH);
            bundle.putLong("user_id", getSharedPreferences(SharedPrefConfigUtils.MY_PREFERENCES, Context.MODE_PRIVATE).getLong(SharedPrefConfigUtils.USER_ID, 1));
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment, "TAG ").commit();
        }
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(false).build();
        firebaseRemoteConfig.setConfigSettings(configSettings);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(LATEST_VERSION_KEY, LATEST_VERSION);
        configMap.put(UPDATE_CONTENTS_KEY, UPDATE_CONTENTS);
        firebaseRemoteConfig.setDefaults(configMap);
        fetchConfig();

    }


    @Override
    protected void onPostResume() {
        super.onPostResume();


    }

    public void fetchConfig() {
        long cacheExipration = 2400;
        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExipration = 0;
        }
        firebaseRemoteConfig.fetch(cacheExipration).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                firebaseRemoteConfig.activateFetched();
                updateRetrievedData();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error fetching!!");
            }
        });

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            finish();
        }
        super.onBackPressed();

    }

    private void updateRetrievedData() {
        Long version = firebaseRemoteConfig.getLong(LATEST_VERSION_KEY);
        String contents = firebaseRemoteConfig.getString(UPDATE_CONTENTS_KEY);
        if (version.intValue() > LATEST_VERSION) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    MainArticlesListActivity.this, R.style.AlertDialogCustom);
            alertDialogBuilder.setTitle(getResources().getString(R.string.update_alert));
            alertDialogBuilder.setCancelable(true);

            alertDialogBuilder
                    .setMessage(getResources().getString(R.string.update_msg) + contents)
                    .setCancelable(true)
                    .setPositiveButton(getResources().getText(R.string.update), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            Toast.makeText(getApplicationContext(), getResources().getText(R.string.app_url_missing), Toast.LENGTH_LONG).show();
                                //code for future use after adding app on playstore
//                            Uri uri = Uri.parse("market://details?id="
//                                    + getApplicationContext().getPackageName());
//                            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
//                            try {
//                                startActivity(goToMarket);
//                            } catch (ActivityNotFoundException e) {
//                                startActivity(new Intent(
//                                        Intent.ACTION_VIEW,
//                                        Uri.parse("http://play.google.com/store/apps/details?id="
//                                                + getApplicationContext().getPackageName())));
//                            }
                        }
                    })
                    .setNegativeButton(getResources().getText(R.string.later), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            dialog.cancel();
                        }
                    }).setIcon(android.R.drawable.ic_dialog_alert);

            AlertDialog alertDialog = alertDialogBuilder.create();

            alertDialog.show();

        }

    }

    public void showAboutAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                MainArticlesListActivity.this, R.style.AlertDialogCustom);
        alertDialogBuilder.setTitle(getResources().getString(R.string.about_alert));
        alertDialogBuilder.setCancelable(true);

        alertDialogBuilder
                .setMessage(getResources().getString(R.string.about_msg))
                .setCancelable(true)
                .setNeutralButton(getResources().getText(R.string.ok_string), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info);

        AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.show();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            showAboutAlertDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_favorite_articles) {
            // Create new fragment and transaction
            ArticleListFragment newFragment = new ArticleListFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("isLocal", true);
            bundle.putLong("user_id", getSharedPreferences(SharedPrefConfigUtils.MY_PREFERENCES, Context.MODE_PRIVATE).getLong(SharedPrefConfigUtils.USER_ID, 1));
            newFragment.setArguments(bundle);
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack
            fragmentTransaction.replace(R.id.fragment_container, newFragment);
            fragmentTransaction.addToBackStack(null);

            // Commit the transaction
            fragmentTransaction.commit();

        } else if (id == R.id.nav_ars_tehcnica) {
            openNewFragment(false, Constants.ARS_TECHNICA);
        } else if (id == R.id.nav_hacker_news) {
            openNewFragment(false, Constants.HACKER_NEWS);
        } else if (id == R.id.nav_tech_crunch) {
            openNewFragment(false, Constants.TECHCRUNCH);
        } else if (id == R.id.nav_tech_radar) {
            openNewFragment(false, Constants.TECHRADAR);
        } else if (id == R.id.nav_the_verge) {
            openNewFragment(false, Constants.THE_VERGE);
        } else if (id == R.id.nav_google_news) {
            openNewFragment(false, Constants.GOOGLE_NEWS);
        } else if (id == R.id.nav_logout) {
            SharedPreferences.Editor editor = SharedPrefConfigUtils.getSharedPreference(getApplicationContext()).edit();
            editor.clear();
            editor.commit();
            Intent i = new Intent(getApplicationContext(), LoginActivity.class);
//            i.putExtra("isSignOut", true);
            startActivity(i);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void openNewFragment(boolean isLocal, String source) {
        ArticleListFragment newFragment = new ArticleListFragment();
        Bundle firebaseBundle = new Bundle();
        firebaseBundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Source");
        firebaseBundle.putString(FirebaseAnalytics.Param.ITEM_NAME, source);
        //Logs an app event.
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, firebaseBundle);
        //Sets the user ID property.
        firebaseAnalytics.setUserId(SharedPrefConfigUtils.getUserEmailId(getApplicationContext()));

        //Sets a user property to a given value.
        firebaseAnalytics.setUserProperty("Source", source);

        //Sets whether analytics collection is enabled for this app on this device.
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);

        //Sets the minimum engagement time required before starting a session.
        firebaseAnalytics.setMinimumSessionDuration(5000);

        //Sets the duration of inactivity that terminates the current session. The default value is 1800000 (30 minutes).
        firebaseAnalytics.setSessionTimeoutDuration(500);

        Bundle bundle = new Bundle();
        bundle.putBoolean("isLocal", isLocal);
        bundle.putString("source", source);
        bundle.putLong("user_id", getSharedPreferences(SharedPrefConfigUtils.MY_PREFERENCES, Context.MODE_PRIVATE).getLong(SharedPrefConfigUtils.USER_ID, 1));
        newFragment.setArguments(bundle);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        fragmentTransaction.replace(R.id.fragment_container, newFragment);
        fragmentTransaction.addToBackStack(null);

        // Commit the transaction
        fragmentTransaction.commit();
    }


}
