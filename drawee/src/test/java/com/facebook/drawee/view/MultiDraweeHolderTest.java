/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.view;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link MultiDraweeHolder}
 */
@RunWith(RobolectricTestRunner.class)
public class MultiDraweeHolderTest {

  MultiDraweeHolder mMultiHolder;
  DraweeHolder mHolder1;
  DraweeHolder mHolder2;
  DraweeHolder mHolder3;

  @Before
  public void setUp() {
    mMultiHolder = new MultiDraweeHolder();
    mHolder1 = mock(DraweeHolder.class);
    mHolder2 = mock(DraweeHolder.class);
    mHolder3 = mock(DraweeHolder.class);

    mMultiHolder.add(mHolder1);
    mMultiHolder.add(mHolder2);
    mMultiHolder.add(mHolder3);
  }

  @Test
  public void testAttaching() {
    mMultiHolder.onAttach();
    verify(mHolder1).onAttach();
    verify(mHolder2).onAttach();
    verify(mHolder3).onAttach();
    mMultiHolder.onDetach();
    verify(mHolder1).onDetach();
    verify(mHolder2).onDetach();
    verify(mHolder3).onDetach();
  }

  @Test
  public void testTouchEvent_Handled() {
    MotionEvent event = mock(MotionEvent.class);
    when(mHolder1.onTouchEvent(event)).thenReturn(false);
    when(mHolder2.onTouchEvent(event)).thenReturn(true);
    when(mHolder3.onTouchEvent(event)).thenReturn(true);
    boolean ret = mMultiHolder.onTouchEvent(event);
    assertEquals(true, ret);
    verify(mHolder1).onTouchEvent(event);
    verify(mHolder2).onTouchEvent(event);
    verify(mHolder3, never()).onTouchEvent(event);
  }

  @Test
  public void testTouchEvent_NotHandled() {
    MotionEvent event = mock(MotionEvent.class);
    when(mHolder1.onTouchEvent(event)).thenReturn(false);
    when(mHolder2.onTouchEvent(event)).thenReturn(false);
    when(mHolder3.onTouchEvent(event)).thenReturn(false);
    boolean ret = mMultiHolder.onTouchEvent(event);
    assertEquals(false, ret);
    verify(mHolder1).onTouchEvent(event);
    verify(mHolder2).onTouchEvent(event);
    verify(mHolder3).onTouchEvent(event);
  }

  @Test
  public void testClear_Detached() {
    assertEquals(3, mMultiHolder.mHolders.size());
    mMultiHolder.clear();
    assertTrue(mMultiHolder.mHolders.isEmpty());
  }

  @Test
  public void testClear_Attached() {
    mMultiHolder.onAttach();
    reset(mHolder1, mHolder2, mHolder3);

    assertEquals(3, mMultiHolder.mHolders.size());
    mMultiHolder.clear();
    assertTrue(mMultiHolder.mHolders.isEmpty());

    verify(mHolder1).onDetach();
    verify(mHolder2).onDetach();
    verify(mHolder2).onDetach();
  }

  @Test
  public void testAdd_Detached() {
    mMultiHolder.clear();
    reset(mHolder1, mHolder2, mHolder3);

    assertEquals(0, mMultiHolder.mHolders.size());
    mMultiHolder.add(mHolder1);
    assertEquals(1, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    mMultiHolder.add(1, mHolder3);
    assertEquals(2, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    assertEquals(mHolder3, mMultiHolder.mHolders.get(1));
    mMultiHolder.add(1, mHolder2);
    assertEquals(3, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    assertEquals(mHolder2, mMultiHolder.mHolders.get(1));
    assertEquals(mHolder3, mMultiHolder.mHolders.get(2));

    verify(mHolder1, never()).onAttach();
    verify(mHolder2, never()).onAttach();
    verify(mHolder3, never()).onAttach();
  }

  @Test
  public void testAdd_Attached() {
    mMultiHolder.clear();
    mMultiHolder.onAttach();
    reset(mHolder1, mHolder2, mHolder3);

    assertEquals(0, mMultiHolder.mHolders.size());
    mMultiHolder.add(mHolder1);
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    mMultiHolder.add(1, mHolder3);
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    assertEquals(mHolder3, mMultiHolder.mHolders.get(1));
    mMultiHolder.add(1, mHolder2);
    assertEquals(3, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    assertEquals(mHolder2, mMultiHolder.mHolders.get(1));
    assertEquals(mHolder3, mMultiHolder.mHolders.get(2));

    verify(mHolder1).onAttach();
    verify(mHolder2).onAttach();
    verify(mHolder3).onAttach();
  }

  @Test
  public void testRemove_Detached() {
    assertEquals(3, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    assertEquals(mHolder2, mMultiHolder.mHolders.get(1));
    assertEquals(mHolder3, mMultiHolder.mHolders.get(2));
    mMultiHolder.remove(1);
    assertEquals(2, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    assertEquals(mHolder3, mMultiHolder.mHolders.get(1));
    mMultiHolder.remove(1);
    assertEquals(1, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    mMultiHolder.remove(0);
    assertEquals(0, mMultiHolder.mHolders.size());
  }

  @Test
  public void testRemove_attached() {
    mMultiHolder.onAttach();
    reset(mHolder1, mHolder2, mHolder3);

    assertEquals(3, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    assertEquals(mHolder2, mMultiHolder.mHolders.get(1));
    assertEquals(mHolder3, mMultiHolder.mHolders.get(2));
    mMultiHolder.remove(1);
    assertEquals(2, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    assertEquals(mHolder3, mMultiHolder.mHolders.get(1));
    mMultiHolder.remove(1);
    assertEquals(1, mMultiHolder.mHolders.size());
    assertEquals(mHolder1, mMultiHolder.mHolders.get(0));
    mMultiHolder.remove(0);
    assertEquals(0, mMultiHolder.mHolders.size());

    verify(mHolder1).onDetach();
    verify(mHolder2).onDetach();
    verify(mHolder3).onDetach();
  }

  @Test
  public void testGet() {
    assertEquals(mHolder1, mMultiHolder.get(0));
    assertEquals(mHolder2, mMultiHolder.get(1));
    assertEquals(mHolder3, mMultiHolder.get(2));
  }

  @Test
  public void testDraw() {
    Canvas canvas = mock(Canvas.class);
    Drawable drawable1 = mock(Drawable.class);
    Drawable drawable2 = mock(Drawable.class);
    Drawable drawable3 = mock(Drawable.class);
    when(mHolder1.getTopLevelDrawable()).thenReturn(drawable1);
    when(mHolder2.getTopLevelDrawable()).thenReturn(drawable2);
    when(mHolder3.getTopLevelDrawable()).thenReturn(drawable3);

    mMultiHolder.draw(canvas);

    verify(drawable1).draw(canvas);
    verify(drawable2).draw(canvas);
    verify(drawable3).draw(canvas);
  }

  @Test
  public void testVerifyDrawable() {
    Drawable drawable1 = mock(Drawable.class);
    Drawable drawable2 = mock(Drawable.class);
    when(mHolder1.getTopLevelDrawable()).thenReturn(drawable1);
    assertTrue(mMultiHolder.verifyDrawable(drawable1));
    assertFalse(mMultiHolder.verifyDrawable(drawable2));
  }
}
