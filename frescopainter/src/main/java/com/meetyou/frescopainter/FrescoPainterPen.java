package com.meetyou.frescopainter;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.FrescoPainterDraweeInterceptor;

/**
 * 画师的笔
 * Created by Linhh on 16/9/10.
 */
public class FrescoPainterPen {

    public final static String HTTP_PERFIX = "http://";
    public final static String HTTPS_PERFIX = "https://";
    public final static String FILE_PERFIX = "file://";
    public final static String ASSET_PERFIX = "asset://";

    private String mSourceUri;
    private int mResID;
    private String mLowSourceUri;
    private int mDefaultHolder;

    private int mFailHolder;
    private int mRetryHolder;
    private int mBgHolder;
    private int mOverColor;
    private int mProgressHolder;
    private ScalingUtils.ScaleType mDefaultHolderScaleType;
    private ScalingUtils.ScaleType mFailHolderScaleType;
    private ScalingUtils.ScaleType mRetryHolderScaleType;
    private ScalingUtils.ScaleType mProgressHolderScaleType;
    private int mWidth;
    private int mHeight;
    private PenRadius mPenRadius;
    private ScalingUtils.ScaleType mScaleType;
    private boolean mEnableRetry;
    private boolean mAutoRotateEnabled = true;
    private boolean mAnim = true;
    private int mFade = 300;
    private boolean mProgressiveRenderingEnabled = false;

    private int mLoadMode = PainterMode.FULL_LOAD;

    public FrescoPainterDraweeInterceptor getFrescoPainterDraweeInterceptor() {
        return mFrescoPainterDraweeInterceptor;
    }

    public FrescoPainterPen setFrescoPainterDraweeInterceptor(FrescoPainterDraweeInterceptor frescoPainterDraweeInterceptor) {
        mFrescoPainterDraweeInterceptor = frescoPainterDraweeInterceptor;
        return this;
    }

    private FrescoPainterDraweeInterceptor mFrescoPainterDraweeInterceptor;

    private int mBorderColor;
    private float mBorderWidth;
    private float mRoundePadding;

    public ScalingUtils.ScaleType getProgressHolderScaleType() {
        return mProgressHolderScaleType;
    }

    public FrescoPainterPen setProgressHolderScaleType(ScalingUtils.ScaleType progressHolderScaleType) {
        this.mProgressHolderScaleType = progressHolderScaleType;
        return this;
    }

    public ScalingUtils.ScaleType getFailHolderScaleType() {
        return mFailHolderScaleType;
    }

    public FrescoPainterPen setFailHolderScaleType(ScalingUtils.ScaleType failHolderScaleType) {
        this.mFailHolderScaleType = failHolderScaleType;
        return this;
    }

    public FrescoPainterPen setProgressiveRenderingEnabled(boolean enabled){
        this.mProgressiveRenderingEnabled = enabled;
        return this;
    }

    public boolean getProgressiveRenderingEnabled(){
        return mProgressiveRenderingEnabled;
    }

    public ScalingUtils.ScaleType getRetryHolderScaleType() {
        return mRetryHolderScaleType;
    }

    public FrescoPainterPen setRetryHolderScaleType(ScalingUtils.ScaleType retryHolderScaleType) {
        this.mRetryHolderScaleType = retryHolderScaleType;
        return this;
    }

    public FrescoPainterPen setLoadMode(int mode){
        this.mLoadMode = mode;
        return this;
    }

    public int getLoadMode(){
        return mLoadMode;
    }

    public int getResID(){
        return mResID;
    }

    public String getSourceUri() {
        return mSourceUri;
    }

    public int getDefaultHolder() {
        return mDefaultHolder;
    }

    public int getFailHolder() {
        return mFailHolder;
    }

    public int getBorderColor(){
        return mBorderColor;
    }

    public FrescoPainterPen setBorderColor(int borderColor){
        mBorderColor = borderColor;
        return this;
    }

    public float getBorderWidth(){
        return mBorderWidth;
    }

    public FrescoPainterPen setBorderWidth(float width){
        mBorderWidth = width;
        return this;
    }

    public float getRoundePadding(){
        return mRoundePadding;
    }

    public FrescoPainterPen setRoundePadding(float padding){
        mRoundePadding = padding;
        return this;
    }

    public int getProgressHolder(){
        return mProgressHolder;
    }

    public int getRetryHolder() {
        return mRetryHolder;
    }

    public int getBgHolder() {
        return mBgHolder;
    }

