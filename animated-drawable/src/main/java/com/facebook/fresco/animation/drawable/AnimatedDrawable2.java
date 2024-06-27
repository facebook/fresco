/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import com.facebook.common.logging.FLog;
import com.facebook.drawable.base.DrawableWithCaches;
import com.facebook.drawee.drawable.DrawableProperties;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.frame.DropFramesFrameScheduler;
import com.facebook.fresco.animation.frame.FrameScheduler;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Experimental new animated drawable that uses a supplied [AnimationBackend] for drawing frames.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class AnimatedDrawable2 extends Drawable implements Animatable, DrawableWithCaches {

  /** [draw(Canvas)] listener that is notified for each draw call. Can be used for debugging. */
  public interface DrawListener {

    void onDraw(
        AnimatedDrawable2 animatedDrawable,
        FrameScheduler frameScheduler,
        int frameNumberToDraw,
        boolean frameDrawn,
        boolean isAnimationRunning,
        long animationStartTimeMs,
        long animationTimeMs,
        long lastFrameAnimationTimeMs,
        long actualRenderTimeStartMs,
        long actualRenderTimeEndMs,
        long startRenderTimeForNextFrameMs,
        long scheduledRenderTimeForNextFrameMs);
  }

  private static final Class<?> TAG = AnimatedDrawable2.class;

  private static final AnimationListener NO_OP_LISTENER = new BaseAnimationListener();

  private static final int DEFAULT_FRAME_SCHEDULING_DELAY_MS = 8;
  private static final int DEFAULT_FRAME_SCHEDULING_OFFSET_MS = 0;

  @Nullable private AnimationBackend mAnimationBackend;
  @Nullable private FrameScheduler mFrameScheduler;

  // Animation parameters
  private volatile boolean mIsRunning;
  private long mStartTimeMs;

  private long mLastFrameAnimationTimeMs;
  private long mExpectedRenderTimeMs;
  private int mLastDrawnFrameNumber;

  private long mPausedStartTimeMsDifference;
  private long mPausedLastFrameAnimationTimeMsDifference;
  private int mPausedLastDrawnFrameNumber;

  private long mFrameSchedulingDelayMs = DEFAULT_FRAME_SCHEDULING_DELAY_MS;
  private long mFrameSchedulingOffsetMs = DEFAULT_FRAME_SCHEDULING_OFFSET_MS;

  // Animation statistics
  private int mDroppedFrames;

  // Listeners
  private volatile AnimationListener mAnimationListener = NO_OP_LISTENER;
  @Nullable private volatile DrawListener mDrawListener = null;
  private final AnimationBackend.Listener animationBackendListener =
      () -> mAnimationListener.onAnimationLoaded();

  // Holder for drawable properties like alpha to be able to re-apply if the backend changes.
  // The instance is created lazily only if needed.
  @Nullable private DrawableProperties mDrawableProperties;

  /**
   * Runnable that invalidates the drawable that will be scheduled according to the next target
   * frame.
   */
  private final Runnable mInvalidateRunnable =
      new Runnable() {
        @Override
        public void run() {
          // Remove all potential other scheduled runnables
          // (e.g. if the view has been invalidated a lot)
          unscheduleSelf(mInvalidateRunnable);
          // Draw the next frame
          invalidateSelf();
        }
      };

  public AnimatedDrawable2() {
    this(null);
  }

  public AnimatedDrawable2(@Nullable AnimationBackend animationBackend) {
    mAnimationBackend = animationBackend;
    mFrameScheduler = createSchedulerForBackendAndDelayMethod(mAnimationBackend);

    if (animationBackend != null) {
      animationBackend.setAnimationListener(animationBackendListener);
    }
  }

  @Override
  public int getIntrinsicWidth() {
    if (mAnimationBackend == null) {
      return super.getIntrinsicWidth();
    }
    return mAnimationBackend.getIntrinsicWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    if (mAnimationBackend == null) {
      return super.getIntrinsicHeight();
    }
    return mAnimationBackend.getIntrinsicHeight();
  }

  /** Start the animation. */
  @Override
  public void start() {
    if (mIsRunning || mAnimationBackend == null || mAnimationBackend.getFrameCount() <= 1) {
      return;
    }
    mIsRunning = true;

    final long now = now();
    mStartTimeMs = now - mPausedStartTimeMsDifference;
    mExpectedRenderTimeMs = mStartTimeMs;
    mLastFrameAnimationTimeMs = now - mPausedLastFrameAnimationTimeMsDifference;
    mLastDrawnFrameNumber = mPausedLastDrawnFrameNumber;
    invalidateSelf();
    mAnimationListener.onAnimationStart(this);
  }

  /** Stop the animation at the current frame. It can be resumed by calling [start()] again. */
  @Override
  public void stop() {
    if (!mIsRunning) {
      return;
    }
    final long now = now();
    mPausedStartTimeMsDifference = now - mStartTimeMs;
    mPausedLastFrameAnimationTimeMsDifference = now - mLastFrameAnimationTimeMs;
    mPausedLastDrawnFrameNumber = mLastDrawnFrameNumber;

    mIsRunning = false;
    mStartTimeMs = 0;
    mExpectedRenderTimeMs = mStartTimeMs;
    mLastFrameAnimationTimeMs = -1;
    mLastDrawnFrameNumber = -1;
    unscheduleSelf(mInvalidateRunnable);
    mAnimationListener.onAnimationStop(this);
  }

  /**
   * Check whether the animation is running.
   *
   * @return true if the animation is currently running
   */
  @Override
  public boolean isRunning() {
    return mIsRunning;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    if (mAnimationBackend != null) {
      mAnimationBackend.setBounds(bounds);
    }
  }

  @Override
  public void draw(Canvas canvas) {
    if (mAnimationBackend == null || mFrameScheduler == null) {
      return;
    }

    long actualRenderTimeStartMs = now();
    long animationTimeMs =
        mIsRunning
            ? actualRenderTimeStartMs - mStartTimeMs + mFrameSchedulingOffsetMs
            : Math.max(mLastFrameAnimationTimeMs, 0);

    // What frame should be drawn?

    int frameNumberToDraw =
        mFrameScheduler.getFrameNumberToRender(animationTimeMs, mLastFrameAnimationTimeMs);

    // Check if the animation is finished and draw last frame if so

    if (frameNumberToDraw == FrameScheduler.FRAME_NUMBER_DONE) {
      frameNumberToDraw = mAnimationBackend.getFrameCount() - 1;
      mAnimationListener.onAnimationStop(this);
      mIsRunning = false;
    } else if (frameNumberToDraw == 0) {
      if (mLastDrawnFrameNumber != -1 && actualRenderTimeStartMs >= mExpectedRenderTimeMs) {
        mAnimationListener.onAnimationRepeat(this);
      }
    }

    // Draw the frame
    boolean frameDrawn = mAnimationBackend.drawFrame(this, canvas, frameNumberToDraw);
    if (frameDrawn) {
      // Notify listeners that we draw a new frame and
      // that the animation might be repeated
      mAnimationListener.onAnimationFrame(this, frameNumberToDraw);
      mLastDrawnFrameNumber = frameNumberToDraw;
    }
    // Log potential dropped frames

    if (!frameDrawn) {
      onFrameDropped();
    }

    long targetRenderTimeForNextFrameMs = FrameScheduler.NO_NEXT_TARGET_RENDER_TIME;
    long scheduledRenderTimeForNextFrameMs = -1;
    long actualRenderTimeEnd = now();
    if (mIsRunning) {
      // Schedule the next frame if needed.
      targetRenderTimeForNextFrameMs =
          mFrameScheduler.getTargetRenderTimeForNextFrameMs(actualRenderTimeEnd - mStartTimeMs);
      if (targetRenderTimeForNextFrameMs != FrameScheduler.NO_NEXT_TARGET_RENDER_TIME) {
        scheduledRenderTimeForNextFrameMs =
            targetRenderTimeForNextFrameMs + mFrameSchedulingDelayMs;
        scheduleNextFrame(scheduledRenderTimeForNextFrameMs);
      } else {
        mAnimationListener.onAnimationStop(this);
        mIsRunning = false;
      }
    }

    DrawListener listener = mDrawListener;
    if (listener != null) {
      listener.onDraw(
          this,
          mFrameScheduler,
          frameNumberToDraw,
          frameDrawn,
          mIsRunning,
          mStartTimeMs,
          animationTimeMs,
          mLastFrameAnimationTimeMs,
          actualRenderTimeStartMs,
          actualRenderTimeEnd,
          targetRenderTimeForNextFrameMs,
          scheduledRenderTimeForNextFrameMs);
    }
    mLastFrameAnimationTimeMs = animationTimeMs;
  }

  @Override
  public void setAlpha(int alpha) {
    if (mDrawableProperties == null) {
      mDrawableProperties = new DrawableProperties();
    }
    mDrawableProperties.setAlpha(alpha);
    if (mAnimationBackend != null) {
      mAnimationBackend.setAlpha(alpha);
    }
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    if (mDrawableProperties == null) {
      mDrawableProperties = new DrawableProperties();
    }
    mDrawableProperties.setColorFilter(colorFilter);
    if (mAnimationBackend != null) {
      mAnimationBackend.setColorFilter(colorFilter);
    }
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  public void preloadAnimation() {
    if (mAnimationBackend != null) {
      mAnimationBackend.preloadAnimation();
    }
  }

  /**
   * Update the animation backend to be used for the animation. This will also stop the animation.
   * In order to remove the current animation backend, call this method with null.
   *
   * @param animationBackend the animation backend to be used or null
   */
  public void setAnimationBackend(@Nullable AnimationBackend animationBackend) {
    if (this.mAnimationBackend != null) {
      this.mAnimationBackend.setAnimationListener(null);
    }

    this.mAnimationBackend = animationBackend;
    if (mAnimationBackend != null) {
      mFrameScheduler = new DropFramesFrameScheduler(mAnimationBackend);
      mAnimationBackend.setAnimationListener(animationBackendListener);
      mAnimationBackend.setBounds(getBounds());
      if (mDrawableProperties != null) {
        // re-apply to the same drawable so that the animation backend is updated.
        mDrawableProperties.applyTo(this);
      }
    }
    mFrameScheduler = createSchedulerForBackendAndDelayMethod(mAnimationBackend);
    stop();
  }

  @Nullable
  public AnimationBackend getAnimationBackend() {
    return mAnimationBackend;
  }

  public long getDroppedFrames() {
    return mDroppedFrames;
  }

  public long getStartTimeMs() {
    return mStartTimeMs;
  }

  public boolean isInfiniteAnimation() {
    return mFrameScheduler != null && mFrameScheduler.isInfiniteAnimation();
  }

  /**
   * Jump immediately to the given frame number. The animation will not be paused if it is running.
   * If the animation is not running, the animation will not be started.
   *
   * @param targetFrameNumber the frame number to jump to
   */
  public void jumpToFrame(int targetFrameNumber) {
    if (mAnimationBackend == null || mFrameScheduler == null) {
      return;
    }
    // In order to jump to a given frame, we have to compute the correct start time
    mLastFrameAnimationTimeMs = mFrameScheduler.getTargetRenderTimeMs(targetFrameNumber);

    // Reset the paused timing as we broke the animation frame flow
    mPausedLastDrawnFrameNumber = targetFrameNumber;
    mPausedStartTimeMsDifference = 0;
    mPausedLastFrameAnimationTimeMsDifference = 0;

    mStartTimeMs = now() - mLastFrameAnimationTimeMs;
    mExpectedRenderTimeMs = mStartTimeMs;
    invalidateSelf();
  }

  /**
   * Get the animation duration for 1 loop by summing all frame durations.
   *
   * @return the duration of 1 animation loop in ms
   */
  public long getLoopDurationMs() {
    if (mAnimationBackend == null) {
      return 0;
    }
    if (mFrameScheduler != null) {
      return mFrameScheduler.getLoopDurationMs();
    }
    int loopDurationMs = 0;
    for (int i = 0; i < mAnimationBackend.getFrameCount(); i++) {
      loopDurationMs += mAnimationBackend.getFrameDurationMs(i);
    }
    return loopDurationMs;
  }

  /**
   * Get the number of frames for the animation. If no animation backend is set, 0 will be returned.
   *
   * @return the number of frames of the animation
   */
  public int getFrameCount() {
    return mAnimationBackend == null ? 0 : mAnimationBackend.getFrameCount();
  }

  /**
   * Get the duration of a specific frame. If not animation backend is set, 0 will be returned.
   *
   * @param frameNumber the requested frame
   * @return the duration of the frame
   */
  public int getFrameDurationMs(int frameNumber) {
    return mAnimationBackend == null ? 0 : mAnimationBackend.getFrameDurationMs(frameNumber);
  }

  /**
   * Get the loop count of the animation. The returned value is either
   * [AnimationInformation#LOOP_COUNT_INFINITE] if the animation is repeated infinitely or a
   * positive integer that corresponds to the number of loops. If no animation backend is set,
   * [AnimationInformation#LOOP_COUNT_INFINITE] will be returned.
   *
   * @return the loop count of the animation or [AnimationInformation#LOOP_COUNT_INFINITE]
   */
  public int getLoopCount() {
    return mAnimationBackend == null ? 0 : mAnimationBackend.getLoopCount();
  }

  /**
   * Frame scheduling delay to shift the target render time for a frame within the frame's visible
   * window. If the value is set to 0, the frame will be scheduled right at the beginning of the
   * frame's visible window.
   *
   * @param frameSchedulingDelayMs the delay to use in ms
   */
  public void setFrameSchedulingDelayMs(long frameSchedulingDelayMs) {
    this.mFrameSchedulingDelayMs = frameSchedulingDelayMs;
  }

  /**
   * Frame scheduling offset to shift the animation time by the given offset. This is similar to
   * [mFrameSchedulingDelayMs] but instead of delaying the invalidation, this offsets the animation
   * time by the given value.
   *
   * @param frameSchedulingOffsetMs the offset to use in ms
   */
  public void setFrameSchedulingOffsetMs(long frameSchedulingOffsetMs) {
    this.mFrameSchedulingOffsetMs = frameSchedulingOffsetMs;
  }

  /**
   * Set an animation listener that is notified for various animation events.
   *
   * @param animationListener the listener to use
   */
  public void setAnimationListener(@Nullable AnimationListener animationListener) {
    this.mAnimationListener = animationListener != null ? animationListener : NO_OP_LISTENER;
  }

  /**
   * Set a draw listener that is notified for each [draw(Canvas)] call.
   *
   * @param drawListener the listener to use
   */
  public void setDrawListener(@Nullable DrawListener drawListener) {
    this.mDrawListener = drawListener;
  }

  /**
   * Schedule the next frame to be rendered after the given delay.
   *
   * @param targetAnimationTimeMs the time in ms to update the frame
   */
  private void scheduleNextFrame(long targetAnimationTimeMs) {
    mExpectedRenderTimeMs = mStartTimeMs + targetAnimationTimeMs;
    scheduleSelf(mInvalidateRunnable, mExpectedRenderTimeMs);
  }

  private void onFrameDropped() {
    mDroppedFrames++;
    // we need to drop frames
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "Dropped a frame. Count: %s", mDroppedFrames);
    }
  }

  /**
   * @return the current uptime in ms
   */
  private long now() {
    // This call has to return [SystemClock#uptimeMillis()] in order to preserve correct
    // frame scheduling.
    return SystemClock.uptimeMillis();
  }

  @Nullable
  private static FrameScheduler createSchedulerForBackendAndDelayMethod(
      @Nullable AnimationBackend animationBackend) {
    if (animationBackend == null) {
      return null;
    }
    return new DropFramesFrameScheduler(animationBackend);
  }

  /**
   * Set the animation to the given level. The level represents the animation time in ms. If the
   * animation time is greater than the last frame time for the last loop, the last frame will be
   * displayed.
   *
   * <p>If the animation is running (e.g. if [start()] has been called, the level change will be
   * ignored. In this case, [stop()] the animation first.
   *
   * @param level the animation time in ms
   * @return true if the level change could be performed
   */
  @Override
  protected boolean onLevelChange(int level) {
    if (mIsRunning) {
      // If the client called start on us, they expect us to run the animation. In that case,
      // we ignore level changes.
      return false;
    }
    if (mLastFrameAnimationTimeMs != level) {
      mLastFrameAnimationTimeMs = level;
      invalidateSelf();
      return true;
    }
    return false;
  }

  @Override
  public void dropCaches() {
    if (mAnimationBackend != null) {
      mAnimationBackend.clear();
    }
  }
}
