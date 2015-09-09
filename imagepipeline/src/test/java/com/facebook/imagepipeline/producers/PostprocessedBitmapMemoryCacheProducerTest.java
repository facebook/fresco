/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;

import com.android.internal.util.Predicate;
import com.facebook.common.internal.ImmutableMap;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Checks basic properties of post-processed bitmap memory cache producer operation, that is:
 *   - it delegates to the {@link MemoryCache#get(Object)}
 *   - if {@link MemoryCache#get(Object)} is unsuccessful, then it passes the
 *   request to the next producer in the sequence.
 *   - if the next producer returns the value, then it is put into the bitmap cache.
 *   - responses from the next producer are passed back down to the consumer.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class PostprocessedBitmapMemoryCacheProducerTest {
  private static final String PRODUCER_NAME = PostprocessedBitmapMemoryCacheProducer.PRODUCER_NAME;
  @Mock public MemoryCache<CacheKey, CloseableImage> mMemoryCache;
  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public ImageRequest mImageRequest;
  @Mock public Postprocessor mPostprocessor;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  private CacheKey mCacheKey;
  private CloseableImage mImage1;
  private CloseableImage mImage2;
  private CloseableReference<CloseableImage> mFinalImageReference;
  private CloseableReference<CloseableImage> mIntermediateImageReference;
  private CloseableReference<CloseableImage> mFinalImageReferenceClone;
  private PostprocessedBitmapMemoryCacheProducer mMemoryCacheProducer;
  private final String mRequestId = "mRequestId";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mMemoryCacheProducer =
        new PostprocessedBitmapMemoryCacheProducer(mMemoryCache, mCacheKeyFactory, mInputProducer);
    mCacheKey = new SimpleCacheKey("http://dummy.uri");
    mImage1 = mock(CloseableImage.class);
    mImage2 = mock(CloseableImage.class);
    mFinalImageReference = CloseableReference.of(mImage1);
    mIntermediateImageReference = CloseableReference.of(mImage2);
    mFinalImageReferenceClone = mFinalImageReference.clone();
    mPostprocessor = mock(Postprocessor.class);

    when(mMemoryCache.cache(mCacheKey, mFinalImageReference)).thenReturn(mFinalImageReferenceClone);
    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);
    when(mProducerContext.getListener()).thenReturn(mProducerListener);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(mProducerContext.getId()).thenReturn(mRequestId);
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);
    when(mCacheKeyFactory.getPostprocessedBitmapCacheKey(mImageRequest)).thenReturn(mCacheKey);
  }

  @Test
  public void testPostprocessedBitmapMemoryCacheGetSuccessful() {
    setupGetSuccess();
    when(mPostprocessor.getPostprocessorCacheKey()).thenReturn(mCacheKey);
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE);
    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalImageReference, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap = ImmutableMap.of(
        PostprocessedBitmapMemoryCacheProducer.VALUE_FOUND,
        "true");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    Assert.assertFalse(mFinalImageReference.isValid());
  }

  @Test
  public void testGetNotFoundInputProducerSuccess() {
    setupGetNotFound();
    setupInputProducerStreamingSuccess();
    when(mPostprocessor.getPostprocessorCacheKey()).thenReturn(mCacheKey);
    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mConsumer).onNewResult(mFinalImageReferenceClone, true);
    Map<String, String> extraMap = ImmutableMap.of(
        PostprocessedBitmapMemoryCacheProducer.VALUE_FOUND,
        "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testGetNotFoundInputProducerNotFound() {
    setupGetNotFound();
    setupInputProducerNotFound();
    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap = ImmutableMap.of(
        PostprocessedBitmapMemoryCacheProducer.VALUE_FOUND,
        "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testGetNotFoundInputProducerFailure() {
    setupGetNotFound();
    setupInputProducerFailure();
    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap = ImmutableMap.of(
        PostprocessedBitmapMemoryCacheProducer.VALUE_FOUND,
        "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testFindNothingIfUsingBitmapCacheKey() {
    setupInputProducerNotFound();
    CacheKey bitmapCacheKey = mock(CacheKey.class);
    when(mCacheKeyFactory.getBitmapCacheKey(mImageRequest)).thenReturn(bitmapCacheKey);
    when(mMemoryCache.get(bitmapCacheKey)).thenReturn(mFinalImageReference);
    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, true);
  }

  @Test
  public void testCachedPostprocessorConsumerWithNullResult() {
    // We invoke the produceResults with no cached value
    PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer cachedConsumer =
        new PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer(mConsumer,
            mCacheKey,
            false,
            null,
            mMemoryCache);
    cachedConsumer.onNewResultImpl(null, true);
    verify(mConsumer).onNewResult(null, true);
    verify(mMemoryCache, never()).removeAll(any(Predicate.class));
    verify(mMemoryCache, never()).cache(mCacheKey, mFinalImageReference);
    verify(mConsumer, never()).onProgressUpdate(1f);
    verify(mConsumer, never()).onNewResult(mFinalImageReference, true);

  }


  @Test
  public void testCachedPostprocessorConsumerWithNullKey() {
    // We invoke the produceResults with no cached value
    PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer cachedConsumer =
        new PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer(mConsumer,
            null,
            false,
            null,
            mMemoryCache);
    cachedConsumer.onNewResultImpl(mFinalImageReference, true);
    verify(mConsumer, never()).onNewResult(null, true);
    verify(mMemoryCache, never()).removeAll(any(Predicate.class));
    verify(mMemoryCache, never()).cache(mCacheKey, mFinalImageReference);
    verify(mConsumer).onProgressUpdate(1f);
    verify(mConsumer).onNewResult(mFinalImageReference, true);
  }

  @Test
  public void testCachedPostprocessorConsumerIsNotRepeatedNotLastPostprocessor() {
    // We invoke the produceResults with no cached value
    PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer cachedConsumer =
        new PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer(mConsumer,
            mCacheKey,
            false,
            null,
            mMemoryCache);
    cachedConsumer.onNewResultImpl(mFinalImageReference, false);
    verify(mConsumer, never()).onNewResult(null, true);
    verify(mMemoryCache, never()).removeAll(any(Predicate.class));
    verify(mMemoryCache, never()).cache(mCacheKey, mFinalImageReference);
    verify(mConsumer, never()).onProgressUpdate(1f);
    verify(mConsumer, never()).onNewResult(mFinalImageReferenceClone, true);
  }

  @Test
  public void testCachedPostprocessorWithAllDataNotRepeated() {
    when(mPostprocessor.getPostprocessorCacheKey()).thenReturn(mCacheKey);
    // We invoke the produceResults with no cached value
    PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer cachedConsumer =
        new PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer(mConsumer,
            mCacheKey,
            false,
            "POSTPROCESSOR_NAME",
            mMemoryCache);
    cachedConsumer.onNewResultImpl(mFinalImageReference, true);
    verify(mConsumer, never()).onNewResult(null, true);
    verify(mMemoryCache).removeAll(any(Predicate.class));
    verify(mMemoryCache).cache(mCacheKey, mFinalImageReference);
    verify(mConsumer).onProgressUpdate(1f);
    verify(mConsumer).onNewResult(mFinalImageReferenceClone, true);
  }

  @Test
  public void testCachedPostprocessorWithAllDataRepeated() {
    when(mPostprocessor.getPostprocessorCacheKey()).thenReturn(mCacheKey);
    // We invoke the produceResults with no cached value
    PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer cachedConsumer =
        new PostprocessedBitmapMemoryCacheProducer.CachedPostprocessorConsumer(mConsumer,
            mCacheKey,
            true,
            "POSTPROCESSOR_NAME",
            mMemoryCache);
    cachedConsumer.onNewResultImpl(mFinalImageReference, true);
    verify(mConsumer, never()).onNewResult(null, true);
    verify(mMemoryCache).removeAll(any(Predicate.class));
    verify(mMemoryCache).cache(mCacheKey, mFinalImageReference);
    verify(mConsumer).onProgressUpdate(1f);
    verify(mConsumer).onNewResult(mFinalImageReferenceClone, true);
  }

  @Test
  public void testDoNothingIfImageNotPostprocessed() {
    setupGetSuccess();
    when(mImageRequest.getPostprocessor()).thenReturn(null);
    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verifyNoMoreInteractions(mCacheKeyFactory, mMemoryCache);
  }

  private void setupGetSuccess() {
    when(mMemoryCache.get(mCacheKey)).thenReturn(mFinalImageReference);
  }

  private void setupGetNotFound() {
    when(mMemoryCache.get(mCacheKey)).thenReturn(null);
  }

  private void setupInputProducerStreamingSuccess() {
    doAnswer(new ProduceResultsNewResultAnswer(
            Arrays.asList(mIntermediateImageReference, mFinalImageReference)))
        .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerNotFound() {
    final List<CloseableReference<CloseableImage>> listWithNull = new ArrayList<>(1);
    listWithNull.add(null);
    doAnswer(
        new ProduceResultsNewResultAnswer(listWithNull))
            .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerFailure() {
    doAnswer(new ProduceResultsFailureAnswer()).
        when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private static class ProduceResultsNewResultAnswer implements Answer<Void> {
    private final List<CloseableReference<CloseableImage>> mResults;

    private ProduceResultsNewResultAnswer(List<CloseableReference<CloseableImage>> results) {
      mResults = results;
    }

    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      Consumer consumer = (Consumer) invocation.getArguments()[0];
      Iterator<CloseableReference<CloseableImage>> iterator = mResults.iterator();
      while (iterator.hasNext()) {
        CloseableReference<CloseableImage> result = iterator.next();
        consumer.onNewResult(result, !iterator.hasNext());
      }
      return null;
    }
  }

  private class ProduceResultsFailureAnswer implements Answer<Void> {
    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      Consumer consumer = (Consumer) invocation.getArguments()[0];
      consumer.onFailure(mException);
      return null;
    }
  }
}
