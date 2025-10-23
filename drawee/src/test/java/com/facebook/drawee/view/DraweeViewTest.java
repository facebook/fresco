/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import com.facebook.drawee.drawable.DrawableTestUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.testing.DraweeMocks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Unit test for {@link DraweeView}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
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
  public void testSetContentDescription() {
    String CONTENT_DESCRIPTION = "Test Photo";
    mController.setContentDescription(CONTENT_DESCRIPTION);
    mDraweeView.setController(mController);
    mDraweeView.setContentDescription(mController.getContentDescription());
    assertThat(mDraweeView.getContentDescription()).isSameAs(CONTENT_DESCRIPTION);
  }

  @Test
  public void testSetHierarchy() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    assertThat(mDraweeView.getHierarchy()).isSameAs(mDraweeHierarchy);
    assertThat(mDraweeView.getDrawable()).isSameAs(mTopLevelDrawable);

    DraweeHierarchy hierarchy2 = DraweeMocks.mockDraweeHierarchy();
    mDraweeView.setHierarchy(hierarchy2);
    assertThat(mDraweeView.getHierarchy()).isSameAs(hierarchy2);
    assertThat(mDraweeView.getDrawable()).isSameAs(hierarchy2.getTopLevelDrawable());
  }

  @Test
  public void testSetController() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.setController(mController);
    assertThat(mDraweeView.getController()).isSameAs(mController);
    assertThat(mDraweeView.getDrawable()).isSameAs(mTopLevelDrawable);
    verify(mController).setHierarchy(mDraweeHierarchy);
  }

  @Test
  public void testClearControllerKeepsHierarchy() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.setController(mController);
    mDraweeView.resetActualImage();
    assertThat(mDraweeView.getController()).isNull();
    assertThat(mDraweeView.getDrawable()).isSameAs(mTopLevelDrawable);
    verify(mController).setHierarchy(null);
  }

  @Test
  public void testNewControllerKeepsHierarchy() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.setController(mController);
    DraweeController controller2 = DraweeMocks.mockController();
    mDraweeView.setController(controller2);
    assertThat(mDraweeView.getController()).isSameAs(controller2);
    assertThat(mDraweeView.getDrawable()).isSameAs(mTopLevelDrawable);
    verify(mController).setHierarchy(null);
    verify(controller2).setHierarchy(mDraweeHierarchy);
  }

  @Test
  public void testSetDrawable() {
    mDraweeView.setImageDrawable(mDrawable);
    assertThat(mDraweeView.getDrawable()).isSameAs(mDrawable);
    assertThat(mDraweeView.getController()).isNull();
  }

  @Test
  public void testSetDrawableAfterController() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.onAttachedToWindow();
    mDraweeView.setController(mController);
    mDraweeView.setImageDrawable(mDrawable);
    assertThat(mDraweeView.getController()).isNull();
    assertThat(mDraweeView.getDrawable()).isSameAs(mDrawable);
  }

  @Test
  public void testSetControllerAfterDrawable() {
    mDraweeView.setHierarchy(mDraweeHierarchy);
    mDraweeView.onAttachedToWindow();
    mDraweeView.setImageDrawable(mDrawable);
    mDraweeView.setController(mController);
    assertThat(mDraweeView.getController()).isSameAs(mController);
    assertThat(mDraweeView.getDrawable()).isSameAs(mTopLevelDrawable);
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
    mDraweeView.resetActualImage();
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
