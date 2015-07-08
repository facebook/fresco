/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.impl;

import java.util.Map;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.testing.FakeClock;
import org.robolectric.RobolectricTestRunner;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableOptions;
import com.facebook.imagepipeline.animated.testing.MyShadowBitmap;
import com.facebook.imagepipeline.animated.testing.MyShadowCanvas;
import com.facebook.imagepipeline.animated.testing.TestAnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.testing.TestExecutorService;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;

import bolts.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.facebook.imagepipeline.animated.testing.TestAnimatedDrawableBackend.pixelValue;
import static org.junit.Assert.*;

/**
 * Tests for {@link AnimatedDrawableCachingBackendImpl}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {MyShadowCanvas.class, MyShadowBitmap.class})
public class AnimatedDrawableCachingBackendImplTest {

  private static final int WIDTH = 200;
  private static final int HEIGHT = 100;
  private static final int[] FRAME_DURATIONS = new int[] { 60, 30, 15, 30, 60, 30, 45, 15, 30 };

  private FakeClock mFakeClock;
  private TestExecutorService mExecutorService;
  private ActivityManager mActivityManager;
  private AnimatedDrawableUtil mAnimatedDrawableUtil;
  private TestAnimatedDrawableBackend mDrawableBackend;
  private AnimatedDrawableCachingBackendImpl mCachingBackend;

  @Before
  public void setup() {
    mActivityManager =
        (ActivityManager) Robolectric.application.getSystemService(Context.ACTIVITY_SERVICE);
    mFakeClock = new FakeClock();
    mExecutorService = new TestExecutorService(mFakeClock);
    mAnimatedDrawableUtil = new AnimatedDrawableUtil();
    mDrawableBackend = new TestAnimatedDrawableBackend(WIDTH, HEIGHT, FRAME_DURATIONS);
  }

  @Test
  public void testForceImmediate() {
    mCachingBackend = makeCachingBackend(0 /* no caching */);

    // Spot check a pixel on frame 0
    CloseableReference<Bitmap> bitmap0 = mCachingBackend.getBitmapForFrameBlocking(0);
    assertNotNull(bitmap0);
    assertEquals(pixelValue(0, 10, 20), bitmap0.get().getPixel(10, 20));
  }

  @Test
  public void testAsyncFetches() {
    mCachingBackend = makeCachingBackend(0 /* no caching */);

    // Fetch frame with async API. Verify it returns null initially and put the decodes in flight.
    CloseableReference<Bitmap> bitmap0 = mCachingBackend.getBitmapForFrame(0);
    assertNull(bitmap0);
    assertDecodesInFlight(0, 1, 2);

    // Complete one command verify we now get the frame back.
    mExecutorService.getScheduledQueue().runNextPendingCommand();
    bitmap0 = mCachingBackend.getBitmapForFrame(0);
    assertNotNull(bitmap0);
    assertEquals(pixelValue(0, 10, 20), bitmap0.get().getPixel(10, 20));
    assertDecodesInFlight(1, 2);

    // Verify next frame returns null and puts one more decode in flight.
    CloseableReference<Bitmap> bitmap1 = mCachingBackend.getBitmapForFrame(1);
    assertNull(bitmap1);
    assertDecodesInFlight(1, 2, 3);

    // Make two more commands complete.
    mExecutorService.getScheduledQueue().runNextPendingCommand();
    mExecutorService.getScheduledQueue().runNextPendingCommand();
    assertDecodesInFlight(3);

    // Verify next two frames returns non-null and puts more decodes in flight.
    bitmap1 = mCachingBackend.getBitmapForFrame(1);
    assertNotNull(bitmap1);
    CloseableReference<Bitmap> bitmap2 = mCachingBackend.getBitmapForFrame(2);
    assertNotNull(bitmap2);
    assertDecodesInFlight(3, 4);

    // Skip to asking for frame
    CloseableReference<Bitmap> bitmap7 = mCachingBackend.getBitmapForFrame(7);
    assertNull(bitmap7);

    // Expect decodes in flight to change based on new current frame.
    assertDecodesInFlight(7, 8, 0);
    mExecutorService.getScheduledQueue().runNextPendingCommand(); // cancelled frame 3
    assertDecodesInFlight(7, 8, 0);
    mExecutorService.getScheduledQueue().runNextPendingCommand(); // cancelled frame 4
    assertDecodesInFlight(7, 8, 0);
    mExecutorService.getScheduledQueue().runNextPendingCommand(); // frame 7
    assertDecodesInFlight(8, 0);
    mExecutorService.getScheduledQueue().runNextPendingCommand(); // frame 8
    assertDecodesInFlight(0);
    mExecutorService.getScheduledQueue().runNextPendingCommand(); // frame 0
    assertDecodesInFlight();
  }

  @Test
  public void testFramesCachedInMemory() {
    mCachingBackend = makeCachingBackend(50 * 1024 * 1024); // Enough to cache all frames

    // Fetch frames and verify they stay cached.
    for (int i = 0; i < FRAME_DURATIONS.length; i++) {
      CloseableReference<Bitmap> bitmap = mCachingBackend.getBitmapForFrameBlocking(i);
      assertNotNull(bitmap);
      mExecutorService.getScheduledQueue().runUntilIdle();
    }
    assertDecodesInFlight();
    assertEquals(9, mCachingBackend.getFramesCached().size());
  }

  private void assertDecodesInFlight(int... frames) {
    assertTrue(mExecutorService.getScheduledQueue().getPendingCount() >= frames.length);
    Map<Integer, Task<?>> decodesInFlight =  mCachingBackend.getDecodesInFlight();
    for (int i = 0; i < frames.length; i++) {
      if (!decodesInFlight.containsKey(frames[i])) {
        fail("Expected " + frames[i] + " to be in flight");
      }
    }
    assertEquals(frames.length, decodesInFlight.size());
  }

  private AnimatedDrawableCachingBackendImpl makeCachingBackend(int maxBytes) {
    AnimatedDrawableOptions options = AnimatedDrawableOptions.newBuilder()
        .setMaximumBytes(maxBytes)
        .build();
    return new AnimatedDrawableCachingBackendImpl(
        mExecutorService,
        mActivityManager,
        mAnimatedDrawableUtil,
        mFakeClock,
        mDrawableBackend,
        options);
  }
}
