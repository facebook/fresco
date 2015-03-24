/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.time.MonotonicClock;
import com.facebook.drawable.base.DrawableWithCaches;

import com.nineoldandroids.animation.ValueAnimator;

/**
 * A {@link Drawable} that renders a animated image. The details of the format are abstracted by the
 * {@link AnimatedDrawableBackend} interface. The drawable can work either as an {@link Animatable}
 * where the client calls start/stop to animate it or it can work as a level-based drawable where
 * the client drives the animation by calling {@link Drawable#setLevel}.
 */
public class AnimatedDrawable extends Drawable implements AnimatableDrawable, DrawableWithCaches {

  private static final Class<?> TAG = AnimatedDrawable.class;

  private static final long WATCH_DOG_TIMER_POLL_INTERVAL_MS = 2000;
  private static final long WATCH_DOG_TIMER_MIN_TIMEOUT_MS = 1000;

  private static final int POLL_FOR_RENDERED_FRAME_MS = 5;
  private static final int NO_FRAME = -1;

  private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;
  private final AnimatedDrawableDiagnostics mAnimatedDrawableDiagnostics;
  private final MonotonicClock mMonotonicClock;
  private final int mDurationMs;
  private final int mFrameCount;

  // Paint used to draw on a Canvas
  private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
  private final Rect mDstRect = new Rect();
  private final Paint mTransparentPaint;

  private volatile String mLogId;

  private AnimatedDrawableCachingBackend mAnimatedDrawableBackend;
  private long mStartTimeMs;

  // Index of frame scheduled to be drawn. Between 0 and mFrameCount - 1
  private int mScheduledFrameNumber;

  // Index of frame scheduled to be drawn but never is reset to zero. Keeps growing.
  private int mScheduledFrameMonotonicNumber;

  // Index of frame that will be drawn next. Between 0 and mFrameCount - 1. May fall behind
  // mScheduledFrameIndex if we can't keep up.
  private int mPendingRenderedFrameNumber;

  // Corresponds to mPendingRenderedFrameNumber but keeps growing.
  private int mPendingRenderedFrameMonotonicNumber;

  // Index of last frame that was drawn.
  private int mLastDrawnFrameNumber = -1;

  // Corresponds to mLastDrawnFrameNumber but keeps growing.
  private int mLastDrawnFrameMonotonicNumber = -1;

  // Bitmap for last drawn frame. Corresponds to mLastDrawnFrameNumber.
  private CloseableReference<Bitmap> mLastDrawnFrame;

  private boolean mWaitingForDraw;
  private long mLastInvalidateTimeMs = -1;

  private boolean mIsRunning;
  private boolean mHaveWatchdogScheduled;

  private float mSx = 1f;
  private float mSy = 1f;
  private boolean mApplyTransformation;
  private boolean mInvalidateTaskScheduled;
  private long mNextFrameTaskMs = -1;

  private final Runnable mStartTask = new Runnable() {
    @Override
    public void run() {
      onStart();
    }
  };

  private final Runnable mNextFrameTask = new Runnable() {
    @Override
    public void run() {
      FLog.v(TAG, "(%s) Next Frame Task", mLogId);
      onNextFrame();
    }
  };

  private final Runnable mInvalidateTask = new Runnable() {
    @Override
    public void run() {
      FLog.v(TAG, "(%s) Invalidate Task", mLogId);
      mInvalidateTaskScheduled = false;
      doInvalidateSelf();
    }
  };

  private final Runnable mWatchdogTask = new Runnable() {
    @Override
    public void run() {
      FLog.v(TAG, "(%s) Watchdog Task", mLogId);
      doWatchdogCheck();
    }
  };

