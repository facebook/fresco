/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.testing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.facebook.common.time.MonotonicClock;

/**
 *
 *
 */
public class FakeClock implements MonotonicClock {

  public interface OnTickListener {
    void onTick();
  }

  private final Map<OnTickListener, OnTickListener> otherClocks =
      new ConcurrentHashMap<OnTickListener, OnTickListener>();
  private final Map<OnTickListener, OnTickListener> listeners =
      new ConcurrentHashMap<OnTickListener, OnTickListener>();

  private long now;

  public FakeClock() {
    now = 100000;
  }

  public FakeClock(long now) {
    this.now = now;
  }

  public void incrementBy(long amountMs) {
    synchronized (this) {
      now += amountMs;
    }
    // Update other clocks before dispatching to listeners.  This is not a perfect solution, since
    // the other clocks might still dispatch events before subsequent clocks are updated.  All
    // clocks should be forced to update their time before dispatching events so all clocks are
    // guaranteed in sync, but it's better than the random order of having everything in a single
    // listeners Map where some events will be dispatched before other clocks are updated depending
    // on hashing randomness.
    for (OnTickListener otherClock : otherClocks.keySet()) {
      otherClock.onTick();
    }
    for (OnTickListener listener : listeners.keySet()) {
      listener.onTick();
    }
  }

  /**
   * Increments the clock's time gradually in several steps
   *
   * The clock will be incremented by tickMs amount repeatedly until it has progress by totalMs
   * from the given time when this call was made.
   *
   * This call is useful for working with animations in tests.
   *
   * In the end of this call, the call will progress by exactly totalMs, even if it means the last
   * tick of the clock is smaller than tickMs
   *
   * @param tickMs the amount to tick the clock at each step
   * @param totalMs the total amount of time to elapse during the call
   */
  public void incrementInSteps(long tickMs, long totalMs) {
    long start = now;
    long end = start + totalMs;
    while (now < end) {
      incrementBy(Math.min(tickMs, end - now));
    }
  }

  @Override
  public synchronized long now() {
    return now;
  }

  /**
   * Add a clock that will get called before {@link #listeners}, in order to guarantee call order.
   */
  public void addOtherClock(OnTickListener otherClock) {
    otherClocks.put(otherClock, otherClock);
  }

  public void addListener(OnTickListener listener) {
    listeners.put(listener, listener);
  }

  public void removeListener(OnTickListener listener) {
    listeners.remove(listener);
  }
}
