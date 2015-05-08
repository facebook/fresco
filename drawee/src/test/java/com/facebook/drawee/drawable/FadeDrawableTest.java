/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import org.robolectric.RobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class FadeDrawableTest {
  private Drawable[] mLayers = new Drawable[] {
      DrawableTestUtils.mockDrawable(),
      DrawableTestUtils.mockDrawable(),
      DrawableTestUtils.mockDrawable(),
  };

  private FakeFadeDrawable mFadeDrawable;
  private Canvas mCanvas = mock(Canvas.class);
  private Drawable.Callback mCallback = mock(Drawable.Callback.class);

  @Before
  public void setUp() {
    mFadeDrawable = new FakeFadeDrawable(mLayers);
    mFadeDrawable.setCallback(mCallback);
  }

  private void resetInteractions() {
    reset(mCallback, mLayers[0], mLayers[1], mLayers[2]);
    when(mLayers[0].mutate()).thenReturn(mLayers[0]);
    when(mLayers[1].mutate()).thenReturn(mLayers[1]);
    when(mLayers[2].mutate()).thenReturn(mLayers[2]);
  }

  @Test
  public void testIntrinsicDimensions() {
    when(mLayers[0].getIntrinsicWidth()).thenReturn(100);
    when(mLayers[1].getIntrinsicWidth()).thenReturn(200);
    when(mLayers[2].getIntrinsicWidth()).thenReturn(150);
    when(mLayers[0].getIntrinsicHeight()).thenReturn(400);
    when(mLayers[1].getIntrinsicHeight()).thenReturn(350);
    when(mLayers[2].getIntrinsicHeight()).thenReturn(300);
    Assert.assertEquals(200, mFadeDrawable.getIntrinsicWidth());
    Assert.assertEquals(400, mFadeDrawable.getIntrinsicHeight());
  }

  @Test
  public void testInitialState() {
    // initially only the fist layer is displayed and there is no transition
    Assert.assertEquals(FadeDrawable.TRANSITION_NONE, mFadeDrawable.mTransitionState);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
  }

  @Test
  public void testFadeToLayer() {
    // start fade
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.fadeToLayer(1);
    Assert.assertEquals(100, mFadeDrawable.mDurationMs);
    Assert.assertEquals(FadeDrawable.TRANSITION_STARTING, mFadeDrawable.mTransitionState);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
    // alphas will change only when the next draw happens
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
  }

  @Test
  public void testFadeUpToLayer() {
    // start fade
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.fadeUpToLayer(1);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
    Assert.assertEquals(100, mFadeDrawable.mDurationMs);
    Assert.assertEquals(FadeDrawable.TRANSITION_STARTING, mFadeDrawable.mTransitionState);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    // alphas will change only when the next draw happens
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
  }

  @Test
  public void testFadeInLayer() {
    //start fade in
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.fadeInLayer(2);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[2]);
    Assert.assertEquals(100, mFadeDrawable.mDurationMs);
    Assert.assertEquals(FadeDrawable.TRANSITION_STARTING, mFadeDrawable.mTransitionState);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    // alphas will change only when the next draw happens
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
  }

  @Test
  public void testFadeOutLayer() {
    //start fade out
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.fadeOutLayer(0);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
    Assert.assertEquals(100, mFadeDrawable.mDurationMs);
    Assert.assertEquals(FadeDrawable.TRANSITION_STARTING, mFadeDrawable.mTransitionState);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    // alphas will change only when the next draw happens
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
  }

  @Test
  public void testFadeOutAllLayers() {
    //start fade out
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.mIsLayerOn[1] = true;
    mFadeDrawable.mIsLayerOn[2] = true;
    mFadeDrawable.fadeOutAllLayers();
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
    Assert.assertEquals(100, mFadeDrawable.mDurationMs);
    Assert.assertEquals(FadeDrawable.TRANSITION_STARTING, mFadeDrawable.mTransitionState);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    // alphas will change only when the next draw happens
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
  }

  @Test
  public void testImmediateTransition() {
    testImmediateTransition(true);
    testImmediateTransition(false);
  }
  private void testImmediateTransition(boolean fadeUpToLayer) {
    resetInteractions();
    if (fadeUpToLayer) {
      mFadeDrawable.fadeUpToLayer(1);
    } else {
      mFadeDrawable.fadeToLayer(1);
    }
    Assert.assertEquals(fadeUpToLayer, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
    verify(mCallback).invalidateDrawable(mFadeDrawable);

    mFadeDrawable.finishTransitionImmediately();
    verify(mCallback, times(2)).invalidateDrawable(mFadeDrawable);
    Assert.assertEquals(fadeUpToLayer ? 255 : 0, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
    Assert.assertEquals(FadeDrawable.TRANSITION_NONE, mFadeDrawable.mTransitionState);
  }

  @Test
  public void testZeroTransition() {
    testZeroTransition(true);
    testZeroTransition(false);
  }
  private void testZeroTransition(boolean fadeUpToLayer) {
    resetInteractions();
    mFadeDrawable.setTransitionDuration(0);
    if (fadeUpToLayer) {
      mFadeDrawable.fadeUpToLayer(1);
    } else {
      mFadeDrawable.fadeToLayer(1);
    }
    Assert.assertEquals(fadeUpToLayer, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
    verify(mCallback).invalidateDrawable(mFadeDrawable);

    mFadeDrawable.draw(mCanvas);
    Assert.assertEquals(fadeUpToLayer ? 255 : 0, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
    Assert.assertEquals(FadeDrawable.TRANSITION_NONE, mFadeDrawable.mTransitionState);
    if (fadeUpToLayer) {
      verify(mLayers[0]).draw(mCanvas);
    }
    verify(mLayers[1]).draw(mCanvas);
  }

  @Test
  public void testTransition() {
    testTransition(true);
    testTransition(false);
  }
  private void testTransition(boolean fadeUpToLayer) {
    // duration is set to 85 ms
    // 85 = 5 * 17; 5 frames of 17ms
    // 255 / 5 = 51; each frame alpha should increase by 51

    // reset drawable
    mFadeDrawable.reset();

    // start animation
    resetInteractions();
    mFadeDrawable.setTransitionDuration(85);
    if (fadeUpToLayer) {
      mFadeDrawable.fadeUpToLayer(1);
    } else {
      mFadeDrawable.fadeToLayer(1);
    }
    Assert.assertEquals(fadeUpToLayer, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    verifyNoMoreInteractions(mCallback, mLayers[0], mLayers[1], mLayers[2]);

    // first frame
    resetInteractions();
    mFadeDrawable.draw(mCanvas);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
    Assert.assertEquals(FadeDrawable.TRANSITION_RUNNING, mFadeDrawable.mTransitionState);
    verify(mLayers[0]).mutate();
    verify(mLayers[0]).setAlpha(255);
    verify(mLayers[0]).draw(mCanvas);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    verifyNoMoreInteractions(mCallback, mLayers[0], mLayers[1], mLayers[2]);

    // intermediate frames
    for (int i = 1; i < 5; i++) {
      resetInteractions();
      mFadeDrawable.incrementCurrentTimeMs(17);
      mFadeDrawable.draw(mCanvas);
      Assert.assertEquals(fadeUpToLayer ? 255 : 255 - 51 * i, mFadeDrawable.mAlphas[0]);
      Assert.assertEquals(51 * i, mFadeDrawable.mAlphas[1]);
      Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
      Assert.assertEquals(FadeDrawable.TRANSITION_RUNNING, mFadeDrawable.mTransitionState);
      if (fadeUpToLayer) {
        verify(mLayers[0]).mutate();
        verify(mLayers[0]).setAlpha(255);
        verify(mLayers[0]).draw(mCanvas);
      } else {
        verify(mLayers[0]).mutate();
        verify(mLayers[0]).setAlpha(255 - 51 * i);
        verify(mLayers[0]).draw(mCanvas);
      }
      verify(mLayers[1]).mutate();
      verify(mLayers[1]).setAlpha(51 * i);
      verify(mLayers[1]).draw(mCanvas);
      verify(mCallback).invalidateDrawable(mFadeDrawable);
      verifyNoMoreInteractions(mCallback, mLayers[0], mLayers[1], mLayers[2]);
    }

    // last frame
    resetInteractions();
    mFadeDrawable.incrementCurrentTimeMs(17);
    mFadeDrawable.draw(mCanvas);
    Assert.assertEquals(fadeUpToLayer ? 255 : 0, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
    Assert.assertEquals(FadeDrawable.TRANSITION_NONE, mFadeDrawable.mTransitionState);
    if (fadeUpToLayer) {
      verify(mLayers[0]).mutate();
      verify(mLayers[0]).setAlpha(255);
      verify(mLayers[0]).draw(mCanvas);
    }
    verify(mLayers[1]).mutate();
    verify(mLayers[1]).setAlpha(255);
    verify(mLayers[1]).draw(mCanvas);
    verifyNoMoreInteractions(mCallback, mLayers[0], mLayers[1], mLayers[2]);
  }

  @Test
  public void testSetAlpha() {
    InOrder inOrder = inOrder(mLayers[0], mLayers[1], mLayers[2], mCallback);
    // reset drawable
    mFadeDrawable.reset();
    inOrder.verify(mCallback).invalidateDrawable(mFadeDrawable);
    // start animation
    mFadeDrawable.setTransitionDuration(85);
    mFadeDrawable.fadeUpToLayer(1);
    inOrder.verify(mCallback).invalidateDrawable(mFadeDrawable);
    // first frame
    mFadeDrawable.draw(mCanvas);
    inOrder.verify(mCallback, atLeastOnce()).invalidateDrawable(mFadeDrawable);
    // setAlpha
    mFadeDrawable.setAlpha(128);
    Assert.assertEquals(128, mFadeDrawable.getAlpha());
    inOrder.verify(mCallback).invalidateDrawable(mFadeDrawable);
    // next frame
    mFadeDrawable.incrementCurrentTimeMs(17);
    mFadeDrawable.draw(mCanvas);
    Assert.assertEquals(128, mFadeDrawable.getAlpha());
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(51, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
    Assert.assertEquals(FadeDrawable.TRANSITION_RUNNING, mFadeDrawable.mTransitionState);
    inOrder.verify(mLayers[0]).mutate();
    inOrder.verify(mLayers[0]).setAlpha(128);
    inOrder.verify(mLayers[0]).draw(mCanvas);
    inOrder.verify(mLayers[1]).mutate();
    inOrder.verify(mLayers[1]).setAlpha(25);
    inOrder.verify(mLayers[1]).draw(mCanvas);
    inOrder.verify(mCallback, atLeastOnce()).invalidateDrawable(mFadeDrawable);
    inOrder.verifyNoMoreInteractions();

    // make sure the fade has finished, and verify that after that we don't invalidate
    mFadeDrawable.incrementCurrentTimeMs(1000);
    mFadeDrawable.draw(mCanvas);
    inOrder.verify(mCallback, never()).invalidateDrawable(mFadeDrawable);
  }

  @Test
  public void testReset() {
    // go to some non-initial state
    mFadeDrawable.fadeToLayer(2);
    mFadeDrawable.finishTransitionImmediately();
    resetInteractions();

    mFadeDrawable.reset();
    Assert.assertEquals(FadeDrawable.TRANSITION_NONE, mFadeDrawable.mTransitionState);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
  }

  @Test
  public void testBatchMode() {
    mFadeDrawable.beginBatchMode();
    mFadeDrawable.reset();
    mFadeDrawable.fadeInLayer(1);
    mFadeDrawable.fadeOutLayer(0);
    mFadeDrawable.fadeOutAllLayers();
    mFadeDrawable.fadeToLayer(2);
    mFadeDrawable.fadeUpToLayer(1);
    mFadeDrawable.finishTransitionImmediately();
    mFadeDrawable.endBatchMode();
    verify(mCallback, times(1)).invalidateDrawable(mFadeDrawable);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
  }

  @Test
  public void testNoBatchMode() {
    mFadeDrawable.reset();
    mFadeDrawable.fadeInLayer(1);
    mFadeDrawable.fadeOutLayer(0);
    mFadeDrawable.fadeOutAllLayers();
    mFadeDrawable.fadeToLayer(2);
    mFadeDrawable.fadeUpToLayer(1);
    mFadeDrawable.finishTransitionImmediately();
    verify(mCallback, times(7)).invalidateDrawable(mFadeDrawable);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[0]);
    Assert.assertEquals(255, mFadeDrawable.mAlphas[1]);
    Assert.assertEquals(0, mFadeDrawable.mAlphas[2]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[0]);
    Assert.assertEquals(true, mFadeDrawable.mIsLayerOn[1]);
    Assert.assertEquals(false, mFadeDrawable.mIsLayerOn[2]);
  }

  private static class FakeFadeDrawable extends FadeDrawable {

    private long mCurrentTimeMs;

    public FakeFadeDrawable(Drawable[] layers) {
      super(layers);
      mCurrentTimeMs = 0;
    }

    @Override
    protected long getCurrentTimeMs() {
      return mCurrentTimeMs;
    }

    void incrementCurrentTimeMs(long increment) {
      mCurrentTimeMs += increment;
    }
  }
}
