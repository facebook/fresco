/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests {@link FadeDrawable} with the default configuration.
 *
 * @see FadeDrawableAllOnTest for more tests
 */
@RunWith(RobolectricTestRunner.class)
public class FadeDrawableTest {
  private final Drawable[] mLayers =
      new Drawable[] {
        DrawableTestUtils.mockDrawable(),
        DrawableTestUtils.mockDrawable(),
        DrawableTestUtils.mockDrawable(),
      };

  private FakeFadeDrawable mFadeDrawable;
  private final Canvas mCanvas = mock(Canvas.class);
  private final Drawable.Callback mCallback = mock(Drawable.Callback.class);

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
    assertThat(mFadeDrawable.getIntrinsicWidth()).isEqualTo(200);
    assertThat(mFadeDrawable.getIntrinsicHeight()).isEqualTo(400);
  }

  @Test
  public void testInitialState() {
    // initially only the fist layer is displayed and there is no transition
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_NONE);
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(0);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
    assertThat(mFadeDrawable.mIsLayerOn[0]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[1]).isFalse();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
  }

  @Test
  public void testFadeToLayer() {
    // start fade
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.fadeToLayer(1);
    assertThat(mFadeDrawable.mDurationMs).isEqualTo(100);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_STARTING);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    assertThat(mFadeDrawable.mIsLayerOn[0]).isFalse();
    assertThat(mFadeDrawable.mIsLayerOn[1]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
    // alphas will change only when the next draw happens
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(0);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
  }

  @Test
  public void testFadeUpToLayer() {
    // start fade
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.fadeUpToLayer(1);
    assertThat(mFadeDrawable.mIsLayerOn[0]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[1]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
    assertThat(mFadeDrawable.mDurationMs).isEqualTo(100);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_STARTING);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    // alphas will change only when the next draw happens
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(0);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
  }

  @Test
  public void testFadeInLayer() {
    // start fade in
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.fadeInLayer(2);
    assertThat(mFadeDrawable.mIsLayerOn[0]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[1]).isFalse();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isTrue();
    assertThat(mFadeDrawable.mDurationMs).isEqualTo(100);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_STARTING);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    // alphas will change only when the next draw happens
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(0);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
  }

  @Test
  public void testFadeOutLayer() {
    // start fade out
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.fadeOutLayer(0);
    assertThat(mFadeDrawable.mIsLayerOn[0]).isFalse();
    assertThat(mFadeDrawable.mIsLayerOn[1]).isFalse();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
    assertThat(mFadeDrawable.mDurationMs).isEqualTo(100);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_STARTING);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    // alphas will change only when the next draw happens
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(0);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
  }

  @Test
  public void testFadeOutAllLayers() {
    // start fade out
    mFadeDrawable.setTransitionDuration(100);
    mFadeDrawable.mIsLayerOn[1] = true;
    mFadeDrawable.mIsLayerOn[2] = true;
    mFadeDrawable.fadeOutAllLayers();
    assertThat(mFadeDrawable.mIsLayerOn[0]).isFalse();
    assertThat(mFadeDrawable.mIsLayerOn[1]).isFalse();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
    assertThat(mFadeDrawable.mDurationMs).isEqualTo(100);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_STARTING);
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    // alphas will change only when the next draw happens
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(0);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
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
    assertThat(mFadeDrawable.mIsLayerOn[0]).isEqualTo(fadeUpToLayer);
    assertThat(mFadeDrawable.mIsLayerOn[1]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
    verify(mCallback).invalidateDrawable(mFadeDrawable);

    mFadeDrawable.finishTransitionImmediately();
    verify(mCallback, times(2)).invalidateDrawable(mFadeDrawable);
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(fadeUpToLayer ? 255 : 0);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_NONE);
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
    assertThat(mFadeDrawable.mIsLayerOn[0]).isEqualTo(fadeUpToLayer);
    assertThat(mFadeDrawable.mIsLayerOn[1]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
    verify(mCallback).invalidateDrawable(mFadeDrawable);

    mFadeDrawable.draw(mCanvas);
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(fadeUpToLayer ? 255 : 0);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_NONE);
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
    assertThat(mFadeDrawable.mIsLayerOn[0]).isEqualTo(fadeUpToLayer);
    assertThat(mFadeDrawable.mIsLayerOn[1]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
    verify(mCallback).invalidateDrawable(mFadeDrawable);
    verifyNoMoreInteractions(mCallback, mLayers[0], mLayers[1], mLayers[2]);

    // first frame
    resetInteractions();
    mFadeDrawable.draw(mCanvas);
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(0);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_RUNNING);
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
      assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(fadeUpToLayer ? 255 : 255 - 51 * i);
      assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(51 * i);
      assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
      assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_RUNNING);
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
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(fadeUpToLayer ? 255 : 0);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_NONE);
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
    assertThat(mFadeDrawable.getAlpha()).isEqualTo(128);
    inOrder.verify(mCallback).invalidateDrawable(mFadeDrawable);
    // next frame
    mFadeDrawable.incrementCurrentTimeMs(17);
    mFadeDrawable.draw(mCanvas);
    assertThat(mFadeDrawable.getAlpha()).isEqualTo(128);
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(51);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_RUNNING);
    inOrder.verify(mLayers[0]).mutate();
    inOrder.verify(mLayers[0]).setAlpha(128);
    inOrder.verify(mLayers[0]).draw(mCanvas);
    inOrder.verify(mLayers[1]).mutate();
    inOrder.verify(mLayers[1]).setAlpha(26);
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
    assertThat(mFadeDrawable.mTransitionState).isEqualTo(FadeDrawable.TRANSITION_NONE);
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(0);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
    assertThat(mFadeDrawable.mIsLayerOn[0]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[1]).isFalse();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
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
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
    assertThat(mFadeDrawable.mIsLayerOn[0]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[1]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
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
    assertThat(mFadeDrawable.mAlphas[0]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[1]).isEqualTo(255);
    assertThat(mFadeDrawable.mAlphas[2]).isEqualTo(0);
    assertThat(mFadeDrawable.mIsLayerOn[0]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[1]).isTrue();
    assertThat(mFadeDrawable.mIsLayerOn[2]).isFalse();
  }

  private static class FakeFadeDrawable extends FadeDrawable {

    private long mCurrentTimeMs;

    public FakeFadeDrawable(Drawable[] layers) {
      super(layers, false, 0);
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
