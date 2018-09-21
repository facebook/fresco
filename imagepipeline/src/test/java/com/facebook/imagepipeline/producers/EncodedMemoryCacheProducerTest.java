/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

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
  @Mock public Object mCallerContext;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  private MultiCacheKey mCacheKey;
  private PooledByteBuffer mPooledByteBuffer1;
  private PooledByteBuffer mPooledByteBuffer2;
  private CloseableReference<PooledByteBuffer> mFinalImageReference;
  private CloseableReference<PooledByteBuffer> mIntermediateImageReference;
  private CloseableReference<PooledByteBuffer> mFinalImageReferenceClone;
  private EncodedImage mFinalEncodedImage;
  private EncodedImage mFinalEncodedImageFormatUnknown;
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
    mFinalEncodedImage.setImageFormat(new ImageFormat("jpeg", null));
    mFinalEncodedImage.setWidth(100);
    mFinalEncodedImage.setHeight(100);

    mFinalEncodedImageFormatUnknown = new EncodedImage(mFinalImageReference);
    mIntermediateEncodedImage = new EncodedImage(mIntermediateImageReference);
    mFinalEncodedImageClone = new EncodedImage(mFinalImageReferenceClone);
    List<CacheKey> list = new ArrayList<>();
    list.add(new SimpleCacheKey("http://dummy.uri"));
    mCacheKey = new MultiCacheKey(list);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, mCallerContext)).thenReturn(mCacheKey);

    when(mMemoryCache.cache(mCacheKey, mFinalImageReference)).thenReturn(mFinalImageReferenceClone);
    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);
    when(mProducerContext.getCallerContext()).thenReturn(mCallerContext);
    when(mProducerContext.getListener()).thenReturn(mProducerListener);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(mProducerContext.getId()).thenReturn(mRequestId);
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);

    when(mImageRequest.isMemoryCacheEnabled()).thenReturn(true);
  }

  @Test
  public void testDisableMemoryCache() {
    setupEncodedMemoryCacheGetNotFound();
    setupInputProducerStreamingSuccess();
    when(mImageRequest.isMemoryCacheEnabled()).thenReturn(false);
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mMemoryCache, never()).cache(any(CacheKey.class), any(CloseableReference.class));
  }

  @Test
  public void testEncodedMemoryCacheGetSuccessful() {
    setupEncodedMemoryCacheGetSuccess();
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE);
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(Consumer.IS_LAST));
    EncodedImage encodedImage = argumentCaptor.getValue();
    Assert.assertSame(
        mFinalEncodedImage.getUnderlyingReferenceTestOnly(),
        encodedImage.getUnderlyingReferenceTestOnly());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(EncodedMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "true");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, true);
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
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    Assert.assertTrue(EncodedImage.isValid(mFinalEncodedImageClone));
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(EncodedMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundInputProducerSuccessButResultNotCacheable() {
    testInputProducerSuccessButResultNotCacheableDueToStatusFlags(Consumer.DO_NOT_CACHE_ENCODED);
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundInputProducerSuccessButResultIsPartial() {
    testInputProducerSuccessButResultNotCacheableDueToStatusFlags(Consumer.IS_PARTIAL_RESULT);
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundInputProducerSuccessButResultIsUnknownFormat() {
    setupEncodedMemoryCacheGetNotFound();
    setupInputProducerStreamingSuccessFormatUnknown();
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mMemoryCache, never()).cache(any(CacheKey.class), any(CloseableReference.class));
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer).onNewResult(mFinalEncodedImageFormatUnknown, Consumer.IS_LAST);
    Assert.assertTrue(EncodedImage.isValid(mFinalEncodedImageClone));
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(EncodedMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(anyString(), anyString(), anyBoolean());
  }

  private void testInputProducerSuccessButResultNotCacheableDueToStatusFlags(
      final @Consumer.Status int statusFlags) {
    setupInputProducerStreamingSuccessWithStatusFlags(statusFlags, mFinalEncodedImageFormatUnknown);
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);

    verify(mMemoryCache, never()).cache(any(CacheKey.class), any(CloseableReference.class));
    verify(mConsumer)
        .onNewResult(mIntermediateEncodedImage, statusFlags);
    verify(mConsumer).onNewResult(mFinalEncodedImageFormatUnknown, Consumer.IS_LAST | statusFlags);
    Assert.assertTrue(EncodedImage.isValid(mFinalEncodedImageClone));
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(EncodedMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundInputProducerNotFound() {
    setupEncodedMemoryCacheGetNotFound();
    setupInputProducerNotFound();
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(EncodedMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundInputProducerFailure() {
    setupEncodedMemoryCacheGetNotFound();
    setupInputProducerFailure();
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(EncodedMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testEncodedMemoryCacheGetNotFoundLowestLevelReached() {
    setupEncodedMemoryCacheGetNotFound();
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE);
    mEncodedMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(EncodedMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, false);
    verifyNoMoreInteractions(mInputProducer);
  }

  private void setupEncodedMemoryCacheGetSuccess() {
    when(mMemoryCache.get(eq(mCacheKey))).thenReturn(mFinalImageReference);
  }

  private void setupEncodedMemoryCacheGetNotFound() {
    when(mMemoryCache.get(eq(mCacheKey))).thenReturn(null);
  }

  private void setupInputProducerStreamingSuccessFormatUnknown() {
    setupInputProducerStreamingSuccessWithStatusFlags(0, mFinalEncodedImageFormatUnknown);
  }

  private void setupInputProducerStreamingSuccess() {
    setupInputProducerStreamingSuccessWithStatusFlags(0, mFinalEncodedImage);
  }

  private void setupInputProducerStreamingSuccessWithStatusFlags(
      @Consumer.Status int statusFlags, EncodedImage finalEncodedImage) {
    doAnswer(
            new ProduceResultsNewResultAnswer(
                Arrays.asList(mIntermediateEncodedImage, finalEncodedImage), statusFlags))
        .when(mInputProducer)
        .produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerNotFound() {
    final List<EncodedImage> nullArray = new ArrayList<EncodedImage>(1);
    nullArray.add(null);
    doAnswer(new ProduceResultsNewResultAnswer(nullArray, 0))
        .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerFailure() {
    doAnswer(new ProduceResultsFailureAnswer()).
        when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private static class ProduceResultsNewResultAnswer implements Answer<Void> {
    private final List<EncodedImage> mResults;
    private final @Consumer.Status int mExtraStatusFlags;

    private ProduceResultsNewResultAnswer(List<EncodedImage> results, int extraStatusFlags) {
      mResults = results;
      mExtraStatusFlags = extraStatusFlags;
    }

    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      Consumer consumer = (Consumer) invocation.getArguments()[0];
      Iterator<EncodedImage> iterator = mResults.iterator();
      while (iterator.hasNext()) {
        EncodedImage result = iterator.next();
        consumer.onNewResult(
            result,
            BaseConsumer.simpleStatusForIsLast(!iterator.hasNext()) | mExtraStatusFlags);
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
