package com.gjf.rings.ringsview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guojunfu on 16/11/27.
 */

public class RingsView extends View {

    private static String TAG = "RingsView";

    // 扩散圆圈颜色
    private int mColor = getResources().getColor(R.color.red);

    // 圆圈中心颜色
    private int mCoreColor = -1;

    // 圆圈中心图片
    private Bitmap mBitmap;

    // 中心圆半径
    private int mCoreRadius = 150;

    // 是否正在扩散中
    private boolean mIsDiffuse = false;

    // 透明度集合
    private List<Integer> mAlphas = new ArrayList<>();

    // 扩散圆半径集合 使用double增加精度
    private List<Double> mWidths = new ArrayList<>();

    private Paint mPaint;


    ///////////////////////////// 新增属性 ////////////////////////////////

    //// 扩散椭圆 ////

    // 是否为扩散椭圆
    private boolean mIsOval = false;
    // 扩散圆的最大个数：扩散范围内可同时出现的最多圆数
    private int mDiffuseNum = 3;
    // 扩散椭圆高宽比
    private float mOvalScale;

    //// 中心椭圆 ////

    // 中心圆是否为椭圆
    private boolean mCenterIsOval = false;
    // 中心椭圆的宽度
    private int mCenterOvalWidth;
    // 中心椭圆的高度
    private int mCenterOvalHeight;

    //// 圆环 ////

    // 是否为圆环
    private boolean mIsRing = false;
    // 圆环宽度
    private int mRingWidth;
    // 填充圆颜色: 默认为白色，建议设置为屏幕背景色
    private int mSpaceColor = getResources().getColor(R.color.white);


    ///////////////////////////// 内部变量 ////////////////////////////////


    // 渐增宽度：每增长mIncreasingWidth宽度时减少1个透明度
    private double mIncreasingRadius = 0;
    // 单独显示最大半径：每个圆单独出现的最大半径，超过这个半径值将增加其他扩散圆
    private double mMaxAloneAppearRadius = 0;

    // 填充圆画笔
    private Paint mSpacePaint;
    // 中心圆画笔
    private Paint mCorePaint;

    // 扩散椭圆外接矩形
    private RectF mOvalRec;
    // 填充圆外接矩形
    private RectF mOvalInsideRec;
    // 中心椭圆外接矩形
    private RectF mOvalCenterRec;

    public RingsView(Context context) {
        this(context, null);
    }

