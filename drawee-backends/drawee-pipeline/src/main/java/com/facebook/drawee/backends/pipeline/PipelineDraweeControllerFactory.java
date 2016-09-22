/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.backends.pipeline;

import java.util.concurrent.Executor;

import android.content.res.Resources;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.animated.factory.AnimatedDrawableFactory;

import javax.annotation.Nullable;

/**
 * Default implementation of {@link PipelineDraweeControllerFactory}.
 */
public class PipelineDraweeControllerFactory {

  private Resources mResources;
  private DeferredReleaser mDeferredReleaser;
  private AnimatedDrawableFactory mAnimatedDrawableFactory;
  private Executor mUiThreadExecutor;
  private MemoryCache<CacheKey, CloseableImage> mMemoryCache;
  @Nullable
  private ImmutableList<DrawableFactory> mDrawableFactories;

  public PipelineDraweeControllerFactory(
      Resources resources,
      DeferredReleaser deferredReleaser,
      AnimatedDrawableFactory animatedDrawableFactory,
      Executor uiThreadExecutor,
      MemoryCache<CacheKey, CloseableImage> memoryCache,
      @Nullable ImmutableList<DrawableFactory> drawableFactories) {
    mResources = resources;
    mDeferredReleaser = deferredReleaser;
    mAnimatedDrawableFactory = animatedDrawableFactory;
    mUiThreadExecutor = uiThreadExecutor;
    mMemoryCache = memoryCache;
    mDrawableFactories = drawableFactories;
  }

  public PipelineDraweeController newController(
      Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier,
      String id,
      CacheKey cacheKey,
      Object callerContext) {
    return new PipelineDraweeController(
        mResources,
        mDeferredReleaser,
        mAnimatedDrawableFactory,
        mUiThreadExecutor,
        mMemoryCache,
        dataSourceSupplier,
        id,
        cacheKey,
        callerContext,
        mDrawableFactories);
  }
}
