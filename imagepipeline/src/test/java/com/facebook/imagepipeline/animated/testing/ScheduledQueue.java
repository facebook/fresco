/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.testing;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.testing.FakeClock;

/**
 * A queue that executes items according to the passage of time.
 */
public class ScheduledQueue {

  private final FakeClock clock;
  private final DeltaQueue<Runnable> deltaQueue = new DeltaQueue<Runnable>();
  private long now;
  private boolean isTicking;
  private long tickRemaining;

  public ScheduledQueue(FakeClock clock) {
    this.clock = clock;
    now = clock.now();
    clock.addListener(
        new FakeClock.OnTickListener() {
          @Override
          public void onTick() {
            ScheduledQueue.this.tick();
          }
        });
  }

  /**
   * Runs time forwards by a given duration, executing any commands scheduled for
   * execution during that time period, and any background tasks spawned by the
   * scheduled tasks.  Therefore, when a call to tick returns, the executor
   * will be idle.
   */
  private void tick() {
    long newNow = clock.now();
    Preconditions.checkState(!isTicking);
    isTicking = true;
    try {
      tickRemaining = newNow - now;
      now = newNow;
      do {
        tickRemaining = deltaQueue.tick(tickRemaining);
        runUntilIdle();
      } while (deltaQueue.isNotEmpty() && tickRemaining > 0);
      Preconditions.checkState(isTicking);
    } finally {
      isTicking = false;
      tickRemaining = 0;
    }
  }

  /**
   * Runs all commands scheduled to be executed immediately but does
   * not tick time forward.
   */
  public void runUntilIdle() {
    while (!isIdle()) {
      runNextPendingCommand();
    }
  }

  /**
   * Gets whether the queue is idle.
   *
   * @return whether there are no more items in the queue that are ready to be processed
   */
  public boolean isIdle() {
    return deltaQueue.isEmpty() || deltaQueue.delay() > 0;
  }

  /**
   * Runs the next command scheduled to be executed immediately.
   */
  public void runNextPendingCommand() {
    Runnable runnable = deltaQueue.pop();
    runnable.run();
  }

  /**
   * Returns how much time left until next command.
   *
   * If there are no pending commands left will throw and exception.
   */
  public long getNextPendingCommandDelay() {
    if (getPendingCount() == 0) {
      throw new IllegalStateException();
    }
    return deltaQueue.delay();
  }

  /**
   * Adds a task to the queue that is ready to be executed.
   *
   * @param runnable the task to execute
   */
  public void add(Runnable runnable) {
    deltaQueue.add(tickRemaining, runnable);
  }

  /**
   * Adds a task to the queue that is to be executed to after the specified delay.
   *
   * @param runnable the task to execute
   * @param delayMs the delay before the task should be executed
   */
  public void add(Runnable runnable, long delayMs) {
    deltaQueue.add(tickRemaining + delayMs, runnable);
  }

  /**
   * Removes a task from the queue
   * @param runnable The runnable to remove
   * @return whether the element was removed
   */
  public boolean remove(Runnable runnable) {
    return deltaQueue.remove(runnable);
  }

  /**
   * Gets the count of queued runnables, regardless of whether they are ready to be run.
   *
   * @return the count of queued runnables
   */
  public int getPendingCount() {
    return deltaQueue.size();
  }
}
