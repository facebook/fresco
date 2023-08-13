/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests {@link FixedNumberBitmapFramePreparationStrategy}. */
@RunWith(RobolectricTestRunner.class)
public class FixedNumberBitmapFramePreparationStrategyTest {

  private static final int NUMBER_OF_FRAMES_TO_PREPARE = 3;
  private static final int FRAME_COUNT = 10;

  @Mock public AnimationBackend mAnimationBackend;
  @Mock public BitmapFramePreparer mBitmapFramePreparer;
  @Mock public BitmapFrameCache mBitmapFrameCache;
  @Mock public Function0<Unit> onAnimationLoaded;

  private BitmapFramePreparationStrategy mBitmapFramePreparationStrategy;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mBitmapFramePreparationStrategy =
        new FixedNumberBitmapFramePreparationStrategy(NUMBER_OF_FRAMES_TO_PREPARE);
    when(mAnimationBackend.getFrameCount()).thenReturn(FRAME_COUNT);
    when(mBitmapFramePreparer.prepareFrame(eq(mBitmapFrameCache), eq(mAnimationBackend), anyInt()))
        .thenReturn(true);
  }

  @Test
  public void testPrepareFrames_FromFirstFrame() throws Exception {
    mBitmapFramePreparationStrategy.prepareFrames(
        mBitmapFramePreparer, mBitmapFrameCache, mAnimationBackend, 0, onAnimationLoaded);
    verifyPrepareCalledForFramesInOrder(1, 2, 3);
  }

  @Test
  public void testPrepareFrames_FromLastFrame() throws Exception {
    mBitmapFramePreparationStrategy.prepareFrames(
        mBitmapFramePreparer, mBitmapFrameCache, mAnimationBackend, 9, onAnimationLoaded);
    verifyPrepareCalledForFramesInOrder(0, 1, 2);
  }

  @Test
  public void testPrepareFrames_ExactlyLastFrames() throws Exception {
    mBitmapFramePreparationStrategy.prepareFrames(
        mBitmapFramePreparer, mBitmapFrameCache, mAnimationBackend, 6, onAnimationLoaded);
    verifyPrepareCalledForFramesInOrder(7, 8, 9);
  }

  @Test
  public void testPrepareFrames_FrameOverflow() throws Exception {
    mBitmapFramePreparationStrategy.prepareFrames(
        mBitmapFramePreparer, mBitmapFrameCache, mAnimationBackend, 8, onAnimationLoaded);
    verifyPrepareCalledForFramesInOrder(9, 0, 1);
  }

  @Test
  public void testPrepareFrames_FromFirstFrame_WhenBitmapFramePreparerAlwaysFails()
      throws Exception {
    when(mBitmapFramePreparer.prepareFrame(eq(mBitmapFrameCache), eq(mAnimationBackend), anyInt()))
        .thenReturn(false);
    mBitmapFramePreparationStrategy.prepareFrames(
        mBitmapFramePreparer, mBitmapFrameCache, mAnimationBackend, 0, onAnimationLoaded);
    verifyPrepareCalledForFramesInOrder(1);
  }

  @Test
  public void testPrepareFrames_FromFirstFrame_WhenBitmapFramePreparerFailsForSelectedFrames()
      throws Exception {
    when(mBitmapFramePreparer.prepareFrame(eq(mBitmapFrameCache), eq(mAnimationBackend), eq(2)))
        .thenReturn(false);
    when(mBitmapFramePreparer.prepareFrame(eq(mBitmapFrameCache), eq(mAnimationBackend), eq(3)))
        .thenReturn(false);
    mBitmapFramePreparationStrategy.prepareFrames(
        mBitmapFramePreparer, mBitmapFrameCache, mAnimationBackend, 0, onAnimationLoaded);
    verifyPrepareCalledForFramesInOrder(1, 2);
  }

  @Test
  public void testPrepareFrames_onAnimationLoadedIsTrigger_WhenFramesAreLoaded() {
    mBitmapFramePreparationStrategy.prepareFrames(
        mBitmapFramePreparer, mBitmapFrameCache, mAnimationBackend, 0, onAnimationLoaded);

    verify(onAnimationLoaded).invoke();
  }

  private void verifyPrepareCalledForFramesInOrder(int... frameNumbers) {
    InOrder inOrderBitmapFramePreparer = inOrder(mBitmapFramePreparer);
    for (int frameNumber : frameNumbers) {
      inOrderBitmapFramePreparer
          .verify(mBitmapFramePreparer)
          .prepareFrame(mBitmapFrameCache, mAnimationBackend, frameNumber);
    }
    inOrderBitmapFramePreparer.verifyNoMoreInteractions();
  }
}
