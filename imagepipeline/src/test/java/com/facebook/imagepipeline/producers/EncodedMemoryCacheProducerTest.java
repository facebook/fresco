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
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Checks basic properties of encoded memory cache producer operation, that is:
 *   - it delegates to the {@link MemoryCache#get(Object)}
 *   - if {@link MemoryCache#get(Object)} is unsuccessful, then it passes the
 *   request to the next producer in the sequence.
 *   - if the next producer returns the value, then it is put into the disk cache.
 *   - responses from the next producer are passed back down to the consumer.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class EncodedMemoryCacheProducerTest {
  private static final String PRODUCER_NAME = EncodedMemoryCacheProducer.PRODUCER_NAME;
  @Mock public MemoryCache<CacheKey, PooledByteBuffer> mMemoryCache;
  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  private MultiCacheKey mCacheKey;
  private PooledByteBuffer mPooledByteBuffer1;
  private PooledByteBuffer mPooledByteBuffer2;
  private CloseableReference<PooledByteBuffer> mFinalImageReference;
  private CloseableReference<PooledByteBuffer> mIntermediateImageReference;
  private CloseableReference<PooledByteBuffer> mFinalImageReferenceClone;
  private EncodedImage mFinalEncodedImage;
  private EncodedImage mIntermediateEncodedImage;
  private EncodedImage mFinalEncodedImageClone;
  private EncodedMemoryCacheProducer mEncodedMemoryCacheProducer;
  private final String mRequestId = "mRequestId";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mEncodedMemoryCacheProducer =
        new EncodedMemoryCacheProducer(mMemoryCache, mCacheKeyFactory, mInputProducer);
    mPooledByteBuffer1 = mock(PooledByteBuffer.class);
    mPooledByteBuffer2 = mock(PooledByteBuffer.class);
    mFinalImageReference = CloseableReference.of(mPooledByteBuffer1);
    mIntermediateImageReference = CloseableReference.of(mPooledByteBuffer2);
    mFinalImageReferenceClone = mFinalImageReference.clone();
    mFinalEncodedImage = new EncodedImage(mFinalImageReference);
    mIntermediateEncodedImage = new EncodedImage(mIntermediateImageReference);
    mFinalEncodedImageClone = new EncodedImage(mFinalImageReferenceClone);
    List<CacheKey> list = new ArrayList<>();
    list.add(new SimpleCacheKey("http://dummy.uri"));
    mCacheKey = new MultiCacheKey(list);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest)).thenReturn(mCacheKey);

    when(mMemoryCache.cache(mCacheKey, mFinalImageReference)).thenReturn(mFinalImageReferenceClone);
    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);
    when(mProducerContext.getListener()).thenReturn(mProducerListener);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(mProducerContext.getId()).thenReturn(mRequestId);
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);

  }

  @Test
  public void testEncodedMemoryCacheGetSuccessful() {
    setupEncodedMemoryCacheGetSuccess();
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE);
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(true));
    EncodedImage encodedImage = argumentCaptor.getValue();
    Assert.assertSame(
        mFinalEncodedImage.getUnderlyingReferenceTestOnly(),
        encodedImage.getUnderlyingReferenceTestOnly());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap = ImmutableMap.of(EncodedMemoryCacheProducer.VALUE_FOUND, "true");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    Assert.assertFalse(mFinalImageReference.isValid());
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundInputProducerSuccess() {
    setupEncodedMemoryCacheGetNotFound();
    setupInputProducerStreamingSuccess();
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mMemoryCache, never()).cache(mCacheKey, mIntermediateImageReference);
    ArgumentCaptor<CloseableReference> argumentCaptor =
        ArgumentCaptor.forClass(CloseableReference.class);
    verify(mMemoryCache).cache(eq(mCacheKey), argumentCaptor.capture());
    CloseableReference<PooledByteBuffer> capturedRef =
        (CloseableReference<PooledByteBuffer>) argumentCaptor.getValue();
    Assert.assertSame(
        mFinalImageReference.getUnderlyingReferenceTestOnly(),
        capturedRef.getUnderlyingReferenceTestOnly());
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, false);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    Assert.assertTrue(EncodedImage.isValid(mFinalEncodedImageClone));
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap = ImmutableMap.of(EncodedMemoryCacheProducer.VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundInputProducerNotFound() {
    setupEncodedMemoryCacheGetNotFound();
    setupInputProducerNotFound();
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap = ImmutableMap.of(EncodedMemoryCacheProducer.VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundInputProducerFailure() {
    setupEncodedMemoryCacheGetNotFound();
    setupInputProducerFailure();
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap = ImmutableMap.of(EncodedMemoryCacheProducer.VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundLowestLevelReached() {
    setupEncodedMemoryCacheGetNotFound();
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE);
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap = ImmutableMap.of(EncodedMemoryCacheProducer.VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verifyNoMoreInteractions(mInputProducer);
  }

  private void setupEncodedMemoryCacheGetSuccess() {
    when(mMemoryCache.get(eq(mCacheKey))).thenReturn(mFinalImageReference);
  }

  private void setupEncodedMemoryCacheGetNotFound() {
    when(mMemoryCache.get(eq(mCacheKey))).thenReturn(null);
  }

  private void setupInputProducerStreamingSuccess() {
    doAnswer(new ProduceResultsNewResultAnswer(
            Arrays.asList(mIntermediateEncodedImage, mFinalEncodedImage)))
        .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerNotFound() {
    final List<EncodedImage> nullArray = new ArrayList<EncodedImage>(1);
    nullArray.add(null);
    doAnswer(new ProduceResultsNewResultAnswer(nullArray))
        .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerFailure() {
    doAnswer(new ProduceResultsFailureAnswer()).
        when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private static class ProduceResultsNewResultAnswer implements Answer<Void> {
    private final List<EncodedImage> mResults;

    private ProduceResultsNewResultAnswer(List<EncodedImage> results) {
      mResults = results;
    }

    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      Consumer consumer = (Consumer) invocation.getArguments()[0];
      Iterator<EncodedImage> iterator = mResults.iterator();
      while (iterator.hasNext()) {
        EncodedImage result = iterator.next();
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
