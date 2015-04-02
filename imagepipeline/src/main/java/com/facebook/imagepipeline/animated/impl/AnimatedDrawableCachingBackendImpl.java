/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.impl;

import javax.annotation.concurrent.GuardedBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.support.v4.util.SparseArrayCompat;

import com.facebook.common.executors.SerialExecutorService;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.common.time.MonotonicClock;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableCachingBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo.DisposalMethod;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableOptions;
import com.facebook.imagepipeline.animated.base.DelegatingAnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;

import bolts.Continuation;
import bolts.Task;

/**
 * A caching and prefetching layer that delegates to a {@link AnimatedDrawableBackend}.
 */
public class AnimatedDrawableCachingBackendImpl extends DelegatingAnimatedDrawableBackend
    implements AnimatedDrawableCachingBackend {

  private static final Class<?> TAG = AnimatedDrawableCachingBackendImpl.class;

  private static final AtomicInteger sTotalBitmaps = new AtomicInteger();

  private static final int PREFETCH_FRAMES = 3;

  private final SerialExecutorService mExecutorService;
  private final AnimatedDrawableUtil mAnimatedDrawableUtil;
  private final ActivityManager mActivityManager;
  private final MonotonicClock mMonotonicClock;
  private final AnimatedDrawableBackend mAnimatedDrawableBackend;
  private final AnimatedDrawableOptions mAnimatedDrawableOptions;
  private final AnimatedImageCompositor mAnimatedImageCompositor;
  private final ResourceReleaser<Bitmap> mResourceReleaserForBitmaps;
  private final int mMaximumBytes;

  private final int mApproxBytesToHoldAllFrames;

  @GuardedBy("this")
  private final List<Bitmap> mFreeBitmaps;

  @GuardedBy("this")
  private final SparseArrayCompat<Task<Object>> mDecodesInFlight;

  @GuardedBy("this")
  private final SparseArrayCompat<CloseableReference<Bitmap>> mCachedBitmaps;

  @GuardedBy("this")
  private final WhatToKeepCachedArray mBitmapsToKeepCached;

  @GuardedBy("ui-thread")
  private int mCurrentFrameIndex;

  public AnimatedDrawableCachingBackendImpl(
      SerialExecutorService executorService,
      ActivityManager activityManager,
      AnimatedDrawableUtil animatedDrawableUtil,
      MonotonicClock monotonicClock,
      AnimatedDrawableBackend animatedDrawableBackend,
      AnimatedDrawableOptions options) {
    super(animatedDrawableBackend);
    mExecutorService = executorService;
    mActivityManager = activityManager;
    mAnimatedDrawableUtil = animatedDrawableUtil;
    mMonotonicClock = monotonicClock;
    mAnimatedDrawableBackend = animatedDrawableBackend;
    mAnimatedDrawableOptions = options;
    mMaximumBytes = options.maximumBytes >= 0 ?
        options.maximumBytes : getDefaultMaxBytes(activityManager);
    mAnimatedImageCompositor = new AnimatedImageCompositor(
        animatedDrawableBackend,
        new AnimatedImageCompositor.Callback() {
          @Override
          public void onIntermediateResult(int frameNumber, Bitmap bitmap) {
            maybeCacheBitmapDuringRender(frameNumber, bitmap);
          }

          @Override
          public CloseableReference<Bitmap> getCachedBitmap(int frameNumber) {
            return getCachedOrPredecodedFrame(frameNumber);
          }
        });
    mResourceReleaserForBitmaps = new ResourceReleaser<Bitmap>() {
      @Override
      public void release(Bitmap value) {
        releaseBitmapInternal(value);
      }
    };
    mFreeBitmaps = new ArrayList<Bitmap>();
    mDecodesInFlight = new SparseArrayCompat<Task<Object>>(10);
    mCachedBitmaps = new SparseArrayCompat<CloseableReference<Bitmap>>(10);
    mBitmapsToKeepCached = new WhatToKeepCachedArray(mAnimatedDrawableBackend.getFrameCount());
    mApproxBytesToHoldAllFrames =
        mAnimatedDrawableBackend.getFrameCount() *
            mAnimatedDrawableBackend.getRenderedWidth() *
            mAnimatedDrawableBackend.getRenderedHeight() * 4;
  }

  @Override
  protected synchronized void finalize() throws Throwable {
    super.finalize();
    if (mCachedBitmaps.size() > 0) {
      FLog.d(TAG, "Finalizing with rendered bitmaps");
    }
    sTotalBitmaps.addAndGet(-mFreeBitmaps.size());
    mFreeBitmaps.clear();
  }

  private Bitmap createNewBitmap() {
    FLog.v(TAG, "Creating new bitmap");
    sTotalBitmaps.incrementAndGet();
    FLog.v(TAG, "Total bitmaps: %d", sTotalBitmaps.get());
    return Bitmap.createBitmap(
        mAnimatedDrawableBackend.getRenderedWidth(),
        mAnimatedDrawableBackend.getRenderedHeight(),
        Bitmap.Config.ARGB_8888);
  }

  @Override
  public void renderFrame(int frameNumber, Canvas canvas) {
    // renderFrame method should not be called on cache.
    throw new IllegalStateException();
  }

  @Override
  public CloseableReference<Bitmap> getBitmapForFrame(int frameNumber) {
    mCurrentFrameIndex = frameNumber;
    CloseableReference<Bitmap> result = getBitmapForFrameInternal(frameNumber, false);
    schedulePrefetches();
    return result;
  }

  @Override
  public CloseableReference<Bitmap> getPreviewBitmap() {
    return getAnimatedImageResult().getPreviewBitmap();
  }

  @VisibleForTesting
  CloseableReference<Bitmap> getBitmapForFrameBlocking(int frameNumber) {
    mCurrentFrameIndex = frameNumber;
    CloseableReference<Bitmap> result = getBitmapForFrameInternal(frameNumber, true);
    schedulePrefetches();
    return result;
  }

  @Override
  public AnimatedDrawableCachingBackend forNewBounds(Rect bounds) {
    AnimatedDrawableBackend newBackend = mAnimatedDrawableBackend.forNewBounds(bounds);
    if (newBackend == mAnimatedDrawableBackend) {
      return this;
    }
    return new AnimatedDrawableCachingBackendImpl(
        mExecutorService,
        mActivityManager,
        mAnimatedDrawableUtil,
        mMonotonicClock,
        newBackend,
        mAnimatedDrawableOptions);
  }

  @Override
  public synchronized void dropCaches() {
    mBitmapsToKeepCached.setAll(false);
    dropBitmapsThatShouldNotBeCached();
    for (Bitmap freeBitmap : mFreeBitmaps) {
      freeBitmap.recycle();
      sTotalBitmaps.decrementAndGet();
    }
    mFreeBitmaps.clear();
    mAnimatedDrawableBackend.dropCaches();
    FLog.v(TAG, "Total bitmaps: %d", sTotalBitmaps.get());
  }

  @Override
  public int getMemoryUsage() {
    int bytes = 0;
    synchronized (this) {
      for (Bitmap bitmap : mFreeBitmaps) {
        bytes += mAnimatedDrawableUtil.getSizeOfBitmap(bitmap);
      }
      for (int i = 0; i < mCachedBitmaps.size(); i++) {
        CloseableReference<Bitmap> bitmapReference = mCachedBitmaps.valueAt(i);
        bytes += mAnimatedDrawableUtil.getSizeOfBitmap(bitmapReference.get());
      }
    }
    bytes += mAnimatedDrawableBackend.getMemoryUsage();
    return bytes;
  }

  @Override
  public void appendDebugOptionString(StringBuilder sb) {
    if (mAnimatedDrawableOptions.forceKeepAllFramesInMemory) {
      sb.append("Pinned To Memory");
    } else {
      if (mApproxBytesToHoldAllFrames < mMaximumBytes) {
        sb.append("within ");
      } else {
        sb.append("exceeds ");
      }
      mAnimatedDrawableUtil.appendMemoryString(sb, mMaximumBytes);
    }
    if (shouldKeepAllFramesInMemory() && mAnimatedDrawableOptions.allowPrefetching) {
      sb.append(" MT");
    }
  }

  private CloseableReference<Bitmap> getBitmapForFrameInternal(
      int frameNumber,
      boolean forceImmediate) {
    boolean renderedOnCallingThread = false;
    boolean deferred = false;
    long startMs = mMonotonicClock.now();
    try {
      synchronized (this) {
        mBitmapsToKeepCached.set(frameNumber, true);
        CloseableReference<Bitmap> bitmapReference = getCachedOrPredecodedFrame(frameNumber);
        if (bitmapReference != null) {
          return bitmapReference;
        }
      }

      if (forceImmediate) {
        // Give up and try to do it on the calling thread.
        renderedOnCallingThread = true;
        CloseableReference<Bitmap> bitmapReference = obtainBitmapInternal();
        try {
          mAnimatedImageCompositor.renderFrame(frameNumber, bitmapReference.get());
          maybeCacheRenderedBitmap(frameNumber, bitmapReference);
          return bitmapReference.clone();
        } finally {
          bitmapReference.close();
        }
      }
      deferred = true;
      return null;
    } finally {
      long elapsedMs = mMonotonicClock.now() - startMs;
      if (elapsedMs > 10) {
        String comment = "";
        if (renderedOnCallingThread) {
          comment = "renderedOnCallingThread";
        } else if (deferred) {
          comment = "deferred";
        } else {
          comment = "ok";
        }
        FLog.v(TAG, "obtainBitmap for frame %d took %d ms (%s)", frameNumber, elapsedMs, comment);
      }
    }
  }

  /**
   * Called while rendering intermediate frames into the bitmap. If this is a frame we want cached,
   * we'll copy it and cache it.
   *
   * @param frameNumber the index of the frame
   * @param bitmap the rendered bitmap for that frame
   */
  private void maybeCacheBitmapDuringRender(int frameNumber, Bitmap bitmap) {
    boolean cacheBitmap = false;
    synchronized (this) {
      boolean shouldCache = mBitmapsToKeepCached.get(frameNumber);
      if (shouldCache) {
        cacheBitmap = mCachedBitmaps.get(frameNumber) == null;
      }
    }
    if (cacheBitmap) {
      copyAndCacheBitmapDuringRendering(frameNumber, bitmap);
    }
  }

  /**
   * Copies the source bitmap for the specified frame and caches it.
   *
   * @param frameNumber the frame number
   * @param sourceBitmap the rendered bitmap to be cached (after copying)
   */
  private void copyAndCacheBitmapDuringRendering(int frameNumber, Bitmap sourceBitmap) {
    CloseableReference<Bitmap> destBitmapReference = obtainBitmapInternal();
    try {
      Canvas copyCanvas = new Canvas(destBitmapReference.get());
      copyCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
      copyCanvas.drawBitmap(sourceBitmap, 0, 0, null);
      maybeCacheRenderedBitmap(frameNumber, destBitmapReference);
    } finally {
      destBitmapReference.close();
    }
  }

  private CloseableReference<Bitmap> obtainBitmapInternal() {
    Bitmap bitmap;
    synchronized (this) {
      long nowNanos = System.nanoTime();
      long waitUntilNanos = nowNanos + TimeUnit.NANOSECONDS.convert(20, TimeUnit.MILLISECONDS);
      while (mFreeBitmaps.isEmpty() && nowNanos < waitUntilNanos) {
        try {
          TimeUnit.NANOSECONDS.timedWait(this, waitUntilNanos - nowNanos);
          nowNanos = System.nanoTime();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      }

      if (mFreeBitmaps.isEmpty()) {
        bitmap = createNewBitmap();
      } else {
        bitmap = mFreeBitmaps.remove(mFreeBitmaps.size() - 1);
      }
    }
    return CloseableReference.of(bitmap, mResourceReleaserForBitmaps);
  }

  synchronized void releaseBitmapInternal(Bitmap bitmap) {
    mFreeBitmaps.add(bitmap);
  }

  private synchronized void schedulePrefetches() {
    AnimatedDrawableFrameInfo frameInfo = mAnimatedDrawableBackend.getFrameInfo(mCurrentFrameIndex);
    boolean keepOnePreceding = frameInfo.disposalMethod == DisposalMethod.DISPOSE_TO_PREVIOUS;

    int startFrame = Math.max(0, mCurrentFrameIndex - (keepOnePreceding ? 1 : 0));
    int numToPrefetch = mAnimatedDrawableOptions.allowPrefetching ? PREFETCH_FRAMES : 0;
    numToPrefetch = Math.max(numToPrefetch, keepOnePreceding ? 1 : 0);
    int endFrame = (startFrame + numToPrefetch) % mAnimatedDrawableBackend.getFrameCount();
    cancelFuturesOutsideOfRange(startFrame, endFrame);

    if (!shouldKeepAllFramesInMemory()) {
      mBitmapsToKeepCached.setAll(true);
      mBitmapsToKeepCached.removeOutsideRange(startFrame, endFrame);

      // Keep one closest to startFrame that is already cached to reduce the number of frames we
      // need to composite together to draw startFrame.
      for (int frameNumber = startFrame; frameNumber >= 0; frameNumber--) {
        if (mCachedBitmaps.get(frameNumber) != null) {
          mBitmapsToKeepCached.set(frameNumber, true);
          break;
        }
      }
      dropBitmapsThatShouldNotBeCached();
    }
    if (mAnimatedDrawableOptions.allowPrefetching) {
      doPrefetch(startFrame, numToPrefetch);
    } else {
      cancelFuturesOutsideOfRange(mCurrentFrameIndex, mCurrentFrameIndex);
    }
  }

  private static int getDefaultMaxBytes(ActivityManager activityManager) {
    int memory = activityManager.getMemoryClass();
    if (memory > 32) {
      return 5 * 1024 * 1024;
    } else {
      return 3 * 1024 * 1024;
    }
  }

  private boolean shouldKeepAllFramesInMemory() {
    if (mAnimatedDrawableOptions.forceKeepAllFramesInMemory) {
      // This overrides everything.
      return true;
    }
    return mApproxBytesToHoldAllFrames < mMaximumBytes;
  }

  private synchronized void doPrefetch(int startFrame, int count) {
    for (int i = 0; i < count; i++) {
      final int frameNumber = (startFrame + i) % mAnimatedDrawableBackend.getFrameCount();
      boolean hasCached = hasCachedOrPredecodedFrame(frameNumber);
      Task<Object> future = mDecodesInFlight.get(frameNumber);
      if (!hasCached && future == null) {
        final Task<Object> newFuture = Task.call(
            new Callable<Object>() {
              @Override
              public Object call() {
                runPrefetch(frameNumber);
                return null;
              }
            }, mExecutorService);
        mDecodesInFlight.put(frameNumber, newFuture);
        newFuture.continueWith(
            new Continuation<Object, Object>() {
              @Override
              public Object then(Task<Object> task) throws Exception {
                onFutureFinished(newFuture, frameNumber);
                return null;
              }
            });
      }
    }
  }

  /**
   * Renders a frame and caches it. This runs on the worker thread.
   *
   * @param frameNumber the frame to render
   */
  private void runPrefetch(int frameNumber) {
    synchronized (this) {
      if (!mBitmapsToKeepCached.get(frameNumber)) {
        // Looks like we're no longer supposed to keep this cached.
        return;
      }
      if (hasCachedOrPredecodedFrame(frameNumber)) {
        // Looks like it's already cached.
        return;
      }
    }

    CloseableReference<Bitmap> preDecodedFrame =
        mAnimatedDrawableBackend.getPreDecodedFrame(frameNumber);
    try {
      if (preDecodedFrame != null) {
        maybeCacheRenderedBitmap(frameNumber, preDecodedFrame);
      } else {
        CloseableReference<Bitmap> bitmapReference = obtainBitmapInternal();
        try {
          mAnimatedImageCompositor.renderFrame(frameNumber, bitmapReference.get());
          maybeCacheRenderedBitmap(frameNumber, bitmapReference);
          FLog.v(TAG, "Prefetch rendered frame %d", frameNumber);
        } finally {
          bitmapReference.close();
        }
      }
    } finally {
      CloseableReference.closeSafely(preDecodedFrame);
    }
  }

  private synchronized void onFutureFinished(Task<?> future, int frameNumber) {
    int index = mDecodesInFlight.indexOfKey(frameNumber);
    if (index >= 0) {
      Task<?> futureAtIndex = mDecodesInFlight.valueAt(index);
      if (futureAtIndex == future) {
        mDecodesInFlight.removeAt(index);
        if (future.getError() != null) {
          FLog.v(TAG, future.getError(), "Failed to render frame %d", frameNumber);
        }
      }
    }
  }

  private synchronized void cancelFuturesOutsideOfRange(int startFrame, int endFrame) {
    int index = 0;
    while (index < mDecodesInFlight.size()) {
      int frameNumber = mDecodesInFlight.keyAt(index);
      boolean outsideRange = AnimatedDrawableUtil.isOutsideRange(startFrame, endFrame, frameNumber);
      if (outsideRange) {
        Task<?> future = mDecodesInFlight.valueAt(index);
        mDecodesInFlight.removeAt(index);
        //future.cancel(false); -- TODO
      } else {
        index++;
      }
    }
  }

  private synchronized void dropBitmapsThatShouldNotBeCached() {
    int index = 0;
    while (index < mCachedBitmaps.size()) {
      int frameNumber = mCachedBitmaps.keyAt(index);
      boolean keepCached = mBitmapsToKeepCached.get(frameNumber);
      if (!keepCached) {
        CloseableReference<Bitmap> bitmapReference = mCachedBitmaps.valueAt(index);
        mCachedBitmaps.removeAt(index);
        bitmapReference.close();
      } else {
        index++;
      }
    }
  }

  private synchronized void maybeCacheRenderedBitmap(
      int frameNumber,
      CloseableReference<Bitmap> bitmapReference) {
    if (!mBitmapsToKeepCached.get(frameNumber)) {
      return;
    }

    int existingIndex = mCachedBitmaps.indexOfKey(frameNumber);
    if (existingIndex >= 0) {
      CloseableReference<Bitmap> oldReference = mCachedBitmaps.valueAt(existingIndex);
      oldReference.close();
      mCachedBitmaps.removeAt(existingIndex);
    }
    mCachedBitmaps.put(frameNumber, bitmapReference.clone());
  }

  private synchronized CloseableReference<Bitmap> getCachedOrPredecodedFrame(int frameNumber) {
    CloseableReference<Bitmap> ret =
        CloseableReference.cloneOrNull(mCachedBitmaps.get(frameNumber));
    if (ret == null) {
      ret = mAnimatedDrawableBackend.getPreDecodedFrame(frameNumber);
    }
    return ret;
  }

  private synchronized boolean hasCachedOrPredecodedFrame(int frameNumber) {
    return mCachedBitmaps.get(frameNumber) != null ||
        mAnimatedDrawableBackend.hasPreDecodedFrame(frameNumber);
  }

  @VisibleForTesting
  synchronized Map<Integer, Task<?>> getDecodesInFlight() {
    Map<Integer, Task<?>> map = new HashMap<Integer, Task<?>>();
    for (int i = 0; i < mDecodesInFlight.size(); i++) {
      map.put(mDecodesInFlight.keyAt(i), mDecodesInFlight.valueAt(i));
    }
    return map;
  }

  @VisibleForTesting
  synchronized Set<Integer> getFramesCached() {
    Set<Integer> set = new HashSet<Integer>();
    for (int i = 0; i < mCachedBitmaps.size(); i++) {
      set.add(mCachedBitmaps.keyAt(i));
    }
    return set;
  }
}
