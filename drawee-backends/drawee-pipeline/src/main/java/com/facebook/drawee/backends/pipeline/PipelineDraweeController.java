/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.backends.pipeline;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawable.base.DrawableWithCaches;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.drawable.LightBitmapDrawable;
import com.facebook.drawee.drawable.OrientedDrawable;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;
import com.facebook.imagepipeline.animated.factory.AnimatedDrawableFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImageInfo;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

/**
 * Drawee controller that bridges the image pipeline with {@link SettableDraweeHierarchy}. <p> The
 * hierarchy's actual image is set to the image(s) obtained by the provided data source. The data
 * source is automatically obtained and closed based on attach / detach calls.
 */
public class PipelineDraweeController
    extends AbstractDraweeController<CloseableReference<CloseableImage>, ImageInfo> {

  private static final Class<?> TAG = PipelineDraweeController.class;

  // Components
  private final Resources mResources;
  private final AnimatedDrawableFactory mAnimatedDrawableFactory;

  private @Nullable MemoryCache<CacheKey, CloseableImage> mMemoryCache;

  private static Experiment sExperiment;

  private LightBitmapDrawable mLightBitmapDrawable;

  private CacheKey mCacheKey;

  // Constant state (non-final because controllers can be reused)
  private Supplier<DataSource<CloseableReference<CloseableImage>>> mDataSourceSupplier;

  public PipelineDraweeController(
      Resources resources,
      DeferredReleaser deferredReleaser,
      AnimatedDrawableFactory animatedDrawableFactory,
      Executor uiThreadExecutor,
      MemoryCache<CacheKey, CloseableImage> memoryCache,
      Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier,
      String id,
      CacheKey cacheKey,
      Object callerContext) {
      super(deferredReleaser, uiThreadExecutor, id, callerContext);
    mResources = resources;
    mAnimatedDrawableFactory = animatedDrawableFactory;
    mMemoryCache = memoryCache;
    mCacheKey = cacheKey;
    init(dataSourceSupplier);
  }

  /**
   * Initializes this controller with the new data source supplier, id and caller context. This
   * allows for reusing of the existing controller instead of instantiating a new one. This method
   * should be called when the controller is in detached state.
   *
   * @param dataSourceSupplier data source supplier
   * @param id unique id for this controller
   * @param callerContext tag and context for this controller
   */
  public void initialize(
      Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier,
      String id,
      CacheKey cacheKey,
      Object callerContext) {
    super.initialize(id, callerContext);
    init(dataSourceSupplier);
    mCacheKey = cacheKey;
  }

  private void init(Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier) {
    mDataSourceSupplier = dataSourceSupplier;
  }

  protected Resources getResources() {
    return mResources;
  }

  @Override
  protected DataSource<CloseableReference<CloseableImage>> getDataSource() {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "controller %x: getDataSource", System.identityHashCode(this));
    }
    return mDataSourceSupplier.get();
  }

  @Override
  protected Drawable createDrawable(CloseableReference<CloseableImage> image) {
    Preconditions.checkState(CloseableReference.isValid(image));
    CloseableImage closeableImage = image.get();
    if (closeableImage instanceof CloseableStaticBitmap) {
      CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) closeableImage;
      Drawable bitmapDrawable;
      final Experiment experiment = getExperiment();
      if (experiment.mIsLightEnabled) {
        if (experiment.mIsReuseEnabled && mLightBitmapDrawable != null) {
          mLightBitmapDrawable.setBitmap(closeableStaticBitmap.getUnderlyingBitmap());
        } else {
          mLightBitmapDrawable = new LightBitmapDrawable(
              mResources,
              closeableStaticBitmap.getUnderlyingBitmap());
        }
        bitmapDrawable = mLightBitmapDrawable;
      } else {
        bitmapDrawable = new BitmapDrawable(
            mResources,
            closeableStaticBitmap.getUnderlyingBitmap());
      }
      if (closeableStaticBitmap.getRotationAngle() == 0 ||
          closeableStaticBitmap.getRotationAngle() == EncodedImage.UNKNOWN_ROTATION_ANGLE) {
        return bitmapDrawable;
      } else {
        return new OrientedDrawable(bitmapDrawable, closeableStaticBitmap.getRotationAngle());
      }
    } else if (mAnimatedDrawableFactory != null) {
      return mAnimatedDrawableFactory.create(closeableImage);
    } else {
      throw new UnsupportedOperationException("Unrecognized image class: " + closeableImage);
    }
  }

  @Override
  protected ImageInfo getImageInfo(CloseableReference<CloseableImage> image) {
    Preconditions.checkState(CloseableReference.isValid(image));
    return image.get();
  }

  @Override
  protected int getImageHash(@Nullable CloseableReference<CloseableImage> image) {
    return (image != null) ? image.getValueHash() : 0;
  }

  @Override
  protected void releaseImage(@Nullable CloseableReference<CloseableImage> image) {
    CloseableReference.closeSafely(image);
  }

  @Override
  protected void releaseDrawable(@Nullable Drawable drawable) {
    if (drawable instanceof DrawableWithCaches) {
      ((DrawableWithCaches) drawable).dropCaches();
    }
  }

  @Override
  protected CloseableReference<CloseableImage> getCachedImage() {
    if (!getExperiment().mIsFastCheckEnabled) {
      return null;
    }
    if (mMemoryCache == null || mCacheKey == null) {
      return null;
    }
    // We get the CacheKey
    CloseableReference<CloseableImage> closeableImage = mMemoryCache.get(mCacheKey);
    if (closeableImage != null && !closeableImage.get().getQualityInfo().isOfFullQuality()) {
      closeableImage.close();
      return null;
    }
    return closeableImage;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("super", super.toString())
        .add("dataSourceSupplier", mDataSourceSupplier)
        .toString();
  }

  /**
   * @return The Experiment object
   */
  protected static Experiment getExperiment() {
    if (sExperiment == null) {
      sExperiment = new Experiment();
    }
    return sExperiment;
  }

  protected static class Experiment {

    private boolean mIsLightEnabled;
    private boolean mIsReuseEnabled;

    private boolean mIsFastCheckEnabled;

    public Experiment setLightEnabled(final boolean lightEnabled) {
      this.mIsLightEnabled = lightEnabled;
      return this;
    }

    public Experiment setReuseEnabled(final boolean reuseEnabled) {
      this.mIsReuseEnabled = reuseEnabled;
      return this;
    }

    public Experiment setFastCheckEnabled(final boolean fastCheckEnabled) {
      mIsFastCheckEnabled = fastCheckEnabled;
      return this;
    }
  }
}
