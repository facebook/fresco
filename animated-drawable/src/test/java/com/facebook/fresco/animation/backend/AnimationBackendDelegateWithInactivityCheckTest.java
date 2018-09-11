/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.backend;

import static com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck.INACTIVITY_THRESHOLD_MS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestScheduledExecutorService;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link AnimationBackendDelegateWithInactivityCheck}
 */
public class AnimationBackendDelegateWithInactivityCheckTest {

  private AnimationBackendDelegate<AnimationBackend>
      mAnimationBackendDelegateWithInactivityCheck;

  private AnimationBackend mAnimationBackend;
  private AnimationBackendDelegateWithInactivityCheck.InactivityListener mInactivityListener;
  private Drawable mParent;
  private Canvas mCanvas;
  private FakeClock mFakeClock;
  private TestScheduledExecutorService mTestScheduledExecutorService;

  @Before
  public void setup() {
    mAnimationBackend = mock(AnimationBackend.class);
    mInactivityListener =
        mock(AnimationBackendDelegateWithInactivityCheck.InactivityListener.class);
    mParent = mock(Drawable.class);
    mCanvas = mock(Canvas.class);

    mFakeClock = new FakeClock();
    mTestScheduledExecutorService = new TestScheduledExecutorService(mFakeClock);

    mAnimationBackendDelegateWithInactivityCheck =
        AnimationBackendDelegateWithInactivityCheck.createForBackend(
            mAnimationBackend,
            mInactivityListener,
            mFakeClock,
            mTestScheduledExecutorService);
  }

  @Test
  public void testNotifyInactive() {
    verifyZeroInteractions(mInactivityListener);
    mAnimationBackendDelegateWithInactivityCheck.drawFrame(mParent, mCanvas, 0);
    verifyZeroInteractions(mInactivityListener);
    mFakeClock.incrementBy(100);
    verifyZeroInteractions(mInactivityListener);
    mFakeClock.incrementBy(INACTIVITY_THRESHOLD_MS);
    verify(mInactivityListener).onInactive();
  }
}
