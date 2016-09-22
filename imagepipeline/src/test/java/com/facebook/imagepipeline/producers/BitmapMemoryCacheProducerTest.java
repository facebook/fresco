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
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
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
 * Checks basic properties of bitmap memory cache producer operation, that is:
 *   - it delegates to the {@link MemoryCache#get(Object)}
 *   - if {@link MemoryCache#get(Object)} is unsuccessful, then it passes the
 *   request to the next producer in the sequence.
 *   - if the next producer returns the value of higher quality,
 *   then it is put into the bitmap cache.
 *   - responses from the next producer are passed back down to the consumer.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class BitmapMemoryCacheProducerTest {
  private static final String PRODUCER_NAME = BitmapMemoryCacheProducer.PRODUCER_NAME;
  private static final int INTERMEDIATE_SCAN_1 = 2;
  private static final int INTERMEDIATE_SCAN_2 = 5;
  @Mock public MemoryCache<CacheKey, CloseableImage> mMemoryCache;
  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  private final String mRequestId = "mRequestId";
  private BitmapMemoryCacheKey mBitmapMemoryCacheKey;
  private CloseableImage mCloseableImage1;
  private CloseableImage mCloseableImage2;
  private CloseableReference<CloseableImage> mFinalImageReference;
  private CloseableReference<CloseableImage> mIntermediateImageReference;
  private CloseableReference<CloseableImage> mFinalImageReferenceClone;
  private CloseableReference<CloseableImage> mIntermediateImageReferenceClone;
  private BitmapMemoryCacheProducer mBitmapMemoryCacheProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mBitmapMemoryCacheProducer =
        new BitmapMemoryCacheProducer(mMemoryCache, mCacheKeyFactory, mInputProducer);
    mBitmapMemoryCacheKey = mock(BitmapMemoryCacheKey.class);
    mCloseableImage1 = mock(CloseableImage.class);
    when(mCloseableImage1.getQualityInfo()).thenReturn(ImmutableQualityInfo.FULL_QUALITY);
    mCloseableImage2 = mock(CloseableImage.class);
    when(mCloseableImage2.getQualityInfo())
        .thenReturn(ImmutableQualityInfo.of(INTERMEDIATE_SCAN_2, true, false));
    mFinalImageReference = CloseableReference.of(mCloseableImage1);
    mIntermediateImageReference = CloseableReference.of(mCloseableImage2);
    mFinalImageReferenceClone = mFinalImageReference.clone();
    mIntermediateImageReferenceClone = mIntermediateImageReference.clone();

    when(mMemoryCache.cache(mBitmapMemoryCacheKey, mIntermediateImageReference))
        .thenReturn(mIntermediateImageReferenceClone);
    when(mMemoryCache.cache(mBitmapMemoryCacheKey, mFinalImageReference))
        .thenReturn(mFinalImageReferenceClone);
    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);
    when(mProducerContext.getListener()).thenReturn(mProducerListener);
    when(mProducerContext.getId()).thenReturn(mRequestId);
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    when(mProducerContext.getCallerContext()).thenReturn(PRODUCER_NAME);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(mCacheKeyFactory.getBitmapCacheKey(mImageRequest, PRODUCER_NAME))
        .thenReturn(mBitmapMemoryCacheKey);
  }

  @Test
  public void testBitmapMemoryCacheGetSuccessful() {
    setupBitmapMemoryCacheGetSuccess();
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalImageReference, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "true");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    Assert.assertTrue(!mFinalImageReference.isValid());
  }

  /**
   * Verify that stateful image results, both intermediate and final, are never cached.
   */
  @Test
  public void testDoNotCacheStatefulImage() {
    when(mCloseableImage1.isStateful()).thenReturn(true);
    when(mCloseableImage2.isStateful()).thenReturn(true);

    setupBitmapMemoryCacheGetNotFound();
    setupInputProducerStreamingSuccess();
    when(mMemoryCache.get(mBitmapMemoryCacheKey)).thenReturn(null);

    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mIntermediateImageReference, false);
    verify(mConsumer).onNewResult(mFinalImageReference, true);
    verify(mMemoryCache, never()).cache(
        any(BitmapMemoryCacheKey.class),
        any(CloseableReference.class));
  }

  @Test
  public void testBitmapMemoryCacheGetIntermediateImage() {
    setupBitmapMemoryCacheGetIntermediateImage();
    setupInputProducerNotFound();
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mIntermediateImageReference, false);
    verify(mConsumer).onNewResult(null, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    Assert.assertTrue(!mIntermediateImageReference.isValid());
  }

  @Test
  public void testCacheIntermediateImageAsNoImagePresent() {
    setupBitmapMemoryCacheGetNotFound();
    setupInputProducerStreamingSuccess();
    when(mMemoryCache.get(mBitmapMemoryCacheKey)).thenReturn(null);
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mMemoryCache).cache(mBitmapMemoryCacheKey, mIntermediateImageReference);
    verify(mMemoryCache).cache(mBitmapMemoryCacheKey, mFinalImageReference);
    verify(mConsumer).onNewResult(mIntermediateImageReferenceClone, false);
    verify(mConsumer).onNewResult(mFinalImageReferenceClone, true);
    Assert.assertTrue(!mIntermediateImageReferenceClone.isValid());
    Assert.assertTrue(!mFinalImageReferenceClone.isValid());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testCacheIntermediateImageAsBetterScan() {
    setupBitmapMemoryCacheGetNotFound();
    setupInputProducerStreamingSuccess();
    CloseableImage closeableImage = mock(CloseableImage.class);
    when(closeableImage.getQualityInfo())
        .thenReturn(ImmutableQualityInfo.of(INTERMEDIATE_SCAN_1, false, false));
    CloseableReference<CloseableImage> closeableImageRef = CloseableReference.of(closeableImage);
    when(mMemoryCache.get(mBitmapMemoryCacheKey))
        .thenReturn(null)
        .thenReturn(closeableImageRef);
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mMemoryCache).cache(mBitmapMemoryCacheKey, mIntermediateImageReference);
    verify(mMemoryCache).cache(mBitmapMemoryCacheKey, mFinalImageReference);
    verify(mConsumer).onNewResult(mIntermediateImageReferenceClone, false);
    verify(mConsumer).onNewResult(mFinalImageReferenceClone, true);
    Assert.assertTrue(!mIntermediateImageReferenceClone.isValid());
    Assert.assertTrue(!mFinalImageReferenceClone.isValid());
    Assert.assertEquals(
        0,
        closeableImageRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testDontCacheIntermediateImageAsAlreadyHaveSameQuality() {
    setupBitmapMemoryCacheGetNotFound();
    setupInputProducerStreamingSuccess();
    CloseableImage closeableImage = mock(CloseableImage.class);
    when(closeableImage.getQualityInfo())
        .thenReturn(ImmutableQualityInfo.of(INTERMEDIATE_SCAN_2, true, false));
    CloseableReference<CloseableImage> closeableImageRef = CloseableReference.of(closeableImage);
    when(mMemoryCache.get(mBitmapMemoryCacheKey))
        .thenReturn(null)
        .thenReturn(closeableImageRef);
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mMemoryCache, never()).cache(mBitmapMemoryCacheKey, mIntermediateImageReference);
    verify(mMemoryCache).cache(mBitmapMemoryCacheKey, mFinalImageReference);
    verify(mConsumer).onNewResult(closeableImageRef, false);
    verify(mConsumer).onNewResult(mFinalImageReferenceClone, true);
    Assert.assertTrue(!mFinalImageReferenceClone.isValid());
    Assert.assertEquals(
        0,
        closeableImageRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testDontCacheIntermediateImageAsAlreadyHaveFullQuality() {
    setupBitmapMemoryCacheGetNotFound();
    setupInputProducerStreamingSuccess();
    CloseableImage closeableImage = mock(CloseableImage.class);
    when(closeableImage.getQualityInfo()).thenReturn(ImmutableQualityInfo.FULL_QUALITY);
    CloseableReference<CloseableImage> closeableImageRef = CloseableReference.of(closeableImage);
    when(mMemoryCache.get(mBitmapMemoryCacheKey))
        .thenReturn(null)
        .thenReturn(closeableImageRef);
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mMemoryCache, never()).cache(mBitmapMemoryCacheKey, mIntermediateImageReference);
    verify(mMemoryCache).cache(mBitmapMemoryCacheKey, mFinalImageReference);
    verify(mConsumer).onNewResult(closeableImageRef, false);
    verify(mConsumer).onNewResult(mFinalImageReferenceClone, true);
    Assert.assertTrue(!mFinalImageReferenceClone.isValid());
    Assert.assertEquals(
        0,
        closeableImageRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testBitmapMemoryCacheGetNotFoundInputProducerNotFound() {
    setupBitmapMemoryCacheGetNotFound();
    setupInputProducerNotFound();
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  @Test
  public void testBitmapMemoryCacheGetNotFoundLowestLevelReached() {
    setupBitmapMemoryCacheGetNotFound();
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verifyNoMoreInteractions(mInputProducer);
  }

  @Test
  public void testBitmapMemoryCacheGetIntermediateImageLowestLevelReached() {
    setupBitmapMemoryCacheGetIntermediateImage();
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mIntermediateImageReference, false);
    verify(mConsumer).onNewResult(null, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    Assert.assertTrue(!mIntermediateImageReference.isValid());
    verifyNoMoreInteractions(mInputProducer);
  }

  @Test
  public void testBitmapMemoryCacheGetNotFoundInputProducerFailure() {
    setupBitmapMemoryCacheGetNotFound();
    setupInputProducerFailure();
    mBitmapMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
  }

  private void setupBitmapMemoryCacheGetSuccess() {
    when(mMemoryCache.get(eq(mBitmapMemoryCacheKey)))
        .thenReturn(mFinalImageReference);
  }

  private void setupBitmapMemoryCacheGetNotFound() {
    when(mMemoryCache.get(eq(mBitmapMemoryCacheKey))).thenReturn(null);
  }

  private void setupBitmapMemoryCacheGetIntermediateImage() {
    when(mMemoryCache.get(eq(mBitmapMemoryCacheKey)))
        .thenReturn(mIntermediateImageReference);
  }

  private void setupInputProducerStreamingSuccess() {
    doAnswer(new ProduceResultsNewResultAnswer(
            Arrays.asList(mIntermediateImageReference, mFinalImageReference)))
        .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerNotFound() {
    final List<CloseableReference<CloseableImage>> nullArray =
        new ArrayList<CloseableReference<CloseableImage>>(1);
    nullArray.add(null);
    doAnswer(new ProduceResultsNewResultAnswer(nullArray))
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
