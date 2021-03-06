package com.androidog.loadmorerecyclerview;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.androidog.loadmorerecyclerview.adapter.LinearLayoutManagerAdapter;
import com.androidog.loadmorerecyclerview.api.Api;
import com.androidog.loadmorerecyclerview.bean.GanHuo;
import com.androidog.loadmorerecyclerview.widget.HeaderAndFooterView;
import com.androidog.loadmorerecyclerviewlibrary.BaseAdapter;
import com.androidog.loadmorerecyclerviewlibrary.BaseViewHolder;
import com.androidog.loadmorerecyclerviewlibrary.LoadMoreRecyclerView;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.DraweeTransition;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

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

    Retrofit retrofit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            getWindow().setSharedElementEnterTransition(DraweeTransition.createTransitionSet(ScalingUtils.ScaleType.CENTER_CROP, ScalingUtils.ScaleType.CENTER_CROP)); // 进入
            getWindow().setSharedElementReturnTransition(DraweeTransition.createTransitionSet(ScalingUtils.ScaleType.CENTER_CROP, ScalingUtils.ScaleType.CENTER_CROP)); // 返回
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linearlayoutmanager);
        ButterKnife.bind(this);

        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);
                for (View view : sharedElements) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        });

        final ImageView iv_shared = (ImageView) findViewById(R.id.iv_shared);
        iv_shared.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LinearLayoutManagerActivity.this , DetailActivity.class);
                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        LinearLayoutManagerActivity.this, iv_shared, getString(R.string.shared_element_pic));
                startActivity(intent, optionsCompat.toBundle());
            }
        });

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
        mAdapter.setOnItemClickListener(new BaseAdapter.OnItemClickListener<GanHuo.Result>() {
            @Override
            public void onItemClick(@android.support.annotation.NonNull BaseViewHolder viewHolder, int position, @android.support.annotation.NonNull GanHuo.Result result) {
                Toast.makeText(LinearLayoutManagerActivity.this, "单击第 " + position + " 项：", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LinearLayoutManagerActivity.this , DetailActivity.class);
                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        LinearLayoutManagerActivity.this, viewHolder.getView(R.id.drawee), getString(R.string.shared_element_drawee));
                startActivity(intent, optionsCompat.toBundle());
            }
        });
        mAdapter.setOnItemLongClickListener(new BaseAdapter.OnItemLongClickListener<GanHuo.Result>() {
            @Override
            public boolean onItemLongClick(@android.support.annotation.NonNull BaseViewHolder viewHolder, int position, @android.support.annotation.NonNull GanHuo.Result result) {
                Toast.makeText(LinearLayoutManagerActivity.this, "长按第 " + position + " 项：" + result.getDesc(), Toast.LENGTH_SHORT).show();
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
        Api.getInstance().getGankService()
                .getGanHuo("福利", PAGE_COUNT, page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<GanHuo>() {
                    @Override
                    public void onNext(@NonNull GanHuo ganHuo) {
                        Log.e(TAG, "onNext" + ganHuo);
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
                        Log.e(TAG, e.toString());
                        mRecyclerView.loadMoreError();
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "onComplete");
                    }
                });

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
                page = 53;
                showTime(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
