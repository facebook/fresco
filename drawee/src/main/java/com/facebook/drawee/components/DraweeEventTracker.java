/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.components;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class keeps a record of internal events that take place in the Drawee.
 * <p/> Having a record of a last few events is useful for debugging purposes.
 */
public class DraweeEventTracker {

  private static final int MAX_EVENTS_TO_TRACK = 20;

  public static enum Event {
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
    ON_ACTIVITY_STOP
  }

  private final Queue<Event> mEventQueue = new ArrayBlockingQueue<Event>(MAX_EVENTS_TO_TRACK);

  public void recordEvent(Event event) {
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