  public AnimatedDrawable(
      ScheduledExecutorService scheduledExecutorServiceForUiThread,
      AnimatedDrawableCachingBackend animatedDrawableBackend,
      AnimatedDrawableDiagnostics animatedDrawableDiagnostics,
      MonotonicClock monotonicClock) {
    mScheduledExecutorServiceForUiThread = scheduledExecutorServiceForUiThread;
    mAnimatedDrawableBackend = animatedDrawableBackend;
    mAnimatedDrawableDiagnostics = animatedDrawableDiagnostics;
    mMonotonicClock = monotonicClock;
    mDurationMs = mAnimatedDrawableBackend.getDurationMs();
    mFrameCount = mAnimatedDrawableBackend.getFrameCount();
    mAnimatedDrawableDiagnostics.setBackend(mAnimatedDrawableBackend);
    mTransparentPaint = new Paint();
    mTransparentPaint.setColor(Color.TRANSPARENT);
    mTransparentPaint.setStyle(Paint.Style.FILL);

    // Show last frame when not animating.
    resetToPreviewFrame();
  }

  private void resetToPreviewFrame() {
    mScheduledFrameNumber = mAnimatedDrawableBackend.getFrameForPreview();
    mScheduledFrameMonotonicNumber = mScheduledFrameNumber;
    mPendingRenderedFrameNumber = NO_FRAME;
    mPendingRenderedFrameMonotonicNumber = NO_FRAME;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    if (mLastDrawnFrame != null) {
      mLastDrawnFrame.close();
      mLastDrawnFrame = null;
    }
  }

  /**
   * Sets an id that will be logged with any of the logging calls. Useful for debugging.
   *
   * @param logId the id to log
   */
  public void setLogId(String logId) {
    mLogId = logId;
  }

  @Override
  public int getIntrinsicWidth() {
    return mAnimatedDrawableBackend.getWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    return mAnimatedDrawableBackend.getHeight();
  }

  @Override
  public void setAlpha(int alpha) {
    mPaint.setAlpha(alpha);
    doInvalidateSelf();
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mPaint.setColorFilter(cf);
    doInvalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    mApplyTransformation = true;
    if (mLastDrawnFrame != null) {
      mLastDrawnFrame.close();
      mLastDrawnFrame = null;
    }
    mLastDrawnFrameNumber = -1;
    mLastDrawnFrameMonotonicNumber = -1;
    mAnimatedDrawableBackend.dropCaches();
  }

  private void onStart() {
    if (!mIsRunning) {
      return;
    }
    mAnimatedDrawableDiagnostics.onStartMethodBegin();
    try {
      mStartTimeMs = mMonotonicClock.now();
      mScheduledFrameNumber = 0;
      mScheduledFrameMonotonicNumber = 0;
      long nextFrameMs = mStartTimeMs + mAnimatedDrawableBackend.getDurationMsForFrame(0);
      scheduleSelf(mNextFrameTask, nextFrameMs);
      mNextFrameTaskMs = nextFrameMs;
      doInvalidateSelf();
    } finally {
      mAnimatedDrawableDiagnostics.onStartMethodEnd();
    }
  }

  private void onNextFrame() {
    mNextFrameTaskMs = -1;
    if (!mIsRunning) {
      return;
    }
    if (mDurationMs == 0) {
      return;
    }
    mAnimatedDrawableDiagnostics.onNextFrameMethodBegin();
    try {
      computeAndScheduleNextFrame(true /* schedule next frame */);
    } finally {
      mAnimatedDrawableDiagnostics.onNextFrameMethodEnd();
    }
  }

  private void computeAndScheduleNextFrame(boolean scheduleNextFrame) {
    if (mDurationMs == 0) {
      return;
    }
    long nowMs = mMonotonicClock.now();
    int loops = (int) ((nowMs - mStartTimeMs) / mDurationMs);
    int timestampMs = (int) ((nowMs - mStartTimeMs) % mDurationMs);
    int newCurrentFrameNumber = mAnimatedDrawableBackend.getFrameForTimestampMs(timestampMs);
    boolean changed = mScheduledFrameNumber != newCurrentFrameNumber;
    mScheduledFrameNumber = newCurrentFrameNumber;
    mScheduledFrameMonotonicNumber = loops * mFrameCount + newCurrentFrameNumber;

    if (!scheduleNextFrame) {
      // We're about to draw. We don't need to schedule anything because we're going to draw
      // that frame right now. the onDraw method just wants to make sure the current frame is set.
      return;
    }

    if (changed) {
      doInvalidateSelf();
    } else {
      int durationMs = mAnimatedDrawableBackend.getTimestampMsForFrame(mScheduledFrameNumber) +
          mAnimatedDrawableBackend.getDurationMsForFrame(mScheduledFrameNumber) -
          timestampMs;
      int nextFrame = (mScheduledFrameNumber + 1) % mFrameCount;
      long nextFrameMs = nowMs + durationMs;
      if (mNextFrameTaskMs == -1 || mNextFrameTaskMs > nextFrameMs) {
        FLog.v(TAG, "(%s) Next frame (%d) in %d ms", mLogId, nextFrame, durationMs);
        unscheduleSelf(mNextFrameTask); // Cancel any existing task.
        scheduleSelf(mNextFrameTask, nextFrameMs);
        mNextFrameTaskMs = nextFrameMs;
      }
    }
  }

