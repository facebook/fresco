/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class SmallCacheIfRequestedDiskCachePolicyTest {

  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public ImageRequest mImageRequest;
  @Mock public Object mCallerContext;
  @Mock public Exception mException;
  @Mock private EncodedImage mEncodedImage;
  private final BufferedDiskCache mDefaultBufferedDiskCache = mock(BufferedDiskCache.class);
  private final BufferedDiskCache mSmallImageBufferedDiskCache = mock(BufferedDiskCache.class);
  private MultiCacheKey mCacheKey;
  private AtomicBoolean mIsCancelled;

  private SmallCacheIfRequestedDiskCachePolicy mSmallCacheIfRequestedDiskCachePolicy;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    List<CacheKey> keys = new ArrayList<>(1);
    keys.add(new SimpleCacheKey("http://dummy.uri"));
    mCacheKey = new MultiCacheKey(keys);

    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, mCallerContext)).thenReturn(mCacheKey);
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(true);

    mIsCancelled = new AtomicBoolean(false);

    mSmallCacheIfRequestedDiskCachePolicy = new SmallCacheIfRequestedDiskCachePolicy(
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory);
  }

  @Test
  public void testSmallImageIsReadFromSmallCache() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);

    mSmallCacheIfRequestedDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    verify(mSmallImageBufferedDiskCache).get(mCacheKey, mIsCancelled);
    verify(mDefaultBufferedDiskCache, never()).get(mCacheKey, mIsCancelled);
  }

  @Test
  public void testBigImageIsReadFromDefaultCache() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);

    mSmallCacheIfRequestedDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    verify(mSmallImageBufferedDiskCache).get(mCacheKey, mIsCancelled);
    verify(mDefaultBufferedDiskCache, never()).get(mCacheKey, mIsCancelled);
  }

  @Test
  public void testSmallImageIsWrittenToSmallCache() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);

    mSmallCacheIfRequestedDiskCachePolicy
        .writeToCache(mEncodedImage, mImageRequest, mCallerContext);

    verify(mSmallImageBufferedDiskCache).put(mCacheKey, mEncodedImage);
    verify(mDefaultBufferedDiskCache, never()).put(eq(mCacheKey), any(EncodedImage.class));
  }

  @Test
  public void testBigImageIsWrittenToDefaultCache() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);

    mSmallCacheIfRequestedDiskCachePolicy
        .writeToCache(mEncodedImage, mImageRequest, mCallerContext);

    verify(mDefaultBufferedDiskCache).put(mCacheKey, mEncodedImage);
    verify(mSmallImageBufferedDiskCache, never()).put(eq(mCacheKey), any(EncodedImage.class));
  }
}
