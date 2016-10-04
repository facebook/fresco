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
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;

import bolts.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class SplitCachesByImageSizeDiskCachePolicyTest {

  private static final int FORCE_SMALL_CACHE_THRESHOLD = 2048;

  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public ImageRequest mImageRequest;
  @Mock public Object mCallerContext;
  @Mock public Exception mException;
  @Mock public PooledByteBuffer mImagePooledByteBuffer;
  private EncodedImage mEncodedImage;
  private Task.TaskCompletionSource mTaskCompletionSource;
  private final BufferedDiskCache mDefaultBufferedDiskCache = mock(BufferedDiskCache.class);
  private final BufferedDiskCache mSmallImageBufferedDiskCache =
      mock(BufferedDiskCache.class);
  private MultiCacheKey mCacheKey;
  private AtomicBoolean mIsCancelled;

  private SplitCachesByImageSizeDiskCachePolicy mSplitCachesByImageSizeDiskCachePolicy;

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

    mEncodedImage = new EncodedImage(CloseableReference.of(mImagePooledByteBuffer));

    mSplitCachesByImageSizeDiskCachePolicy = new SplitCachesByImageSizeDiskCachePolicy(
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        FORCE_SMALL_CACHE_THRESHOLD);
  }

  @Test
  public void testChecksSmallCacheFirst() {
    setupDiskCacheGetSuccess(mSmallImageBufferedDiskCache);

    mSplitCachesByImageSizeDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    verify(mDefaultBufferedDiskCache, never()).get(eq(mCacheKey), any(AtomicBoolean.class));
  }

  @Test
  public void testAvoidsDiskReadWhenFoundInIndexOfDefaultCache() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);
    when(mDefaultBufferedDiskCache.containsSync(mCacheKey)).thenReturn(true);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);

    mSplitCachesByImageSizeDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    verify(mDefaultBufferedDiskCache).get(eq(mCacheKey), any(AtomicBoolean.class));
    verify(mSmallImageBufferedDiskCache, never()).get(eq(mCacheKey), any(AtomicBoolean.class));
  }

  @Test
  public void testAvoidsDiskReadWhenFoundInIndexOfSmallCache() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);
    when(mSmallImageBufferedDiskCache.containsSync(mCacheKey)).thenReturn(true);
    setupDiskCacheGetSuccess(mSmallImageBufferedDiskCache);

    mSplitCachesByImageSizeDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    verify(mSmallImageBufferedDiskCache).get(eq(mCacheKey), any(AtomicBoolean.class));
    verify(mDefaultBufferedDiskCache, never()).get(eq(mCacheKey), any(AtomicBoolean.class));
  }

  @Test
  public void testReadsTwiceWhenNecessary() {
    setupDiskCacheGetNotFound(mSmallImageBufferedDiskCache);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);

    Task<EncodedImage> task = mSplitCachesByImageSizeDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    assertThat(task.getResult()).isSameAs(mEncodedImage);
    verify(mSmallImageBufferedDiskCache).get(eq(mCacheKey), any(AtomicBoolean.class));
    verify(mDefaultBufferedDiskCache).get(eq(mCacheKey), any(AtomicBoolean.class));
  }

  @Test
  public void testIgnoresSmallHintIndex() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);
    when(mDefaultBufferedDiskCache.containsSync(mCacheKey)).thenReturn(true);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);

    Task<EncodedImage> task = mSplitCachesByImageSizeDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    assertThat(task.getResult()).isSameAs(mEncodedImage);
    verify(mSmallImageBufferedDiskCache, never())
        .get(any(CacheKey.class), any(AtomicBoolean.class));
  }

  @Test
  public void testIgnoresSmallHintDisk() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);
    setupDiskCacheGetNotFound(mSmallImageBufferedDiskCache);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);

    Task<EncodedImage> task = mSplitCachesByImageSizeDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    assertThat(task.getResult()).isSameAs(mEncodedImage);
    verify(mDefaultBufferedDiskCache).get(eq(mCacheKey), any(AtomicBoolean.class));
  }

  @Test
  public void testSmallFailureDoesntStopDefaultRead() {
    setupDiskCacheGetFailure(mSmallImageBufferedDiskCache);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);

    Task<EncodedImage> task = mSplitCachesByImageSizeDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    assertThat(task.getResult()).isSameAs(mEncodedImage);
  }

  @Test
  public void testCancellationStopsSecondRead() {
    setupDiskCacheGetWait(mSmallImageBufferedDiskCache);

    Task<EncodedImage> task = mSplitCachesByImageSizeDiskCachePolicy
        .createAndStartCacheReadTask(mImageRequest, mCallerContext, mIsCancelled);

    mTaskCompletionSource.setCancelled();

    assertThat(task.getResult()).isNull();
    verify(mDefaultBufferedDiskCache, never()).get(eq(mCacheKey), any(AtomicBoolean.class));
  }

  @Test
  public void testSmallImageIsWrittenToSmallCache() {
    when(mImagePooledByteBuffer.size()).thenReturn(FORCE_SMALL_CACHE_THRESHOLD - 1);

    mSplitCachesByImageSizeDiskCachePolicy
        .writeToCache(mEncodedImage, mImageRequest, mCallerContext);

    verify(mSmallImageBufferedDiskCache).put(mCacheKey, mEncodedImage);
    verifyZeroInteractions(mDefaultBufferedDiskCache);
  }

  @Test
  public void testBigImageInDefaultCache() {
    when(mImagePooledByteBuffer.size()).thenReturn(FORCE_SMALL_CACHE_THRESHOLD);

    mSplitCachesByImageSizeDiskCachePolicy
        .writeToCache(mEncodedImage, mImageRequest, mCallerContext);

    verify(mDefaultBufferedDiskCache).put(mCacheKey, mEncodedImage);
    verifyZeroInteractions(mSmallImageBufferedDiskCache);
  }

  @Test
  public void testIgnoresSmallHintOnWrite() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);
    when(mImagePooledByteBuffer.size()).thenReturn(FORCE_SMALL_CACHE_THRESHOLD);

    mSplitCachesByImageSizeDiskCachePolicy
        .writeToCache(mEncodedImage, mImageRequest, mCallerContext);

    verify(mDefaultBufferedDiskCache).put(mCacheKey, mEncodedImage);
    verifyZeroInteractions(mSmallImageBufferedDiskCache);
  }

  private void setupDiskCacheGetWait(BufferedDiskCache bufferedDiskCache) {
    mTaskCompletionSource = Task.create();
    when(bufferedDiskCache.get(mCacheKey, mIsCancelled))
        .thenReturn(mTaskCompletionSource.getTask());
  }

  private void setupDiskCacheGetSuccess(BufferedDiskCache bufferedDiskCache) {
    when(bufferedDiskCache.get(mCacheKey, mIsCancelled))
        .thenReturn(Task.forResult(mEncodedImage));
  }

  private void setupDiskCacheGetNotFound(BufferedDiskCache bufferedDiskCache) {
    when(bufferedDiskCache.get(mCacheKey, mIsCancelled))
        .thenReturn(Task.<EncodedImage>forResult(null));
  }

  private void setupDiskCacheGetFailure(BufferedDiskCache bufferedDiskCache) {
    when(bufferedDiskCache.get(mCacheKey, mIsCancelled))
        .thenReturn(Task.<EncodedImage>forError(mException));
  }
}