    public int getOverColor() {
        return mOverColor;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public PenRadius getPenRadius() {
        return mPenRadius;
    }

    public ScalingUtils.ScaleType getScaleType() {
        return mScaleType;
    }

    public ScalingUtils.ScaleType getDefaultHolderScaleType(){
        return mDefaultHolderScaleType;
    }

    public int getFadeDuration(){
        return mFade;
    }

    public String getLowSourceUri(){
        return mLowSourceUri;
    }

    public boolean getAutoRotateEnabled(){
        return mAutoRotateEnabled;
    }

    public boolean isEnableRetry() {
        return mEnableRetry;
    }

    public boolean isAnim() {
        return mAnim;
    }

    private FrescoPainterPen(){

    }

    public static FrescoPainterPen newBuilderWithURL(String uri) {
        return new FrescoPainterPen().setSource(uri);
    }

    public static FrescoPainterPen newBuilderWithResource(int resid){
        return new FrescoPainterPen().setResID(resid);
    }

    public static FrescoPainterPen newBuilderWithAsset(String asset){
        StringBuilder sb = new StringBuilder(ASSET_PERFIX);
        sb.append(asset);
        return new FrescoPainterPen().setSource(sb.toString());
    }

    public static FrescoPainterPen newBuilderWithDisk(String path){
        StringBuilder sb = new StringBuilder(FILE_PERFIX);
        sb.append(path);
        return new FrescoPainterPen().setSource(sb.toString());
    }

    public FrescoPainterPen setDefaultHolderScaleType(ScalingUtils.ScaleType scaleType){
        mDefaultHolderScaleType = scaleType;
        return this;
    }

    /**
     * 图片加载中的加载图
     * @param progressHolder
     * @return
     */
    public FrescoPainterPen setProgressHolder(int progressHolder){
        mProgressHolder = progressHolder;
        return this;
    }

    /**
     * 设置画笔的网络请求方式
     * @param uri
     * @return
     */
    private FrescoPainterPen setSource(String uri){
        mSourceUri = uri;
        return this;
    }

    private FrescoPainterPen setResID(int resID){
        mResID = resID;
        return this;
    }

    public FrescoPainterPen setLowSource(String uri){
        mLowSourceUri = uri;
        return this;
    }

    public FrescoPainterPen setFadeDuration(int fade){
        mFade = fade;
        return this;
    }

    public FrescoPainterPen setDefaultHolder(int defaultHolder){
        mDefaultHolder = defaultHolder;
        return this;
    }

    public FrescoPainterPen setFailHolder(int failHolder){
        mFailHolder = failHolder;
        return this;
    }

    public FrescoPainterPen setAutoRotateEnabled(boolean autoRotateEnabled){
        mAutoRotateEnabled = autoRotateEnabled;
        return this;
    }

    public FrescoPainterPen setRetryHolder(int retryHolder){
        mRetryHolder = retryHolder;
        return this;
    }

    public FrescoPainterPen setBgHolder(int bgholder){
        mBgHolder = bgholder;
        return this;
    }

    public FrescoPainterPen setOverColor(int overColor){
        mOverColor = overColor;
        return this;
    }

    public FrescoPainterPen setWidth(int width){
        mWidth = width;
        return this;
    }

    public FrescoPainterPen setHeight(int height){
        mHeight = height;
        return this;
    }

    public FrescoPainterPen setRadius(int radius){
        if(mPenRadius == null){
            mPenRadius = new PenRadius(radius, radius, radius, radius);
        }else {
            mPenRadius.setBottomLeftRadius(radius);
            mPenRadius.setBottomRightRadius(radius);
            mPenRadius.setTopLeftRadius(radius);
            mPenRadius.setTopRightRadius(radius);
        }
        return this;
    }

    public FrescoPainterPen setRadius(PenRadius penRadius){
        mPenRadius = penRadius.clone();
        return this;
    }

    public FrescoPainterPen setScaleType(ScalingUtils.ScaleType scaleType){
        mScaleType = scaleType;
        return this;
    }

    public FrescoPainterPen asCircle(boolean circle){
        if(circle){
            if(mPenRadius == null){
                mPenRadius = new PenRadius(-1, -1, -1, -1);
            }else {
                mPenRadius.setBottomRightRadius(-1);
                mPenRadius.setTopRightRadius(-1);
                mPenRadius.setBottomLeftRadius(-1);
                mPenRadius.setTopLeftRadius(-1);
            }
        }
        return this;
    }

    public boolean isCircle(){
        if(mPenRadius == null){
            return false;
        }
        if(mPenRadius.getBottomLeftRadius() == -1 && mPenRadius.getBottomRightRadius() == -1
                && mPenRadius.getTopLeftRadius() == -1 && mPenRadius.getTopRightRadius() == -1){
            return true;
        }else{
            return false;
        }
    }

    public FrescoPainterPen setRetryEnable(boolean retryEnable){
        mEnableRetry = retryEnable;
        return this;
    }

    /**
     * 设置是否能跑动画，默认让跑
     * @param anim
     * @return
     */
    public FrescoPainterPen setAnim(boolean anim){
        mAnim = anim;
        return this;
    }

}
