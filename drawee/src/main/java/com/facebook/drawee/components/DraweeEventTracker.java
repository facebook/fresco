/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.components;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class keeps a record of internal events that take place in the Drawee.
 * <p/> Having a record of a last few events is useful for debugging purposes. If you want to
 * disable it, call {@link DraweeEventTracker.disable()} before {@link Fresco.initialize()}.
 */
public class DraweeEventTracker {

  private final Queue<Event> mEventQueue = new ArrayBlockingQueue<>(MAX_EVENTS_TO_TRACK);

  private static final int MAX_EVENTS_TO_TRACK = 20;
  private static final DraweeEventTracker sInstance = new DraweeEventTracker();

  private static boolean sEnabled = true;

  public enum Event {
    ON_SET_HIERARCHY,
    ON_CLEAR_HIERARCHY,
    ON_SET_CONTROLLER,
    ON_CLEAR_OLD_CONTROLLER,
    ON_CLEAR_CONTROLLER,
    ON_INIT_CONTROLLER,
    ON_ATTACH_CONTROLLER,
    ON_DETACH_CONTROLLER,
    ON_RELEASE_CONTROLLER,
    ON_DATASOURCE_SUBMIT,
    ON_DATASOURCE_RESULT,
    ON_DATASOURCE_RESULT_INT,
    ON_DATASOURCE_FAILURE,
    ON_DATASOURCE_FAILURE_INT,
    ON_HOLDER_ATTACH,
    ON_HOLDER_DETACH,
    ON_DRAWABLE_SHOW,
    ON_DRAWABLE_HIDE,
    ON_ACTIVITY_START,
    ON_ACTIVITY_STOP,
    ON_RUN_CLEAR_CONTROLLER,
    ON_SCHEDULE_CLEAR_CONTROLLER,
    ON_SAME_CONTROLLER_SKIPPED,
    ON_SUBMIT_CACHE_HIT
  }

  private DraweeEventTracker() {
  }

  public static DraweeEventTracker newInstance() {
    return sEnabled ? new DraweeEventTracker() : sInstance;
  }

  /**
   * Disable DraweeEventTracker. Need to call before initialize Fresco.
   */
  public static void disable() {
    sEnabled = false;
  }

  public void recordEvent(Event event) {
    if (!sEnabled) {
      return;
    }
    if (mEventQueue.size() + 1 > MAX_EVENTS_TO_TRACK) {
      mEventQueue.poll();
    }
    mEventQueue.add(event);
  }

  @Override
  public String toString() {
    return mEventQueue.toString();
  }
}
