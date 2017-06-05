package com.meetyou.frescopainter;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;

import com.facebook.imagepipeline.common.ResizeOptions;

/**
 * Created by Linhh on 16/9/11.
 */
public interface FrescoWork {

    /**
     * 异步加载
     * @param url
     * @param callBack
     */
    public void loadImageCallBackWork(String url, PainterCallBack callBack);

    /**
     * 异步加载
     * @param url
     * @param callBack
     */
    public void loadImageCallBackUi(String url, PainterCallBack callBack);

    public void loadImageCallBackWork(int res, PainterCallBack callBack);

    public void loadImageCallBackUi(int res, PainterCallBack callBack);

    public void loadImageCallBackUi(final String url, final ResizeOptions imageSize, final PainterCallBack callBack);

    public void loadImageCallBackWork(final String url, final ResizeOptions imageSize, final PainterCallBack callBack);

    public void loadImageCallBackUi(final int res, final ResizeOptions imageSize, final PainterCallBack callBack);

    public void loadImageCallBackWork(final int res, final ResizeOptions imageSize, final PainterCallBack callBack);

    /**
     * 缓存清除
     */
    public void clearCaches();

    /**
     * 清除磁盘缓存
     */
    public void clearDiskCaches();

    /**
     * 清除内存
     */
    public void clearMemoryCaches();

    /**
     * 判断该文件是否存在于磁盘
     * @param url
     * @return
     */
    public boolean isInDiskCacheSync(String url);

    /**
     * 判断该文件是否存在于内存
     * @param url
     * @return
     */
    public boolean isInBitmapMemoryCache(String url);

    public void removeFromDiskCache(String url);

    public void removeFromMemoryCache(String url);

    public void removeFromCache(String url);

    public File getDiskCacheFile(String url);

    public void fetchDiskCache(Context context, FrescoPainterPen pen, PainterFetchCallBack callBack);

    public boolean fetchDiskCache(Context context, FrescoPainterPen pen);

    public void evictFromPainterCache(String url);

}
