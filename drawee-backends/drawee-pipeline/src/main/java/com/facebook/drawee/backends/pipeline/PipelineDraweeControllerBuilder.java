/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline;

import android.content.Context;
import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.info.ImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Concrete implementation of ImagePipeline Drawee controller builder.
 *
 * <p>See {@link AbstractDraweeControllerBuilder} for more details.
 */
public class PipelineDraweeControllerBuilder
    extends AbstractDraweeControllerBuilder<
        PipelineDraweeControllerBuilder,
        ImageRequest,
        CloseableReference<CloseableImage>,
        ImageInfo> {

  private final ImagePipeline mImagePipeline;
  private final PipelineDraweeControllerFactory mPipelineDraweeControllerFactory;

  @Nullable private ImmutableList<DrawableFactory> mCustomDrawableFactories;
  @Nullable private ImageOriginListener mImageOriginListener;
  @Nullable private ImagePerfDataListener mImagePerfDataListener;

  public PipelineDraweeControllerBuilder(
      Context context,
      PipelineDraweeControllerFactory pipelineDraweeControllerFactory,
      ImagePipeline imagePipeline,
      Set<ControllerListener> boundControllerListeners,
      Set<ControllerListener2> boundControllerListeners2) {
    super(context, boundControllerListeners, boundControllerListeners2);
    mImagePipeline = imagePipeline;
    mPipelineDraweeControllerFactory = pipelineDraweeControllerFactory;
  }

  @Override
  public PipelineDraweeControllerBuilder setUri(@Nullable Uri uri) {
    if (uri == null) {
      return super.setImageRequest(null);
    }
    ImageRequest imageRequest =
        ImageRequestBuilder.newBuilderWithSource(uri)
            .setRotationOptions(RotationOptions.autoRotateAtRenderTime())
            .build();
    return super.setImageRequest(imageRequest);
  }

  @Override
  public PipelineDraweeControllerBuilder setUri(@Nullable String uriString) {
    if (uriString == null || uriString.isEmpty()) {
      return super.setImageRequest(ImageRequest.fromUri(uriString));
    }
    return setUri(Uri.parse(uriString));
  }

  public PipelineDraweeControllerBuilder setCustomDrawableFactories(
      @Nullable ImmutableList<DrawableFactory> customDrawableFactories) {
    mCustomDrawableFactories = customDrawableFactories;
    return getThis();
  }

  public PipelineDraweeControllerBuilder setCustomDrawableFactories(
      DrawableFactory... drawableFactories) {
    Preconditions.checkNotNull(drawableFactories);
    return setCustomDrawableFactories(ImmutableList.of(drawableFactories));
  }

  public PipelineDraweeControllerBuilder setCustomDrawableFactory(DrawableFactory drawableFactory) {
    Preconditions.checkNotNull(drawableFactory);
    return setCustomDrawableFactories(ImmutableList.of(drawableFactory));
  }

  public PipelineDraweeControllerBuilder setImageOriginListener(
      @Nullable ImageOriginListener imageOriginListener) {
    mImageOriginListener = imageOriginListener;
    return getThis();
  }

  public PipelineDraweeControllerBuilder setPerfDataListener(
      @Nullable ImagePerfDataListener imagePerfDataListener) {
    mImagePerfDataListener = imagePerfDataListener;
    return getThis();
  }

  @Override
  protected PipelineDraweeController obtainController() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("PipelineDraweeControllerBuilder#obtainController");
    }
    try {
      DraweeController oldController = getOldController();
      PipelineDraweeController controller;
      final String controllerId = generateUniqueControllerId();
      if (oldController instanceof PipelineDraweeController) {
        controller = (PipelineDraweeController) oldController;
      } else {
        controller = mPipelineDraweeControllerFactory.newController();
      }
      controller.initialize(
          obtainDataSourceSupplier(controller, controllerId),
          controllerId,
          getCacheKey(),
          getCallerContext(),
          mCustomDrawableFactories,
          mImageOriginListener);
      controller.initializePerformanceMonitoring(
          mImagePerfDataListener, this, Suppliers.BOOLEAN_FALSE);
      return controller;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  private @Nullable CacheKey getCacheKey() {
    final ImageRequest imageRequest = getImageRequest();
    final CacheKeyFactory cacheKeyFactory = mImagePipeline.getCacheKeyFactory();
    CacheKey cacheKey = null;
    if (cacheKeyFactory != null && imageRequest != null) {
      if (imageRequest.getPostprocessor() != null) {
        cacheKey = cacheKeyFactory.getPostprocessedBitmapCacheKey(imageRequest, getCallerContext());
      } else {
        cacheKey = cacheKeyFactory.getBitmapCacheKey(imageRequest, getCallerContext());
      }
    }
    return cacheKey;
  }

  @Override
  protected DataSource<CloseableReference<CloseableImage>> getDataSourceForRequest(
      DraweeController controller,
      String controllerId,
      ImageRequest imageRequest,
      Object callerContext,
      AbstractDraweeControllerBuilder.CacheLevel cacheLevel) {
    return mImagePipeline.fetchDecodedImage(
        imageRequest,
        callerContext,
        convertCacheLevelToRequestLevel(cacheLevel),
        getRequestListener(controller),
        controllerId);
  }

  @Nullable
  protected RequestListener getRequestListener(final DraweeController controller) {
    if (controller instanceof PipelineDraweeController) {
      return ((PipelineDraweeController) controller).getRequestListener();
    }
    return null;
  }

  public static ImageRequest.RequestLevel convertCacheLevelToRequestLevel(
      AbstractDraweeControllerBuilder.CacheLevel cacheLevel) {
    switch (cacheLevel) {
      case FULL_FETCH:
        return ImageRequest.RequestLevel.FULL_FETCH;
      case DISK_CACHE:
        return ImageRequest.RequestLevel.DISK_CACHE;
      case BITMAP_MEMORY_CACHE:
        return ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE;
      default:
        throw new RuntimeException("Cache level" + cacheLevel + "is not supported. ");
    }
  }
}