  @Override
  public void draw(Canvas canvas) {
    mAnimatedDrawableDiagnostics.onDrawMethodBegin();
    try {
      mWaitingForDraw = false;
      if (mIsRunning && !mHaveWatchdogScheduled) {
        mScheduledExecutorServiceForUiThread.schedule(
            mWatchdogTask,
            WATCH_DOG_TIMER_POLL_INTERVAL_MS,
            TimeUnit.MILLISECONDS);
        mHaveWatchdogScheduled = true;
      }

      if (mApplyTransformation) {
        mDstRect.set(getBounds());
        if (!mDstRect.isEmpty()) {
          AnimatedDrawableCachingBackend newBackend =
              mAnimatedDrawableBackend.forNewBounds(mDstRect);
          if (newBackend != mAnimatedDrawableBackend) {
            mAnimatedDrawableBackend.dropCaches();
            mAnimatedDrawableBackend = newBackend;
            mAnimatedDrawableDiagnostics.setBackend(newBackend);
          }
          mSx = (float) mDstRect.width() / mAnimatedDrawableBackend.getRenderedWidth();
          mSy = (float) mDstRect.height() / mAnimatedDrawableBackend.getRenderedHeight();
          mApplyTransformation = false;
        }
      }

      if (mDstRect.isEmpty()) {
        // Don't try to draw if the dest rect is empty.
        return;
      }

      canvas.save();
      canvas.scale(mSx, mSy);

      // TODO(6169940) we overdraw if both pending frame is ready and current frame is ready.
      boolean didDrawFrame = false;
      if (mPendingRenderedFrameNumber != NO_FRAME) {
        // We tried to render a frame and it wasn't yet ready. See if it's ready now.
        boolean rendered =
            renderFrame(canvas, mPendingRenderedFrameNumber, mPendingRenderedFrameMonotonicNumber);
        didDrawFrame |= rendered;
        if (rendered) {
          FLog.v(TAG, "(%s) Rendered pending frame %d", mLogId, mPendingRenderedFrameNumber);
          mPendingRenderedFrameNumber = NO_FRAME;
          mPendingRenderedFrameMonotonicNumber = NO_FRAME;
        } else {
          // Try again later.
          FLog.v(TAG, "(%s) Trying again later for pending %d", mLogId, mPendingRenderedFrameNumber);
          scheduleInvalidatePoll();
        }
      }

      if (mPendingRenderedFrameNumber == NO_FRAME) {
        // We don't have a frame that's pending so render the current frame.
        if (mIsRunning) {
          computeAndScheduleNextFrame(false /* don't schedule yet */);
        }
        boolean rendered = renderFrame(
            canvas,
            mScheduledFrameNumber,
            mScheduledFrameMonotonicNumber);
        didDrawFrame |= rendered;
        if (rendered) {
          FLog.v(TAG, "(%s) Rendered current frame %d", mLogId, mScheduledFrameNumber);
          if (mIsRunning) {
            computeAndScheduleNextFrame(true /* schedule next frame */);
          }
        } else {
          FLog.v(TAG, "(%s) Trying again later for current %d", mLogId, mScheduledFrameNumber);
          mPendingRenderedFrameNumber = mScheduledFrameNumber;
          mPendingRenderedFrameMonotonicNumber = mScheduledFrameMonotonicNumber;
          scheduleInvalidatePoll();
        }
      }

      if (!didDrawFrame) {
        if (mLastDrawnFrame != null) {
          canvas.drawBitmap(mLastDrawnFrame.get(), 0f, 0f, mPaint);
          didDrawFrame = true;
          FLog.v(TAG, "(%s) Rendered last known frame %d", mLogId, mLastDrawnFrameNumber);
        }
      }

      if (!didDrawFrame) {
        // Last ditch effort, use preview bitmap.
        CloseableReference<Bitmap> previewBitmapReference =
            mAnimatedDrawableBackend.getPreviewBitmap();
        if (previewBitmapReference != null) {
          canvas.drawBitmap(previewBitmapReference.get(), 0f, 0f, mPaint);
          previewBitmapReference.close();
          FLog.v(TAG, "(%s) Rendered preview frame", mLogId);
          didDrawFrame = true;
        }
      }

      if (!didDrawFrame) {
        // TODO(6169940) this may not be necessary. Confirm with Rich.
        canvas.drawRect(0, 0, mDstRect.width(), mDstRect.height(), mTransparentPaint);
        FLog.v(TAG, "(%s) Failed to draw a frame", mLogId);
      }

      canvas.restore();
      mAnimatedDrawableDiagnostics.drawDebugOverlay(canvas, mDstRect);
    } finally {
      mAnimatedDrawableDiagnostics.onDrawMethodEnd();
    }
  }

