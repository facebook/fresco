/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.bitmap.preparation;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
/**
 * Tests {@link DefaultBitmapFramePreparer}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CloseableReference.class)
public class DefaultBitmapFramePreparerTest {

  private static final int FRAME_COUNT = 10;
  private static final int BACKEND_INTRINSIC_WIDTH = 160;
  private static final int BACKEND_INTRINSIC_HEIGHT = 90;
  private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

  @Mock public AnimationBackend mAnimationBackend;
  @Mock public BitmapFrameCache mBitmapFrameCache;
  @Mock public PlatformBitmapFactory mPlatformBitmapFactory;
  @Mock public BitmapFrameRenderer mBitmapFrameRenderer;
  @Mock public CloseableReference<Bitmap> mBitmapReference;
  @Mock public Bitmap mBitmap;

  private FakeClock mFakeClock;
  private TestExecutorService mExecutorService;

  private DefaultBitmapFramePreparer mDefaultBitmapFramePreparer;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    mFakeClock = new FakeClock();
    mExecutorService = new TestExecutorService(mFakeClock);

    mDefaultBitmapFramePreparer = new DefaultBitmapFramePreparer(
        mPlatformBitmapFactory,
        mBitmapFrameRenderer,
        BITMAP_CONFIG,
        mExecutorService);
    when(mAnimationBackend.getFrameCount()).thenReturn(FRAME_COUNT);
    when(mAnimationBackend.getIntrinsicWidth()).thenReturn(BACKEND_INTRINSIC_WIDTH);
    when(mAnimationBackend.getIntrinsicHeight()).thenReturn(BACKEND_INTRINSIC_HEIGHT);
    when(mBitmapReference.isValid()).thenReturn(true);
    when(mBitmapReference.get()).thenReturn(mBitmap);
  }

  @Test
  public void testPrepareFrame_whenBitmapAlreadyCached_thenDoNothing() {
    when(mBitmapFrameCache.contains(1)).thenReturn(true);
    when(mBitmapFrameRenderer.renderFrame(1, mBitmap)).thenReturn(true);

    mDefaultBitmapFramePreparer.prepareFrame(mBitmapFrameCache, mAnimationBackend, 1);

    assertThat(mExecutorService.getScheduledQueue().isIdle()).isTrue();

    verify(mBitmapFrameCache).contains(1);
    verifyNoMoreInteractions(mBitmapFrameCache);
    verifyZeroInteractions(mPlatformBitmapFactory, mBitmapFrameRenderer, mBitmapReference);
  }

  @Test
  public void testPrepareFrame_whenNoBitmapAvailable_thenDoNothing() {
    mDefaultBitmapFramePreparer.prepareFrame(mBitmapFrameCache, mAnimationBackend, 1);

    verify(mBitmapFrameCache).contains(1);
    verifyNoMoreInteractions(mBitmapFrameCache);
    reset(mBitmapFrameCache);

    mExecutorService.getScheduledQueue().runNextPendingCommand();

    verify(mBitmapFrameCache).contains(1);
    verify(mBitmapFrameCache).getBitmapToReuseForFrame(
        1,
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT);
    verifyNoMoreInteractions(mBitmapFrameCache);

    verify(mPlatformBitmapFactory).createBitmap(
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT,
        BITMAP_CONFIG);

    verifyZeroInteractions(mBitmapFrameRenderer);
  }

  @Test
  public void testPrepareFrame_whenReusedBitmapAvailable_thenCacheReusedBitmap() {
    when(mBitmapFrameCache.getBitmapToReuseForFrame(
        1,
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT))
        .thenReturn(mBitmapReference);
    when(mBitmapFrameRenderer.renderFrame(1, mBitmap)).thenReturn(true);

    mDefaultBitmapFramePreparer.prepareFrame(mBitmapFrameCache, mAnimationBackend, 1);

    mExecutorService.getScheduledQueue().runNextPendingCommand();

    verify(mBitmapFrameCache, times(2)).contains(1);
    verify(mBitmapFrameCache).getBitmapToReuseForFrame(
        1,
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT);
    verify(mBitmapFrameRenderer).renderFrame(1, mBitmap);

    verify(mBitmapFrameCache).onFramePrepared(
        1,
        mBitmapReference,
        BitmapAnimationBackend.FRAME_TYPE_REUSED);

    verifyZeroInteractions(mPlatformBitmapFactory);
  }

  @Test
  public void testPrepareFrame_whenPlatformBitmapAvailable_thenCacheCreatedBitmap() {
    when(mPlatformBitmapFactory.createBitmap(
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT,
        BITMAP_CONFIG))
        .thenReturn(mBitmapReference);
    when(mBitmapFrameRenderer.renderFrame(1, mBitmap)).thenReturn(true);

    mDefaultBitmapFramePreparer.prepareFrame(mBitmapFrameCache, mAnimationBackend, 1);

    mExecutorService.getScheduledQueue().runNextPendingCommand();

    verify(mBitmapFrameCache, times(2)).contains(1);
    verify(mBitmapFrameCache).getBitmapToReuseForFrame(
        1,
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT);
    verify(mPlatformBitmapFactory).createBitmap(
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT,
        BITMAP_CONFIG);
    verify(mBitmapFrameRenderer).renderFrame(1, mBitmap);

    verify(mBitmapFrameCache).onFramePrepared(
        1,
        mBitmapReference,
        BitmapAnimationBackend.FRAME_TYPE_CREATED);

    verifyNoMoreInteractions(mPlatformBitmapFactory);
  }

  @Test
  public void testPrepareFrame_whenReusedAndPlatformBitmapAvailable_thenCacheReusedBitmap() {
    when(mBitmapFrameCache.getBitmapToReuseForFrame(
        1,
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT))
        .thenReturn(mBitmapReference);
    when(mPlatformBitmapFactory.createBitmap(
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT,
        BITMAP_CONFIG))
        .thenReturn(mBitmapReference);
    when(mBitmapFrameRenderer.renderFrame(1, mBitmap)).thenReturn(true);

    mDefaultBitmapFramePreparer.prepareFrame(mBitmapFrameCache, mAnimationBackend, 1);

    mExecutorService.getScheduledQueue().runNextPendingCommand();

    verify(mBitmapFrameCache, times(2)).contains(1);
    verify(mBitmapFrameCache).getBitmapToReuseForFrame(
        1,
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT);
    verify(mBitmapFrameRenderer).renderFrame(1, mBitmap);

    verify(mBitmapFrameCache).onFramePrepared(
        1,
        mBitmapReference,
        BitmapAnimationBackend.FRAME_TYPE_REUSED);

    verifyZeroInteractions(mPlatformBitmapFactory);
  }

  @Test
  public void testPrepareFrame_whenRenderingFails_thenDoNothing() {
    when(mBitmapFrameCache.getBitmapToReuseForFrame(
        1,
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT))
        .thenReturn(mBitmapReference);
    when(mPlatformBitmapFactory.createBitmap(
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT,
        BITMAP_CONFIG))
        .thenReturn(mBitmapReference);
    when(mBitmapFrameRenderer.renderFrame(1, mBitmap)).thenReturn(false);

    mDefaultBitmapFramePreparer.prepareFrame(mBitmapFrameCache, mAnimationBackend, 1);

    mExecutorService.getScheduledQueue().runNextPendingCommand();

    verify(mBitmapFrameCache, times(2)).contains(1);
    verify(mBitmapFrameCache).getBitmapToReuseForFrame(
        1,
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT);
    verify(mPlatformBitmapFactory).createBitmap(
        BACKEND_INTRINSIC_WIDTH,
        BACKEND_INTRINSIC_HEIGHT,
        BITMAP_CONFIG);
    verify(mBitmapFrameRenderer, times(2)).renderFrame(1, mBitmap);

    verifyNoMoreInteractions(mBitmapFrameCache);
  }
}
