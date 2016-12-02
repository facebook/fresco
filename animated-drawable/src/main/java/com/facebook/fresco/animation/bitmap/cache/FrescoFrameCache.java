/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.bitmap.cache;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.imagepipeline.animated.impl.AnimatedFrameCache;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imageutils.BitmapUtil;

/**
 * Bitmap frame cache that uses Fresco's {@link AnimatedFrameCache} to cache frames.
 */
public class FrescoFrameCache implements BitmapFrameCache {

  private final AnimatedFrameCache mAnimatedFrameCache;
  private final boolean mEnableBitmapReusing;

  @GuardedBy("this")
  @Nullable
  private CloseableReference<CloseableImage> mLastCachedItem;

  public FrescoFrameCache(AnimatedFrameCache animatedFrameCache, boolean enableBitmapReusing) {
    mAnimatedFrameCache = animatedFrameCache;
    mEnableBitmapReusing = enableBitmapReusing;
  }

  @Nullable
  @Override
  public synchronized CloseableReference<Bitmap> getCachedFrame(int frameNumber) {
    return extractAndClose(mAnimatedFrameCache.get(frameNumber));
  }

  @Nullable
  @Override
  public synchronized CloseableReference<Bitmap> getFallbackFrame(int frameNumber) {
    return extractAndClose(CloseableReference.cloneOrNull(mLastCachedItem));
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
    return extractAndClose(mAnimatedFrameCache.getForReuse());
  }

  @Override
  public synchronized int getSizeInBytes() {
    // This currently does not include the size of the frame cache
    return getBitmapSizeBytes(mLastCachedItem);
  }

  @Override
  public synchronized void clear() {
    CloseableReference.closeSafely(mLastCachedItem);
    mLastCachedItem = null;
    // The frame cache will free items when needed
  }

  @Override
  public synchronized void onFrameRendered(
      int frameNumber,
      CloseableReference<Bitmap> bitmapReference,
      @BitmapAnimationBackend.FrameType int frameType) {
    Preconditions.checkNotNull(bitmapReference);
    CloseableReference<CloseableImage> closableReference = null;
    try {
      // The given CloseableStaticBitmap will be cached and then released by the resource releaser
      // of the closeable reference
      CloseableImage closeableImage = new CloseableStaticBitmap(
          bitmapReference,
          ImmutableQualityInfo.FULL_QUALITY,
          0);
      closableReference = CloseableReference.of(closeableImage);
      CloseableReference.closeSafely(mLastCachedItem);
      mLastCachedItem =
          mAnimatedFrameCache.cache(frameNumber, closableReference);
    } finally {
      CloseableReference.closeSafely(closableReference);
    }
  }

  @Nullable
  private static CloseableReference<Bitmap> extractAndClose(
      @Nullable CloseableReference<CloseableImage> closeableImage) {
    try {
      if (CloseableReference.isValid(closeableImage) &&
          closeableImage.get() instanceof CloseableStaticBitmap) {
        return ((CloseableStaticBitmap) closeableImage.get()).convertToBitmapReference();
      }
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
}
