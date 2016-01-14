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

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.imagepipeline.request.RepeatedPostprocessor;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
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
  @Mock public RepeatedPostprocessor mRepeatedPostprocessor;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  @Mock public CloseableImage mImage1;
  @Mock public CloseableImage mImage2;
  private CloseableReference<CloseableImage> mImageRef1;
  private CloseableReference<CloseableImage> mImageRef2;
  private CloseableReference<CloseableImage> mImageRef1Clone;
  private CloseableReference<CloseableImage> mImageRef2Clone;
  private CacheKey mPostProcessorCacheKey;
  private CacheKey mPostprocessedBitmapCacheKey;
  private PostprocessedBitmapMemoryCacheProducer mMemoryCacheProducer;
  private final String mRequestId = "mRequestId";
  private final Map<String, String> mExtraOnHit =
      ImmutableMap.of(PostprocessedBitmapMemoryCacheProducer.VALUE_FOUND, "true");
  private final Map<String, String> mExtraOnMiss =
      ImmutableMap.of(PostprocessedBitmapMemoryCacheProducer.VALUE_FOUND, "false");

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mMemoryCacheProducer =
        new PostprocessedBitmapMemoryCacheProducer(mMemoryCache, mCacheKeyFactory, mInputProducer);
    mPostProcessorCacheKey = new SimpleCacheKey("blur");
    mPostprocessedBitmapCacheKey = new SimpleCacheKey("http://dummy.uri:blur");
    mImageRef2 = CloseableReference.of(mImage1);
    mImageRef1 = CloseableReference.of(mImage2);
    mImageRef2Clone = mImageRef2.clone();
    mImageRef1Clone = mImageRef1.clone();

    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);
    when(mProducerContext.getListener()).thenReturn(mProducerListener);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(mProducerContext.getId()).thenReturn(mRequestId);
    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);
    when(mPostprocessor.getPostprocessorCacheKey()).thenReturn(mPostProcessorCacheKey);
    when(mRepeatedPostprocessor.getPostprocessorCacheKey()).thenReturn(mPostProcessorCacheKey);
    when(mCacheKeyFactory.getPostprocessedBitmapCacheKey(mImageRequest))
        .thenReturn(mPostprocessedBitmapCacheKey);
  }

  @Test
  public void testNoPostProcessor() {
    when(mImageRequest.getPostprocessor()).thenReturn(null);
    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verifyNoMoreInteractions(mConsumer, mProducerListener);
  }

  @Test
  public void testNoPostProcessorCaching() {
    when(mPostprocessor.getPostprocessorCacheKey()).thenReturn(null);
    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verifyNoMoreInteractions(mConsumer, mProducerListener);
  }

  @Test
  public void testCacheHit() {
    when(mMemoryCache.get(mPostprocessedBitmapCacheKey)).thenReturn(mImageRef2Clone);

    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);

    verify(mInputProducer, never()).produceResults(any(Consumer.class), any(ProducerContext.class));
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, mExtraOnHit);
    verify(mConsumer).onNewResult(mImageRef2Clone, true);
    // reference must be closed after `consumer.onNewResult` returns
    Assert.assertFalse(mImageRef2Clone.isValid());
  }

  @Test
  public void testCacheMiss_UnderlyingNull() {
    Consumer consumer = performCacheMiss();
    consumer.onNewResult(null, true);
    verify(mConsumer).onNewResult(null, true);
  }

  @Test
  public void testCacheMiss_UnderlyingSuccessCacheSucceeded() {
    when(mMemoryCache.cache(mPostprocessedBitmapCacheKey, mImageRef2)).thenReturn(mImageRef2Clone);

    Consumer consumer = performCacheMiss();
    consumer.onNewResult(mImageRef1, false);
    mImageRef1.close();
    consumer.onNewResult(mImageRef2, true);
    mImageRef2.close();

    verify(mConsumer).onNewResult(mImageRef2Clone, true);
    // reference must be closed after `consumer.onNewResult` returns
    Assert.assertFalse(mImageRef2Clone.isValid());
  }

  @Test
  public void testCacheMiss_UnderlyingSuccessCacheFailed() {
    when(mMemoryCache.cache(mPostprocessedBitmapCacheKey, mImageRef2)).thenReturn(null);

    Consumer consumer = performCacheMiss();
    consumer.onNewResult(mImageRef1, false);
    mImageRef1.close();
    consumer.onNewResult(mImageRef2, true);
    mImageRef2.close();

    verify(mConsumer).onNewResult(mImageRef2, true);
    // reference must be closed after `consumer.onNewResult` returns
    Assert.assertFalse(mImageRef2.isValid());
  }

  @Test
  public void testCacheMiss_UnderlyingFailure() {
    Consumer consumer = performCacheMiss();
    consumer.onFailure(mException);
    verify(mConsumer).onFailure(mException);
  }

  @Test
  public void testRepeatedPostProcessor() {
    when(mImageRequest.getPostprocessor()).thenReturn(mRepeatedPostprocessor);
    when(mMemoryCache.cache(mPostprocessedBitmapCacheKey, mImageRef1)).thenReturn(mImageRef1Clone);
    when(mMemoryCache.cache(mPostprocessedBitmapCacheKey, mImageRef2)).thenReturn(mImageRef2Clone);

    Consumer consumer = performCacheMiss();
    consumer.onNewResult(mImageRef1, false);
    mImageRef1.close();
    consumer.onNewResult(mImageRef2, false);
    mImageRef2.close();

    verify(mConsumer).onNewResult(mImageRef1Clone, false);
    verify(mConsumer).onNewResult(mImageRef2Clone, false);
    // reference must be closed after `consumer.onNewResult` returns
    Assert.assertFalse(mImageRef1Clone.isValid());
    Assert.assertFalse(mImageRef2Clone.isValid());
  }

  private Consumer performCacheMiss() {
    when(mMemoryCache.get(mPostprocessedBitmapCacheKey)).thenReturn(null);

    mMemoryCacheProducer.produceResults(mConsumer, mProducerContext);

    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
    verify(mInputProducer).produceResults(captor.capture(), eq(mProducerContext));
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, mExtraOnMiss);
    return captor.getValue();
  }
}
