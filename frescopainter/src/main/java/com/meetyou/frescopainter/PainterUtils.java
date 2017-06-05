package com.meetyou.frescopainter;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.Executor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * 画匠工具
 * Created by Linhh on 16/9/10.
 */
class PainterUtils{

//    private static HashMap<Uri,Bitmap> mBitmapCache = new HashMap<>();

    public static ImagePipeline getImagePipeline(){
        return Fresco.getImagePipeline();
    }

    public static ImagePipelineFactory getImagePipelineFactory() {
        return Fresco.getImagePipelineFactory();
    }

    public static Drawable getDrawable(Context context, int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getDrawable(resId);
        } else {
            return context.getResources().getDrawable(resId);
        }
    }

    private static void loadImage(Context context, final String url, final ImageRequest request, final PainterCallBack callBack, final
        Executor executor){
//        if(mBitmapCache.containsKey(request.getSourceUri()) && !mBitmapCache.get(request.getSourceUri()).isRecycled()){
//            if(callBack != null) {
//                callBack.onSuccess(url, mBitmapCache.get(request.getSourceUri()));
//            }
//        }else {
            DataSource<CloseableReference<CloseableImage>>
                dataSource = getImagePipeline().fetchDecodedImage(request, context);
            dataSource.subscribe(new DataSubscriber<CloseableReference<CloseableImage>>() {
                @Override
                public void onNewResult(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = convertBitmap(dataSource.getResult());
//                        mBitmapCache.put(request.getSourceUri(), bitmap);
                    } finally {
                        CloseableReference.closeSafely(dataSource.getResult());
                    }
                    if (callBack != null) {
                        callBack.onSuccess(url, bitmap);
                    }
                }

                @Override
                public void onFailure(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    if (callBack != null) {
                        callBack.onFailure(url, dataSource.getFailureCause());
                    }
                }

                @Override
                public void onCancellation(DataSource<CloseableReference<CloseableImage>> dataSource) {

                }

                @Override
                public void onProgressUpdate(DataSource<CloseableReference<CloseableImage>> dataSource) {

                }
            }, executor);
