/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.backends.pipeline;

import android.content.Context;
import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Concrete implementation of ImagePipeline Drawee controller builder.
 * <p/> See {@link AbstractDraweeControllerBuilder} for more details.
 */
public class PipelineDraweeControllerBuilder extends AbstractDraweeControllerBuilder<
    PipelineDraweeControllerBuilder,
    ImageRequest,
    CloseableReference<CloseableImage>,
    ImageInfo> {

  private final ImagePipeline mImagePipeline;
  private final PipelineDraweeControllerFactory mPipelineDraweeControllerFactory;

  @Nullable
  private ImmutableList<DrawableFactory> mCustomDrawableFactories;

  public PipelineDraweeControllerBuilder(
      Context context,
      PipelineDraweeControllerFactory pipelineDraweeControllerFactory,
      ImagePipeline imagePipeline,
      Set<ControllerListener> boundControllerListeners) {
    super(context, boundControllerListeners);
    mImagePipeline = imagePipeline;
    mPipelineDraweeControllerFactory = pipelineDraweeControllerFactory;
  }

  @Override
  public PipelineDraweeControllerBuilder setUri(@Nullable Uri uri) {
    if (uri == null) {
      return super.setImageRequest(null);
    }
    ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(uri)
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

  @Override
  protected PipelineDraweeController obtainController() {
    DraweeController oldController = getOldController();
    PipelineDraweeController controller;
    if (oldController instanceof PipelineDraweeController) {
      controller = (PipelineDraweeController) oldController;
      controller.initialize(
          obtainDataSourceSupplier(),
          generateUniqueControllerId(),
          getCacheKey(),
          getCallerContext(),
          mCustomDrawableFactories);
    } else {
      controller = mPipelineDraweeControllerFactory.newController(
          obtainDataSourceSupplier(),
          generateUniqueControllerId(),
          getCacheKey(),
          getCallerContext(),
          mCustomDrawableFactories);
    }
    return controller;
  }

  private CacheKey getCacheKey() {
    final ImageRequest imageRequest = getImageRequest();
    final CacheKeyFactory cacheKeyFactory = mImagePipeline.getCacheKeyFactory();
    CacheKey cacheKey = null;
    if (cacheKeyFactory != null && imageRequest != null) {
      if (imageRequest.getPostprocessor() != null) {
        cacheKey = cacheKeyFactory.getPostprocessedBitmapCacheKey(
            imageRequest,
            getCallerContext());
      } else {
        cacheKey = cacheKeyFactory.getBitmapCacheKey(
            imageRequest,
            getCallerContext());
      }
    }
    return cacheKey;
  }

  @Override
  protected DataSource<CloseableReference<CloseableImage>> getDataSourceForRequest(
      ImageRequest imageRequest,
      Object callerContext,
      CacheLevel cacheLevel) {
    return mImagePipeline.fetchDecodedImage(
        imageRequest,
        callerContext,
        convertCacheLevelToRequestLevel(cacheLevel));
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
