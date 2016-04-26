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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;

import bolts.Task;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Checks basic properties of disk cache producer operation, that is:
 *   - it delegates to the {@link BufferedDiskCache#get(CacheKey key, AtomicBoolean isCancelled)}
 *   - it returns a 'copy' of the cached value
 *   - if {@link BufferedDiskCache#get(CacheKey key, AtomicBoolean isCancelled)} is unsuccessful,
 *   then it passes the request to the next producer in the sequence.
 *   - if the next producer returns the value, then it is put into the disk cache.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class DiskCacheProducerTest {
  private static final String PRODUCER_NAME = DiskCacheProducer.PRODUCER_NAME;
  private static final Map EXPECTED_MAP_ON_CACHE_HIT =
      ImmutableMap.of(DiskCacheProducer.VALUE_FOUND, "true");
  private static final Map EXPECTED_MAP_ON_CACHE_MISS =
      ImmutableMap.of(DiskCacheProducer.VALUE_FOUND, "false");
  private static final int FORCE_SMALL_CACHE_THRESHOLD = 2048;

  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  private final BufferedDiskCache mDefaultBufferedDiskCache = mock(BufferedDiskCache.class);
  private final BufferedDiskCache mSmallImageBufferedDiskCache =
      mock(BufferedDiskCache.class);
  private SettableProducerContext mProducerContext;
  private SettableProducerContext mLowestLevelProducerContext;
  private final String mRequestId = "mRequestId";
  private MultiCacheKey mCacheKey;
  private PooledByteBuffer mIntermediatePooledByteBuffer;
  private PooledByteBuffer mFinalPooledByteBuffer;
  private CloseableReference<PooledByteBuffer> mIntermediateImageReference;
  private CloseableReference<PooledByteBuffer> mFinalImageReference;
  private EncodedImage mIntermediateEncodedImage;
  private EncodedImage mFinalEncodedImage;
  private Task.TaskCompletionSource mTaskCompletionSource;
  private ArgumentCaptor<AtomicBoolean> mIsCancelled;
  private DiskCacheProducer mDiskCacheProducer;
  private DiskCacheProducer mForceSmallCacheProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mDiskCacheProducer = new DiskCacheProducer(
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        mInputProducer,
        -1);
    mForceSmallCacheProducer = new DiskCacheProducer(
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        mInputProducer,
        FORCE_SMALL_CACHE_THRESHOLD);
    List<CacheKey> keys = new ArrayList<>(1);
    keys.add(new SimpleCacheKey("http://dummy.uri"));
    mCacheKey = new MultiCacheKey(keys);
    mIntermediatePooledByteBuffer = mock(PooledByteBuffer.class);
    mFinalPooledByteBuffer = mock(PooledByteBuffer.class);
    mIntermediateImageReference = CloseableReference.of(mIntermediatePooledByteBuffer);
    mFinalImageReference = CloseableReference.of(mFinalPooledByteBuffer);
    mIntermediateEncodedImage = new EncodedImage(mIntermediateImageReference);
    mFinalEncodedImage = new EncodedImage(mFinalImageReference);
    mIsCancelled = ArgumentCaptor.forClass(AtomicBoolean.class);

    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    mLowestLevelProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.DISK_CACHE,
        false,
        true,
        Priority.MEDIUM);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest)).thenReturn(mCacheKey);
    when(mImageRequest.getImageType()).thenReturn(ImageRequest.ImageType.DEFAULT);
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(true);
  }

  @Test
  public void testStartInputProducerIfNotEnabled() {
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(false);
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verifyNoMoreInteractions(
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testNotEnabledAndLowestLevel() {
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(false);
    mDiskCacheProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mConsumer).onNewResult(null, true);
    verifyNoMoreInteractions(
        mProducerListener,
        mInputProducer,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testDefaultDiskCacheGetSuccessful() {
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, EXPECTED_MAP_ON_CACHE_HIT);
    Assert.assertFalse(EncodedImage.isValid(mFinalEncodedImage));
  }

  @Test
  public void testSmallImageDiskCacheGetSuccessful() {
    when(mImageRequest.getImageType()).thenReturn(ImageRequest.ImageType.SMALL);
    setupDiskCacheGetSuccess(mSmallImageBufferedDiskCache);
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, EXPECTED_MAP_ON_CACHE_HIT);
    Assert.assertFalse(EncodedImage.isValid(mFinalEncodedImage));
  }

  @Test
  public void testDiskCacheGetSuccessfulNoExtraMap() {
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(false);
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, null);
    Assert.assertFalse(EncodedImage.isValid(mFinalEncodedImage));
  }

  @Test
  public void testDiskCacheGetSuccessfulLowestLevelReached() {
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(false);
    mDiskCacheProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, null);
    Assert.assertFalse(EncodedImage.isValid(mFinalEncodedImage));
  }

  @Test
  public void testDefaultDiskCacheGetFailureInputProducerSuccess() {
    setupDiskCacheGetFailure(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mDefaultBufferedDiskCache, never()).put(mCacheKey, mIntermediateEncodedImage);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mDefaultBufferedDiskCache).put(eq(mCacheKey), argumentCaptor.capture());
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertSame(
        encodedImage.getByteBufferRef().getUnderlyingReferenceTestOnly(),
        mFinalImageReference.getUnderlyingReferenceTestOnly());
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, false);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
  }

  @Test
  public void testSmallImageDiskCacheGetFailureInputProducerSuccess() {
    when(mImageRequest.getImageType()).thenReturn(ImageRequest.ImageType.SMALL);
    setupDiskCacheGetFailure(mSmallImageBufferedDiskCache);
    setupInputProducerSuccess();
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mSmallImageBufferedDiskCache, never()).put(mCacheKey, mIntermediateEncodedImage);
    verify(mSmallImageBufferedDiskCache).put(mCacheKey, mFinalEncodedImage);
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, false);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
  }

  @Test
  public void testDiskCacheGetFailureInputProducerNotFound() {
    setupDiskCacheGetFailure(mDefaultBufferedDiskCache);
    setupInputProducerNotFound();
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, true);
  }

  @Test
  public void testDiskCacheGetFailureInputProducerFailure() {
    setupDiskCacheGetFailure(mDefaultBufferedDiskCache);
    setupInputProducerFailure();
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        mRequestId, PRODUCER_NAME, mException, null);
  }

  @Test
  public void testDiskCacheGetFailureLowestLevelReached() {
    setupDiskCacheGetFailure(mDefaultBufferedDiskCache);
    mDiskCacheProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        mRequestId, PRODUCER_NAME, mException, null);
    verify(mConsumer).onNewResult(null, true);
    verifyNoMoreInteractions(mInputProducer);
  }

  @Test
  public void testDefaultDiskCacheGetNotFoundInputProducerSuccess() {
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mDefaultBufferedDiskCache).put(mCacheKey, mFinalEncodedImage);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, EXPECTED_MAP_ON_CACHE_MISS);
  }

  @Test
  public void testSmallImageDiskCacheGetNotFoundInputProducerSuccess() {
    when(mImageRequest.getImageType()).thenReturn(ImageRequest.ImageType.SMALL);
    setupDiskCacheGetNotFound(mSmallImageBufferedDiskCache);
    setupInputProducerSuccess();
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mSmallImageBufferedDiskCache).put(mCacheKey, mFinalEncodedImage);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, EXPECTED_MAP_ON_CACHE_MISS);
  }

  @Test
  public void testDiskCacheGetNotFoundInputProducerSuccessNoExtraMap() {
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(false);
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mDefaultBufferedDiskCache).put(mCacheKey, mFinalEncodedImage);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, null);
  }

  @Test
  public void testDiskCacheGetNotFoundInputProducerNotFound() {
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerNotFound();
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, true);
  }

  @Test
  public void testDiskCacheGetNotFoundInputProducerFailure() {
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerFailure();
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, EXPECTED_MAP_ON_CACHE_MISS);
  }

  @Test
  public void testDiskCacheGetNotFoundLowestLevelReached() {
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(false);
    mDiskCacheProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, null);
    verify(mConsumer).onNewResult(null, true);
    verifyNoMoreInteractions(mInputProducer);
  }

  @Test
  public void testGetExtraMap() {
    assertEquals(
        ImmutableMap.of(DiskCacheProducer.VALUE_FOUND, "true"),
        DiskCacheProducer.getExtraMap(mProducerListener, mRequestId, true));
    assertEquals(
        ImmutableMap.of(DiskCacheProducer.VALUE_FOUND, "false"),
        DiskCacheProducer.getExtraMap(mProducerListener, mRequestId, false));
  }

  @Test
  public void testDiskCacheGetCancelled() {
    setupDiskCacheGetWait(mDefaultBufferedDiskCache);
    mDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer, never()).onCancellation();
    assertFalse(mIsCancelled.getValue().get());
    mProducerContext.cancel();
    assertTrue(mIsCancelled.getValue().get());
    mTaskCompletionSource.trySetCancelled();
    verify(mConsumer).onCancellation();
    verify(mInputProducer, never()).produceResults(any(Consumer.class), eq(mProducerContext));
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithCancellation(mRequestId, PRODUCER_NAME, null);
    verify(mProducerListener, never()).onProducerFinishWithFailure(
        eq(mRequestId),
        any(String.class),
        any(Exception.class),
        any(Map.class));
    verify(mProducerListener, never()).onProducerFinishWithSuccess(
        eq(mRequestId),
        any(String.class),
        any(Map.class));
  }

  @Test
  public void testSmallImageInSmallCache() {
    setupDiskCacheGetNotFound(mSmallImageBufferedDiskCache);
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    when(mFinalPooledByteBuffer.size()).thenReturn(FORCE_SMALL_CACHE_THRESHOLD - 1);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mSmallImageBufferedDiskCache).put(mCacheKey, mFinalEncodedImage);
    verify(mDefaultBufferedDiskCache, never()).put(eq(mCacheKey), any(EncodedImage.class));
  }

  @Test
  public void testBigImageInDefaultCache() {
    setupDiskCacheGetNotFound(mSmallImageBufferedDiskCache);
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    when(mFinalPooledByteBuffer.size()).thenReturn(FORCE_SMALL_CACHE_THRESHOLD);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mDefaultBufferedDiskCache).put(mCacheKey, mFinalEncodedImage);
    verify(mSmallImageBufferedDiskCache, never()).put(eq(mCacheKey), any(EncodedImage.class));
  }

  @Test
  public void testIgnoresSmallHintOnWrite() {
    when(mImageRequest.getImageType()).thenReturn(ImageRequest.ImageType.SMALL);
    setupDiskCacheGetNotFound(mSmallImageBufferedDiskCache);
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    when(mFinalPooledByteBuffer.size()).thenReturn(FORCE_SMALL_CACHE_THRESHOLD);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mDefaultBufferedDiskCache).put(mCacheKey, mFinalEncodedImage);
    verify(mSmallImageBufferedDiskCache, never()).put(eq(mCacheKey), any(EncodedImage.class));
  }

  @Test
  public void testChecksSmallCacheFirst() {
    setupDiskCacheGetSuccess(mSmallImageBufferedDiskCache);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mDefaultBufferedDiskCache, never())
        .get(any(MultiCacheKey.class), any(AtomicBoolean.class));
  }

  @Test
  public void testAvoidsDiskReadWhenFoundInIndex() {
    when(mDefaultBufferedDiskCache.containsSync(mCacheKey)).thenReturn(true);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mSmallImageBufferedDiskCache, never())
        .get(any(MultiCacheKey.class), any(AtomicBoolean.class));
  }

  @Test
  public void testReadsTwiceWhenNecessary() {
    setupDiskCacheGetNotFound(mSmallImageBufferedDiskCache);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mSmallImageBufferedDiskCache).get(eq(mCacheKey), any(AtomicBoolean.class));
    verify(mDefaultBufferedDiskCache).get(eq(mCacheKey), any(AtomicBoolean.class));
  }

  @Test
  public void testIgnoresSmallHintIndex() {
    when(mImageRequest.getImageType()).thenReturn(ImageRequest.ImageType.SMALL);
    when(mDefaultBufferedDiskCache.containsSync(mCacheKey)).thenReturn(true);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mSmallImageBufferedDiskCache, never())
        .get(any(MultiCacheKey.class), any(AtomicBoolean.class));
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
  }

  @Test
  public void testIgnoresSmallHintDisk() {
    when(mImageRequest.getImageType()).thenReturn(ImageRequest.ImageType.SMALL);
    setupDiskCacheGetNotFound(mSmallImageBufferedDiskCache);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
  }

  @Test
  public void testSmallFailureDoesntStopDefaultRead() {
    setupDiskCacheGetFailure(mSmallImageBufferedDiskCache);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, EXPECTED_MAP_ON_CACHE_HIT);
  }

  @Test
  public void testSmallFailureDoesntStopNetwork() {
    setupDiskCacheGetFailure(mSmallImageBufferedDiskCache);
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, EXPECTED_MAP_ON_CACHE_MISS);
  }

  @Test
  public void testCancellationStopsSecondRead() {
    setupDiskCacheGetWait(mSmallImageBufferedDiskCache);
    mForceSmallCacheProducer.produceResults(mConsumer, mProducerContext);
    mTaskCompletionSource.setCancelled();
    verify(mDefaultBufferedDiskCache, never())
        .get(any(MultiCacheKey.class), any(AtomicBoolean.class));
    verifyNoMoreInteractions(mInputProducer);
    verify(mProducerListener).onProducerFinishWithCancellation(
        mRequestId, PRODUCER_NAME, null);
  }

  private void setupDiskCacheGetWait(BufferedDiskCache bufferedDiskCache) {
    mTaskCompletionSource = Task.create();
    when(bufferedDiskCache.get(eq(mCacheKey), mIsCancelled.capture()))
        .thenReturn(mTaskCompletionSource.getTask());
  }

  private void setupDiskCacheGetSuccess(BufferedDiskCache bufferedDiskCache) {
    when(bufferedDiskCache.get(eq(mCacheKey), any(AtomicBoolean.class)))
        .thenReturn(Task.forResult(mFinalEncodedImage));
  }

  private void setupDiskCacheGetNotFound(BufferedDiskCache bufferedDiskCache) {
    when(bufferedDiskCache.get(eq(mCacheKey), any(AtomicBoolean.class)))
        .thenReturn(Task.<EncodedImage>forResult(null));
  }

  private void setupDiskCacheGetFailure(BufferedDiskCache bufferedDiskCache) {
    when(bufferedDiskCache.get(eq(mCacheKey), any(AtomicBoolean.class)))
        .thenReturn(Task.<EncodedImage>forError(mException));
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

  private void setupInputProducerFailure() {
    doAnswer(
        new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            Consumer consumer = (Consumer) invocation.getArguments()[0];
            consumer.onFailure(mException);
            return null;
          }
        }).when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerNotFound() {
    doAnswer(
        new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            Consumer consumer = (Consumer) invocation.getArguments()[0];
            consumer.onNewResult(null, true);
            return null;
          }
        }).when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }
}
