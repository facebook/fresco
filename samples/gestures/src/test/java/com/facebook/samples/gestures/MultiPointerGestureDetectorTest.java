/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.gestures;

import static com.facebook.samples.gestures.MotionEventTestUtils.obtainMotionEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import android.view.MotionEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MultiPointerGestureDetector} */
@RunWith(RobolectricTestRunner.class)
public class MultiPointerGestureDetectorTest {

  private MultiPointerGestureDetector.Listener mListener;
  private MultiPointerGestureDetector mGestureDetector;

  @Before
  public void setup() {
    mListener = mock(MultiPointerGestureDetector.Listener.class);
    mGestureDetector = new MultiPointerGestureDetector();
    mGestureDetector.setListener(mListener);
  }

  @Test
  public void testInitialstate() {
    assertThat(mGestureDetector.isGestureInProgress()).isEqualTo(false);
  }

  @Test
  public void testSinglePointer() {
    MotionEvent event1 = obtainMotionEvent(1000, 1000, MotionEvent.ACTION_DOWN, 0, 100f, 300f);
    MotionEvent event2 = obtainMotionEvent(1000, 1010, MotionEvent.ACTION_MOVE, 0, 150f, 350f);
    MotionEvent event3 = obtainMotionEvent(1000, 1020, MotionEvent.ACTION_MOVE, 0, 200f, 400f);
    MotionEvent event4 = obtainMotionEvent(1000, 1030, MotionEvent.ACTION_UP, 0, 200f, 400f);

    InOrder inOrder = inOrder(mListener);

    mGestureDetector.onTouchEvent(event1);
    mGestureDetector.onTouchEvent(event2);
    assertThat(mGestureDetector.isGestureInProgress()).isTrue();
    assertThat(mGestureDetector.getPointerCount()).isEqualTo(1);
    assertThat(mGestureDetector.getStartX()[0]).isEqualTo(100f);
    assertThat(mGestureDetector.getStartY()[0]).isEqualTo(300f);
    assertThat(mGestureDetector.getCurrentX()[0]).isEqualTo(150f);
    assertThat(mGestureDetector.getCurrentY()[0]).isEqualTo(350f);
    inOrder.verify(mListener).onGestureBegin(mGestureDetector);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event3);
    assertThat(mGestureDetector.isGestureInProgress()).isTrue();
    assertThat(mGestureDetector.getPointerCount()).isEqualTo(1);
    assertThat(mGestureDetector.getStartX()[0]).isEqualTo(100f);
    assertThat(mGestureDetector.getStartY()[0]).isEqualTo(300f);
    assertThat(mGestureDetector.getCurrentX()[0]).isEqualTo(200f);
    assertThat(mGestureDetector.getCurrentY()[0]).isEqualTo(400f);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event4);
    assertThat(mGestureDetector.isGestureInProgress()).isFalse();
    assertThat(mGestureDetector.getPointerCount()).isEqualTo(0);
    inOrder.verify(mListener).onGestureEnd(mGestureDetector);
    inOrder.verifyNoMoreInteractions();

    event1.recycle();
    event2.recycle();
    event3.recycle();
    event4.recycle();
  }

  @Test
  public void testTwoPointers() {
    MotionEvent event1 = obtainMotionEvent(100, 100, MotionEvent.ACTION_DOWN, 0, 100f, 300f);
    MotionEvent event2 = obtainMotionEvent(100, 110, MotionEvent.ACTION_MOVE, 0, 150f, 350f);
    MotionEvent event3 =
        obtainMotionEvent(100, 120, MotionEvent.ACTION_POINTER_DOWN, 0, 150f, 350f, 1, 500f, 600f);
    MotionEvent event4 =
        obtainMotionEvent(100, 130, MotionEvent.ACTION_MOVE, 0, 200f, 400f, 1, 550f, 650f);
    MotionEvent event5 =
        obtainMotionEvent(100, 140, MotionEvent.ACTION_POINTER_UP, 0, 200f, 400f, 1, 550f, 650f);
    MotionEvent event6 = obtainMotionEvent(100, 150, MotionEvent.ACTION_MOVE, 1, 600f, 700f);
    MotionEvent event7 = obtainMotionEvent(100, 160, MotionEvent.ACTION_UP, 1, 600f, 700f);

    InOrder inOrder = inOrder(mListener);

    mGestureDetector.onTouchEvent(event1);
    mGestureDetector.onTouchEvent(event2);
    assertThat(mGestureDetector.isGestureInProgress()).isTrue();
    assertThat(mGestureDetector.getPointerCount()).isEqualTo(1);
    assertThat(mGestureDetector.getStartX()[0]).isEqualTo(100f);
    assertThat(mGestureDetector.getStartY()[0]).isEqualTo(300f);
    assertThat(mGestureDetector.getCurrentX()[0]).isEqualTo(150f);
    assertThat(mGestureDetector.getCurrentY()[0]).isEqualTo(350f);
    inOrder.verify(mListener).onGestureBegin(mGestureDetector);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event3);
    assertThat(mGestureDetector.isGestureInProgress()).isTrue();
    assertThat(mGestureDetector.getPointerCount()).isEqualTo(2);
    assertThat(mGestureDetector.getStartX()[0]).isEqualTo(150f);
    assertThat(mGestureDetector.getStartY()[0]).isEqualTo(350f);
    assertThat(mGestureDetector.getCurrentX()[0]).isEqualTo(150f);
    assertThat(mGestureDetector.getCurrentY()[0]).isEqualTo(350f);
    assertThat(mGestureDetector.getStartX()[1]).isEqualTo(500f);
    assertThat(mGestureDetector.getStartY()[1]).isEqualTo(600f);
    assertThat(mGestureDetector.getCurrentX()[1]).isEqualTo(500f);
    assertThat(mGestureDetector.getCurrentY()[1]).isEqualTo(600f);
    inOrder.verify(mListener).onGestureEnd(mGestureDetector);
    inOrder.verify(mListener).onGestureBegin(mGestureDetector);

    mGestureDetector.onTouchEvent(event4);
    assertThat(mGestureDetector.isGestureInProgress()).isTrue();
    assertThat(mGestureDetector.getPointerCount()).isEqualTo(2);
    assertThat(mGestureDetector.getStartX()[0]).isEqualTo(150f);
    assertThat(mGestureDetector.getStartY()[0]).isEqualTo(350f);
    assertThat(mGestureDetector.getCurrentX()[0]).isEqualTo(200f);
    assertThat(mGestureDetector.getCurrentY()[0]).isEqualTo(400f);
    assertThat(mGestureDetector.getStartX()[1]).isEqualTo(500f);
    assertThat(mGestureDetector.getStartY()[1]).isEqualTo(600f);
    assertThat(mGestureDetector.getCurrentX()[1]).isEqualTo(550f);
    assertThat(mGestureDetector.getCurrentY()[1]).isEqualTo(650f);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event5);
    assertThat(mGestureDetector.isGestureInProgress()).isTrue();
    assertThat(mGestureDetector.getPointerCount()).isEqualTo(1);
    assertThat(mGestureDetector.getStartX()[0]).isEqualTo(550f);
    assertThat(mGestureDetector.getStartY()[0]).isEqualTo(650f);
    assertThat(mGestureDetector.getCurrentX()[0]).isEqualTo(550f);
    assertThat(mGestureDetector.getCurrentY()[0]).isEqualTo(650f);
    inOrder.verify(mListener).onGestureEnd(mGestureDetector);
    inOrder.verify(mListener).onGestureBegin(mGestureDetector);

    mGestureDetector.onTouchEvent(event6);
    assertThat(mGestureDetector.isGestureInProgress()).isTrue();
    assertThat(mGestureDetector.getPointerCount()).isEqualTo(1);
    assertThat(mGestureDetector.getStartX()[0]).isEqualTo(550f);
    assertThat(mGestureDetector.getStartY()[0]).isEqualTo(650f);
    assertThat(mGestureDetector.getCurrentX()[0]).isEqualTo(600f);
    assertThat(mGestureDetector.getCurrentY()[0]).isEqualTo(700f);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event7);
    assertThat(mGestureDetector.isGestureInProgress()).isFalse();
    assertThat(mGestureDetector.getPointerCount()).isEqualTo(0);
    inOrder.verify(mListener).onGestureEnd(mGestureDetector);
    inOrder.verifyNoMoreInteractions();

    event1.recycle();
    event2.recycle();
    event3.recycle();
    event4.recycle();
  }
}
