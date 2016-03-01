/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.view;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import com.facebook.drawee.drawable.DrawableTestUtils;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.testing.DraweeMocks;
import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.Robolectric;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Unit test for {@link DraweeView}. */
@RunWith(RobolectricTestRunner.class)
public class DraweeViewTest {

  private DraweeView<DraweeHierarchy> mDraweeView;

  private Drawable mDrawable;
  private Drawable mTopLevelDrawable;
  private DraweeHierarchy mDraweeHierarchy;
  private DraweeController mController;

  @Before
  public void setUp() {
    Activity activity = Robolectric.buildActivity(Activity.class).create().get();
    mDrawable = DrawableTestUtils.mockDrawable();
    mTopLevelDrawable = DrawableTestUtils.mockDrawable();
    mDraweeHierarchy = DraweeMocks.mockDraweeHierarchyOf(mTopLevelDrawable);
    mController = DraweeMocks.mockController();
    mDraweeView = new DraweeView<DraweeHierarchy>(activity);
  }

  @Test
  public void testSetHierarchy() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    assertSame(mDraweeHierarchy, mDraweeView.getHierarchy());
    assertSame(mTopLevelDrawable, mDraweeView.getDrawable());

    DraweeHierarchy hierarchy2 = DraweeMocks.mockDraweeHierarchy();
    mDraweeView.setHierarchy(hierarchy2);
    assertSame(hierarchy2, mDraweeView.getHierarchy());
    assertSame(hierarchy2.getTopLevelDrawable(), mDraweeView.getDrawable());
  }

  @Test
  public void testSetController() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.setController(mController);
    assertSame(mController, mDraweeView.getController());
    assertSame(mTopLevelDrawable, mDraweeView.getDrawable());
    verify(mController).setHierarchy(mDraweeHierarchy);
  }

  @Test
  public void testClearControllerKeepsHierarchy() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.setController(mController);
    mDraweeView.setController(null);
    assertNull(mDraweeView.getController());
    assertSame(mTopLevelDrawable, mDraweeView.getDrawable());
    verify(mController).setHierarchy(null);
  }

  @Test
  public void testNewControllerKeepsHierarchy() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.setController(mController);
    DraweeController controller2 = DraweeMocks.mockController();
    mDraweeView.setController(controller2);
    assertSame(controller2, mDraweeView.getController());
    assertSame(mTopLevelDrawable, mDraweeView.getDrawable());
    verify(mController).setHierarchy(null);
    verify(controller2).setHierarchy(mDraweeHierarchy);
  }

  @Test
  public void testSetDrawable() {
    mDraweeView.setImageDrawable(mDrawable);
    assertSame(mDrawable, mDraweeView.getDrawable());
    assertNull(mDraweeView.getController());
  }

  @Test
  public void testSetDrawableAfterController() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.onAttachedToWindow();
    mDraweeView.setController(mController);
    mDraweeView.setImageDrawable(mDrawable);
    assertNull(mDraweeView.getController());
    assertSame(mDrawable, mDraweeView.getDrawable());
  }

  @Test
  public void testSetControllerAfterDrawable() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.onAttachedToWindow();
    mDraweeView.setImageDrawable(mDrawable);
    mDraweeView.setController(mController);
    assertSame(mController, mDraweeView.getController());
    assertSame(mTopLevelDrawable, mDraweeView.getDrawable());
  }

  @Test
  public void testLifecycle_Controller() {
    InOrder inOrder = inOrder(mController);
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.setController(mController);
    inOrder.verify(mController).setHierarchy(mDraweeHierarchy);
    mDraweeView.onAttachedToWindow();
    inOrder.verify(mController).onAttach();
    mDraweeView.onStartTemporaryDetach();
    inOrder.verify(mController).onDetach();
    mDraweeView.onFinishTemporaryDetach();
    inOrder.verify(mController).onAttach();
    mDraweeView.onDetachedFromWindow();
    inOrder.verify(mController).onDetach();
  }

  @Test
  public void testLifecycle_ControllerSetWhileAttached() {
    InOrder inOrder = inOrder(mController);
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.onAttachedToWindow();
    mDraweeView.setController(mController);
    inOrder.verify(mController).setHierarchy(mDraweeHierarchy);
    inOrder.verify(mController).onAttach();
    mDraweeView.onDetachedFromWindow();
    inOrder.verify(mController).onDetach();
  }

  @Test
  public void testLifecycle_NullController() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.setController(null);
    mDraweeView.onStartTemporaryDetach();
    mDraweeView.onFinishTemporaryDetach();
  }

  @Test
  public void testLifecycle_Drawable() {
    mDraweeView.setImageDrawable(mDrawable);
    mDraweeView.onStartTemporaryDetach();
    mDraweeView.onFinishTemporaryDetach();
  }
}
