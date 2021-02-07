/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.zoomable;

import android.view.GestureDetector;
import android.view.MotionEvent;
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.List;

/**
 * Gesture listener that allows multiple child listeners to be added and notified about gesture
 * events.
 *
 * <p>NOTE: The order of the listeners is important. Listeners can consume gesture events. For
 * example, if one of the child listeners consumes {@link #onLongPress(MotionEvent)} (the listener
 * returned true), subsequent listeners will not be notified about the event any more since it has
 * been consumed.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class MultiGestureListener extends GestureDetector.SimpleOnGestureListener {

  private final List<GestureDetector.SimpleOnGestureListener> mListeners = new ArrayList<>();

  /**
   * Adds a listener to the multi gesture listener.
   *
   * <p>NOTE: The order of the listeners is important since gesture events can be consumed.
   *
   * @param listener the listener to be added
   */
  public synchronized void addListener(GestureDetector.SimpleOnGestureListener listener) {
    mListeners.add(listener);
  }

  /**
   * Removes the given listener so that it will not be notified about future events.
   *
   * <p>NOTE: The order of the listeners is important since gesture events can be consumed.
   *
   * @param listener the listener to remove
   */
  public synchronized void removeListener(GestureDetector.SimpleOnGestureListener listener) {
    mListeners.remove(listener);
  }

  @Override
  public synchronized boolean onSingleTapUp(MotionEvent e) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      if (mListeners.get(i).onSingleTapUp(e)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized void onLongPress(MotionEvent e) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      mListeners.get(i).onLongPress(e);
    }
  }

  @Override
  public synchronized boolean onScroll(
      MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      if (mListeners.get(i).onScroll(e1, e2, distanceX, distanceY)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized boolean onFling(
      MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      if (mListeners.get(i).onFling(e1, e2, velocityX, velocityY)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized void onShowPress(MotionEvent e) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      mListeners.get(i).onShowPress(e);
    }
  }

  @Override
  public synchronized boolean onDown(MotionEvent e) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      if (mListeners.get(i).onDown(e)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized boolean onDoubleTap(MotionEvent e) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      if (mListeners.get(i).onDoubleTap(e)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized boolean onDoubleTapEvent(MotionEvent e) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      if (mListeners.get(i).onDoubleTapEvent(e)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized boolean onSingleTapConfirmed(MotionEvent e) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      if (mListeners.get(i).onSingleTapConfirmed(e)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized boolean onContextClick(MotionEvent e) {
    final int size = mListeners.size();
    for (int i = 0; i < size; i++) {
      if (mListeners.get(i).onContextClick(e)) {
        return true;
      }
    }
    return false;
  }
}