//        }
    }

    public static void loadImageCallBackWork(Context context, final int res, final ResizeOptions imageSize, final PainterCallBack callBack){
        ImageRequest request = ImageRequestBuilder.newBuilderWithResourceId(res)
            .setResizeOptions(imageSize)
            .build();
        loadImage(context,"", request,callBack,CallerThreadExecutor.getInstance());
    }

    public static void loadImageCallBackWork(Context context, final String url, final ResizeOptions imageSize, final PainterCallBack callBack){
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(
            Uri.parse(url))
            .setResizeOptions(imageSize)
            .build();
        loadImage(context,url, request,callBack,CallerThreadExecutor.getInstance());
    }

    public static void loadImageCallBackWork(Context context, final int res,final PainterCallBack callBack){
        ImageRequest request = ImageRequestBuilder.newBuilderWithResourceId(res)
            .build();
        loadImage(context,"", request,callBack,CallerThreadExecutor.getInstance());
    }

    public static void loadImageCallBackWork(Context context, final String url, final PainterCallBack callBack){
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(
            Uri.parse(url))
            .build();
        loadImage(context,url, request,callBack,CallerThreadExecutor.getInstance());
    }

    public static void loadImageCallBackUi(Context context, final int res, final ResizeOptions imageSize, final PainterCallBack callBack){
        ImageRequest request = ImageRequestBuilder.newBuilderWithResourceId(res)
            .setResizeOptions(imageSize)
            .build();
        loadImage(context,"", request,callBack,UiThreadImmediateExecutorService.getInstance());
    }

    public static void loadImageCallBackUi(Context context, final String url, final ResizeOptions imageSize, final PainterCallBack callBack){
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(
            Uri.parse(url))
            .setResizeOptions(imageSize)
            .build();
        loadImage(context,url, request,callBack,UiThreadImmediateExecutorService.getInstance());
    }

    public static void loadImageCallBackUi(Context context, final int res, final PainterCallBack callBack){
        ImageRequest request = ImageRequestBuilder.newBuilderWithResourceId(res)
            .build();
        loadImage(context,"", request,callBack,UiThreadImmediateExecutorService.getInstance());
    }

    public static void loadImageCallBackUi(Context context, final String url, final PainterCallBack callBack){
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(
            Uri.parse(url))
            .build();
        loadImage(context,url, request,callBack,UiThreadImmediateExecutorService.getInstance());
    }

      /**
       * 这里不应该copy其实,更应该直接encode未解析拿出来
       * @param closeableImageRef
       * @return
       */
    private static Bitmap convertBitmap(CloseableReference<CloseableImage> closeableImageRef) throws OutOfMemoryError{
        Bitmap bitmap = null;
        if (closeableImageRef != null) {
            if(closeableImageRef.get() instanceof CloseableBitmap) {
                bitmap = ((CloseableBitmap) closeableImageRef.get()).getUnderlyingBitmap();
            }else if(closeableImageRef.get() instanceof CloseableAnimatedImage){

                CloseableAnimatedImage closeableImage = (CloseableAnimatedImage)closeableImageRef.get();
                final AnimatedImageResult result = ((CloseableAnimatedImage) closeableImage).getImageResult();
                bitmap = result.getPreviewBitmap().get();

            }
        }
        if(bitmap != null){
            bitmap = bitmap.createBitmap(bitmap);
        }
        return bitmap;
    }

    /**
     * 清除所有缓存
     */
    public static void clearCaches() {
        getImagePipeline().clearCaches();
    }

    /**
     * 清除磁盘缓存
     */
    public static void clearDiskCaches() {
        getImagePipeline().clearDiskCaches();
    }

    /**
     * 清除内存缓存
     */
    public static void clearMemoryCaches(){
        getImagePipeline().clearMemoryCaches();
    }

    /**
     * 判断是否已经在disk缓存
     * @param url
     * @return
     */
    public static boolean isInDiskCacheSync(String url) {
        return getImagePipeline().isInDiskCacheSync(Uri.parse(url));
    }

    /**
     * 判断是否存在内存缓存中
     * @param url
     * @return
     */
    public static boolean isInBitmapMemoryCache(String url) {
        return getImagePipeline().isInBitmapMemoryCache(Uri.parse(url));
    }

    public static void evictFromPainterCache(String url){
//        Uri uri = Uri.parse(url);
//        Bitmap bitmap = mBitmapCache.get(uri);
//        if(bitmap != null && !bitmap.isRecycled()){
//            bitmap.recycle();
//        }
//        mBitmapCache.remove(uri);
    }

    public static void evictFromDiskCache(String url){
        getImagePipeline().evictFromDiskCache(Uri.parse(url));
        evictFromPainterCache(url);
    }

    public static void evictFromMemoryCache(String url){
        getImagePipeline().evictFromMemoryCache(Uri.parse(url));
        evictFromPainterCache(url);
    }

    public static void evictFromCache(String url){
        getImagePipeline().evictFromCache(Uri.parse(url));
        evictFromPainterCache(url);
    }

  /**
   * 不安全做法，直接处理缓存
   * @param url
   * @return
   */
    public static File getDiskCacheFile(String url){
        File cache = null;
        FileBinaryResource fileBinaryResource = null;
        BufferedDiskCache mainCache = getImagePipeline().getMainBufferdDiskCache();
        fileBinaryResource = (FileBinaryResource)mainCache.getFileCache().getResource(new SimpleCacheKey(url));
        if(fileBinaryResource != null) {
            cache = fileBinaryResource.getFile();
        }
        if(cache != null){
            return cache;
        }
        //取二级缓存
        BufferedDiskCache smallCache = getImagePipeline().getSmallImageBufferedDiskCache();
        fileBinaryResource = (FileBinaryResource)smallCache.getFileCache().getResource(new SimpleCacheKey(url));
        if(fileBinaryResource != null) {
            cache = fileBinaryResource.getFile();
        }
        return cache;
    }

    public static boolean fetchToDiskCache(Context context, FrescoPainterPen pen){
        if(pen == null){
            return false;
        }
        ImageRequest request = null;
        if(TextUtils.isEmpty(pen.getSourceUri())){
            //说明是res
            request = PainterFactory.buildImageRequestWithResource(pen);
        }else{
            request = PainterFactory.buildImageRequestWithSource(pen, true);
        }
        DataSource<Void> dataSource = getImagePipeline().prefetchToDiskCache(request,context);
        if(dataSource == null){
            return false;
        }
        return dataSource.hasResult();
    }

    public static void fetchToDiskCache(Context context, FrescoPainterPen pen, final PainterFetchCallBack callBack){
        if(pen == null){
            if(callBack != null){
                callBack.onFail();
            }
            return;
        }
        ImageRequest request = null;
        if(TextUtils.isEmpty(pen.getSourceUri())){
            //说明是res
            request = PainterFactory.buildImageRequestWithResource(pen);
        }else{
            request = PainterFactory.buildImageRequestWithSource(pen, true);
        }
        DataSource<Void> dataSource = getImagePipeline().prefetchToDiskCache(request,context);
        if(dataSource == null){
            if(callBack != null){
                callBack.onFail();
            }
            return;
        }
        dataSource.subscribe(new DataSubscriber<Void>() {
            @Override
            public void onNewResult(DataSource<Void> dataSource) {
                if(callBack != null){
                    callBack.onSuccess();
                }
            }

            @Override
            public void onFailure(DataSource<Void> dataSource) {
                if(callBack != null){
                    callBack.onFail();
                }
            }

            @Override
            public void onCancellation(DataSource<Void> dataSource) {

            }

            @Override
            public void onProgressUpdate(DataSource<Void> dataSource) {

            }
        },UiThreadImmediateExecutorService.getInstance());
    }

//    public static void clearCache
}
