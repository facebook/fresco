/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import android.net.Uri;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.MediaVariations;

import bolts.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class MediaVariationsFallbackProducerTest {

  public static final String PRODUCER_NAME = "MediaVariationsFallbackProducer";

  private static final Uri URI_S = Uri.parse("http://www.facebook.com/s.png");
  private static final Uri URI_M = Uri.parse("http://www.facebook.com/m.png");
  private static final Uri URI_L = Uri.parse("http://www.facebook.com/l.png");

  private static final CacheKey CACHE_KEY_S = new SimpleCacheKey("http://fb.com/s.png");
  private static final CacheKey CACHE_KEY_M = new SimpleCacheKey("http://fb.com/m.png");
  private static final CacheKey CACHE_KEY_L = new SimpleCacheKey("http://fb.com/l.png");

  private static final Map EXPECTED_MAP_ON_CACHE_HIT_FOR_LAST_IMAGE = ImmutableMap.of(
      MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_FOUND,
      "true",
      MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_USED_AS_LAST,
      "true");
  private static final Map EXPECTED_MAP_ON_CACHE_HIT_FOR_INTERMEDIATE_IMAGE = ImmutableMap.of(
      MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_FOUND,
      "true",
      MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_USED_AS_LAST,
      "false");
  private static final Map EXPECTED_MAP_ON_CACHE_MISS =
      ImmutableMap.of(MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_FOUND, "false");

  private static final int SIZE_S = 100;
  private static final int SIZE_M = 200;
  private static final int SIZE_L = 300;

  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public Object mCallerContext;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  @Mock public EncodedImage mImageS;
  @Mock public EncodedImage mImageM;
  @Mock public EncodedImage mImageL;
  @Mock public EncodedImage mImageXL;
  private SettableProducerContext mProducerContext;
  private final String mRequestId = "mRequestId";
  private final BufferedDiskCache mDefaultBufferedDiskCache = mock(BufferedDiskCache.class);
  private final BufferedDiskCache mSmallImageBufferedDiskCache = mock(BufferedDiskCache.class);
  private MediaVariations mMediaVariationsWithVariants;
  private MediaVariations mEmptyMediaVariations;

  private MediaVariationsFallbackProducer mMediaVariationsFallbackProducer;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mMediaVariationsFallbackProducer = new MediaVariationsFallbackProducer(
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        mInputProducer);

    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mCallerContext,
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);

    mMediaVariationsWithVariants = MediaVariations.newBuilderForMediaId("variations")
        .addVariant(URI_S, SIZE_S, SIZE_S)
        .addVariant(URI_M, SIZE_M, SIZE_M)
        .addVariant(URI_L, SIZE_L, SIZE_L)
        .build();
    mEmptyMediaVariations = MediaVariations.newBuilderForMediaId("no-variations").build();

    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);
    when(mImageRequest.getMediaVariations()).thenReturn(mMediaVariationsWithVariants);
    when(mImageRequest.getResizeOptions()).thenReturn(new ResizeOptions(SIZE_M, SIZE_M));
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(true);

    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);

    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, URI_S, mCallerContext))
        .thenReturn(CACHE_KEY_S);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, URI_M, mCallerContext))
        .thenReturn(CACHE_KEY_M);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, URI_L, mCallerContext))
        .thenReturn(CACHE_KEY_L);
  }

  @Test
  public void testStartInputProducerIfDiskCacheNotEnabled() {
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(false);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verifyNoMoreInteractions(
        mConsumer,
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testStartInputProducerIfNullMediaVariations() {
    when(mImageRequest.getMediaVariations()).thenReturn(null);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verifyNoMoreInteractions(
        mConsumer,
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testStartInputProducerIfMediaVariationsContainNoVariants() {
    when(mImageRequest.getMediaVariations()).thenReturn(mEmptyMediaVariations);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verifyNoMoreInteractions(
        mConsumer,
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testStartInputProducerIfNullResizeOptions() {
    when(mImageRequest.getResizeOptions()).thenReturn(null);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verifyNoMoreInteractions(
        mConsumer,
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testStartsInputProducerIfNoCachedVariantFound() {
    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_MISS);
    verifyZeroInteractions(mConsumer, mSmallImageBufferedDiskCache);
  }

  @Test
  public void testSendsNonFinalImageToConsumerAndStartsInputProducerIfNoCachedVariantBigEnough() {
    whenDefaultCacheContains(CACHE_KEY_S, mImageS);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mImageS, false);
    verify(mConsumer, never()).onProgressUpdate(anyFloat());
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_HIT_FOR_INTERMEDIATE_IMAGE);
    verifyZeroInteractions(mSmallImageBufferedDiskCache);
  }

  @Test
  public void testSendsLargestCachedNonFinalImageToConsumerAndStartsInputProducerIfNoCachedVariantBigEnough() {
    whenDefaultCacheContains(CACHE_KEY_S, mImageS);
    whenDefaultCacheContains(CACHE_KEY_M, mImageM);
    whenDefaultCacheContains(CACHE_KEY_L, mImageL);
    when(mImageRequest.getResizeOptions()).thenReturn(new ResizeOptions(SIZE_L + 80, SIZE_L + 80));

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mImageL, false);
    verify(mConsumer, never()).onProgressUpdate(anyFloat());
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_HIT_FOR_INTERMEDIATE_IMAGE);
    verifyZeroInteractions(mSmallImageBufferedDiskCache);
  }

  @Test
  public void testSendsSmallestLargerFinalImageToConsumerWhenLargerVariantsFound() {
    whenDefaultCacheContains(CACHE_KEY_S, mImageS);
    whenDefaultCacheContains(CACHE_KEY_M, mImageM);
    whenDefaultCacheContains(CACHE_KEY_L, mImageL);
    when(mImageRequest.getResizeOptions()).thenReturn(new ResizeOptions(SIZE_M - 10, SIZE_M - 10));

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mImageM, true);
    verify(mConsumer).onProgressUpdate(1L);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_HIT_FOR_LAST_IMAGE);
    verifyZeroInteractions(mInputProducer, mSmallImageBufferedDiskCache);
  }

  @Test
  public void testUsesSmallCacheIfRequestedByImageRequest() {
    whenSmallCacheContains(CACHE_KEY_S, mImageS);
    whenSmallCacheContains(CACHE_KEY_M, mImageM);
    whenSmallCacheContains(CACHE_KEY_L, mImageL);
    when(mImageRequest.getResizeOptions()).thenReturn(new ResizeOptions(SIZE_S, SIZE_S));
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mImageS, true);
    verify(mConsumer).onProgressUpdate(1L);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_HIT_FOR_LAST_IMAGE);
    verifyZeroInteractions(mInputProducer, mDefaultBufferedDiskCache);
  }

  private void whenDefaultCacheContains(CacheKey cacheKey, EncodedImage image) {
    when(mDefaultBufferedDiskCache.containsSync(cacheKey)).thenReturn(true);
    when(mDefaultBufferedDiskCache.contains(cacheKey)).thenReturn(Task.forResult(Boolean.TRUE));
    when(mDefaultBufferedDiskCache.get(eq(cacheKey), any(AtomicBoolean.class)))
        .thenReturn(Task.forResult(image));
  }

  private void whenSmallCacheContains(CacheKey cacheKey, EncodedImage image) {
    when(mSmallImageBufferedDiskCache.containsSync(cacheKey)).thenReturn(true);
    when(mSmallImageBufferedDiskCache.contains(cacheKey)).thenReturn(Task.forResult(Boolean.TRUE));
    when(mSmallImageBufferedDiskCache.get(eq(cacheKey), any(AtomicBoolean.class)))
        .thenReturn(Task.forResult(image));
  }

  private void verifySuccessSentToListener(Map extrasMap) {
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extrasMap);
    verify(mProducerListener, never())
        .onProducerFinishWithCancellation(anyString(), anyString(), anyMap());
    verify(mProducerListener, never())
        .onProducerFinishWithFailure(anyString(), anyString(), any(Throwable.class), anyMap());
  }
}
