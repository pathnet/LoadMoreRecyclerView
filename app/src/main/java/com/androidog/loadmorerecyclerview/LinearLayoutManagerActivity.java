package com.androidog.loadmorerecyclerview;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.androidog.loadmorerecyclerview.adapter.LinearLayoutManagerAdapter;
import com.androidog.loadmorerecyclerview.api.RetrofitService;
import com.androidog.loadmorerecyclerview.bean.GanHuo;
import com.androidog.loadmorerecyclerviewlibrary.BaseSingleViewTypeAdapter;
import com.androidog.loadmorerecyclerviewlibrary.LoadMoreRecyclerView;
import com.androidog.loadmorerecyclerview.widget.HeaderAndFooterView;
import com.androidog.loadmorerecyclerviewlibrary.LoadMoreRecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author wpq
 * @version 1.0
 */
public class LinearLayoutManagerActivity extends AppCompatActivity {

    public static final String TAG = LinearLayoutManagerActivity.class.getSimpleName();
    /** 分页设置每页10条 */
    public static final int PAGE_COUNT = 10;

    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recyclerView)
    LoadMoreRecyclerView mRecyclerView;

    private LinearLayoutManagerAdapter mAdapter;
    private List<GanHuo.Result> mList = new ArrayList<>();

    private int page = 50; // 当前用的接口最多512条数据

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linearlayoutmanager);
        ButterKnife.bind(this);

        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                page = 1; // gank.io 0和1一样，所以从1开始
                showTime(true);
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLoadMoreEnabled(true);
        mRecyclerView.setOnLoadListener(new LoadMoreRecyclerView.OnLoadListener() {
            @Override
            public void onLoadMore() {
                showTime(false);
            }
        });

        View header0 = new HeaderAndFooterView(this, 0xff235840, "header0");
        mRecyclerView.addHeaderView(header0);
        View header1 = new HeaderAndFooterView(this, 0xff840395, "header1");
        mRecyclerView.addHeaderView(header1);

        mAdapter = new LinearLayoutManagerAdapter(mList);
        mAdapter.setOnItemClickListener(new BaseSingleViewTypeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                try {
                    GanHuo.Result result = mList.get(position - mRecyclerView.getHeadersCount());
                    Toast.makeText(LinearLayoutManagerActivity.this, "单击第 " + position + " 项：" + result.getDesc(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {}
            }
        });
        mAdapter.setOnItemLongClickListener(new BaseSingleViewTypeAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                try {
                    GanHuo.Result result = mList.get(position - mRecyclerView.getHeadersCount());
                    Toast.makeText(LinearLayoutManagerActivity.this, "长按第 " + position + " 项：" + result.getDesc(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {}
                return true;
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        showTime(false);
    }

    /**
     * 一大波美女即将登场
     * @param isRefresh 刷新 or 加载更多
     */
    private void showTime(final boolean isRefresh) {
        // 使用gank.io api 福利接口：http://gank.io/api/data/福利/{每页条数}/页数
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://gank.io/")
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        RetrofitService service = retrofit.create(RetrofitService.class);
        service.getGanHuo("福利", PAGE_COUNT, page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<GanHuo>() {
                    @Override
                    public void onNext(@NonNull GanHuo ganHuo) {
                        Log.e(TAG, "onNext");
                        if (ganHuo != null) {
                            if (isRefresh) {
                                mSwipeRefreshLayout.setRefreshing(false);
                                mRecyclerView.scrollToPosition(0);
                                mList.clear();
                                mAdapter.notifyDataSetChanged();
                            }
                            mList.addAll(ganHuo.getResults());
//                            mAdapter.notifyDataSetChanged();
                            mAdapter.notifyItemInserted(mRecyclerView.getHeadersCount() + mList.size());
//                            mAdapter.notifyItemRangeInserted(mRecyclerView.getHeadersCount() + mList.size(), ganhuo.getResults().size());
                            if (mList.size() < PAGE_COUNT) {
                                mRecyclerView.noNeedToLoadMore();
                            } else if (ganHuo.getResults().size() < PAGE_COUNT) {
                                mRecyclerView.noMore();
                            } else {
                                mRecyclerView.loadMoreComplete();
                                page++;
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, "onError");
                        mRecyclerView.loadMoreError();
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "onComplete");
                    }
                });

//        Call<GanHuo> call = service.getGanHuo("福利", PAGE_COUNT, page);
//        call.enqueue(new Callback<GanHuo>() {
//            @Override
//            public void onResponse(Call<GanHuo> call, Response<GanHuo> response) {
//                Log.e(TAG, response.code() + ", " + response.isSuccessful() + ", " + response.message());
//                if (response.isSuccessful()) {
//                    Log.e(TAG, response.body() + "");
//                    GanHuo ganhuo = response.body();
//
//                    if (isRefresh) {
//                        mSwipeRefreshLayout.setRefreshing(false);
//                        mRecyclerView.scrollToPosition(0);
//                        mList.clear();
//                        mAdapter.notifyDataSetChanged();
//                    }
//                    mList.addAll(ganhuo.getResults());
////                    mAdapter.notifyDataSetChanged();
//                    mAdapter.notifyItemInserted(mRecyclerView.getHeadersCount() + mList.size());
////                    mAdapter.notifyItemRangeInserted(mRecyclerView.getHeadersCount() + mList.size(), ganhuo.getResults().size());
//                    if (mList.size() < PAGE_COUNT) {
//                        mRecyclerView.noNeedToLoadMore();
//                    } else if (ganhuo.getResults().size() < PAGE_COUNT) {
//                        mRecyclerView.noMore();
//                    } else {
//                        mRecyclerView.loadMoreComplete();
//                        page++;
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<GanHuo> call, Throwable t) {
//                Log.e(TAG, t.getMessage());
//                mRecyclerView.loadMoreError();
//            }
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_linearlayoutmanager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_more_than_one_page: // 一页以上
                page = 50;
                showTime(true);
                break;
            case R.id.action_not_enough_one_page: // 不足一页
                page = 52;
                showTime(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