    public RingsView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public RingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RingsView, defStyleAttr, 0);
        mColor = a.getColor(R.styleable.RingsView_diffuse_color, mColor);
        mCoreColor = a.getColor(R.styleable.RingsView_diffuse_coreColor, mCoreColor);
        mCoreRadius = (int) a.getDimension(R.styleable.RingsView_diffuse_coreRadius, mCoreRadius);
        int imageId = a.getResourceId(R.styleable.RingsView_diffuse_coreImage, -1);
        if (imageId != -1) mBitmap = BitmapFactory.decodeResource(getResources(), imageId);

        mIsOval = a.getBoolean(R.styleable.RingsView_rings_isOval, mIsOval);
        mCenterIsOval = a.getBoolean(R.styleable.RingsView_rings_isCenterOval, mCenterIsOval);
        mIsRing = a.getBoolean(R.styleable.RingsView_rings_isRing, mIsRing);
        mDiffuseNum = a.getInt(R.styleable.RingsView_rings_num, mDiffuseNum);
        mOvalScale = a.getFloat(R.styleable.RingsView_rings_ovalScale, mOvalScale);
        mSpaceColor = a.getColor(R.styleable.RingsView_rings_spaceColor, mSpaceColor);
        mCenterOvalWidth = (int) a.getDimension(R.styleable.RingsView_rings_centerOvalWidth, mCenterOvalWidth);
        mCenterOvalHeight = (int) a.getDimension(R.styleable.RingsView_rings_centerOvalHeight, mCenterOvalHeight);
        mRingWidth = (int) a.getDimension(R.styleable.RingsView_rings_ringWidth, mRingWidth);

        init();
        a.recycle();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mColor);

        if(mCoreColor != -1) {
            mCorePaint = new Paint();
            mCorePaint.setAntiAlias(true);
            mCorePaint.setColor(mCoreColor);
        }

        if(mIsRing) {
            mSpacePaint = new Paint();
            mSpacePaint.setColor(mSpaceColor);
            mSpacePaint.setAntiAlias(true);
        }

        if (mIsOval) {
            mOvalRec = new RectF();
        }

        if (mIsOval && mIsRing) {
            mOvalInsideRec = new RectF();
        }

        if (mCenterIsOval) {
            mOvalCenterRec = new RectF();
        }

        mAlphas.add(255);
        mWidths.add(0.0);
    }

    @Override
    public void invalidate() {
        if (hasWindowFocus()) {
            super.invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        //mPaint.setColor(mColor);

        // 每增长mIncreasingWidth宽度时减少1个透明度
        if (mIncreasingRadius == 0) {
            if (mIsOval) {
                mIncreasingRadius = (0.5 * (getWidth() - mCenterOvalWidth)) / 255;
            } else {
                mIncreasingRadius = (0.5 * getWidth() - mCoreRadius) / 255;
            }
        }

        // 每个圆单独出现在屏幕上的最大半径  即 从半径为mMaxAloneAppearRadius时开始增加扩散圆
        if (mMaxAloneAppearRadius == 0) {
            if (mIsOval) {
                mMaxAloneAppearRadius = (int) ((0.5 * getWidth() - mCenterOvalWidth) / mDiffuseNum);
            } else {
                mMaxAloneAppearRadius = (int) ((0.5 * getWidth() - mCoreRadius) / mDiffuseNum);
            }
        }

        for (int i = 0; i < mAlphas.size(); i++) {
            // 设置透明度
            Integer alpha = mAlphas.get(i);
            mPaint.setAlpha(alpha);
            // 绘制扩散圆
            Double width = mWidths.get(i);

            if (mIsOval) {
                mOvalRec.set((float) (0.5 * (getWidth() - width)),
                        (float) (0.5 * (getHeight() - mOvalScale * width)),
                        (float) (width + 0.5 * (getWidth() - width)),
                        (float) ((0.5 * (getHeight() - mOvalScale * width)) + mOvalScale * width));

                canvas.drawOval(mOvalRec, mPaint);

                if (mIsRing) {
                    Log.i(TAG, "onDraw: ");
                    mOvalInsideRec.set((float) ((0.5 * (getWidth() - width)) + mRingWidth),
                            (float) ((0.5 * (getHeight() - mOvalScale * width)) + mRingWidth),
                            (float) (width + (int) (0.5 * (getWidth() - width)) - mRingWidth),
                            (float) ((0.5 * (getHeight() - mOvalScale * width)) + mOvalScale * width - mRingWidth));

                    canvas.drawOval(mOvalInsideRec, mSpacePaint);
                }
            } else {
                canvas.drawCircle(getWidth() / 2, getHeight() / 2, (float) (mCoreRadius + width / 2), mPaint);

                if (mIsRing) {
                    // 内圆环  填充色
                    canvas.drawCircle(getWidth() / 2, getHeight() / 2, (float) (mCoreRadius + width / 2 - mRingWidth), mSpacePaint);
                }
            }

            if (alpha > 0 && width < getWidth()) {
                mAlphas.set(i, alpha - 1);
                mWidths.set(i, width + mIncreasingRadius * 2);
            }
        }

        // 判断当扩散圆扩散到指定宽度时添加新扩散圆
        if (mWidths.get(mWidths.size() - 1) > mMaxAloneAppearRadius * 2) {
            mAlphas.add(255);
            mWidths.add(0.0);
        }

        // 超过10个扩散圆，删除最外层
        if (mWidths.size() >= 10) {
            mWidths.remove(0);
            mAlphas.remove(0);
        }

//        mPaint.setAlpha(255);
//        mPaint.setColor(mColor);

        // 绘制中心圆及图片
        if (mCenterIsOval) {
            mOvalCenterRec.set((int) (0.5 * (getWidth() - mCenterOvalWidth)),
                    (int) (0.5 * (getHeight() - mCenterOvalHeight)),
                    (int) (0.5 * (getWidth() - mCenterOvalWidth)) + mCenterOvalWidth,
                    (int) (0.5 * (getHeight() - mCenterOvalHeight)) + mCenterOvalHeight);
            canvas.drawOval(mOvalCenterRec, mCorePaint);
        } else {
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, mCoreRadius, mCorePaint);
        }

        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, getWidth() / 2 - mBitmap.getWidth() / 2
                    , getHeight() / 2 - mBitmap.getHeight() / 2, mPaint);
        }

        if (mIsDiffuse) {
            invalidate();
        }
    }

    // 开始扩散
    public void start() {
        mIsDiffuse = true;
        invalidate();
    }

    // 停止扩散
    public void stop() {
        mIsDiffuse = false;
    }

    // 是否扩散中
    public boolean isDiffuse() {
        return mIsDiffuse;
    }

    // 设置扩散圆颜色
    public void setColor(int colorId) {
        mColor = colorId;
    }

    public int getColor() {
        return mColor;
    }

    // 设置中心圆颜色
    public void setCoreColor(int colorId) {
        mCoreColor = colorId;
    }

    public int getCoreColor() {
        return mCoreColor;
    }

    // 设置中心圆图片
    public void setCoreImage(int imageId) {
        mBitmap = BitmapFactory.decodeResource(getResources(), imageId);
    }

    public Bitmap getCoreImage() {
        return mBitmap;
    }

    // 设置中心圆半径
    public void setCoreRadius(int radius) {
        mCoreRadius = radius;
    }

    public int getCoreRadius() {
        return mCoreRadius;
    }

    // 设置圆环宽度
    public void setRingWidth(int mRingWidth) {
        this.mRingWidth = mRingWidth;
    }

    public int getRingWidth() {
        return mRingWidth;
    }

    // 扩散圆是否为椭圆
    public boolean isIsOval() {
        return mIsOval;
    }

    public void setIsOval(boolean isOval) {
        this.mIsOval = isOval;
    }

    // 填充圆颜色
    public void setSpaceColor(int spaceColor) {
        mSpaceColor = spaceColor;
    }

    public int getSpaceColor() {
        return mSpaceColor;
    }

    // 扩散圆的最大个数：扩散范围内可同时出现的最多圆数
    public void setDiffuseNum(int mDiffuseNum) {
        this.mDiffuseNum = mDiffuseNum;
    }

    public int getDiffuseNum() {
        return mDiffuseNum;
    }

    // 扩散椭圆的高／宽
    public void setOvalScale(float mOvalScale) {
        this.mOvalScale = mOvalScale;
    }

    public float getOvalScale() {
        return mOvalScale;
    }

    // 中心是否为椭圆
    public void setCenterIsOval(boolean mCenterIsOval) {
        this.mCenterIsOval = mCenterIsOval;
    }

    public boolean isCenterIsOval() {
        return mCenterIsOval;
    }

    // 中心椭圆外接圆的宽
    public void setCenterOvalWidth(int mCenterOvalWidth) {
        this.mCenterOvalWidth = mCenterOvalWidth;
    }

    public int getCenterOvalWidth() {
        return mCenterOvalWidth;
    }

    // 中心椭圆外接圆的高
    public void setCenterOvalHeight(int mCenterOvalHeight) {
        this.mCenterOvalHeight = mCenterOvalHeight;
    }

    public int getCenterOvalHeight() {
        return mCenterOvalHeight;
    }

    public void setIsRing(boolean mIsRing) {
        this.mIsRing = mIsRing;
    }

    public boolean getIsRing() {
        return mIsRing;
    }

    // 每增长mIncreasingWidth宽度时减少1个透明度
    public double getIncreasingRadius() {
        return mIncreasingRadius;
    }

    // 每个圆单独出现在屏幕上的最大半径  即 从半径为mMaxAloneAppearRadius时开始增加扩散圆
    public double getMaxAloneAppearRadius() {
        return mMaxAloneAppearRadius;
    }
}
