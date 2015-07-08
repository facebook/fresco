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
import android.view.ViewConfiguration;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static android.view.MotionEvent.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link GestureDetector}
 */
@RunWith(RobolectricTestRunner.class)
public class GestureDetectorTest {

  private GestureDetector.ClickListener mClickListener;
  private ViewConfiguration mViewConfiguration;
  private long mScaledTouchSlop;
  private long mLongPressTimeout;
  private GestureDetector mGestureDetector;

  @Before
  public void setup() {
    mClickListener = mock(GestureDetector.ClickListener.class);
    mViewConfiguration = ViewConfiguration.get(Robolectric.application);
    mScaledTouchSlop = mViewConfiguration.getScaledTouchSlop();
    mLongPressTimeout = mViewConfiguration.getLongPressTimeout();
    mGestureDetector = new GestureDetector(Robolectric.application);
    mGestureDetector.setClickListener(mClickListener);
  }

  @Test
  public void testInitialstate() {
    assertEquals(mScaledTouchSlop, mGestureDetector.mSingleTapSlopPx, 0f);
    assertEquals(false, mGestureDetector.mIsCapturingGesture);
    assertEquals(false, mGestureDetector.mIsClickCandidate);
  }

  @Test
  public void testSetClickListener() {
    GestureDetector.ClickListener clickListener = mock(GestureDetector.ClickListener.class);
    mGestureDetector.setClickListener(clickListener);
    assertSame(clickListener, mGestureDetector.mClickListener);
    mGestureDetector.setClickListener(null);
    assertSame(null, mGestureDetector.mClickListener);
  }

  @Test
  public void testOnClick_NoListener() {
    MotionEvent event1 = obtain(1000, 1000, ACTION_DOWN, 100.f, 100.f, 0);
    MotionEvent event2 = obtain(1000, 1001, ACTION_UP, 100.f, 100.f, 0);

    mGestureDetector.setClickListener(mClickListener);
    mGestureDetector.onTouchEvent(event1);
    mGestureDetector.onTouchEvent(event2);
    verify(mClickListener).onClick();

    mGestureDetector.setClickListener(null);
    mGestureDetector.onTouchEvent(event1);
    mGestureDetector.onTouchEvent(event2);
    verifyNoMoreInteractions(mClickListener);

    event1.recycle();
    event2.recycle();
  }

  @Test
  public void testOnClick_Valid() {
    float s = mScaledTouchSlop;
    long T0 = 1000;
    long T1 = T0;
    MotionEvent event1 = obtain(T0, T1, ACTION_DOWN, 100.f, 100.f, 0);
    mGestureDetector.onTouchEvent(event1);
    assertEquals(true, mGestureDetector.mIsCapturingGesture);
    assertEquals(true, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);

    long T2 = T0 + mLongPressTimeout * 1 / 3;
    MotionEvent event2 = obtain(T0, T2, ACTION_MOVE, 100.f + s * 0.3f, 100.f - s * 0.3f, 0);
    mGestureDetector.onTouchEvent(event2);
    assertEquals(true, mGestureDetector.mIsCapturingGesture);
    assertEquals(true, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);

    long T3 = T0 + mLongPressTimeout * 2 / 3;
    MotionEvent event3 = obtain(T0, T3, ACTION_MOVE, 100.f + s * 0.6f, 100.f - s * 0.6f, 0);
    mGestureDetector.onTouchEvent(event3);
    assertEquals(true, mGestureDetector.mIsCapturingGesture);
    assertEquals(true, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);

    long T4 = T0 + mLongPressTimeout;
    MotionEvent event4 = obtain(T0, T4, ACTION_UP, 100.f + s, 100.f - s, 0);
    mGestureDetector.onTouchEvent(event4);
    assertEquals(false, mGestureDetector.mIsCapturingGesture);
    assertEquals(false, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);
    verify(mClickListener).onClick();

    event1.recycle();
    event2.recycle();
    event3.recycle();
    event4.recycle();
  }

