/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.backend;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.time.MonotonicClock;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * Animation backend delegate for animation backends that implement {@link InactivityListener}.
 * After a certain inactivity period (default = {@link #INACTIVITY_THRESHOLD_MS},
 * {@link InactivityListener#onInactive()} will be called.
 *
 * This can for example be used to drop caches if needed.
 *
 * New instances can be created with
 * {@link #createForBackend(AnimationBackend, MonotonicClock, ScheduledExecutorService)}.
 */
public class AnimationBackendDelegateWithInactivityCheck<T extends AnimationBackend>
    extends AnimationBackendDelegate<T> {

  public interface InactivityListener {

    /**
     * Called when the animation backend has not been used to draw frames within
     * the given threshold.
     */
    void onInactive();
  }

  public static <T extends AnimationBackend &
      AnimationBackendDelegateWithInactivityCheck.InactivityListener>
  AnimationBackendDelegate<T> createForBackend(
      T backend,
      MonotonicClock monotonicClock,
      ScheduledExecutorService scheduledExecutorServiceForUiThread) {
    return createForBackend(backend, backend, monotonicClock, scheduledExecutorServiceForUiThread);
  }

  public static <T extends AnimationBackend>
  AnimationBackendDelegate<T> createForBackend(
      T backend,
      InactivityListener inactivityListener,
      MonotonicClock monotonicClock,
      ScheduledExecutorService scheduledExecutorServiceForUiThread) {
    return new AnimationBackendDelegateWithInactivityCheck<>(
        backend,
        inactivityListener,
        monotonicClock,
        scheduledExecutorServiceForUiThread);
  }

  @VisibleForTesting
  static final long INACTIVITY_THRESHOLD_MS = 2000;
  @VisibleForTesting
  static final long INACTIVITY_CHECK_POLLING_TIME_MS = 1000;

  private final MonotonicClock mMonotonicClock;
  private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;
  private boolean mInactivityCheckScheduled = false;
  private long mLastDrawnTimeMs;
  private long mInactivityThresholdMs = INACTIVITY_THRESHOLD_MS;
  private long mInactivityCheckPollingTimeMs = INACTIVITY_CHECK_POLLING_TIME_MS;
  @Nullable
  private InactivityListener mInactivityListener;

  /**
   * Watchdog runnable that calls {@link InactivityListener#onInactive()} if necessary
   * or schedules a new watchdog task otherwise.
   */
  private final Runnable mIsInactiveCheck = new Runnable() {
    @Override
    public void run() {
      synchronized (AnimationBackendDelegateWithInactivityCheck.this) {
        mInactivityCheckScheduled = false;
        if (isInactive()) {
          if (mInactivityListener != null) {
            mInactivityListener.onInactive();
          }
        } else {
          maybeScheduleInactivityCheck();
        }
      }
    }
  };

  private AnimationBackendDelegateWithInactivityCheck(
      @Nullable T animationBackend,
      @Nullable InactivityListener inactivityListener,
      MonotonicClock monotonicClock,
      ScheduledExecutorService scheduledExecutorServiceForUiThread) {
    super(animationBackend);
    mInactivityListener = inactivityListener;
    mMonotonicClock = monotonicClock;
    mScheduledExecutorServiceForUiThread = scheduledExecutorServiceForUiThread;
  }

  @Override
  public boolean drawFrame(Drawable parent, Canvas canvas, int frameNumber) {
    mLastDrawnTimeMs = mMonotonicClock.now();
    boolean result = super.drawFrame(parent, canvas, frameNumber);
    maybeScheduleInactivityCheck();
    return result;
  }

  public void setInactivityListener(@Nullable InactivityListener inactivityListener) {
    mInactivityListener = inactivityListener;
  }

  public long getInactivityCheckPollingTimeMs() {
    return mInactivityCheckPollingTimeMs;
  }

  public void setInactivityCheckPollingTimeMs(long inactivityCheckPollingTimeMs) {
    mInactivityCheckPollingTimeMs = inactivityCheckPollingTimeMs;
  }

  public long getInactivityThresholdMs() {
    return mInactivityThresholdMs;
  }

  public void setInactivityThresholdMs(long inactivityThresholdMs) {
    mInactivityThresholdMs = inactivityThresholdMs;
  }

  private boolean isInactive() {
    return mMonotonicClock.now() - mLastDrawnTimeMs > mInactivityThresholdMs;
  }

  private synchronized void maybeScheduleInactivityCheck() {
    if (!mInactivityCheckScheduled) {
      mInactivityCheckScheduled = true;
      mScheduledExecutorServiceForUiThread.schedule(
          mIsInactiveCheck,
          mInactivityCheckPollingTimeMs,
          TimeUnit.MILLISECONDS);
    }
  }
}
