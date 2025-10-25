/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import com.facebook.drawee.drawable.DrawableTestUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.testing.DraweeMocks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DraweeHolderTest {

  private DraweeHolder mDraweeHolder;

  private Drawable mTopLevelDrawable;
  private DraweeHierarchy mDraweeHierarchy;
  private DraweeController mController;

  private InOrder mInOrderVerifier;

  @Before
  public void setUp() {
    mTopLevelDrawable = DrawableTestUtils.mockDrawable();
    mDraweeHierarchy = DraweeMocks.mockDraweeHierarchyOf(mTopLevelDrawable);
    mController = DraweeMocks.mockController();
    mDraweeHolder = new DraweeHolder(mDraweeHierarchy);
    mInOrderVerifier = inOrder(mController);
  }

  @Test
  public void testOverrideControllerHierarchy() {
    DraweeHierarchy otherHierarchy = mock(DraweeHierarchy.class);
    mController.setHierarchy(otherHierarchy);
    assertThat(mController.getHierarchy()).isSameAs(otherHierarchy);
    mDraweeHolder.setController(mController);
    assertThat(mDraweeHolder.getController()).isSameAs(mController);
    assertThat(mDraweeHolder.getHierarchy()).isSameAs(mDraweeHierarchy);
    assertThat(mController.getHierarchy()).isSameAs(mDraweeHierarchy);
  }

  @Test
  public void testSetControllerWithoutHierarchy() {
    mDraweeHolder.setController(mController);
    assertThat(mDraweeHolder.getController()).isSameAs(mController);
    assertThat(mDraweeHolder.getHierarchy()).isSameAs(mDraweeHierarchy);
    assertThat(mController.getHierarchy()).isSameAs(mDraweeHierarchy);
  }

  @Test
  public void testSetControllerBeforeHierarchy() {
    mDraweeHolder = new DraweeHolder(null);
    mDraweeHolder.setController(mController);
    mDraweeHolder.setHierarchy(mDraweeHierarchy);
    assertThat(mDraweeHolder.getController()).isSameAs(mController);
    assertThat(mDraweeHolder.getHierarchy()).isSameAs(mDraweeHierarchy);
    assertThat(mController.getHierarchy()).isSameAs(mDraweeHierarchy);
  }

  @Test
  public void testClearControllerKeepsHierarchy() {
    mDraweeHolder.setController(mController);
    mDraweeHolder.resetActualImage();
    assertThat(mDraweeHolder.getHierarchy()).isSameAs(mDraweeHierarchy);
    assertThat(mDraweeHolder.getController()).isNull();
    assertThat(mController.getHierarchy()).isNull();
  }

  @Test
  public void testNewControllerKeepsHierarchy() {
    mDraweeHolder.setController(mController);
    assertThat(mDraweeHolder.getHierarchy()).isSameAs(mDraweeHierarchy);
    DraweeController another = DraweeMocks.mockController();
    mDraweeHolder.setController(another);
    assertThat(mDraweeHolder.getHierarchy()).isSameAs(mDraweeHierarchy);
    assertThat(mDraweeHolder.getController()).isSameAs(another);
    assertThat(mController.getHierarchy()).isNull();
    assertThat(another.getHierarchy()).isSameAs(mDraweeHierarchy);
  }

  @Test
  public void testLifecycle() {
    mDraweeHolder.setController(mController);
    assertThat(mDraweeHolder.isAttached()).isFalse();
    mDraweeHolder.onAttach();
    assertThat(mDraweeHolder.isAttached()).isTrue();
    mDraweeHolder.onDetach();
    assertThat(mDraweeHolder.isAttached()).isFalse();

    verify(mController).onAttach();
    verify(mController).onDetach();
  }

  @Test
  public void testSetControllerWhenAlreadyAttached() {
    mDraweeHolder.onAttach();
    mDraweeHolder.setController(mController);
    mDraweeHolder.onDetach();
    verify(mController).onAttach();
    verify(mController).onDetach();
  }

  @Test
  public void testSetNullController() {
    mDraweeHolder.resetActualImage();
    mDraweeHolder.onAttach();
    mDraweeHolder.onDetach();
    mDraweeHolder.onAttach();
  }

  @Test
  public void testSetNewControllerWithInvalidController() {
    final DraweeHierarchy draweeHierarchy2 = DraweeMocks.mockDraweeHierarchyOf(mTopLevelDrawable);
    final DraweeHolder draweeHolder2 = new DraweeHolder(draweeHierarchy2);

    mDraweeHolder.onAttach();
    mDraweeHolder.setController(mController);
    draweeHolder2.setController(mController);

    mDraweeHolder.resetActualImage();
    verify(mController, never()).onDetach();
    assertThat(mController.getHierarchy()).isEqualTo(draweeHierarchy2);
  }

  @Test
  public void testSetNewHierarchyWithInvalidController() {
    final DraweeHierarchy draweeHierarchy2 = DraweeMocks.mockDraweeHierarchyOf(mTopLevelDrawable);
    final DraweeHolder draweeHolder2 = new DraweeHolder(draweeHierarchy2);

    mDraweeHolder.setController(mController);
    draweeHolder2.setController(mController);

    final DraweeHierarchy draweeHierarchy3 = DraweeMocks.mockDraweeHierarchyOf(mTopLevelDrawable);
    mDraweeHolder.setHierarchy(draweeHierarchy3);
    assertThat(mController.getHierarchy()).isEqualTo(draweeHierarchy2);
  }

  @Test
  public void testOnDetachWithInvalidController() {
    final DraweeHierarchy draweeHierarchy2 = DraweeMocks.mockDraweeHierarchyOf(mTopLevelDrawable);
    final DraweeHolder draweeHolder2 = new DraweeHolder(draweeHierarchy2);

    mDraweeHolder.onAttach();
    mDraweeHolder.setController(mController);
    draweeHolder2.setController(mController);

    mDraweeHolder.onDetach();
    verify(mController, never()).onDetach();
  }

  @Test
  public void testTouchEventWithInvalidController() {
    final DraweeHierarchy draweeHierarchy2 = DraweeMocks.mockDraweeHierarchyOf(mTopLevelDrawable);
    final DraweeHolder draweeHolder2 = new DraweeHolder(draweeHierarchy2);

    mDraweeHolder.setController(mController);
    draweeHolder2.setController(mController);

    mDraweeHolder.onTouchEvent(mock(MotionEvent.class));
    verify(mController, never()).onTouchEvent(any(MotionEvent.class));
  }

  /**
   * There are 8 possible state transitions with two variables 1. (visible, unattached) -> (visible,
   * attached) 2. (visible, attached) -> (invisible, attached) 3. (invisible, attached) ->
   * (invisible, unattached) 4. (invisible, unattached) -> (visible, unattached) 5. (visible,
   * unattached) -> (invisible, unattached) 6. (invisible, unattached) -> (invisible, attached) 7.
   * (invisible, attached) -> (visible, attached) 8. (visible, attached) -> (visible, unattached)
   */
  @Test
  public void testVisibilityStateTransitions() {
    boolean restart = true;

    // Initial state (mIsVisible, !mIsHolderAttached)
    mDraweeHolder.setController(mController);
    verifyControllerLifecycleCalls(0, 0);

    /** 1 */
    mDraweeHolder.onAttach();
    verifyControllerLifecycleCalls(1, 0);

    /** 2 */
    mTopLevelDrawable.setVisible(false, restart);
    verifyControllerLifecycleCalls(0, 1);

    /** 3 */
    mDraweeHolder.onDetach();
    verifyControllerLifecycleCalls(0, 0);

    /** 4 */
    mTopLevelDrawable.setVisible(true, restart);
    verifyControllerLifecycleCalls(0, 0);

    /** 5 */
    mTopLevelDrawable.setVisible(false, restart);
    verifyControllerLifecycleCalls(0, 0);

    /** 6 */
    mDraweeHolder.onAttach();
    verifyControllerLifecycleCalls(0, 0);

    /** 7 */
    mTopLevelDrawable.setVisible(true, restart);
    verifyControllerLifecycleCalls(1, 0);

    /** 8 */
    mDraweeHolder.onDetach();
    verifyControllerLifecycleCalls(0, 1);
  }

  private void verifyControllerLifecycleCalls(int numOnAttach, int numOnDetach) {
    mInOrderVerifier.verify(mController, times(numOnAttach)).onAttach();
    mInOrderVerifier.verify(mController, times(numOnDetach)).onDetach();
  }
}
