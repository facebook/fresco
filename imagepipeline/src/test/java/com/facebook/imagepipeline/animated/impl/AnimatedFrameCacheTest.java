/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.impl;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.ByteConstants;
import com.facebook.imagepipeline.cache.BitmapCountingMemoryCacheFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.image.CloseableImage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AnimatedFrameCacheTest {

  @Mock public MemoryTrimmableRegistry mMemoryTrimmableRegistry;
  @Mock public Supplier<MemoryCacheParams> mMemoryCacheParamsSupplier;

  private CacheKey mCacheKey;
  private AnimatedFrameCache mAnimatedFrameCache;
  private CloseableReference<CloseableImage> mFrame1;
  private CloseableReference<CloseableImage> mFrame2;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    MemoryCacheParams params = new MemoryCacheParams(
        4 * ByteConstants.MB,
        256,
        Integer.MAX_VALUE,
        Integer.MAX_VALUE,
        Integer.MAX_VALUE);
    when(mMemoryCacheParamsSupplier.get()).thenReturn(params);
    CountingMemoryCache<CacheKey, CloseableImage> countingMemoryCache =
        BitmapCountingMemoryCacheFactory.get(mMemoryCacheParamsSupplier, mMemoryTrimmableRegistry);
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
}
