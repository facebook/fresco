/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.bitmap.cache;

import android.graphics.Bitmap;
import android.util.SparseArray;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.imagepipeline.animated.impl.AnimatedFrameCache;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imageutils.BitmapUtil;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Bitmap frame cache that uses Fresco's {@link AnimatedFrameCache} to cache frames.
 */
public class FrescoFrameCache implements BitmapFrameCache {

  private static final Class<?> TAG = FrescoFrameCache.class;

  private final AnimatedFrameCache mAnimatedFrameCache;
  private final boolean mEnableBitmapReusing;
  @GuardedBy("this")
  private final SparseArray<CloseableReference<CloseableImage>> mPreparedPendingFrames;

  @GuardedBy("this")
  @Nullable
  private CloseableReference<CloseableImage> mLastRenderedItem;

  public FrescoFrameCache(AnimatedFrameCache animatedFrameCache, boolean enableBitmapReusing) {
    mAnimatedFrameCache = animatedFrameCache;
    mEnableBitmapReusing = enableBitmapReusing;
    mPreparedPendingFrames = new SparseArray<>();
  }

  @Nullable
  @Override
  public synchronized CloseableReference<Bitmap> getCachedFrame(int frameNumber) {
    return convertToBitmapReferenceAndClose(mAnimatedFrameCache.get(frameNumber));
  }

  @Nullable
  @Override
  public synchronized CloseableReference<Bitmap> getFallbackFrame(int frameNumber) {
    return convertToBitmapReferenceAndClose(CloseableReference.cloneOrNull(mLastRenderedItem));
  }

  @Nullable
  @Override
  public synchronized CloseableReference<Bitmap> getBitmapToReuseForFrame(
      int frameNumber,
      int width,
      int height) {
    if (!mEnableBitmapReusing) {
      return null;
    }
    return convertToBitmapReferenceAndClose(mAnimatedFrameCache.getForReuse());
  }

  @Override
  public synchronized boolean contains(int frameNumber) {
    return mAnimatedFrameCache.contains(frameNumber);
  }

  @Override
  public synchronized int getSizeInBytes() {
    // This currently does not include the size of the animated frame cache
    return getBitmapSizeBytes(mLastRenderedItem) + getPreparedPendingFramesSizeBytes();
  }

  @Override
  public synchronized void clear() {
    CloseableReference.closeSafely(mLastRenderedItem);
    mLastRenderedItem = null;
    for (int i = 0; i < mPreparedPendingFrames.size(); i++) {
      CloseableReference.closeSafely(mPreparedPendingFrames.valueAt(i));
    }
    mPreparedPendingFrames.clear();
    // The frame cache will free items when needed
  }

  @Override
  public synchronized void onFrameRendered(
      int frameNumber,
      CloseableReference<Bitmap> bitmapReference,
      @BitmapAnimationBackend.FrameType int frameType) {
    Preconditions.checkNotNull(bitmapReference);

    // Close up prepared references.
    removePreparedReference(frameNumber);

    // Create the new image reference and cache it.
    CloseableReference<CloseableImage> closableReference = null;
    try {
      closableReference = createImageReference(bitmapReference);
      if (closableReference != null) {
        CloseableReference.closeSafely(mLastRenderedItem);
        mLastRenderedItem = mAnimatedFrameCache.cache(frameNumber, closableReference);
      }
    } finally {
      CloseableReference.closeSafely(closableReference);
    }
  }

  @Override
  public synchronized void onFramePrepared(
      int frameNumber,
      CloseableReference<Bitmap> bitmapReference,
      @BitmapAnimationBackend.FrameType int frameType) {
    Preconditions.checkNotNull(bitmapReference);
    CloseableReference<CloseableImage> closableReference = null;
    try {
      closableReference = createImageReference(bitmapReference);
      if (closableReference == null) {
        return;
      }
      CloseableReference<CloseableImage> newReference =
          mAnimatedFrameCache.cache(frameNumber, closableReference);
      if (CloseableReference.isValid(newReference)) {
        CloseableReference<CloseableImage> oldReference = mPreparedPendingFrames.get(frameNumber);
        CloseableReference.closeSafely(oldReference);
        // For performance reasons, we don't clone the reference and close the original one
        // but cache the reference directly.
        mPreparedPendingFrames.put(frameNumber, newReference);
        FLog.v(
            TAG,
            "cachePreparedFrame(%d) cached. Pending frames: %s",
            frameNumber,
            mPreparedPendingFrames);
      }
    } finally {
      CloseableReference.closeSafely(closableReference);
    }
  }

  @Override
  public void setFrameCacheListener(BitmapFrameCache.FrameCacheListener frameCacheListener) {
    // TODO (t15557326) Not supported for now
  }

  private synchronized int getPreparedPendingFramesSizeBytes() {
    int size = 0;
    for (int i = 0; i < mPreparedPendingFrames.size(); i++) {
      size += getBitmapSizeBytes(mPreparedPendingFrames.valueAt(i));
    }
    return size;
  }

  private synchronized void removePreparedReference(int frameNumber) {
    CloseableReference<CloseableImage> existingPendingReference =
        mPreparedPendingFrames.get(frameNumber);
    if (existingPendingReference != null) {
      mPreparedPendingFrames.delete(frameNumber);
      CloseableReference.closeSafely(existingPendingReference);
      FLog.v(
          TAG,
          "removePreparedReference(%d) removed. Pending frames: %s",
          frameNumber,
          mPreparedPendingFrames);
    }
  }

  /**
   * Converts the given image reference to a bitmap reference
   * and closes the original image reference.
   *
   * @param closeableImage the image to convert. It will be closed afterwards and will be invalid
   * @return the closeable bitmap reference to be used
   */
  @VisibleForTesting
  @Nullable
  static CloseableReference<Bitmap> convertToBitmapReferenceAndClose(
      final @Nullable CloseableReference<CloseableImage> closeableImage) {
    try {
      if (CloseableReference.isValid(closeableImage) &&
          closeableImage.get() instanceof CloseableStaticBitmap) {

        CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) closeableImage.get();
        if (closeableStaticBitmap != null) {
          // We return a clone of the underlying bitmap reference that has to be manually closed
          // and then close the passed CloseableStaticBitmap in order to preserve correct
          // cache size calculations.
          return closeableStaticBitmap.cloneUnderlyingBitmapReference();
        }
      }
      // Not a bitmap reference, so we return null
      return null;
    } finally {
      CloseableReference.closeSafely(closeableImage);
    }
  }

  private static int getBitmapSizeBytes(
      @Nullable CloseableReference<CloseableImage> imageReference) {
    if (!CloseableReference.isValid(imageReference)) {
      return 0;
    }
    return getBitmapSizeBytes(imageReference.get());
  }

  private static int getBitmapSizeBytes(@Nullable CloseableImage image) {
    if (!(image instanceof CloseableBitmap)) {
      return 0;
    }
    return BitmapUtil.getSizeInBytes(((CloseableBitmap) image).getUnderlyingBitmap());
  }

  @Nullable
  private static CloseableReference<CloseableImage> createImageReference(
      CloseableReference<Bitmap> bitmapReference) {
    // The given CloseableStaticBitmap will be cached and then released by the resource releaser
    // of the closeable reference
    CloseableImage closeableImage =
        new CloseableStaticBitmap(bitmapReference, ImmutableQualityInfo.FULL_QUALITY, 0);
    return CloseableReference.of(closeableImage);
  }
}