  /**
   * Schedule a task to invalidate the drawable. Used to poll for a rendered frame.
   */
  private void scheduleInvalidatePoll() {
    if (mInvalidateTaskScheduled) {
      return;
    }
    mInvalidateTaskScheduled = true;
    scheduleSelf(mInvalidateTask, POLL_FOR_RENDERED_FRAME_MS);
  }

  /**
   * Returns whether a previous call to {@link #draw} would have rendered a frame.
   *
   * @return whether a previous call to {@link #draw} would have rendered a frame
   */
  public boolean didLastDrawRender() {
    return mLastDrawnFrame != null;
  }

  /**
   * Renders the specified frame to the canvas.
   *
   * @param canvas the canvas to render to
   * @param frameNumber the relative frame number (between 0 and frame count)
   * @param frameMonotonicNumber the absolute frame number for stats purposes
   * @return whether the frame was available and was rendered
   */
  private boolean renderFrame(
      Canvas canvas,
      int frameNumber,
      int frameMonotonicNumber) {
    CloseableReference<Bitmap> bitmapReference =
        mAnimatedDrawableBackend.getBitmapForFrame(frameNumber);
    if (bitmapReference != null) {
      canvas.drawBitmap(bitmapReference.get(), 0f, 0f, mPaint);
      if (mLastDrawnFrame != null) {
        mLastDrawnFrame.close();
      }

      if (mIsRunning && frameMonotonicNumber > mLastDrawnFrameMonotonicNumber) {
        int droppedFrames = frameMonotonicNumber - mLastDrawnFrameMonotonicNumber - 1;
        mAnimatedDrawableDiagnostics.incrementDrawnFrames(1);
        mAnimatedDrawableDiagnostics.incrementDroppedFrames(droppedFrames);
        if (droppedFrames > 0) {
          FLog.v(TAG, "(%s) Dropped %d frames", mLogId, droppedFrames);
        }
      }
      mLastDrawnFrame = bitmapReference;
      mLastDrawnFrameNumber = frameNumber;
      mLastDrawnFrameMonotonicNumber = frameMonotonicNumber;
      FLog.v(TAG, "(%s) Drew frame %d", mLogId, frameNumber);
      return true;
    }
    return false;
  }

