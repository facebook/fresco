/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.bitmap.preparation;

import android.graphics.Bitmap;
import android.util.SparseArray;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import java.util.concurrent.ExecutorService;

/**
 * Default bitmap frame preparer that uses the given {@link ExecutorService} to schedule jobs.
 * An instance of this class can be shared between multiple animated images.
 */
public class DefaultBitmapFramePreparer
    implements BitmapFramePreparer {

  private static final Class<?> TAG = DefaultBitmapFramePreparer.class;

  private final PlatformBitmapFactory mPlatformBitmapFactory;
  private final BitmapFrameRenderer mBitmapFrameRenderer;
  private final Bitmap.Config mBitmapConfig;
  private final ExecutorService mExecutorService;
  private final SparseArray<Runnable> mPendingFrameDecodeJobs;

  public DefaultBitmapFramePreparer(
      PlatformBitmapFactory platformBitmapFactory,
      BitmapFrameRenderer bitmapFrameRenderer,
      Bitmap.Config bitmapConfig,
      ExecutorService executorService) {
    mPlatformBitmapFactory = platformBitmapFactory;
    mBitmapFrameRenderer = bitmapFrameRenderer;
    mBitmapConfig = bitmapConfig;
    mExecutorService = executorService;
    mPendingFrameDecodeJobs = new SparseArray<>();
  }

  @Override
  public boolean prepareFrame(
      BitmapFrameCache bitmapFrameCache,
      AnimationBackend animationBackend,
      int frameNumber) {
    // Create a unique ID to identify the frame for the given backend.
    int frameId = getUniqueId(animationBackend, frameNumber);
    synchronized (mPendingFrameDecodeJobs) {
      // Check if already scheduled.
      if (mPendingFrameDecodeJobs.get(frameId) != null) {
        FLog.v(TAG, "Already scheduled decode job for frame %d", frameNumber);
        return true;
      }
      // Check if already cached.
      if (bitmapFrameCache.contains(frameNumber)) {
        FLog.v(TAG, "Frame %d is cached already.", frameNumber);
        return true;
      }
      Runnable frameDecodeRunnable = new FrameDecodeRunnable(
          animationBackend,
          bitmapFrameCache,
          frameNumber,
          frameId);
      mPendingFrameDecodeJobs.put(frameId, frameDecodeRunnable);
      mExecutorService.execute(frameDecodeRunnable);
    }
    return true;
  }

  private static int getUniqueId(AnimationBackend backend, int frameNumber) {
    int result = backend.hashCode();
    result = 31 * result + frameNumber;
    return result;
  }

  private class FrameDecodeRunnable implements Runnable {

    private final BitmapFrameCache mBitmapFrameCache;
    private final AnimationBackend mAnimationBackend;
    private final int mFrameNumber;
    private final int mHashCode;

    public FrameDecodeRunnable(
        AnimationBackend animationBackend,
        BitmapFrameCache bitmapFrameCache,
        int frameNumber,
        int hashCode) {
      mAnimationBackend = animationBackend;
      mBitmapFrameCache = bitmapFrameCache;
      mFrameNumber = frameNumber;
      mHashCode = hashCode;
    }

    @Override
    public void run() {
      try {
        // If we have a cached frame already, we don't need to do anything.
        if (mBitmapFrameCache.contains(mFrameNumber)) {
          FLog.v(TAG, "Frame %d is cached already.", mFrameNumber);
          return;
        }

        // Prepare the frame.
        if (prepareFrameAndCache(mFrameNumber, BitmapAnimationBackend.FRAME_TYPE_REUSED)) {
          FLog.v(TAG, "Prepared frame frame %d.", mFrameNumber);
        } else {
          FLog.e(TAG, "Could not prepare frame %d.", mFrameNumber);
        }
      } finally {
        synchronized (mPendingFrameDecodeJobs) {
          mPendingFrameDecodeJobs.remove(mHashCode);
        }
      }
    }

    private boolean prepareFrameAndCache(
        int frameNumber,
        @BitmapAnimationBackend.FrameType int frameType) {
      CloseableReference<Bitmap> bitmapReference = null;
      boolean created;
      int nextFrameType;

      try {
        switch (frameType) {
          case BitmapAnimationBackend.FRAME_TYPE_REUSED:
            bitmapReference =
                mBitmapFrameCache.getBitmapToReuseForFrame(
                    frameNumber,
                    mAnimationBackend.getIntrinsicWidth(),
                    mAnimationBackend.getIntrinsicHeight());
            nextFrameType = BitmapAnimationBackend.FRAME_TYPE_CREATED;
            break;

          case BitmapAnimationBackend.FRAME_TYPE_CREATED:
            try {
              bitmapReference =
                  mPlatformBitmapFactory.createBitmap(
                      mAnimationBackend.getIntrinsicWidth(),
                      mAnimationBackend.getIntrinsicHeight(),
                      mBitmapConfig);
            } catch (RuntimeException e) {
              // Failed to create the bitmap for the frame, return and report that we could not
              // prepare the frame.
              FLog.w(TAG, "Failed to create frame bitmap", e);
              return false;
            }
            nextFrameType = BitmapAnimationBackend.FRAME_TYPE_UNKNOWN;
            break;
          default:
            return false;
        }
        // Try to render and cache the frame
        created = renderFrameAndCache(frameNumber, bitmapReference, frameType);
      } finally {
        CloseableReference.closeSafely(bitmapReference);
      }

      if (created || nextFrameType == BitmapAnimationBackend.FRAME_TYPE_UNKNOWN) {
        return created;
      } else {
        return prepareFrameAndCache(frameNumber, nextFrameType);
      }
    }

    private boolean renderFrameAndCache(
        int frameNumber,
        CloseableReference<Bitmap> bitmapReference,
        @BitmapAnimationBackend.FrameType int frameType) {
      // Check if the bitmap is valid
      if (!CloseableReference.isValid(bitmapReference)) {
        return false;
      }
      // Try to render the frame
      if (!mBitmapFrameRenderer.renderFrame(frameNumber, bitmapReference.get())) {
        return false;
      }
      FLog.v(TAG, "Frame %d ready.", mFrameNumber);
      // Cache the frame
      synchronized (mPendingFrameDecodeJobs) {
        mBitmapFrameCache.onFramePrepared(mFrameNumber, bitmapReference, frameType);
      }
      return true;
    }
  }
}
