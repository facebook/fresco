/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.fresco.animation.factory;


import java.util.concurrent.ScheduledExecutorService;

import android.graphics.Rect;

import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.DrawableFactory;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.fresco.animation.wrapper.AnimatedDrawableCachingBackendWrapper;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableCachingBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableOptions;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableCachingBackendImplProvider;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;

/**
 * Animation factory for {@link com.facebook.fresco.animation.drawable.AnimatedDrawable2}.
 *
 * This basically mimics the backend creation of
 * {@link com.facebook.imagepipeline.animated.factory.AnimatedDrawableFactoryImpl}.
 */
public class ExperimentalAnimationFactory implements DrawableFactory {

  private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private final AnimatedDrawableCachingBackendImplProvider mAnimatedDrawableCachingBackendProvider;
  private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;
  private final MonotonicClock mMonotonicClock;

  public ExperimentalAnimationFactory(
      AnimatedDrawableBackendProvider animatedDrawableBackendProvider,
      AnimatedDrawableCachingBackendImplProvider animatedDrawableCachingBackendProvider,
      ScheduledExecutorService scheduledExecutorServiceForUiThread,
      MonotonicClock monotonicClock) {
    mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
    mAnimatedDrawableCachingBackendProvider = animatedDrawableCachingBackendProvider;
    mScheduledExecutorServiceForUiThread = scheduledExecutorServiceForUiThread;
    mMonotonicClock = monotonicClock;
  }

  @Override
  public boolean supportsImageType(CloseableImage image) {
    return image instanceof CloseableAnimatedImage;
  }

  @Override
  public AnimatedDrawable2 createDrawable(CloseableImage image) {
    return new AnimatedDrawable2(
        createAnimationBackend(
            ((CloseableAnimatedImage) image).getImageResult()));
  }

  private AnimationBackend createAnimationBackend(AnimatedImageResult animatedImageResult) {
    // Create the animated drawable backend
    AnimatedImage animatedImage = animatedImageResult.getImage();
    Rect initialBounds = new Rect(0, 0, animatedImage.getWidth(), animatedImage.getHeight());
    AnimatedDrawableBackend animatedDrawableBackend =
        mAnimatedDrawableBackendProvider.get(animatedImageResult, initialBounds);

    // Add caching backend
    AnimatedDrawableCachingBackend animatedDrawableCachingBackend =
        mAnimatedDrawableCachingBackendProvider.get(
            animatedDrawableBackend,
            AnimatedDrawableOptions.DEFAULTS);
    AnimatedDrawableCachingBackendWrapper animatedDrawableCachingBackendWrapper =
        new AnimatedDrawableCachingBackendWrapper(animatedDrawableCachingBackend);

    // Add inactivity check
    return AnimationBackendDelegateWithInactivityCheck.createForBackend(
        animatedDrawableCachingBackendWrapper,
        mMonotonicClock,
        mScheduledExecutorServiceForUiThread);
  }
}