  @Test
  public void testOnClick_ToFar() {
    float s = mScaledTouchSlop;
    long T0 = 1000;
    long T1 = T0;
    MotionEvent event1 = obtain(T0, T1, ACTION_DOWN, 100.f, 100.f, 0);
    mGestureDetector.onTouchEvent(event1);
    assertEquals(true, mGestureDetector.mIsCapturingGesture);
    assertEquals(true, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);

    long T2 = T0 + mLongPressTimeout * 1 / 3;
    MotionEvent event2 = obtain(T0, T2, ACTION_MOVE, 100.f + s * 0.5f, 100.f - s * 0.5f, 0);
    mGestureDetector.onTouchEvent(event2);
    assertEquals(true, mGestureDetector.mIsCapturingGesture);
    assertEquals(true, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);

    // maximum allowed distance is `s` px, but here we went `s * 1.1` px away from down point
    long T3 = T0 + mLongPressTimeout * 2 / 3;
    MotionEvent event3 = obtain(T0, T3, ACTION_MOVE, 100.f + s * 1.1f, 100.f - s * 0.5f, 0);
    mGestureDetector.onTouchEvent(event3);
    assertEquals(true, mGestureDetector.mIsCapturingGesture);
    assertEquals(false, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);

    long T4 = T0 + mLongPressTimeout;
    MotionEvent event4 = obtain(T0, T4, ACTION_UP, 100.f + s, 100.f - s, 0);
    mGestureDetector.onTouchEvent(event4);
    assertEquals(false, mGestureDetector.mIsCapturingGesture);
    assertEquals(false, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);
    verifyNoMoreInteractions(mClickListener);

    event1.recycle();
    event2.recycle();
    event3.recycle();
    event4.recycle();
  }

  @Test
  public void testOnClick_ToLong() {
    float s = mScaledTouchSlop;
    long T0 = 1000;
    long T1 = T0;
    MotionEvent event1 = obtain(T0, T1, ACTION_DOWN, 100.f, 100.f, 0);
    mGestureDetector.onTouchEvent(event1);
    assertEquals(true, mGestureDetector.mIsCapturingGesture);
    assertEquals(true, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);

    long T2 = T0 + mLongPressTimeout * 1 / 3;
    MotionEvent event2 = obtain(T0, T2, ACTION_MOVE, 100.f + s * 0.3f, 100.f - s * 0.3f, 0);
    mGestureDetector.onTouchEvent(event2);
    assertEquals(true, mGestureDetector.mIsCapturingGesture);
    assertEquals(true, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);

    long T3 = T0 + mLongPressTimeout * 2 / 3;
    MotionEvent event3 = obtain(T0, T3, ACTION_MOVE, 100.f + s * 0.6f, 100.f - s * 0.6f, 0);
    mGestureDetector.onTouchEvent(event3);
    assertEquals(true, mGestureDetector.mIsCapturingGesture);
    assertEquals(true, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);

    // maximum allowed duration is mLongPressTimeout ms, but here we released 1 ms after that
    long T4 = T0 + mLongPressTimeout + 1;
    MotionEvent event4 = obtain(T0, T4, ACTION_UP, 100.f + s, 100.f - s, 0);
    mGestureDetector.onTouchEvent(event4);
    assertEquals(false, mGestureDetector.mIsCapturingGesture);
    assertEquals(false, mGestureDetector.mIsClickCandidate);
    assertEquals(event1.getEventTime(), mGestureDetector.mActionDownTime);
    assertEquals(event1.getX(), mGestureDetector.mActionDownX, 0f);
    assertEquals(event1.getY(), mGestureDetector.mActionDownY, 0f);
    verifyNoMoreInteractions(mClickListener);

    event1.recycle();
    event2.recycle();
    event3.recycle();
    event4.recycle();
  }
}
