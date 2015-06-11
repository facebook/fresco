/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.samples.gestures;

import android.view.MotionEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import robolectric2.shadows.ShadowMotionEvent;

import static com.facebook.drawee.gestures.MotionEventTestUtils.obtainMotionEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MultiPointerGestureDetector}
 */
@Config(shadows={ShadowMotionEvent.class})
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
    assertEquals(false, mGestureDetector.isGestureInProgress());
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
    assertTrue(mGestureDetector.isGestureInProgress());
    assertEquals(1, mGestureDetector.getCount());
    assertEquals(100f, mGestureDetector.getStartX()[0], 0);
    assertEquals(300f, mGestureDetector.getStartY()[0], 0);
    assertEquals(150f, mGestureDetector.getCurrentX()[0], 0);
    assertEquals(350f, mGestureDetector.getCurrentY()[0], 0);
    inOrder.verify(mListener).onGestureBegin(mGestureDetector);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event3);
    assertTrue(mGestureDetector.isGestureInProgress());
    assertEquals(1, mGestureDetector.getCount());
    assertEquals(100f, mGestureDetector.getStartX()[0], 0);
    assertEquals(300f, mGestureDetector.getStartY()[0], 0);
    assertEquals(200f, mGestureDetector.getCurrentX()[0], 0);
    assertEquals(400f, mGestureDetector.getCurrentY()[0], 0);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event4);
    assertFalse(mGestureDetector.isGestureInProgress());
    assertEquals(0, mGestureDetector.getCount());
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
    assertTrue(mGestureDetector.isGestureInProgress());
    assertEquals(1, mGestureDetector.getCount());
    assertEquals(100f, mGestureDetector.getStartX()[0], 0);
    assertEquals(300f, mGestureDetector.getStartY()[0], 0);
    assertEquals(150f, mGestureDetector.getCurrentX()[0], 0);
    assertEquals(350f, mGestureDetector.getCurrentY()[0], 0);
    inOrder.verify(mListener).onGestureBegin(mGestureDetector);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event3);
    assertTrue(mGestureDetector.isGestureInProgress());
    assertEquals(2, mGestureDetector.getCount());
    assertEquals(150f, mGestureDetector.getStartX()[0], 0);
    assertEquals(350f, mGestureDetector.getStartY()[0], 0);
    assertEquals(150f, mGestureDetector.getCurrentX()[0], 0);
    assertEquals(350f, mGestureDetector.getCurrentY()[0], 0);
    assertEquals(500f, mGestureDetector.getStartX()[1], 0);
    assertEquals(600f, mGestureDetector.getStartY()[1], 0);
    assertEquals(500f, mGestureDetector.getCurrentX()[1], 0);
    assertEquals(600f, mGestureDetector.getCurrentY()[1], 0);
    inOrder.verify(mListener).onGestureEnd(mGestureDetector);
    inOrder.verify(mListener).onGestureBegin(mGestureDetector);

    mGestureDetector.onTouchEvent(event4);
    assertTrue(mGestureDetector.isGestureInProgress());
    assertEquals(2, mGestureDetector.getCount());
    assertEquals(150f, mGestureDetector.getStartX()[0], 0);
    assertEquals(350f, mGestureDetector.getStartY()[0], 0);
    assertEquals(200f, mGestureDetector.getCurrentX()[0], 0);
    assertEquals(400f, mGestureDetector.getCurrentY()[0], 0);
    assertEquals(500f, mGestureDetector.getStartX()[1], 0);
    assertEquals(600f, mGestureDetector.getStartY()[1], 0);
    assertEquals(550f, mGestureDetector.getCurrentX()[1], 0);
    assertEquals(650f, mGestureDetector.getCurrentY()[1], 0);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event5);
    assertTrue(mGestureDetector.isGestureInProgress());
    assertEquals(1, mGestureDetector.getCount());
    assertEquals(550f, mGestureDetector.getStartX()[0], 0);
    assertEquals(650f, mGestureDetector.getStartY()[0], 0);
    assertEquals(550f, mGestureDetector.getCurrentX()[0], 0);
    assertEquals(650f, mGestureDetector.getCurrentY()[0], 0);
    inOrder.verify(mListener).onGestureEnd(mGestureDetector);
    inOrder.verify(mListener).onGestureBegin(mGestureDetector);

    mGestureDetector.onTouchEvent(event6);
    assertTrue(mGestureDetector.isGestureInProgress());
    assertEquals(1, mGestureDetector.getCount());
    assertEquals(550f, mGestureDetector.getStartX()[0], 0);
    assertEquals(650f, mGestureDetector.getStartY()[0], 0);
    assertEquals(600f, mGestureDetector.getCurrentX()[0], 0);
    assertEquals(700f, mGestureDetector.getCurrentY()[0], 0);
    inOrder.verify(mListener).onGestureUpdate(mGestureDetector);

    mGestureDetector.onTouchEvent(event7);
    assertFalse(mGestureDetector.isGestureInProgress());
    assertEquals(0, mGestureDetector.getCount());
    inOrder.verify(mListener).onGestureEnd(mGestureDetector);
    inOrder.verifyNoMoreInteractions();

    event1.recycle();
    event2.recycle();
    event3.recycle();
    event4.recycle();
  }

}
