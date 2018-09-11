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
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.Map;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

/**
 * Checks basic properties of bitmap memory cache producer operation, that is:
 *   - it delegates to the {@link MemoryCache#get(Object)}.
 *   - if get is successful, then returned reference is closed.
 *   - if {@link MemoryCache#get(Object)} is unsuccessful, then it passes the
 *   request to the next producer in the sequence.
 *   - responses from the next producer are passed directly to the consumer.
 *   - listener methods are called as expected.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class BitmapMemoryCacheGetProducerTest {
  private static final String PRODUCER_NAME = BitmapMemoryCacheGetProducer.PRODUCER_NAME;
  @Mock public MemoryCache<CacheKey, CloseableImage> mMemoryCache;
  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer<CloseableReference<CloseableImage>> mInputProducer;
  @Mock public Consumer<CloseableReference<CloseableImage>> mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  @Mock public BitmapMemoryCacheKey mCacheKey;
  @Mock public Object mCallerContext;
  private final String mRequestId = "mRequestId";
  private CloseableImage mCloseableImage1;
  private CloseableReference<CloseableImage> mFinalImageReference;
  private BitmapMemoryCacheGetProducer mBitmapMemoryCacheGetProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mBitmapMemoryCacheGetProducer =
        new BitmapMemoryCacheGetProducer(mMemoryCache, mCacheKeyFactory, mInputProducer);
    mCloseableImage1 = mock(CloseableImage.class);
    mFinalImageReference = CloseableReference.of(mCloseableImage1);
    when(mCloseableImage1.getQualityInfo()).thenReturn(ImmutableQualityInfo.FULL_QUALITY);

    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);
    when(mProducerContext.getListener()).thenReturn(mProducerListener);
    when(mProducerContext.getId()).thenReturn(mRequestId);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    when(mProducerContext.getCallerContext())
        .thenReturn(PRODUCER_NAME);
    when(mCacheKeyFactory.getBitmapCacheKey(mImageRequest, PRODUCER_NAME)).thenReturn(mCacheKey);
  }

  @Test
  public void testBitmapMemoryCacheGetSuccessful() {
    setupBitmapCacheGetSuccess();
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
    mBitmapMemoryCacheGetProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalImageReference, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "true");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, true);
    Assert.assertTrue(!mFinalImageReference.isValid());
  }

  @Test
  public void testBitmapMemoryCacheGetNotFoundInputProducerSuccess() {
    setupBitmapCacheGetNotFound();
    setupInputProducerStreamingSuccess();
    mBitmapMemoryCacheGetProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalImageReference, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testBitmapMemoryCacheGetNotFoundInputProducerNotFound() {
    setupBitmapCacheGetNotFound();
    setupInputProducerNotFound();
    mBitmapMemoryCacheGetProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testBitmapMemoryCacheGetNotFoundInputProducerFailure() {
    setupBitmapCacheGetNotFound();
    setupInputProducerFailure();
    mBitmapMemoryCacheGetProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener, never())
        .onUltimateProducerReached(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testBitmapMemoryCacheGetNotFoundLowestLevelReached() {
    setupBitmapCacheGetNotFound();
    when(mProducerContext.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
    mBitmapMemoryCacheGetProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> extraMap =
        ImmutableMap.of(BitmapMemoryCacheProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, extraMap);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, false);
    verifyNoMoreInteractions(mInputProducer);
  }

  private void setupBitmapCacheGetSuccess() {
    when(mMemoryCache.get(eq(mCacheKey))).thenReturn(mFinalImageReference);
  }

  private void setupBitmapCacheGetNotFound() {
    when(mMemoryCache.get(eq(mCacheKey))).thenReturn(null);
  }

  private void setupInputProducerStreamingSuccess() {
    doAnswer(new ProduceResultsNewResultAnswer(mFinalImageReference))
        .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerNotFound() {
    doAnswer(new ProduceResultsNewResultAnswer(null))
        .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerFailure() {
    doAnswer(new ProduceResultsFailureAnswer()).
        when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private static class ProduceResultsNewResultAnswer implements Answer<Void> {
    private final CloseableReference<CloseableImage> mResult;

    private ProduceResultsNewResultAnswer(CloseableReference<CloseableImage> result) {
      mResult = result;
    }

    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      Consumer consumer = (Consumer) invocation.getArguments()[0];
      consumer.onNewResult(mResult, Consumer.IS_LAST);
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
