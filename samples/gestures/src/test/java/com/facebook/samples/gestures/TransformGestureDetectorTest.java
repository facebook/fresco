/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.gestures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.MotionEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MultiPointerGestureDetector} */
@RunWith(RobolectricTestRunner.class)
public class TransformGestureDetectorTest {

  private TransformGestureDetector.Listener mListener;
  private MultiPointerGestureDetector mMultiPointerGestureDetector;
  private TransformGestureDetector mGestureDetector;

  @Before
  public void setup() {
    mListener = mock(TransformGestureDetector.Listener.class);
    mMultiPointerGestureDetector = mock(MultiPointerGestureDetector.class);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    when(mMultiPointerGestureDetector.getStartX()).thenReturn(new float[] {100f, 200f});
    when(mMultiPointerGestureDetector.getStartY()).thenReturn(new float[] {500f, 600f});
    when(mMultiPointerGestureDetector.getCurrentX()).thenReturn(new float[] {10f, 20f});
    when(mMultiPointerGestureDetector.getCurrentY()).thenReturn(new float[] {50f, 40f});
    mGestureDetector = new TransformGestureDetector(mMultiPointerGestureDetector);
    mGestureDetector.setListener(mListener);
  }

  @Test
  public void testInitialstate() {
    assertThat(mGestureDetector.isGestureInProgress()).isFalse();
    verify(mMultiPointerGestureDetector).setListener(mGestureDetector);
  }

  @Test
  public void testReset() {
    mGestureDetector.reset();
    verify(mMultiPointerGestureDetector).reset();
  }

  @Test
  public void testOnTouchEvent() {
    MotionEvent motionEvent = mock(MotionEvent.class);
    mGestureDetector.onTouchEvent(motionEvent);
    verify(mMultiPointerGestureDetector).onTouchEvent(motionEvent);
  }

  @Test
  public void testOnGestureBegin() {
    mGestureDetector.onGestureBegin(mMultiPointerGestureDetector);
    verify(mListener).onGestureBegin(mGestureDetector);
  }

  @Test
  public void testOnGestureUpdate() {
    mGestureDetector.onGestureUpdate(mMultiPointerGestureDetector);
    verify(mListener).onGestureUpdate(mGestureDetector);
  }

  @Test
  public void testOnGestureEnd() {
    mGestureDetector.onGestureEnd(mMultiPointerGestureDetector);
    verify(mListener).onGestureEnd(mGestureDetector);
  }

  @Test
  public void testIsGestureInProgress() {
    when(mMultiPointerGestureDetector.isGestureInProgress()).thenReturn(true);
    assertThat(mGestureDetector.isGestureInProgress()).isTrue();
    verify(mMultiPointerGestureDetector, times(1)).isGestureInProgress();
    when(mMultiPointerGestureDetector.isGestureInProgress()).thenReturn(false);
    assertThat(mGestureDetector.isGestureInProgress()).isFalse();
    verify(mMultiPointerGestureDetector, times(2)).isGestureInProgress();
  }

  @Test
  public void testPivot() {
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    assertThat(mGestureDetector.getPivotX()).isEqualTo(0f, within(0.0f));
    assertThat(mGestureDetector.getPivotY()).isEqualTo(0f, within(0.0f));
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(1);
    assertThat(mGestureDetector.getPivotX()).isEqualTo(100f, within(0.0f));
    assertThat(mGestureDetector.getPivotY()).isEqualTo(500f, within(0.0f));
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(2);
    assertThat(mGestureDetector.getPivotX()).isEqualTo(150f, within(0.0f));
    assertThat(mGestureDetector.getPivotY()).isEqualTo(550f, within(0.0f));
  }

  @Test
  public void testTranslation() {
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    assertThat(mGestureDetector.getTranslationX()).isEqualTo(0f, within(0.0f));
    assertThat(mGestureDetector.getTranslationY()).isEqualTo(0f, within(0.0f));
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(1);
    assertThat(mGestureDetector.getTranslationX()).isEqualTo(-90f, within(0.0f));
    assertThat(mGestureDetector.getTranslationY()).isEqualTo(-450f, within(0.0f));
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(2);
    assertThat(mGestureDetector.getTranslationX()).isEqualTo(-135f, within(0.0f));
    assertThat(mGestureDetector.getTranslationY()).isEqualTo(-505f, within(0.0f));
  }

  @Test
  public void testScale() {
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    assertThat(mGestureDetector.getScale()).isEqualTo(1f, within(0.0f));
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(1);
    assertThat(mGestureDetector.getScale()).isEqualTo(1f, within(0.0f));
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(2);
    assertThat(mGestureDetector.getScale()).isEqualTo(0.1f, within(1e-6f));
  }

  @Test
  public void testRotation() {
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    assertThat(mGestureDetector.getRotation()).isEqualTo(0f, within(0.0f));
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(1);
    assertThat(mGestureDetector.getRotation()).isEqualTo(0f, within(0.0f));
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(2);
    assertThat(mGestureDetector.getRotation()).isEqualTo((float) -Math.PI / 2, within(1e-6f));
  }
}
