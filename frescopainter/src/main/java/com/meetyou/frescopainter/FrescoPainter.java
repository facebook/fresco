package com.meetyou.frescopainter;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.DraweeConfig;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

/**
 * Fresco画师
 * Linhh：阅读完Fresco的源码让我感觉浑身舒服，真爽~！采用嵌套逻辑设计的代码真想试试。
 * Created by Linhh on 16/9/10.
 */
public class FrescoPainter implements PainterWork, FrescoWork{
    private final static String TAG = "FrescoPainter";

    //判断是否已经初始化
    private static volatile boolean sIsInitialized = false;

    private Context mContext;

    private static FrescoPainter sFrescoPainter;

    private PainterWork mPainterWorksapce;

    private FrescoPainter(Context context) {
        mContext = context;
    }

    public static FrescoPainter workspace(){
        if(!sIsInitialized){
            //没有初始化，你能怪谁啊？？
            return null;
        }
        if(sFrescoPainter.mPainterWorksapce == null)
            sFrescoPainter.mPainterWorksapce = new PainterWorksapce();
        return sFrescoPainter;
    }

    /** 初始化 */
    public static void initialize(Context context) {
        initialize(context, null, null);
    }

    /**
     * 初始化FrescoPainter
     * @param context
     * @param imagePipelineConfig
     */
    public static void initialize(
            Context context,
            ImagePipelineConfig imagePipelineConfig) {
        initialize(context, imagePipelineConfig, null);
    }

    /**
     * 初始化fresco painter
     * @param context
     * @param imagePipelineConfig
     * @param draweeConfig
     */
    public static void initialize(
            Context context,
            ImagePipelineConfig imagePipelineConfig,
            DraweeConfig draweeConfig) {
        //初始化fresco
        Fresco.initialize(context, imagePipelineConfig, draweeConfig);
        initializePainter(context);
    }

    private static void initializePainter(Context context){
        if (sIsInitialized) {
//            Log.i(TAG, "已经初始化了Painter");
          return;
        } else {
            sIsInitialized = true;
        }

        sFrescoPainter = new FrescoPainter(context);
    }

    @Override
    public void paint(IFrescoImageView draweeView, FrescoPainterPen pen){
        if(pen == null){
            //no pen ，no paint
            return;
        }
        mPainterWorksapce.paint(draweeView, pen);
    }

    @Override
    public void loadImageCallBackWork(String url, PainterCallBack callBack) {
      PainterUtils.loadImageCallBackWork(mContext, url,callBack);
    }

    @Override
    public void loadImageCallBackUi(String url, PainterCallBack callBack) {
      PainterUtils.loadImageCallBackUi(mContext, url,callBack);
    }

    @Override
    public void loadImageCallBackWork(int res, PainterCallBack callBack) {
      PainterUtils.loadImageCallBackWork(mContext,res,callBack);
    }

    @Override
    public void loadImageCallBackUi(int res, PainterCallBack callBack) {
      PainterUtils.loadImageCallBackUi(mContext,res,callBack);
    }

    @Override
    public void loadImageCallBackUi(
        String url,
        ResizeOptions imageSize,
        PainterCallBack callBack) {
      PainterUtils.loadImageCallBackUi(mContext,url,imageSize,callBack);
    }

    @Override
    public void loadImageCallBackWork(
        String url,
        ResizeOptions imageSize,
        PainterCallBack callBack) {
      PainterUtils.loadImageCallBackWork(mContext,url,imageSize,callBack);
    }

    @Override
    public void loadImageCallBackUi(
        int res, ResizeOptions imageSize, PainterCallBack callBack) {
      PainterUtils.loadImageCallBackUi(mContext,res,imageSize,callBack);
    }

    @Override
    public void loadImageCallBackWork(
        int res, ResizeOptions imageSize, PainterCallBack callBack) {
      PainterUtils.loadImageCallBackWork(mContext,res,imageSize,callBack);
    }

    @Override
    public void clearCaches() {
        PainterUtils.clearCaches();
    }

    @Override
    public void clearDiskCaches() {
        PainterUtils.clearDiskCaches();;
    }

    @Override
    public void clearMemoryCaches() {
        PainterUtils.clearMemoryCaches();
    }

    @Override
    public boolean isInDiskCacheSync(String url) {
        return PainterUtils.isInDiskCacheSync(url);
    }

    @Override
    public boolean isInBitmapMemoryCache(String url) {
        return PainterUtils.isInBitmapMemoryCache(url);
    }

    @Override
    public void removeFromDiskCache(String url) {
        PainterUtils.evictFromDiskCache(url);
    }

    @Override
    public void removeFromMemoryCache(String url) {
        PainterUtils.evictFromMemoryCache(url);
    }

    @Override
    public void removeFromCache(String url) {
        PainterUtils.evictFromCache(url);
    }

    @Override
    public File getDiskCacheFile(String url) {
      return PainterUtils.getDiskCacheFile(url);
    }

    @Override
    public void fetchDiskCache(
        Context context, FrescoPainterPen pen, PainterFetchCallBack callBack) {
      PainterUtils.fetchToDiskCache(context,pen,callBack);
    }

    @Override
    public boolean fetchDiskCache(Context context, FrescoPainterPen pen) {
      return PainterUtils.fetchToDiskCache(context,pen);
    }

    @Override
    public void evictFromPainterCache(String url) {
      PainterUtils.evictFromPainterCache(url);
    }
}
