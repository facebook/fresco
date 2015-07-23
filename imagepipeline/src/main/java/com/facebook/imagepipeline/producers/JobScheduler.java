/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.producers;

import javax.annotation.concurrent.GuardedBy;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.os.SystemClock;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.image.EncodedImage;

/**
 * Manages jobs so that only one can be executed at a time and no more often than once in
 * <code>mMinimumJobIntervalMs</code> milliseconds.
 */
public class JobScheduler {

  static final String QUEUE_TIME_KEY = "queueTime";

  @VisibleForTesting
  static class JobStartExecutorSupplier {

    private static ScheduledExecutorService sJobStarterExecutor;

    static ScheduledExecutorService get() {
      if (sJobStarterExecutor == null) {
        sJobStarterExecutor = Executors.newSingleThreadScheduledExecutor();
      }
      return sJobStarterExecutor;
    }
  }

  public interface JobRunnable {
    void run(EncodedImage encodedImage, boolean isLast);
  }

  private final Executor mExecutor;
  private final JobRunnable mJobRunnable;
  private final Runnable mDoJobRunnable;
  private final Runnable mSubmitJobRunnable;
  private final int mMinimumJobIntervalMs;

  @VisibleForTesting
  enum JobState { IDLE, QUEUED, RUNNING, RUNNING_AND_PENDING }

  // job data
  @GuardedBy("this")
  @VisibleForTesting EncodedImage mEncodedImage;
  @GuardedBy("this")
  @VisibleForTesting boolean mIsLast;

  // job state
  @GuardedBy("this")
  @VisibleForTesting JobState mJobState;
  @GuardedBy("this")
  @VisibleForTesting long mJobSubmitTime;
  @GuardedBy("this")
  @VisibleForTesting long mJobStartTime;

  public JobScheduler(Executor executor, JobRunnable jobRunnable, int minimumJobIntervalMs) {
    mExecutor = executor;
    mJobRunnable = jobRunnable;
    mMinimumJobIntervalMs = minimumJobIntervalMs;
    mDoJobRunnable = new Runnable() {
      @Override
      public void run() {
        doJob();
      }
    };
    mSubmitJobRunnable = new Runnable() {
      @Override
      public void run() {
        submitJob();
      }
    };
    mEncodedImage = null;
    mIsLast = false;
    mJobState = JobState.IDLE;
    mJobSubmitTime = 0;
    mJobStartTime = 0;
  }

  /**
   * Clears the currently set job.
   *
   * <p> In case the currently set job has been scheduled but not started yet, the job won't be
   * executed.
   */
  public void clearJob() {
    EncodedImage oldEncodedImage;
    synchronized (this) {
      oldEncodedImage = mEncodedImage;
      mEncodedImage = null;
      mIsLast = false;
    }
    EncodedImage.closeSafely(oldEncodedImage);
  }

  /**
   * Updates the job.
   *
   * <p> This just updates the job, but it doesn't schedule it. In order to be executed, the job has
   * to be scheduled after being set. In case there was a previous job scheduled that has not yet
   * started, this new job will be executed instead.
   *
   * @return whether the job was successfully updated.
   */
  public boolean updateJob(EncodedImage encodedImage, boolean isLast) {
    if (!shouldProcess(encodedImage, isLast)) {
      return false;
    }
    EncodedImage oldEncodedImage;
    synchronized (this) {
      oldEncodedImage = mEncodedImage;
      mEncodedImage = EncodedImage.cloneOrNull(encodedImage);
      mIsLast = isLast;
    }
    EncodedImage.closeSafely(oldEncodedImage);
    return true;
  }

  /**
   * Schedules the currently set job (if any).
   *
   * <p> This method can be called multiple times. It is guaranteed that each job set will be
   * executed no more than once. It is guaranteed that the last job set will be executed, unless
   * the job was cleared first.
   * <p> The job will be scheduled no sooner than <code>minimumJobIntervalMs</code> milliseconds
   * since the last job started.
   *
   * @return true if the job was scheduled, false if there was no valid job to be scheduled
   */
  public boolean scheduleJob() {
    long now = SystemClock.uptimeMillis();
    long when = 0;
    boolean shouldEnqueue = false;
    synchronized (this) {
      if (!shouldProcess(mEncodedImage, mIsLast)) {
        return false;
      }
      switch (mJobState) {
        case IDLE:
          when = Math.max(mJobStartTime + mMinimumJobIntervalMs, now);
          shouldEnqueue = true;
          mJobSubmitTime = now;
          mJobState = JobState.QUEUED;
          break;
        case QUEUED:
          // do nothing, the job is already queued
          break;
        case RUNNING:
          mJobState = JobState.RUNNING_AND_PENDING;
          break;
        case RUNNING_AND_PENDING:
          // do nothing, the next job is already pending
          break;
      }
    }
    if (shouldEnqueue) {
      enqueueJob(when - now);
    }
    return true;
  }

  private void enqueueJob(long delay) {
    // If we make mExecutor be a {@link ScheduledexecutorService}, we could just have
    // `mExecutor.schedule(mDoJobRunnable, delay)` and avoid mSubmitJobRunnable and
    // JobStartExecutorSupplier altogether. That would require some refactoring though.
    if (delay > 0) {
      JobStartExecutorSupplier.get().schedule(mSubmitJobRunnable, delay, TimeUnit.MILLISECONDS);
    } else {
      mSubmitJobRunnable.run();
    }
  }

  private void submitJob() {
    mExecutor.execute(mDoJobRunnable);
  }

  private void doJob() {
    long now = SystemClock.uptimeMillis();
    EncodedImage input;
    boolean isLast;
    synchronized (this) {
      input = mEncodedImage;
      isLast = mIsLast;
      mEncodedImage = null;
      mIsLast = false;
      mJobState = JobState.RUNNING;
      mJobStartTime = now;
    }

    try {
      // we need to do a check in case the job got cleared in the meantime
      if (shouldProcess(input, isLast)) {
        mJobRunnable.run(input, isLast);
      }
    } finally {
      EncodedImage.closeSafely(input);
      onJobFinished();
    }
  }

  private void onJobFinished() {
    long now = SystemClock.uptimeMillis();
    long when = 0;
    boolean shouldEnqueue = false;
    synchronized (this) {
      if (mJobState == JobState.RUNNING_AND_PENDING) {
        when = Math.max(mJobStartTime + mMinimumJobIntervalMs, now);
        shouldEnqueue = true;
        mJobSubmitTime = now;
        mJobState = JobState.QUEUED;
      } else {
        mJobState = JobState.IDLE;
      }
    }
    if (shouldEnqueue) {
      enqueueJob(when - now);
    }
  }

  private static boolean shouldProcess(EncodedImage encodedImage, boolean isLast) {
    // the last result should always be processed, whereas
    // an intermediate result should be processed only if valid
    return  isLast || EncodedImage.isValid(encodedImage);
  }

  /**
   * Gets the queued time in milliseconds for the currently running job.
   *
   * <p> The result is only valid if called from {@link JobRunnable#run}.
   */
  public synchronized long getQueuedTime() {
    return mJobStartTime - mJobSubmitTime;
  }
}
