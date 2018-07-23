package com.snj.technews.view.ui;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.snj.technews.R;
import com.snj.technews.databinding.ArticleListFragmentBinding;
import com.snj.technews.model.Article;
import com.snj.technews.utils.CheckNet;
import com.snj.technews.view.adapter.ArticleAdapter;
import com.snj.technews.viewmodel.ArticleListViewModel;
import com.snj.technews.viewmodel.ArticleListViewModelLocal;

import java.util.List;

public class ArticleListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    ArticleAdapter articleAdapter;
    ArticleListFragmentBinding listItemBinding;
    private static final String TAG = "ArticleFragment";
    boolean isLocal = false;
    long user_id;
    String source;
    ArticleListViewModel viewModel;
    ArticleListViewModelLocal viewModel1;
    SwipeRefreshLayout swipeRefreshLayout;
    TextView emptyView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() != null) {
            isLocal = getArguments().getBoolean("isLocal", false);
            user_id = getArguments().getLong("user_id");
            source = getArguments().getString("source", "");
        }
        listItemBinding = DataBindingUtil.inflate(inflater, R.layout.article_list_fragment, container, false);
        articleAdapter = new ArticleAdapter(this.getActivity());

        listItemBinding.articleList.setAdapter(articleAdapter);

//        listItemBinding.setIsLoading(true);

        // SwipeRefreshLayout
        swipeRefreshLayout = (SwipeRefreshLayout) listItemBinding.getRoot().findViewById(R.id.swipe_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                // Fetching data from server
            }
        });
        emptyView = listItemBinding.getRoot().findViewById(R.id.empty_view);


        observeData();


        return listItemBinding.getRoot();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("source",source);
        outState.putBoolean("isLocal",isLocal);
        outState.putLong("user_id",user_id);

    }




    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // final ArticleListViewModel viewModel = ViewModelProviders.of(this).get(ArticleListViewModel.class);
        //observeViewModel(viewModel);
        observeData();
    }

    private void observeData() {
        swipeRefreshLayout.setRefreshing(true);
        if (!isLocal) {
//            if (viewModel == null)
            viewModel = ViewModelProviders.of(this, new NewsSourceViewModelFactory(source)).get(ArticleListViewModel.class);
            observeViewModel();

        } else {

            viewModel1 = ViewModelProviders.of(this, new MyViewModelFactory(getActivity().getApplicationContext(), user_id)).get(ArticleListViewModelLocal.class);
            observeViewModelLocal();
        }
    }

    private void observeViewModel() {
        // Update the list when the data changes
        Log.d(TAG, "observe view model");
        if (!CheckNet.isOnline(getContext())) {
            showSnackbar("No Internet!!");
        } else {
            viewModel.getProjectListObservable().observe(this, new Observer<List<Article>>() {
                @Override
                public void onChanged(@Nullable List<Article> articles) {
                    if (articles != null) {
                        Log.d(TAG, " Size: " + articles.size());
//                    listItemBinding.setIsLoading(false);
                        listItemBinding.articleList.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                        articleAdapter.setArticleList(articles);

                    }else{
                        listItemBinding.articleList.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                    swipeRefreshLayout.setRefreshing(false);
                }

            });
        }
    }

    private void showSnackbar(String msg) {
        View parentLayout = getActivity().findViewById(android.R.id.content);
        Snackbar.make(parentLayout, msg, Snackbar.LENGTH_LONG)
                .setAction(getResources().getText(R.string.close), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                .show();
    }

    @Override
    public void onResume() {
        if (viewModel1 != null)
            observeViewModelLocal();
        super.onResume();
    }

    private void observeViewModelLocal() {
        // Update the list when the data changes
        swipeRefreshLayout.setRefreshing(false);
        Log.d(TAG, "observe view model");
        viewModel1.getProjectListObservable().observe(this, new Observer<List<Article>>() {
            @Override
            public void onChanged(@Nullable List<Article> articles) {
                if (articles != null && articles.size()>0) {
                    articleAdapter.setArticleList(articles);
                    articleAdapter.notifyDataSetChanged();
                    emptyView.setVisibility(View.GONE);
                    listItemBinding.articleList.setVisibility(View.VISIBLE);
                }else{

                    listItemBinding.articleList.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                }
            }
        });


    }

    @Override
    public void onRefresh() {
        observeData();
    }

    static class MyViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        public Context context;
        public long user_id;

        public MyViewModelFactory(Context context, long user_id) {
            this.context = context;
            this.user_id = user_id;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ArticleListViewModelLocal(context, user_id);
        }
    }


    static class NewsSourceViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        public String source;

        public NewsSourceViewModelFactory(String source) {
            this.source = source;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ArticleListViewModel(source);
        }
    }
}
