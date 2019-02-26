/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.frame;

import static org.fest.assertions.api.Assertions.assertThat;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.IntRange;
import com.facebook.fresco.animation.backend.AnimationBackend;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link DropFramesFrameScheduler}.
 */
public class DropFramesFrameSchedulerTest {

  private DummyAnimationBackend mDummyAnimationBackend;

  private DropFramesFrameScheduler mFrameScheduler;

  @Before
  public void setUp() throws Exception {
    mDummyAnimationBackend = new DummyAnimationBackend();
    mFrameScheduler = new DropFramesFrameScheduler(mDummyAnimationBackend);
  }

  @Test
  public void testGetFrameNumberToRender() throws Exception {
    assertThat(mFrameScheduler.getFrameNumberToRender(0, -1)).isEqualTo(0);
    assertThat(mFrameScheduler.getFrameNumberToRender(50, -1)).isEqualTo(0);
    assertThat(mFrameScheduler.getFrameNumberToRender(100, -1)).isEqualTo(1);
    assertThat(mFrameScheduler.getFrameNumberToRender(499, -1)).isEqualTo(4);
    assertThat(mFrameScheduler.getFrameNumberToRender(500, -1)).isEqualTo(0);
    assertThat(mFrameScheduler.getFrameNumberToRender(600, -1)).isEqualTo(1);
    assertThat(mFrameScheduler.getFrameNumberToRender(601, -1)).isEqualTo(1);
  }

  @Test
  public void testGetLoopDurationMs() throws Exception {
    assertThat(mFrameScheduler.getLoopDurationMs()).isEqualTo(500);
  }

  @Test
  public void testGetTargetRenderTimeMs() throws Exception {
    assertThat(mFrameScheduler.getTargetRenderTimeMs(0)).isEqualTo(0);
    assertThat(mFrameScheduler.getTargetRenderTimeMs(1)).isEqualTo(100);
    assertThat(mFrameScheduler.getTargetRenderTimeMs(2)).isEqualTo(200);
    assertThat(mFrameScheduler.getTargetRenderTimeMs(3)).isEqualTo(300);
    assertThat(mFrameScheduler.getTargetRenderTimeMs(4)).isEqualTo(400);
  }

  @Test
  public void testGetTargetRenderTimeForNextFrameMs() throws Exception {
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(0)).isEqualTo(100);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(1)).isEqualTo(100);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(50)).isEqualTo(100);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(100)).isEqualTo(200);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(170)).isEqualTo(200);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(460)).isEqualTo(500);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(499)).isEqualTo(500);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(500)).isEqualTo(600);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(501)).isEqualTo(600);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(510)).isEqualTo(600);
  }

  @Test
  public void testGetTargetRenderTimeForNextFrameMsWhenAnimationOver() throws Exception {
    long animationDurationMs = mDummyAnimationBackend.getAnimationDurationMs();

    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs - 1))
        .isEqualTo(animationDurationMs);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs))
        .isEqualTo(FrameScheduler.NO_NEXT_TARGET_RENDER_TIME);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs + 1))
        .isEqualTo(FrameScheduler.NO_NEXT_TARGET_RENDER_TIME);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs + 100))
        .isEqualTo(FrameScheduler.NO_NEXT_TARGET_RENDER_TIME);
    assertThat(mFrameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs * 100))
        .isEqualTo(FrameScheduler.NO_NEXT_TARGET_RENDER_TIME);
  }

  @Test
  public void testIsInfiniteAnimation() throws Exception {
    assertThat(mFrameScheduler.isInfiniteAnimation()).isFalse();
  }

  @Test
  public void testLoopCount() throws Exception {
    long animationDurationMs = mDummyAnimationBackend.getAnimationDurationMs();
    int lastFrameNumber = mDummyAnimationBackend.getFrameCount() - 1;

    assertThat(mFrameScheduler.getFrameNumberToRender(animationDurationMs, -1))
        .isEqualTo(FrameScheduler.FRAME_NUMBER_DONE);

    assertThat(mFrameScheduler.getFrameNumberToRender(animationDurationMs + 1, -1))
        .isEqualTo(FrameScheduler.FRAME_NUMBER_DONE);

    assertThat(mFrameScheduler.getFrameNumberToRender(
        animationDurationMs + mDummyAnimationBackend.getFrameDurationMs(lastFrameNumber), -1))
        .isEqualTo(FrameScheduler.FRAME_NUMBER_DONE);

    assertThat(mFrameScheduler.getFrameNumberToRender(
        animationDurationMs + mDummyAnimationBackend.getFrameDurationMs(lastFrameNumber) + 100, -1))
        .isEqualTo(FrameScheduler.FRAME_NUMBER_DONE);
  }

  @Test
  public void testGetFrameNumberWithinLoop() throws Exception {
    assertThat(mFrameScheduler.getFrameNumberWithinLoop(0)).isEqualTo(0);
    assertThat(mFrameScheduler.getFrameNumberWithinLoop(1)).isEqualTo(0);
    assertThat(mFrameScheduler.getFrameNumberWithinLoop(99)).isEqualTo(0);
    assertThat(mFrameScheduler.getFrameNumberWithinLoop(100)).isEqualTo(1);
    assertThat(mFrameScheduler.getFrameNumberWithinLoop(101)).isEqualTo(1);
    assertThat(mFrameScheduler.getFrameNumberWithinLoop(250)).isEqualTo(2);
    assertThat(mFrameScheduler.getFrameNumberWithinLoop(499)).isEqualTo(4);
  }

  private static class DummyAnimationBackend implements AnimationBackend {

    public long getLoopDurationMs() {
      long loopDuration = 0;
      for (int i = 0; i < getFrameCount(); i++) {
        loopDuration += getFrameDurationMs(i);
      }
      return loopDuration;
    }

    public long getAnimationDurationMs() {
      return getLoopDurationMs() * getLoopCount();
    }

    @Override
    public int getFrameCount() {
      return 5;
    }

    @Override
    public int getFrameDurationMs(int frameNumber) {
      return 100;
    }

    @Override
    public int getLoopCount() {
      return 7;
    }

    @Override
    public boolean drawFrame(
        Drawable parent, Canvas canvas, int frameNumber) {
      return false;
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public void setBounds(Rect bounds) {

    }

    @Override
    public int getIntrinsicWidth() {
      return INTRINSIC_DIMENSION_UNSET;
    }

    @Override
    public int getIntrinsicHeight() {
      return INTRINSIC_DIMENSION_UNSET;
    }

    @Override
    public int getSizeInBytes() {
      return 0;
    }

    @Override
    public void clear() {
    }
  }
}
