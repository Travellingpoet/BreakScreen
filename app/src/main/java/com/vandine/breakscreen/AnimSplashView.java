  package com.vandine.breakscreen;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class AnimSplashView extends View {

    private static final String DEFAULT_LOGO = "KillVanDine";
    private static final int DEFAULT_TEXT_PADDING = 10;
    private static final int ANIM_LOGO_DURATION = 1500;
    private static final int ANIM_LOGO_GRADIENT_DURATION = 1500;
    private static final int ANIM_LOGO_TEXT_SIZE = 30;
    private static final int ANIM_LOGO_TEXT_COLOR = Color.BLACK;
    private static final int ANIM_LOGO_GRADIENT_COLOR = Color.YELLOW;

    private SparseArray<String> mLogoTexts = new SparseArray<>();
    // 最终合成logo后的坐标
    private SparseArray<PointF> mQuietPoints = new SparseArray<>();
    // logo被随机打散的坐标
    private SparseArray<PointF> mRadonPoints = new SparseArray<>();
    private int mWidth;
    private int mHeight;
    private float mMatrixTranslate;
    private Paint mPaint;
    private Paint mImgPaint;
    private Rect mImgRect;
    private Rect mScrRect;
    private float mLogoOffset;
    private float mTextPadding;
    private ValueAnimator mOffsetAnimator;
    private ValueAnimator mGradientAnimator;
    private ValueAnimator mImageAnimator;
    private float mOffsetAnimProgress;
    private long mOffsetDuration;
    private long mGradientDuration;
    private long mImageDuration;
    private boolean isOffsetAnimEnd;
    private LinearGradient mLinearGradient;
    private int mTextColor;
    private int mGradientColor;
    private Matrix mGradientMatrix;
    private boolean isShowGradient;
    private int mTextSize;
    private boolean isAutoPlay;
    private Bitmap bitmap;
    private Drawable logoImage;

    private Animator.AnimatorListener mGradientListener;

    public AnimSplashView(Context context) {
        this(context,null);
    }

    public AnimSplashView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AnimSplashView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs,R.styleable.AnimLogoView);
        String logoName = ta.getString(R.styleable.AnimLogoView_logoName);
        isAutoPlay = ta.getBoolean(R.styleable.AnimLogoView_autoPlay,true);
        isShowGradient = ta.getBoolean(R.styleable.AnimLogoView_showGradient,false);
        mOffsetDuration = ta.getInt(R.styleable.AnimLogoView_offsetAnimDuration,ANIM_LOGO_DURATION);
        mGradientDuration = ta.getInt(R.styleable.AnimLogoView_gradientAnimDuration,ANIM_LOGO_GRADIENT_DURATION);
        mTextColor = ta.getColor(R.styleable.AnimLogoView_textColor,ANIM_LOGO_TEXT_COLOR);
        mTextSize = ta.getDimensionPixelSize(R.styleable.AnimLogoView_textSize,ANIM_LOGO_TEXT_SIZE);
        mTextPadding = ta.getDimensionPixelSize(R.styleable.AnimLogoView_textPadding,DEFAULT_TEXT_PADDING);
        mLogoOffset = ta.getDimensionPixelOffset(R.styleable.AnimLogoView_verticalOffset,0);
        mGradientColor = ta.getColor(R.styleable.AnimLogoView_gradientColor,ANIM_LOGO_GRADIENT_COLOR);
        logoImage = ta.getDrawable(R.styleable.AnimLogoView_logoImage);
        ta.recycle();
        if (TextUtils.isEmpty(logoName)){
            logoName = DEFAULT_LOGO;
        }
        fillLogoTextArray(logoName);
        fillLogoImage(logoImage);
        initPaint();
        initRect();
        initOffsetAnimation();
        initImageAnimation();
    }

    private void fillLogoTextArray(String logoName){
        if (TextUtils.isEmpty(logoName)){
            return;
        }
        if (mLogoTexts.size() > 0){
            mLogoTexts.clear();
        }
        for (int i =0;i<logoName.length();i++){
            char c = logoName.charAt(i);
            mLogoTexts.put(i,String.valueOf(c));
        }
    }

    private void fillLogoImage(Drawable logoImage){
        if (logoImage != null) {
            bitmap = drawableToBitmap(logoImage);
        }
    }
    private void initPaint(){
        if (mPaint == null){
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
        }
        mPaint.setTextSize(mTextSize);
        mPaint.setColor(mTextColor);

        if (mImgPaint == null){
            mImgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mImgPaint.setFilterBitmap(true);
            mImgPaint.setDither(true);
        }
    }

    private void initRect(){
        if (mImgRect == null){
            mImgRect = new Rect(0,0,mWidth,mHeight);
        }
        if (mScrRect == null){
            mScrRect = new Rect(0,0,mWidth,mHeight);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        //logo文字随机位置的初始化
        initLogoCoordinate();
        //logo文字渐变动画的初始化
        initGradientAnimation(w);
        //logo图片位置和动画的初始化
        initImageAnimation();
    }

    private void initLogoCoordinate(){
        float centerY = mHeight / 2f + mPaint.getTextSize() / 2 + mLogoOffset;
        // calculate the final xy of the text
        float totalLength = 0f;
        for (int i = 0;i < mLogoTexts.size(); i++){
            String str = mLogoTexts.get(i);
            float currentLength = mPaint.measureText(str);
            if (i != mLogoTexts.size() - 1){
                totalLength += currentLength + mTextPadding;
            }else {
                totalLength += currentLength;
            }
        }
        // the draw width of the logo must small than the width of this AnimLogoView
        if (totalLength > mWidth){
            throw new IllegalStateException("This view can not display all text of logoName, please change text size.");
        }
        float startX = (mWidth - totalLength) / 2;
        if (mQuietPoints.size() > 0){
            mQuietPoints.clear();
        }
        for (int i = 0; i < mLogoTexts.size(); i++){
            String str = mLogoTexts.get(i);
            float currentLength = mPaint.measureText(str);
            mQuietPoints.put(i,new PointF(startX,centerY));
            startX += currentLength + mTextPadding;
        }
        // generate random start xy of the text
        if (mRadonPoints.size() >0 ){
            mRadonPoints.clear();
        }
        // 构建随机初始坐标
        for (int i = 0; i<mLogoTexts.size();i++){
            mRadonPoints.put(i,new PointF((float) Math.random() * mWidth,(float) Math.random()*mHeight));
        }
    }

    private void initOffsetAnimation(){
        if (mOffsetAnimator == null) {
            mOffsetAnimator = ValueAnimator.ofFloat(0, 1);
            mOffsetAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mOffsetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (mQuietPoints.size() <= 0 || mRadonPoints.size() <= 0) {
                        return;
                    }
                    mOffsetAnimProgress = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            //初始化平移动画
            mOffsetAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mGradientAnimator != null && isShowGradient) {
                        isOffsetAnimEnd = true;
                        mPaint.setShader(mLinearGradient);
                        mGradientAnimator.start();
                    }
                }
            });
        }
        mOffsetAnimator.setDuration(mOffsetDuration);
    }

    private void initImageAnimation(){
        mImageAnimator = ValueAnimator.ofFloat(0,400);
        mImageAnimator.setDuration(mImageDuration);
        mImageAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mImageAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                invalidate();
            }
        });
        mImageAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
       if (!isOffsetAnimEnd){
           mPaint.setAlpha((int)Math.min(255,255 * mOffsetAnimProgress + 100));
           for (int i = 0; i < mQuietPoints.size(); i++){
               PointF quietP = mQuietPoints.get(i);
               PointF radonP = mRadonPoints.get(i);
               float x = radonP.x + (quietP.x - radonP.x) * mOffsetAnimProgress;
               float y = radonP.y + (quietP.y - radonP.y) * mOffsetAnimProgress;
               canvas.drawText(mLogoTexts.get(i),x,y,mPaint);
           }
           if (bitmap != null) {
               canvas.drawBitmap(bitmap, mScrRect, mImgRect, mImgPaint);
           }
       }else {
           for (int i=0;i < mQuietPoints.size(); i++){
               PointF quietP = mQuietPoints.get(i);
               canvas.drawText(mLogoTexts.get(i),quietP.x,quietP.y,mPaint);
           }
           mGradientMatrix.setTranslate(mMatrixTranslate,0);
           mLinearGradient.setLocalMatrix(mGradientMatrix);
       }
    }

    private void initGradientAnimation(int width){
        mGradientAnimator = ValueAnimator.ofFloat(0,2 * width);
        if (mGradientListener != null){
            mGradientAnimator.addListener(mGradientListener);
        }
        mGradientAnimator.setDuration(mGradientDuration);
        mGradientAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mMatrixTranslate = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mLinearGradient = new LinearGradient(-width,0,0,0,new int[]{mTextColor,mGradientColor,mTextColor}, new float[]{0,0.5f,1}, Shader.TileMode.CLAMP);
        mGradientMatrix = new Matrix();
    }

    /**
     * 监听offset动画状态
     *
     * @param listener AnimatorListener
     */
    public void addOffsetAnimListener(Animator.AnimatorListener listener) {
        mOffsetAnimator.addListener(listener);
    }

    /**
     * 监听gradient动画状态
     *
     * @param listener AnimatorListener
     */
    public void addGradientAnimListener(Animator.AnimatorListener listener) {
        mGradientListener = listener;
    }


    /**
     * 开启动画
     */
    public void startAnimation() {
        if (getVisibility() == VISIBLE) {
            if (mOffsetAnimator.isRunning()) {
                mOffsetAnimator.cancel();
            }
            isOffsetAnimEnd = false;
            mOffsetAnimator.start();
            mImageAnimator.start();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getVisibility() == VISIBLE && isAutoPlay){
            mOffsetAnimator.start();
            mImageAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mOffsetAnimator != null && mOffsetAnimator.isRunning()){
            mOffsetAnimator.cancel();
        }
        if (mGradientAnimator != null && mGradientAnimator.isRunning()){
            mGradientAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }

    /**
     * Drawable转换成一个Bitmap
     *
     * @param drawable drawable对象
     * @return
     */
    public static final Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap( drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 设置logo名
     *
     * @param logoName logo名称
     */
    public void setLogoText(String logoName) {
        fillLogoTextArray(logoName);
        // if set the new logoName, should refresh the coordinate again
        initLogoCoordinate();
    }

    /**
     * 设置logo文字动效时长
     *
     * @param duration 动效时长
     */
    public void setOffsetAnimDuration(int duration) {
        mOffsetDuration = duration;
        initOffsetAnimation();
    }

    /**
     * 设置logo文字渐变动效时长
     *
     * @param duration 动效时长
     */
    public void setGradientAnimDuration(int duration) {
        mGradientDuration = duration;
        initGradientAnimation(mWidth);
    }

    /**
     * 设置logo文字渐变颜色
     *
     * @param gradientColor 渐变颜色
     */
    public void setGradientColor(int gradientColor) {
        this.mGradientColor = gradientColor;
    }

    /**
     * 设置是否显示logo文字渐变
     *
     * @param isShowGradient 是否显示logo渐变动效
     */
    public void setShowGradient(boolean isShowGradient) {
        this.isShowGradient = isShowGradient;
    }

    /**
     * 设置logo字体边距
     *
     * @param padding 字体边距
     */
    public void setTextPadding(int padding) {
        mTextPadding = padding;
        initLogoCoordinate();
    }

    /**
     * 设置logo字体颜色
     *
     * @param color 字体颜色
     */
    public void setTextColor(int color) {
        mTextColor = color;
        initPaint();
    }

    /**
     * 设置logo字体大小
     *
     * @param size 字体大小
     */
    public void setTextSize(int size) {
        mTextSize = size;
        initPaint();
    }

    /**
     * 设置logo图片
     *
     * @param drawable logo图片
     */
    public void setImgResource(Drawable drawable) {
        logoImage = drawable;
        initPaint();
        initRect();
    }

}
