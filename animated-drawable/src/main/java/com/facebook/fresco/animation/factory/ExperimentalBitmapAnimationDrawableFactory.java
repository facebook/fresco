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
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.bitmap.cache.NoOpCache;
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendAnimationInformation;
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendFrameRenderer;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;

/**
 * Animation factory for {@link AnimatedDrawable2}.
 *
 * This factory is similar to {@link ExperimentalAnimationFactory} but it wraps around the new
 * {@link BitmapAnimationBackend} and does not rely on
 * {@link com.facebook.imagepipeline.animated.base.AnimatedDrawableCachingBackend}
 * to do the caching.
 */
public class ExperimentalBitmapAnimationDrawableFactory implements DrawableFactory {

  private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;
  private final MonotonicClock mMonotonicClock;
  private final PlatformBitmapFactory mPlatformBitmapFactory;

  public ExperimentalBitmapAnimationDrawableFactory(
      AnimatedDrawableBackendProvider animatedDrawableBackendProvider,
      ScheduledExecutorService scheduledExecutorServiceForUiThread,
      MonotonicClock monotonicClock,
      PlatformBitmapFactory platformBitmapFactory) {
    mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
    mScheduledExecutorServiceForUiThread = scheduledExecutorServiceForUiThread;
    mMonotonicClock = monotonicClock;
    mPlatformBitmapFactory = platformBitmapFactory;
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
    AnimatedDrawableBackend animatedDrawableBackend =
        createAnimatedDrawableBackend(animatedImageResult);

    BitmapAnimationBackend bitmapAnimationBackend = new BitmapAnimationBackend(
        mPlatformBitmapFactory,
        createBitmapFrameCache(),
        new AnimatedDrawableBackendAnimationInformation(animatedDrawableBackend),
        new AnimatedDrawableBackendFrameRenderer(animatedDrawableBackend));

    return AnimationBackendDelegateWithInactivityCheck.createForBackend(
        bitmapAnimationBackend,
        mMonotonicClock,
        mScheduledExecutorServiceForUiThread);
  }

  private AnimatedDrawableBackend createAnimatedDrawableBackend(
      AnimatedImageResult animatedImageResult) {
    AnimatedImage animatedImage = animatedImageResult.getImage();
    Rect initialBounds = new Rect(0, 0, animatedImage.getWidth(), animatedImage.getHeight());
    return mAnimatedDrawableBackendProvider.get(animatedImageResult, initialBounds);
  }

  private BitmapFrameCache createBitmapFrameCache() {
    // No caching for now
    return new NoOpCache();
  }
}
