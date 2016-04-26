/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.factory;

import java.util.concurrent.ScheduledExecutorService;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.graphics.drawable.Drawable;

import com.facebook.common.time.MonotonicClock;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableCachingBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableDiagnostics;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableOptions;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableSupport;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableCachingBackendImplProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableDiagnosticsImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableDiagnosticsNoop;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;

/**
 * Factory for instances of {@link AnimatedDrawableSupport}.
 */
public class AnimatedDrawableFactoryImplSupport implements AnimatedDrawableFactory {

  private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private final AnimatedDrawableCachingBackendImplProvider mAnimatedDrawableCachingBackendProvider;
  private final AnimatedDrawableUtil mAnimatedDrawableUtil;
  private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;
  private final MonotonicClock mMonotonicClock;
  private final Resources mResources;

  public AnimatedDrawableFactoryImplSupport(
      AnimatedDrawableBackendProvider animatedDrawableBackendProvider,
      AnimatedDrawableCachingBackendImplProvider animatedDrawableCachingBackendProvider,
      AnimatedDrawableUtil animatedDrawableUtil,
      ScheduledExecutorService scheduledExecutorService,
      Resources resources) {
    mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
    mAnimatedDrawableCachingBackendProvider = animatedDrawableCachingBackendProvider;
    mAnimatedDrawableUtil = animatedDrawableUtil;
    mScheduledExecutorServiceForUiThread = scheduledExecutorService;
    mMonotonicClock = new MonotonicClock() {
      @Override
      public long now() {
        // Must be SystemClock.uptimeMillis to be compatible with what Android's View uses.
        return SystemClock.uptimeMillis();
      }
    };
    mResources = resources;
  }

  /**
   * Creates an {@link AnimatedDrawable} based on an {@link CloseableImage} which should be a
   * CloseableAnimatedImage.
   *
   * @param closeableImage The CloseableAnimatedImage to use for the AnimatedDrawable
   * @return a newly constructed {@link AnimatedDrawable}
   */
  @Override
  public Drawable create(CloseableImage closeableImage) {
    if (closeableImage instanceof CloseableAnimatedImage) {
      final AnimatedImageResult result = ((CloseableAnimatedImage) closeableImage).getImageResult();
      return create(result, AnimatedDrawableOptions.DEFAULTS);
    } else {
      throw new UnsupportedOperationException("Unrecognized image class: " + closeableImage);
    }
  }

  /**
   * Creates an {@link AnimatedDrawable} based on an {@link AnimatedImage}.
   *
   * @param animatedImageResult the result of the code
   * @param options additional options
   * @return a newly constructed {@link AnimatedDrawable}
   */
  private AnimatedDrawableSupport create(
      AnimatedImageResult animatedImageResult,
      AnimatedDrawableOptions options) {
    AnimatedImage animatedImage = animatedImageResult.getImage();
    Rect initialBounds = new Rect(0, 0, animatedImage.getWidth(), animatedImage.getHeight());
    AnimatedDrawableBackend animatedDrawableBackend =
        mAnimatedDrawableBackendProvider.get(animatedImageResult, initialBounds);
    return createAnimatedDrawable(options, animatedDrawableBackend);
  }

  private AnimatedImageResult getImageIfCloseableAnimatedImage(CloseableImage image) {
    if (image instanceof CloseableAnimatedImage) {
      return ((CloseableAnimatedImage) image).getImageResult();
    }
    return null;
  }

  private AnimatedDrawableSupport createAnimatedDrawable(
      AnimatedDrawableOptions options,
      AnimatedDrawableBackend animatedDrawableBackend) {
    DisplayMetrics displayMetrics = mResources.getDisplayMetrics();
    AnimatedDrawableDiagnostics animatedDrawableDiagnostics;
    AnimatedDrawableCachingBackend animatedDrawableCachingBackend =
        mAnimatedDrawableCachingBackendProvider.get(
            animatedDrawableBackend,
            options);
    if (options.enableDebugging) {
      animatedDrawableDiagnostics =
          new AnimatedDrawableDiagnosticsImpl(mAnimatedDrawableUtil, displayMetrics);
    } else {
      animatedDrawableDiagnostics = AnimatedDrawableDiagnosticsNoop.getInstance();
    }

    return new AnimatedDrawableSupport(
        mScheduledExecutorServiceForUiThread,
        animatedDrawableCachingBackend,
        animatedDrawableDiagnostics,
        mMonotonicClock);
  }
}
