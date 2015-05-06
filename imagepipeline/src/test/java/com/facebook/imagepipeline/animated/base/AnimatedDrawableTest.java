/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.common.testing.FakeClock;
import org.robolectric.RobolectricTestRunner;
import com.facebook.imagepipeline.animated.testing.MyShadowBitmap;
import com.facebook.imagepipeline.animated.testing.MyShadowCanvas;
import com.facebook.imagepipeline.animated.testing.TestAnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableDiagnosticsNoop;
import com.facebook.imagepipeline.animated.testing.TestScheduledExecutorService;

import com.nineoldandroids.animation.ValueAnimator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.facebook.imagepipeline.animated.testing.TestAnimatedDrawableBackend.pixelValue;
import static org.junit.Assert.*;

/**
 * Tests for {@link AnimatedDrawable}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {MyShadowCanvas.class, MyShadowBitmap.class})
public class AnimatedDrawableTest {

  private static final int WIDTH = 200;
  private static final int HEIGHT = 100;
  private static final int[] FRAME_DURATIONS = new int[]{ 60, 30, 15, 30, 60 };
  private static final int[] FRAME_DURATIONS_LONG = new int[]{ 10000, 10000 };

  private FakeClock mFakeClock;
  private TestScheduledExecutorService mTestScheduledExecutorService;
  private TestAnimatedDrawableBackend mBackend;
  private TestAnimatedDrawableCachingBackend mCachingBackend;
  private MyCallback mCallback;
  private AnimatedDrawable mDrawable;

  @Before
  public void setup() {
    mFakeClock = new FakeClock();
    mTestScheduledExecutorService = new TestScheduledExecutorService(mFakeClock);
    mBackend = new TestAnimatedDrawableBackend(WIDTH, HEIGHT, FRAME_DURATIONS);
    mCachingBackend = new TestAnimatedDrawableCachingBackend(mBackend);
    mCallback = new MyCallback(mFakeClock);
    mDrawable = new AnimatedDrawable(
        mTestScheduledExecutorService,
        mCachingBackend,
        AnimatedDrawableDiagnosticsNoop.getInstance(),
        mFakeClock);
    mDrawable.setCallback(mCallback);
  }

  @Test
  public void testIntrinsicDimensions() {
    assertEquals(WIDTH, mDrawable.getIntrinsicWidth());
    assertEquals(HEIGHT, mDrawable.getIntrinsicHeight());
  }

  @Test
  public void testValueAnimator() {
    ValueAnimator valueAnimator = mDrawable.createValueAnimator();
    assertEquals(mBackend.getDurationMs(), valueAnimator.getDuration());
    assertEquals(ValueAnimator.INFINITE, valueAnimator.getRepeatCount());
  }

  @Test
  public void testScheduling() {
    Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    prepareDrawable();

    // Spot check a pixel. Should be frame 0.
    mDrawable.draw(canvas);
    assertEquals(pixelValue(0, 10, 20), bitmap.getPixel(10, 20));

    // Advance just before next frame.
    mFakeClock.incrementBy(FRAME_DURATIONS[0] - 1);
    mDrawable.draw(canvas);
    assertEquals(pixelValue(0, 10, 20), bitmap.getPixel(10, 20));

    // Advance just to second frame.
    mFakeClock.incrementBy(1);
    mDrawable.draw(canvas);
    assertEquals(pixelValue(1, 10, 20), bitmap.getPixel(10, 20));

    // Advance to the last millisecond of the last frame.
    mFakeClock.incrementBy(FRAME_DURATIONS[1]);
    mFakeClock.incrementBy(FRAME_DURATIONS[2]);
    mFakeClock.incrementBy(FRAME_DURATIONS[3]);
    mFakeClock.incrementBy(FRAME_DURATIONS[4] - 1);
    mDrawable.draw(canvas);
    assertEquals(pixelValue(4, 10, 20), bitmap.getPixel(10, 20));

    // Make sure wrapping works.
    mFakeClock.incrementBy(1);
    mDrawable.draw(canvas);
    assertEquals(pixelValue(0, 10, 20), bitmap.getPixel(10, 20));
    assertEquals(1, mBackend.getDropCachesCallCount());
  }

  @Test
  public void testDropCachesAfterDrawTimeout() {
    Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    prepareDrawable();

    // Draw and advance to next frame.
    mDrawable.draw(canvas);

    // Make sure caches are dropped after couple seconds without a draw call.
    assertFalse(mDrawable.isWaitingForDraw());
    mFakeClock.incrementBy(60);
    assertTrue(mDrawable.isWaitingForDraw());
    mFakeClock.incrementBy(1940);
    assertEquals(2, mBackend.getDropCachesCallCount());
  }

  @Test
  public void testDropCachesAfterNextFrameTimeout() {
    Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    prepareDrawable();

    // Draw and advance to next frame.
    mDrawable.draw(canvas);

    // Make sure caches are dropped after couple seconds.
    assertFalse(mDrawable.isWaitingForDraw());
    assertTrue(mDrawable.isWaitingForNextFrame());
    mCallback.setDropCallbacks(true);
    mFakeClock.incrementBy(2000);
    assertEquals(2, mBackend.getDropCachesCallCount());
  }

  @Test
  public void testDoNotDropCacheIfFramesAreLongDurationSpecialCase() {
    mBackend = new TestAnimatedDrawableBackend(WIDTH, HEIGHT, FRAME_DURATIONS_LONG);
    mCachingBackend = new TestAnimatedDrawableCachingBackend(mBackend);
    mCallback = new MyCallback(mFakeClock);
    mDrawable = new AnimatedDrawable(
        mTestScheduledExecutorService,
        mCachingBackend,
        AnimatedDrawableDiagnosticsNoop.getInstance(),
        mFakeClock);
    mDrawable.setCallback(mCallback);

    Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    prepareDrawable();

    // Draw and advance to next frame.
    mDrawable.draw(canvas);

    // After 10 seconds, it should move to the next frame. It shouldn't drop the caches though
    // until it's 2 seconds after the invalidate.
    assertFalse(mDrawable.isWaitingForDraw());
    assertTrue(mDrawable.isWaitingForNextFrame());
    assertEquals(0, mDrawable.getScheduledFrameNumber());
    mFakeClock.incrementBy(10000);
    assertTrue(mDrawable.isWaitingForDraw());
    assertFalse(mDrawable.isWaitingForNextFrame());
    assertEquals(1, mDrawable.getScheduledFrameNumber());
    assertEquals(1, mBackend.getDropCachesCallCount());
    mFakeClock.incrementBy(1000);
    assertEquals(1, mBackend.getDropCachesCallCount());
    mFakeClock.incrementBy(2000);
    assertEquals(2, mBackend.getDropCachesCallCount());
  }

  private void prepareDrawable() {
    mDrawable.start();
    mFakeClock.incrementBy(0); // Just to trigger the callbacks to run.
    mDrawable.setBounds(0, 0, 200, 100);
    assertEquals(1, mBackend.getDropCachesCallCount());
  }

  private static class ScheduledRunnable {

    final Runnable runnable;
    final long when;

    private ScheduledRunnable(Runnable runnable, long when) {
      this.runnable = runnable;
      this.when = when;
    }
  }

  private static class TestAnimatedDrawableCachingBackend extends DelegatingAnimatedDrawableBackend
      implements AnimatedDrawableCachingBackend {

    public TestAnimatedDrawableCachingBackend(AnimatedDrawableBackend animatedDrawableBackend) {
      super(animatedDrawableBackend);
    }

    @Override
    public CloseableReference<Bitmap> getBitmapForFrame(int frameNumber) {
      Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
      getDelegate().renderFrame(frameNumber, new Canvas(bitmap));
      return CloseableReference.of(
          bitmap,
          new ResourceReleaser<Bitmap>() {
            @Override
            public void release(Bitmap value) {

            }
          });
    }

    @Override
    public CloseableReference<Bitmap> getPreviewBitmap() {
      return null;
    }

    @Override
    public CloseableReference<Bitmap> getPreDecodedFrame(int frameNumber) {
      return null;
    }

    @Override
    public void appendDebugOptionString(StringBuilder sb) {
    }

    @Override
    public AnimatedDrawableCachingBackend forNewBounds(Rect bounds) {
      return this;
    }
  }

  private static class MyCallback implements Drawable.Callback {

    private final FakeClock mFakeClock;
    private final List<ScheduledRunnable> mScheduledRunnables = new ArrayList<>();
    private boolean mDropCallbacks;

    MyCallback(FakeClock fakeClock) {
      mFakeClock = fakeClock;
      mFakeClock.addListener(
          new FakeClock.OnTickListener() {
            @Override
            public void onTick() {
              runReadyTasks();
            }
          });
    }

    @Override
    public void invalidateDrawable(Drawable who) {

    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
      mScheduledRunnables.add(new ScheduledRunnable(what, when));
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
      Iterator<ScheduledRunnable> iterator = mScheduledRunnables.iterator();
      while (iterator.hasNext()) {
        ScheduledRunnable next = iterator.next();
        if (next.runnable == what) {
          iterator.remove();
        }
      }
    }

    void runReadyTasks() {
      List<Runnable> toRun = new ArrayList<>();
      long now = mFakeClock.now();
      Iterator<ScheduledRunnable> iterator = mScheduledRunnables.iterator();
      while (iterator.hasNext()) {
        ScheduledRunnable next = iterator.next();
        if (next.when <= now) {
          iterator.remove();
          toRun.add(next.runnable);
        }
      }
      if (!mDropCallbacks) {
        for (Runnable runnable : toRun) {
          runnable.run();
        }
      }
    }

    void setDropCallbacks(boolean dropCallbacks) {
      mDropCallbacks = dropCallbacks;
    }
  }
}
