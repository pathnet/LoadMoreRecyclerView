package com.androidog.loadmorerecyclerviewlibrary;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * 顶部加载更多，适用于聊天界面
 * @author wpq
 * @version 1.0
 */
public class LoadMoreOnTopRecyclerView extends RecyclerView {

    public static final String TAG = LoadMoreOnTopRecyclerView.class.getSimpleName();

    /** LoadMore viewType */
    private static final int VIEW_TYPE_LOAD_MORE = 300000;

    private LoadMoreOnTopView mLoadMoreOnTopView;
    private WrapAdapter mWrapAdapter;
    private AdapterDataObserver mAdapterDataObserver = new DataObserver();
    private OnLoadListener mOnLoadListener;
    /** 是否正在执行网络请求，切换标记位保证滚动到顶部时不会频繁触发网络请求 */
    private boolean isLoading = true;
    private boolean noMore = false;

    public LoadMoreOnTopRecyclerView(Context context) {
        this(context, null);
    }

    public LoadMoreOnTopRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadMoreOnTopRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    private void init() {
        mLoadMoreOnTopView = new LoadMoreOnTopView(getContext());
    }

    private void doLoadMore() {
        isLoading = true;
        noMore = false;
        if (mLoadMoreOnTopView != null) {
            mLoadMoreOnTopView.setState(LoadMoreOnTopView.STATE_LOADING);
        }
        if (mOnLoadListener != null) {
            mOnLoadListener.onLoadMore();
        }
    }

    public void loadMoreComplete() {
        isLoading = false;
        noMore = false;
        if (mLoadMoreOnTopView != null) {
            mLoadMoreOnTopView.setState(LoadMoreOnTopView.STATE_COMPLETE);
        }
    }

    /**
     * 没有更多了
     */
    public void noMore(){
        isLoading = false;
        noMore = true;
        if (mLoadMoreOnTopView != null) {
            mLoadMoreOnTopView.setState(LoadMoreOnTopView.STATE_NOMORE);
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        mWrapAdapter = new WrapAdapter(adapter);
        super.setAdapter(mWrapAdapter);
        adapter.registerAdapterDataObserver(mAdapterDataObserver);
        mAdapterDataObserver.onChanged();
    }

    @Override
    public Adapter getAdapter() {
        if (mWrapAdapter != null) {
            return mWrapAdapter.getInnerAdapter();
        }
        return super.getAdapter();
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        /*  state：
            SCROLL_STATE_IDLE     = 0 ：静止,没有滚动
            SCROLL_STATE_DRAGGING = 1 ：正在被外部拖拽,一般为用户正在用手指滚动
            SCROLL_STATE_SETTLING = 2 ：自动滚动开始
         */

        /*
            RecyclerView.canScrollVertically(1)的值表示是否能向上滚动，false表示已经滚动到底部
            RecyclerView.canScrollVertically(-1)的值表示是否能向下滚动，false表示已经滚动到顶部
         */

//        Log.e(TAG, state + ", " + ViewCompat.canScrollVertically(this, -1) + ", " + this.canScrollVertically(-1));
        // 判断RecyclerView滚动到底部，参考：http://www.jianshu.com/p/c138055af5d2
        if (state == RecyclerView.SCROLL_STATE_IDLE && !this.canScrollVertically(-1) && !noMore && !isLoading) {
            doLoadMore();
        }
    }

    /**
     * refs: http://blog.csdn.net/biezhihua/article/details/49506781
     *
     * 解决 canScrollVertically(-1) 始终返回 true 的问题
     * 原因: RecyclerView会对空布局或者是布局中都是Gone的元素做优化，导致在ViewComapt.conScrollVertically(v,-1)时，始终认为还是可以进行下拉的
     * 解决：重写RecyclerView的canScrollVertically()方法，手动判断第一个孩子的位置，只要getTop()大于0，就认为孩子已经显示了
     * @param direction 滚动方向：1 是否能向上滚(是否可以上拉)  -1 是否能向下滚(是否可以下拉)
     * @return
     */
    @Override
    public boolean canScrollVertically(int direction) {
        if (direction < 1) {
            boolean original = super.canScrollVertically(direction);
            View view = getChildAt(0);
            if (!original || view != null && view.getTop() >= 0) {
                return false;
            }else {
                return true;
            }
        }
        return super.canScrollVertically(direction);
    }

    private class WrapAdapter extends Adapter<ViewHolder> {

        private Adapter mInnerAdapter;

        private class WrapViewHolder extends ViewHolder {
            public WrapViewHolder(View itemView) {
                super(itemView);
            }
        }

        public WrapAdapter(Adapter innerAdapter) {
            this.mInnerAdapter = innerAdapter;
        }

        public Adapter getInnerAdapter() {
            return mInnerAdapter;
        }

        public int getInnerItemCount() {
            return mInnerAdapter.getItemCount();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_LOAD_MORE) {
                return new WrapViewHolder(mLoadMoreOnTopView);
            }
            return mInnerAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (isLoadMoreOnTop(position)) {
                return;
            }
            //noinspection unchecked
            mInnerAdapter.onBindViewHolder(holder, position - 1);
        }

        @Override
        public int getItemCount() {
            return 1 + getInnerItemCount();
        }

        @Override
        public int getItemViewType(int position) {
            if (isLoadMoreOnTop(position)) {
                return VIEW_TYPE_LOAD_MORE;
            }
            return mInnerAdapter.getItemViewType(position - 1);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return isLoadMoreOnTop(position) ? gridLayoutManager.getSpanCount() : 1;
                    }
                });
            }
            mInnerAdapter.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onViewAttachedToWindow(ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            int position = holder.getLayoutPosition();
            if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams && isLoadMoreOnTop(position)) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
            //noinspection unchecked
            mInnerAdapter.onViewAttachedToWindow(holder);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            mInnerAdapter.onDetachedFromRecyclerView(recyclerView);
        }

        @Override
        public void onViewDetachedFromWindow(ViewHolder holder) {
            //noinspection unchecked
            mInnerAdapter.onViewDetachedFromWindow(holder);
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            //noinspection unchecked
            mInnerAdapter.onViewRecycled(holder);
        }

        @Override
        public boolean onFailedToRecycleView(ViewHolder holder) {
            //noinspection unchecked
            return mInnerAdapter.onFailedToRecycleView(holder);
        }

        @Override
        public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
            mInnerAdapter.unregisterAdapterDataObserver(observer);
        }

        @Override
        public void registerAdapterDataObserver(AdapterDataObserver observer) {
            mInnerAdapter.registerAdapterDataObserver(observer);
        }

    }

    public boolean isLoadMoreOnTop(int position) {
        return position == 0;
    }

    private class DataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
            if (mWrapAdapter != null) {
                mWrapAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mWrapAdapter.notifyItemMoved(fromPosition, toPosition);
        }

    }

    public void setOnLoadListener(OnLoadListener onLoadListener) {
        this.mOnLoadListener = onLoadListener;
    }

    public interface OnLoadListener{
        void onLoadMore();
    }


}
