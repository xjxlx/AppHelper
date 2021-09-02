package com.android.helper.widget.banner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.helper.R;

/**
 * BannerView的指示器，用于配合Banner的联动
 */
public class BannerIndicator extends LinearLayout {

    private float mInterval;
    private int mSelectorResource, mUnSelectedResource;
    private int mCurrentPosition;// 上一次点击的item位置
    private int mMaxWidth, mMaxHeight;
    private BannerView mBannerView;

    public BannerIndicator(Context context) {
        super(context);
        initView(context, null);
    }

    public BannerIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        // 固定设置横向
        setOrientation(LinearLayout.HORIZONTAL);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerIndicator);
            // 获取间距
            mInterval = typedArray.getDimension(R.styleable.BannerIndicator_bi_interval, 0);
            // 选中的图形
            mSelectorResource = typedArray.getResourceId(R.styleable.BannerIndicator_bi_selector_resource, 0);
            // 未选中的图形
            mUnSelectedResource = typedArray.getResourceId(R.styleable.BannerIndicator_bi_unselected_resource, 0);
            typedArray.recycle();
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isInEditMode()) {
            // 解决预览模式不显示的问题
            int mode = MeasureSpec.getMode(heightMeasureSpec);
            // 如果是wrap_content模式的话，就显示高度为0
            if (mode == MeasureSpec.AT_MOST) {
                mMaxHeight = 0;
            } else {
                mMaxHeight = resolveSize(MeasureSpec.getSize(heightMeasureSpec), heightMeasureSpec);
            }
            mMaxWidth = resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec);
            setMeasuredDimension(mMaxWidth, mMaxHeight);
        } else {
            // 如果宽高为0，则重新去测量一遍
            if (mMaxWidth == 0 || mMaxHeight == 0) {
                int width = 0;
                int childCount = getChildCount();
                if (childCount > 0) {
                    View childAt = getChildAt(0);
                    // 计算高度
                    if (childAt != null) {
                        measureChild(childAt, widthMeasureSpec, heightMeasureSpec);
                        mMaxHeight = childAt.getMeasuredHeight();
                    }
                    // 计算宽度
                    width = childAt.getMeasuredWidth();
                }
                // 总体宽度 = view 的个数 * 宽度 + view的个数 -1 * 间距
                mMaxWidth = (int) (((childCount - 1) * mInterval) + (width * childCount));
            }
            setMeasuredDimension(mMaxWidth, mMaxHeight);
        }
    }

    /**
     * 结合viewPager
     */
    public void setViewPager(BannerView bannerView, int count) {
        this.mBannerView = bannerView;

        // 先清空，在加入
        int childCount = getChildCount();
        if (childCount > 0) {
            removeAllViews();
        }

        // 添加指示器
        if (count > 1) {// 只有数据大于1的时候，采取添加，否则就不添加数据
            for (int i = 0; i < count; i++) {
                LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                ImageView imageView = new ImageView(getContext());
                //:从第二个开始设置
                if (mInterval > 0) {
                    if (i > 0) {
                        // 设置属性
                        params.leftMargin = (int) mInterval;
                    }
                }
                //:3:把view添加到viewGroup中
                addView(imageView, params);
            }
        }

        // 默认的选中
        int currentItem = mBannerView.getCurrentItem();
        onPageSelected(currentItem);
    }

    /**
     * viewPager选中时候的状态监听
     */
    public void onPageSelected(int position) {
        if (mBannerView != null) {
            int childCount = getChildCount();
            if (childCount > 0) {
                // 重新测量宽高
                mCurrentPosition = position;
                position = position % childCount;
                setSelector(position);

                for (int i = 0; i < childCount; i++) {
                    View childAt = getChildAt(i);
                    // 设置点击事件
                    int finalI = i;
                    childAt.setOnClickListener(v -> {
                        // 先停止轮询的事件
                        mBannerView.onStop();
                        // 计算出当前的position是站的第几列
                        int column = mCurrentPosition % childCount;
                        // 跳转到指定的页面去
                        mBannerView.setCurrentItem(mCurrentPosition + (finalI - column));
                        mBannerView.onStart();
                    });
                }
            }
        }
    }

    /**
     * 设置选中和没有选中的资源
     */
    private void setSelector(int position) {
        //:首先将所有的子元素全部都设置为不点击
        if (mUnSelectedResource != 0) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                if (childAt instanceof ImageView) {
                    ImageView imageView = (ImageView) childAt;
                    imageView.setImageResource(mUnSelectedResource);
                }
            }
        }

        //:当选中某个元素的时候设置为true
        if (mSelectorResource != 0) {
            View childAt = getChildAt(position);
            if (childAt instanceof ImageView) {
                ImageView imageView = (ImageView) childAt;
                imageView.setImageResource(mSelectorResource);
            }
        }
    }

}
