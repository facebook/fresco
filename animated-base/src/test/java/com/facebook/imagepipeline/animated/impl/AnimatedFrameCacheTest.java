/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.impl;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.ByteConstants;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheTrimStrategy;
import com.facebook.imagepipeline.cache.CountingLruBitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.image.CloseableImage;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AnimatedFrameCacheTest {

  @Mock public MemoryTrimmableRegistry mMemoryTrimmableRegistry;
  @Mock public Supplier<MemoryCacheParams> mMemoryCacheParamsSupplier;
  @Mock public PlatformBitmapFactory mPlatformBitmapFactory;

  private CacheKey mCacheKey;
  private AnimatedFrameCache mAnimatedFrameCache;
  private CloseableReference<CloseableImage> mFrame1;
  private CloseableReference<CloseableImage> mFrame2;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    MemoryCacheParams params =
        new MemoryCacheParams(
            4 * ByteConstants.MB,
            256,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            TimeUnit.MINUTES.toMillis(5));
    when(mMemoryCacheParamsSupplier.get()).thenReturn(params);
    CountingMemoryCache<CacheKey, CloseableImage> countingMemoryCache =
        new CountingLruBitmapMemoryCacheFactory()
            .create(
                mMemoryCacheParamsSupplier,
                mMemoryTrimmableRegistry,
                new BitmapMemoryCacheTrimStrategy(),
                null);
    mCacheKey = new SimpleCacheKey("key");
    mAnimatedFrameCache = new AnimatedFrameCache(mCacheKey, countingMemoryCache);
    mFrame1 = CloseableReference.of(mock(CloseableImage.class));
    mFrame2 = CloseableReference.of(mock(CloseableImage.class));
  }

  @Test
  public void testBasic() {
    CloseableReference<CloseableImage> ret = mAnimatedFrameCache.cache(1, mFrame1);
    assertSame(ret.get(), mFrame1.get());
  }

  @Test
  public void testMultipleFrames() {
    mAnimatedFrameCache.cache(1, mFrame1);
    mAnimatedFrameCache.cache(2, mFrame2);
    assertSame(mFrame1.get(), mAnimatedFrameCache.get(1).get());
    assertSame(mFrame2.get(), mAnimatedFrameCache.get(2).get());
  }

  @Test
  public void testReplace() {
    mAnimatedFrameCache.cache(1, mFrame1);
    mAnimatedFrameCache.cache(1, mFrame2);
    assertNotSame(mFrame1.get(), mAnimatedFrameCache.get(1).get());
    assertSame(mFrame2.get(), mAnimatedFrameCache.get(1).get());
  }

  @Test
  public void testReuse() {
    CloseableReference<CloseableImage> ret = mAnimatedFrameCache.cache(1, mFrame1);
    ret.close();
    CloseableReference<CloseableImage> free = mAnimatedFrameCache.getForReuse();
    assertNotNull(free);
  }

  @Test
  public void testCantReuseIfNotClosed() {
    CloseableReference<CloseableImage> ret = mAnimatedFrameCache.cache(1, mFrame1);
    CloseableReference<CloseableImage> free = mAnimatedFrameCache.getForReuse();
    assertNull(free);
  }

  @Test
  public void testStillThereIfClosed() {
    CloseableReference<CloseableImage> ret = mAnimatedFrameCache.cache(1, mFrame1);
    ret.close();
    assertNotNull(mAnimatedFrameCache.get(1));
  }

  @Test
  public void testContains() {
    assertFalse(mAnimatedFrameCache.contains(1));

    CloseableReference<CloseableImage> ret = mAnimatedFrameCache.cache(1, mFrame1);

    assertTrue(mAnimatedFrameCache.contains(1));
    assertFalse(mAnimatedFrameCache.contains(2));

    ret.close();

    assertTrue(mAnimatedFrameCache.contains(1));
    assertFalse(mAnimatedFrameCache.contains(2));
  }

  @Test
  public void testContainsWhenReused() {
    CloseableReference<CloseableImage> ret = mAnimatedFrameCache.cache(1, mFrame1);
    ret.close();

    assertTrue(mAnimatedFrameCache.contains(1));
    assertFalse(mAnimatedFrameCache.contains(2));

    CloseableReference<CloseableImage> free = mAnimatedFrameCache.getForReuse();
    free.close();

    assertFalse(mAnimatedFrameCache.contains(1));
    assertFalse(mAnimatedFrameCache.contains(2));
  }

  @Test
  public void testContainsFullReuseFlowWithMultipleItems() {
    assertFalse(mAnimatedFrameCache.contains(1));
    assertFalse(mAnimatedFrameCache.contains(2));

    CloseableReference<CloseableImage> ret = mAnimatedFrameCache.cache(1, mFrame1);
    CloseableReference<CloseableImage> ret2 = mAnimatedFrameCache.cache(2, mFrame2);

    assertTrue(mAnimatedFrameCache.contains(1));
    assertTrue(mAnimatedFrameCache.contains(2));

    ret.close();
    ret2.close();

    assertTrue(mAnimatedFrameCache.contains(1));
    assertTrue(mAnimatedFrameCache.contains(2));

    CloseableReference<CloseableImage> free = mAnimatedFrameCache.getForReuse();
    free.close();

    assertFalse(mAnimatedFrameCache.contains(1));
    assertTrue(mAnimatedFrameCache.contains(2));

    free = mAnimatedFrameCache.getForReuse();
    free.close();

    assertFalse(mAnimatedFrameCache.contains(1));
    assertFalse(mAnimatedFrameCache.contains(2));
  }
}
