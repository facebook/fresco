/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.gestures;

import android.view.MotionEvent;

public class MotionEventTestUtils {
  public static MotionEvent.PointerCoords createCoords(float x, float y) {
    MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
    pointerCoords.x = x;
    pointerCoords.y = y;
    return pointerCoords;
  }

  public static MotionEvent obtainMotionEvent(
      long downTime,
      long eventTime,
      int action,
      int id1,
      float x1,
      float y1) {
    int[] ids = new int[] {id1};
    MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[] {createCoords(x1, y1)};
    return MotionEvent
        .obtain(downTime, eventTime, action, 1, ids, coords, 0, 1.0f, 1.0f, 0, 0, 0, 0);
  }

  public static MotionEvent obtainMotionEvent(
      long downTime,
      long eventTime,
      int action,
      int id1,
      float x1,
      float y1,
      int id2,
      float x2,
      float y2) {
    int[] ids = new int[] {id1, id2};
    MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[] {
        createCoords(x1, y1),
        createCoords(x2, y2)};
    return MotionEvent
        .obtain(downTime, eventTime, action, 2, ids, coords, 0, 1.0f, 1.0f, 0, 0, 0, 0);
  }
}
