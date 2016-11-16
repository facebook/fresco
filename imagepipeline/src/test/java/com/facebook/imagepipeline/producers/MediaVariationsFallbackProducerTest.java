/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.List;
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
import com.facebook.imagepipeline.producers.MediaVariationsFallbackProducer.MediaVariationsConsumer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.MediaVariations;

import bolts.Task;
import org.fest.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
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

  private static final int SIZE_S = 100;
  private static final int SIZE_M = 200;
  private static final int SIZE_L = 300;
  public static final String MEDIA_ID = "variations";

  private static final Uri URI_S = Uri.parse("http://www.facebook.com/s.png");
  private static final Uri URI_M = Uri.parse("http://www.facebook.com/m.png");
  private static final Uri URI_L = Uri.parse("http://www.facebook.com/l.png");

  private static final CacheKey CACHE_KEY_S = new SimpleCacheKey("http://fb.com/s.png");
  private static final CacheKey CACHE_KEY_M = new SimpleCacheKey("http://fb.com/m.png");
  private static final CacheKey CACHE_KEY_L = new SimpleCacheKey("http://fb.com/l.png");
  private static final CacheKey CACHE_KEY_ORIGINAL = new SimpleCacheKey("http://fb.com/orig.png");

  private static final Map<String, String> EXPECTED_MAP_ON_CACHE_HIT_FOR_LAST_IMAGE =
      ImmutableMap.of(
          MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_FOUND,
          "true",
          MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_USED_AS_LAST,
          "true");
  private static final Map<String, String> EXPECTED_MAP_ON_CACHE_HIT_FOR_INTERMEDIATE_IMAGE =
      ImmutableMap.of(
          MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_FOUND,
          "true",
          MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_USED_AS_LAST,
          "false");
  private static final Map<String, String> EXPECTED_MAP_ON_CACHE_MISS =
      ImmutableMap.of(MediaVariationsFallbackProducer.EXTRA_CACHED_VALUE_FOUND, "false");

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
  @Mock public MediaVariationsIndex mMediaVariationsIndex;
  @Mock public EncodedImage mIntermediateEncodedImage;
  @Mock public EncodedImage mFinalEncodedImage;
  @Captor public ArgumentCaptor<Consumer<EncodedImage>> mConsumerCaptor;
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
        mMediaVariationsIndex,
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

    mMediaVariationsWithVariants = MediaVariations.newBuilderForMediaId(MEDIA_ID)
        .addVariant(URI_S, SIZE_S, SIZE_S)
        .addVariant(URI_M, SIZE_M, SIZE_M)
        .addVariant(URI_L, SIZE_L, SIZE_L)
        .build();
    mEmptyMediaVariations = MediaVariations.newBuilderForMediaId(MEDIA_ID).build();

    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);
    when(mImageRequest.getResizeOptions()).thenReturn(new ResizeOptions(SIZE_M, SIZE_M));
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(true);

    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);

    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, mCallerContext))
        .thenReturn(CACHE_KEY_ORIGINAL);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, URI_S, mCallerContext))
        .thenReturn(CACHE_KEY_S);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, URI_M, mCallerContext))
        .thenReturn(CACHE_KEY_M);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, URI_L, mCallerContext))
        .thenReturn(CACHE_KEY_L);

    whenIndexDbReturnsTaskForResult(null);
  }

  @Test
  public void testStartInputProducerIfDiskCacheNotEnabled() {
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(false);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verifyInputProducerProduceResultsWithNewConsumer();
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

    verifyInputProducerProduceResultsWithNewConsumer();
    verifyNoMoreInteractions(
        mConsumer,
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testStartInputProducerIfMediaVariationsAndDatabaseContainNoVariants() {
    when(mImageRequest.getMediaVariations()).thenReturn(mEmptyMediaVariations);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verifyInputProducerProduceResultsWithNewConsumer();
    verifyNoMoreInteractions(
        mConsumer,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testStartInputProducerIfNullResizeOptions() {
    when(mImageRequest.getMediaVariations()).thenReturn(mMediaVariationsWithVariants);
    when(mImageRequest.getResizeOptions()).thenReturn(null);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verifyInputProducerProduceResultsWithNewConsumer();
    verifyNoMoreInteractions(
        mConsumer,
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testStartsInputProducerIfNoCachedVariantFoundFromRequest() {
    when(mImageRequest.getMediaVariations()).thenReturn(mMediaVariationsWithVariants);
    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verifyInputProducerProduceResultsWithNewConsumer();
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_MISS);
    verifyZeroInteractions(mConsumer, mSmallImageBufferedDiskCache);
  }

  @Test
  public void testSendsNonFinalImageToConsumerAndStartsInputProducerIfNoCachedVariantFromRequestBigEnough() {
    when(mImageRequest.getMediaVariations()).thenReturn(mMediaVariationsWithVariants);
    whenCacheContains(mDefaultBufferedDiskCache, CACHE_KEY_S);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mImageS, false);
    verify(mConsumer, never()).onProgressUpdate(anyFloat());
    verifyInputProducerProduceResultsWithNewConsumer();
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_HIT_FOR_INTERMEDIATE_IMAGE);
    verifyZeroInteractions(mSmallImageBufferedDiskCache);
  }

  @Test
  public void testSendsNonFinalImageToConsumerAndStartsInputProducerIfNoCachedVariantFromIndexBigEnough() {
    when(mImageRequest.getMediaVariations()).thenReturn(mEmptyMediaVariations);
    whenCacheContains(mDefaultBufferedDiskCache, CACHE_KEY_S);
    whenIndexDbContains(URI_S, SIZE_S);
    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mImageS, false);
    verify(mConsumer, never()).onProgressUpdate(anyFloat());
    verifyInputProducerProduceResultsWithNewConsumer();
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_HIT_FOR_INTERMEDIATE_IMAGE);
    verifyZeroInteractions(mSmallImageBufferedDiskCache);
  }

  @Test
  public void testSendsLargestCachedNonFinalImageToConsumerAndStartsInputProducerIfNoCachedVariantFromRequestBigEnough() {
    when(mImageRequest.getMediaVariations()).thenReturn(mMediaVariationsWithVariants);
    whenCacheContains(mDefaultBufferedDiskCache, CACHE_KEY_S, CACHE_KEY_M, CACHE_KEY_L);
    when(mImageRequest.getResizeOptions()).thenReturn(new ResizeOptions(SIZE_L + 80, SIZE_L + 80));

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mImageL, false);
    verify(mConsumer, never()).onProgressUpdate(anyFloat());
    verifyInputProducerProduceResultsWithNewConsumer();
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_HIT_FOR_INTERMEDIATE_IMAGE);
    verifyZeroInteractions(mSmallImageBufferedDiskCache);
  }

  @Test
  public void testSendsSmallestLargerFinalImageToConsumerWhenLargerVariantsFound() {
    when(mImageRequest.getMediaVariations()).thenReturn(mMediaVariationsWithVariants);
    whenCacheContains(mDefaultBufferedDiskCache, CACHE_KEY_S, CACHE_KEY_M, CACHE_KEY_L);
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
    when(mImageRequest.getMediaVariations()).thenReturn(mMediaVariationsWithVariants);
    whenCacheContains(mSmallImageBufferedDiskCache, CACHE_KEY_S, CACHE_KEY_M, CACHE_KEY_L);
    when(mImageRequest.getResizeOptions()).thenReturn(new ResizeOptions(SIZE_S, SIZE_S));
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mImageS, true);
    verify(mConsumer).onProgressUpdate(1L);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verifySuccessSentToListener(EXPECTED_MAP_ON_CACHE_HIT_FOR_LAST_IMAGE);
    verifyZeroInteractions(mInputProducer, mDefaultBufferedDiskCache);
  }

  @Test
  public void testInputProducerSuccess() {
    when(mImageRequest.getMediaVariations()).thenReturn(mEmptyMediaVariations);
    setupInputProducerSuccess();

    mMediaVariationsFallbackProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mIntermediateEncodedImage, false);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);

    verify(mMediaVariationsIndex)
        .saveCachedVariant(MEDIA_ID, CACHE_KEY_ORIGINAL, mFinalEncodedImage);
  }

  private void whenIndexDbContains(Uri uri, int size) {
    whenIndexDbReturnsTaskForResult(Lists
        .newArrayList(new MediaVariations.Variant(uri, size, size)));
  }

  private void whenIndexDbReturnsTaskForResult(List<MediaVariations.Variant> variants) {
    Task<List<MediaVariations.Variant>> task = Task.forResult(variants);
    when(mMediaVariationsIndex.getCachedVariants(MEDIA_ID)).thenReturn(task);
  }

  private void whenCacheContains(BufferedDiskCache cache, CacheKey... cacheKeys) {
    for (int i = 0; i < cacheKeys.length; i++) {
      when(cache.containsSync(cacheKeys[i])).thenReturn(true);
      when(cache.contains(cacheKeys[i])).thenReturn(Task.forResult(Boolean.TRUE));
      when(cache.get(eq(cacheKeys[i]), any(AtomicBoolean.class)))
          .thenReturn(Task.forResult(imageForCacheKey(cacheKeys[i])));
    }
  }

  private EncodedImage imageForCacheKey(CacheKey cacheKey) {
    if (cacheKey == CACHE_KEY_S) {
      return mImageS;
    } else if (cacheKey == CACHE_KEY_M) {
      return mImageM;
    } else if (cacheKey == CACHE_KEY_L) {
      return mImageL;
    } else {
      throw new IllegalArgumentException("Cache key must be one of the test class constants");
    }
  }

  private void setupInputProducerSuccess() {
    doAnswer(
        new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            Consumer consumer = (Consumer) invocation.getArguments()[0];
            consumer.onNewResult(mIntermediateEncodedImage, false);
            consumer.onNewResult(mFinalEncodedImage, true);
            return null;
          }
        }).when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void verifyInputProducerProduceResultsWithNewConsumer() {
    verify(mInputProducer).produceResults(mConsumerCaptor.capture(), eq(mProducerContext));

    Consumer<EncodedImage> consumer = mConsumerCaptor.getValue();
    assertThat(consumer).isInstanceOf(MediaVariationsConsumer.class);
    assertThat(((MediaVariationsConsumer) consumer).getConsumer()).isSameAs(mConsumer);
  }

  private void verifySuccessSentToListener(Map<String, String> extrasMap) {
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extrasMap);
    verify(mProducerListener, never())
        .onProducerFinishWithCancellation(anyString(), anyString(), anyMap());
    verify(mProducerListener, never())
        .onProducerFinishWithFailure(anyString(), anyString(), any(Throwable.class), anyMap());
  }
}
