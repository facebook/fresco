/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import com.facebook.fresco.ui.common.OnFadeListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests {@link FadeDrawable.OnFadeListener} */
@RunWith(RobolectricTestRunner.class)
public class FadeDrawableOnFadeListenerTest {

  public static final int DURATION = 1000;
  public static final int ACTUAL_LAYER_INDEX = 1;
  public static final int OTHER_LAYER_INDEX = 2;

  private final Drawable[] mLayers =
      new Drawable[] {
        DrawableTestUtils.mockDrawable(),
        DrawableTestUtils.mockDrawable(),
        DrawableTestUtils.mockDrawable(),
      };

  private FadeDrawable mFadeDrawable;
  private final OnFadeListener mOnFadeListener = mock(OnFadeListener.class);

  private final Canvas mCanvas = mock(Canvas.class);

  @Before
  public void setUp() {
    //    PowerMockito.mockStatic(SystemClock.class);
    mFadeDrawable = new FadeDrawable(mLayers, false, 1);
    mFadeDrawable.setTransitionDuration(DURATION);
    mFadeDrawable.setOnFadeListener(mOnFadeListener);
  }

  @Test
  public void testSimple() {
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.1));

    mFadeDrawable.fadeInLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeStarted();

    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.5));
    mFadeDrawable.draw(mCanvas);
    mFadeDrawable.draw(mCanvas);
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, never()).onFadeFinished();

    SystemClock.setCurrentTimeMillis((long) (DURATION * 1.5));
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeFinished();

    verifyNoMoreInteractions(mOnFadeListener);
  }

  @Test
  public void testComplex() {
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.1));

    mFadeDrawable.fadeInLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.fadeOutLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.fadeInLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.fadeInLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.fadeInLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeStarted();

    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.5));
    mFadeDrawable.draw(mCanvas);
    mFadeDrawable.draw(mCanvas);
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, never()).onFadeFinished();

    SystemClock.setCurrentTimeMillis((long) (DURATION * 1.1));
    //    mFadeDrawable.fadeInAllLayers();
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeFinished();

    verifyNoMoreInteractions(mOnFadeListener);
  }

  @Test
  public void testFinishImmediately() {
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.1));

    mFadeDrawable.fadeInLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeStarted();

    mFadeDrawable.finishTransitionImmediately();
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeFinished();

    verifyNoMoreInteractions(mOnFadeListener);
  }

  @Test
  public void testFadeInAll() {
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.1));

    mFadeDrawable.fadeInAllLayers();
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeStarted();

    SystemClock.setCurrentTimeMillis((long) (DURATION * 1.1));
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeFinished();

    verifyNoMoreInteractions(mOnFadeListener);
  }

  @Test
  public void testFadeTheOtherLayer() {
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.1));

    mFadeDrawable.fadeInLayer(OTHER_LAYER_INDEX);
    mFadeDrawable.draw(mCanvas);

    SystemClock.setCurrentTimeMillis((long) (DURATION * 1.1));
    mFadeDrawable.draw(mCanvas);

    verifyNoMoreInteractions(mOnFadeListener);
  }

  @Test
  public void testFadeOut() {
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.1));
    mFadeDrawable.fadeInAllLayers();
    // Notice we finish immediately before the draw call:
    mFadeDrawable.finishTransitionImmediately();
    mFadeDrawable.draw(mCanvas);

    mFadeDrawable.fadeOutLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.draw(mCanvas);

    SystemClock.setCurrentTimeMillis((long) (DURATION * 1.1));
    mFadeDrawable.draw(mCanvas);

    verifyNoMoreInteractions(mOnFadeListener);
  }

  @Test
  public void testFadeOut2() {
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.1));
    mFadeDrawable.fadeInAllLayers();
    // Notice we call draw before finishing immediately
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeStarted();
    mFadeDrawable.finishTransitionImmediately();
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeFinished();

    verifyNoMoreInteractions(mOnFadeListener);
  }

  @Test
  public void testMultipleFades() {
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.1));

    mFadeDrawable.fadeInLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeStarted();

    mFadeDrawable.fadeInLayer(OTHER_LAYER_INDEX);
    mFadeDrawable.draw(mCanvas);
    verifyNoMoreInteractions(mOnFadeListener);
    SystemClock.setCurrentTimeMillis((long) (DURATION * 1.2));
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeFinished();
  }

  @Test
  public void testLateListener() {
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.1));

    // No listener set:
    mFadeDrawable.setOnFadeListener(null);

    mFadeDrawable.fadeInLayer(ACTUAL_LAYER_INDEX);
    mFadeDrawable.draw(mCanvas);
    SystemClock.setCurrentTimeMillis((long) (DURATION * 0.5));
    mFadeDrawable.draw(mCanvas);
    mFadeDrawable.draw(mCanvas);
    mFadeDrawable.draw(mCanvas);

    // Set the listener back
    mFadeDrawable.setOnFadeListener(mOnFadeListener);

    SystemClock.setCurrentTimeMillis((long) (DURATION * 1.5));
    mFadeDrawable.draw(mCanvas);
    verify(mOnFadeListener, times(1)).onFadeFinished();

    verifyNoMoreInteractions(mOnFadeListener);
  }
}
