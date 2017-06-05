package com.meetyou.frescopainter;

import android.net.Uri;
import android.text.TextUtils;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * Created by Linhh on 16/9/10.
 */
public class PainterFactory {

    public static DraweeController buildDraweeController(IFrescoImageView view){
        FrescoPainterPen pen = view.getFrescoPainterPen();
        return Fresco.newDraweeControllerBuilder()
                .setImageRequest(view.getImageRequest())//图片加载请求
                .setAutoPlayAnimations(pen.isAnim())//是否自动播放动画
                .setTapToRetryEnabled(pen.isEnableRetry())//是否允许点击重试
                .setLowResImageRequest(view.getLowImageRequest())//使用low分辨率加载
                .setControllerListener(view.getControllerListener())//监听器
                .setOldController(view.getController())//使用controller
                .build();
    }

    public static ImageRequest buildImageRequestWithResource(FrescoPainterPen pen){
        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithResourceId(pen.getResID());
        imageRequestBuilder
//                .setPostprocessor(fresco.getPostProcessor())
                .setAutoRotateEnabled(pen.getAutoRotateEnabled())
                .setProgressiveRenderingEnabled(pen.getProgressiveRenderingEnabled())
                .setLocalThumbnailPreviewsEnabled(true);
        if (pen.getWidth() > 0 && pen.getHeight() > 0) {
            imageRequestBuilder.setResizeOptions(new ResizeOptions(pen.getWidth(), pen.getHeight()));
        }
        switch (pen.getLoadMode()){
            case PainterMode.FULL_LOAD:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH);
                break;
            case PainterMode.BITMAP_MEMORY_CACHE_LOAD:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
                break;
            case PainterMode.DISK_CACHE_LOAD:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE);
                break;
            case PainterMode.ENCODED_MEMORY_CACHE_LOAD:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE);
                break;
            default:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH);
                break;
        }
        return imageRequestBuilder.build();
    }

    public static ImageRequest buildImageRequestWithSource(FrescoPainterPen pen){
        return buildImageRequestWithSource(pen, false);
    }

    /**
     * 创建source的请求
     * @param pen
     * @return
     */
    public static ImageRequest buildImageRequestWithSource(FrescoPainterPen pen, boolean sync){
        Uri uri = Uri.parse(pen.getSourceUri());
        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
//                .setPostprocessor(fresco.getPostProcessor())
                .setAutoRotateEnabled(pen.getAutoRotateEnabled())
                .setProgressiveRenderingEnabled(pen.getProgressiveRenderingEnabled())
                .setLocalThumbnailPreviewsEnabled(true);
        if (pen.getWidth() > 0 && pen.getHeight() > 0) {
            imageRequestBuilder.setResizeOptions(new ResizeOptions(pen.getWidth(), pen.getHeight()));
        }
//        imageRequestBuilder.setSync(sync);
        switch (pen.getLoadMode()){
            case PainterMode.FULL_LOAD:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH);
                break;
            case PainterMode.BITMAP_MEMORY_CACHE_LOAD:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
                break;
            case PainterMode.DISK_CACHE_LOAD:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE);
                break;
            case PainterMode.ENCODED_MEMORY_CACHE_LOAD:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE);
                break;
            default:
                imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH);
                break;
        }
        return imageRequestBuilder.build();
    }

    public static ImageRequest buildLowImageRequest(FrescoPainterPen pen){
        if(TextUtils.isEmpty(pen.getLowSourceUri())){
            return null;
        }
        Uri uri = Uri.parse(pen.getLowSourceUri());
        return ImageRequest.fromUri(uri);
    }
}
