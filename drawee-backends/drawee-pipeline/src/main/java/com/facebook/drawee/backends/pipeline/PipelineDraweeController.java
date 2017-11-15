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
import android.media.ExifInterface;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawable.base.DrawableWithCaches;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.debug.DebugControllerOverlayDrawable;
import com.facebook.drawee.drawable.OrientedDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.drawable.DrawableFactory;
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
  private final DrawableFactory mAnimatedDrawableFactory;
  // Global drawable factories that are set when Fresco is initialized
  @Nullable
  private final ImmutableList<DrawableFactory> mGlobalDrawableFactories;

  private @Nullable MemoryCache<CacheKey, CloseableImage> mMemoryCache;

  private CacheKey mCacheKey;

  // Constant state (non-final because controllers can be reused)
  private Supplier<DataSource<CloseableReference<CloseableImage>>> mDataSourceSupplier;

  private boolean mDrawDebugOverlay;

  // Drawable factories that are unique for a given image request
  private @Nullable ImmutableList<DrawableFactory> mCustomDrawableFactories;

  private final DrawableFactory mDefaultDrawableFactory =
      new DrawableFactory() {

        @Override
        public boolean supportsImageType(CloseableImage image) {
          return true;
        }

        @Override
        public Drawable createDrawable(CloseableImage closeableImage) {
          if (closeableImage instanceof CloseableStaticBitmap) {
            CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) closeableImage;
            Drawable bitmapDrawable =
                new BitmapDrawable(mResources, closeableStaticBitmap.getUnderlyingBitmap());
            if (!hasTransformableRotationAngle(closeableStaticBitmap)
                && !hasTransformableExifOrientation(closeableStaticBitmap)) {
              // Return the bitmap drawable directly as there's nothing to transform in it
              return bitmapDrawable;
            } else {
              return new OrientedDrawable(
                  bitmapDrawable,
                  closeableStaticBitmap.getRotationAngle(),
                  closeableStaticBitmap.getExifOrientation());
            }
          } else if (mAnimatedDrawableFactory != null
              && mAnimatedDrawableFactory.supportsImageType(closeableImage)) {
            return mAnimatedDrawableFactory.createDrawable(closeableImage);
          }
          return null;
        }
      };

  /* Returns true if there is anything to rotate using the rotation angle */
  private static boolean hasTransformableRotationAngle(
      CloseableStaticBitmap closeableStaticBitmap) {
    return closeableStaticBitmap.getRotationAngle() != 0
        && closeableStaticBitmap.getRotationAngle() != EncodedImage.UNKNOWN_ROTATION_ANGLE;
  }

  /* Returns true if there is anything to rotate using the EXIF orientation */
  private static boolean hasTransformableExifOrientation(
      CloseableStaticBitmap closeableStaticBitmap) {
    return closeableStaticBitmap.getExifOrientation() != ExifInterface.ORIENTATION_NORMAL
        && closeableStaticBitmap.getExifOrientation() != ExifInterface.ORIENTATION_UNDEFINED;
  }

  public PipelineDraweeController(
          Resources resources,
          DeferredReleaser deferredReleaser,
          DrawableFactory animatedDrawableFactory,
          Executor uiThreadExecutor,
          MemoryCache<CacheKey, CloseableImage> memoryCache,
          Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier,
          String id,
          CacheKey cacheKey,
          Object callerContext) {
    this(
        resources,
        deferredReleaser,
        animatedDrawableFactory,
        uiThreadExecutor,
        memoryCache,
        dataSourceSupplier,
        id,
        cacheKey,
        callerContext,
        null);
  }

  public PipelineDraweeController(
      Resources resources,
      DeferredReleaser deferredReleaser,
      DrawableFactory animatedDrawableFactory,
      Executor uiThreadExecutor,
      MemoryCache<CacheKey, CloseableImage> memoryCache,
      Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier,
      String id,
      CacheKey cacheKey,
      Object callerContext,
      @Nullable ImmutableList<DrawableFactory> globalDrawableFactories) {
    super(deferredReleaser, uiThreadExecutor, id, callerContext);
    mResources = resources;
    mAnimatedDrawableFactory = animatedDrawableFactory;
    mMemoryCache = memoryCache;
    mCacheKey = cacheKey;
    mGlobalDrawableFactories = globalDrawableFactories;
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
      Object callerContext,
      @Nullable ImmutableList<DrawableFactory> customDrawableFactories) {
    super.initialize(id, callerContext);
    init(dataSourceSupplier);
    mCacheKey = cacheKey;
    setCustomDrawableFactories(customDrawableFactories);
  }

  public void setDrawDebugOverlay(boolean drawDebugOverlay) {
    mDrawDebugOverlay = drawDebugOverlay;
  }

  public void setCustomDrawableFactories(
      @Nullable ImmutableList<DrawableFactory> customDrawableFactories) {
    mCustomDrawableFactories = customDrawableFactories;
  }

  private void init(Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier) {
    mDataSourceSupplier = dataSourceSupplier;

    maybeUpdateDebugOverlay(null);
  }

  protected Resources getResources() {
    return mResources;
  }

  protected CacheKey getCacheKey() {
    return mCacheKey;
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

    maybeUpdateDebugOverlay(closeableImage);

    Drawable drawable = maybeCreateDrawableFromFactories(mCustomDrawableFactories, closeableImage);
    if (drawable != null) {
      return drawable;
    }

    drawable = maybeCreateDrawableFromFactories(mGlobalDrawableFactories, closeableImage);
    if (drawable != null) {
      return drawable;
    }

    drawable = mDefaultDrawableFactory.createDrawable(closeableImage);
    if (drawable != null) {
      return drawable;
    }
    throw new UnsupportedOperationException("Unrecognized image class: " + closeableImage);
  }

  private Drawable maybeCreateDrawableFromFactories(
      @Nullable ImmutableList<DrawableFactory> drawableFactories,
      CloseableImage closeableImage) {
    if (drawableFactories == null) {
      return null;
    }
    for (DrawableFactory factory : drawableFactories) {
      if (factory.supportsImageType(closeableImage)) {
        Drawable drawable = factory.createDrawable(closeableImage);
        if (drawable != null) {
          return drawable;
        }
      }
    }
    return null;
  }

  @Override
  public void setHierarchy(@Nullable DraweeHierarchy hierarchy) {
    super.setHierarchy(hierarchy);
    maybeUpdateDebugOverlay(null);
  }

  private void maybeUpdateDebugOverlay(@Nullable CloseableImage image) {
    if (!mDrawDebugOverlay) {
      return;
    }
    Drawable controllerOverlay = getControllerOverlay();

    if (controllerOverlay == null) {
      controllerOverlay = new DebugControllerOverlayDrawable();
      setControllerOverlay(controllerOverlay);
    }

    if (controllerOverlay instanceof DebugControllerOverlayDrawable) {
      DebugControllerOverlayDrawable debugOverlay =
          (DebugControllerOverlayDrawable) controllerOverlay;
      debugOverlay.setControllerId(getId());

      final DraweeHierarchy draweeHierarchy = getHierarchy();
      ScaleType scaleType = null;
      if (draweeHierarchy != null) {
        final ScaleTypeDrawable scaleTypeDrawable =
            ScalingUtils.getActiveScaleTypeDrawable(draweeHierarchy.getTopLevelDrawable());
        scaleType = scaleTypeDrawable != null ? scaleTypeDrawable.getScaleType() : null;
      }
      debugOverlay.setScaleType(scaleType);

      if (image != null) {
        debugOverlay.setDimensions(image.getWidth(), image.getHeight());
        debugOverlay.setImageSize(image.getSizeInBytes());
      } else {
        debugOverlay.reset();
      }
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
}