  /**
   * Checks to make sure we drop our caches if we haven't drawn in a while. There's no reliable
   * way for a Drawable to determine if it's still actively part of a View, so we use a heuristic
   * instead.
   */
  private void doWatchdogCheck() {
    mHaveWatchdogScheduled = false;
    if (!mIsRunning) {
      return;
    }
    long now = mMonotonicClock.now();

    // Timeout if it's been more than 2 seconds with drawn since invalidation.
    boolean hasNotDrawnWithinTimeout =
        mWaitingForDraw && now - mLastInvalidateTimeMs > WATCH_DOG_TIMER_MIN_TIMEOUT_MS;

    // Also timeout onNextFrame is more than 2 seconds late.
    boolean hasNotAdvancedFrameWithinTimeout =
        mNextFrameTaskMs != -1 && now - mNextFrameTaskMs > WATCH_DOG_TIMER_MIN_TIMEOUT_MS;

    if (hasNotDrawnWithinTimeout || hasNotAdvancedFrameWithinTimeout) {
      dropCaches();
      doInvalidateSelf();
    } else {
      mScheduledExecutorServiceForUiThread.schedule(
          mWatchdogTask,
          WATCH_DOG_TIMER_POLL_INTERVAL_MS,
          TimeUnit.MILLISECONDS);
      mHaveWatchdogScheduled = true;
    }
  }

  private void doInvalidateSelf() {
    mWaitingForDraw = true;
    mLastInvalidateTimeMs = mMonotonicClock.now();
    invalidateSelf();
  }

  @VisibleForTesting
  boolean isWaitingForDraw() {
    return mWaitingForDraw;
  }

  @VisibleForTesting
  boolean isWaitingForNextFrame() {
    return mNextFrameTaskMs != -1;
  }

  @VisibleForTesting
  int getScheduledFrameNumber() {
    return mScheduledFrameNumber;
  }

  @Override
  public void start() {
    if (mDurationMs == 0 || mFrameCount <= 1) {
      return;
    }
    mIsRunning = true;
    scheduleSelf(mStartTask, mMonotonicClock.now());
  }

  @Override
  public void stop() {
    mIsRunning = false;
  }

  @Override
  public boolean isRunning() {
    return mIsRunning;
  }

  @Override
  protected boolean onLevelChange(int level) {
    if (mIsRunning) {
      // If the client called start on us, they expect us to run the animation. In that case,
      // we ignore level changes.
      return false;
    }
    int frame = mAnimatedDrawableBackend.getFrameForTimestampMs(level);
    if (frame == mScheduledFrameNumber) {
      return false;
    }

    try {
      mScheduledFrameNumber = frame;
      mScheduledFrameMonotonicNumber = frame;
      doInvalidateSelf();
      return true;
    } catch (IllegalStateException e) {
      // The underlying image was disposed.
      return false;
    }
  }

  @Override
  public ValueAnimator createValueAnimator(int maxDurationMs) {
    ValueAnimator animator = createValueAnimator();
    int repeatCount = Math.max((maxDurationMs / mAnimatedDrawableBackend.getDurationMs()), 1);
    animator.setRepeatCount(repeatCount);
    return animator;
  }

  @Override
  public ValueAnimator createValueAnimator() {
    int loopCount = mAnimatedDrawableBackend.getLoopCount();
    ValueAnimator animator = new ValueAnimator();
    animator.setIntValues(0, mDurationMs);
    animator.setDuration(mDurationMs);
    animator.setRepeatCount(loopCount != 0 ? loopCount : ValueAnimator.INFINITE);
    animator.setRepeatMode(ValueAnimator.RESTART);
    animator.setInterpolator(new LinearInterpolator());
    animator.addUpdateListener(createAnimatorUpdateListener());
    return animator;
  }

  @Override
  public ValueAnimator.AnimatorUpdateListener createAnimatorUpdateListener() {
    return new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        setLevel((Integer) animation.getAnimatedValue());
      }
    };
  }

  @Override
  public void dropCaches() {
    FLog.v(TAG, "(%s) Dropping caches", mLogId);
    if (mLastDrawnFrame != null) {
      mLastDrawnFrame.close();
      mLastDrawnFrame = null;
      mLastDrawnFrameNumber = -1;
      mLastDrawnFrameMonotonicNumber = -1;
    }
    mAnimatedDrawableBackend.dropCaches();
  }
}
