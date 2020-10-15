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

/** Wrapper for SimpleOnGestureListener as GestureDetector does not allow changing its listener. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class GestureListenerWrapper extends GestureDetector.SimpleOnGestureListener {

  private GestureDetector.SimpleOnGestureListener mDelegate;

  public GestureListenerWrapper() {
    mDelegate = new GestureDetector.SimpleOnGestureListener();
  }

  public void setListener(GestureDetector.SimpleOnGestureListener listener) {
    mDelegate = listener;
  }

  @Override
  public void onLongPress(MotionEvent e) {
    mDelegate.onLongPress(e);
  }

  @Override
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    return mDelegate.onScroll(e1, e2, distanceX, distanceY);
  }

  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    return mDelegate.onFling(e1, e2, velocityX, velocityY);
  }

  @Override
  public void onShowPress(MotionEvent e) {
    mDelegate.onShowPress(e);
  }

  @Override
  public boolean onDown(MotionEvent e) {
    return mDelegate.onDown(e);
  }

  @Override
  public boolean onDoubleTap(MotionEvent e) {
    return mDelegate.onDoubleTap(e);
  }

  @Override
  public boolean onDoubleTapEvent(MotionEvent e) {
    return mDelegate.onDoubleTapEvent(e);
  }

  @Override
  public boolean onSingleTapConfirmed(MotionEvent e) {
    return mDelegate.onSingleTapConfirmed(e);
  }

  @Override
  public boolean onSingleTapUp(MotionEvent e) {
    return mDelegate.onSingleTapUp(e);
  }
}
