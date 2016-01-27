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

import com.facebook.common.time.MonotonicClock;
import com.facebook.imagepipeline.animated.base.AnimatedDrawable;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableCachingBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableDiagnostics;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableOptions;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableCachingBackendImplProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableDiagnosticsImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableDiagnosticsNoop;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;


/**
 * Factory for instances of {@link AnimatedDrawable}.
 */
public class AnimatedDrawableFactory {

  private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private final AnimatedDrawableCachingBackendImplProvider mAnimatedDrawableCachingBackendProvider;
  private final AnimatedDrawableUtil mAnimatedDrawableUtil;
  private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;
  private final MonotonicClock mMonotonicClock;
  private final Resources mResources;

  public AnimatedDrawableFactory(
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
   * Creates an {@link AnimatedDrawable} based on an {@link AnimatedImage}.
   *
   * @param animatedImageResult the result of the code
   * @return a newly constructed {@link AnimatedDrawable}
   */
  public AnimatedDrawable create(AnimatedImageResult animatedImageResult) {
    return create(animatedImageResult, AnimatedDrawableOptions.DEFAULTS);
  }

  /**
   * Creates an {@link AnimatedDrawable} based on an {@link AnimatedImage}.
   *
   * @param animatedImageResult the result of the code
   * @param options additional options
   * @return a newly constructed {@link AnimatedDrawable}
   */
  public AnimatedDrawable create(
      AnimatedImageResult animatedImageResult,
      AnimatedDrawableOptions options) {
    AnimatedImage animatedImage = animatedImageResult.getImage();
    Rect initialBounds = new Rect(0, 0, animatedImage.getWidth(), animatedImage.getHeight());
    AnimatedDrawableBackend animatedDrawableBackend =
        mAnimatedDrawableBackendProvider.get(animatedImageResult, initialBounds);
    return createAnimatedDrawable(options, animatedDrawableBackend);
  }

  private AnimatedDrawable createAnimatedDrawable(
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

    return new AnimatedDrawable(
        mScheduledExecutorServiceForUiThread,
        animatedDrawableCachingBackend,
        animatedDrawableDiagnostics,
        mMonotonicClock);
  }
}
