/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import bolts.Task;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.*;
import org.robolectric.annotation.*;

/**
 * Checks basic properties of disk cache producer operation, that is: - it delegates to the {@link
 * BufferedDiskCache#get(CacheKey key, AtomicBoolean isCancelled)} - it returns a 'copy' of the
 * cached value - if {@link BufferedDiskCache#get(CacheKey key, AtomicBoolean isCancelled)} is
 * unsuccessful, then it passes the request to the next producer in the sequence. - if the next
 * producer returns the value, then it is put into the disk cache.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DiskCacheReadProducerTest {
  private static final String PRODUCER_NAME = DiskCacheReadProducer.PRODUCER_NAME;

  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public Object mCallerContext;
  @Mock public ProducerListener2 mProducerListener;
  @Mock public Exception mException;
  @Mock public ImagePipelineConfig mConfig;
  private final BufferedDiskCache mDefaultBufferedDiskCache = mock(BufferedDiskCache.class);
  private final BufferedDiskCache mSmallImageBufferedDiskCache = mock(BufferedDiskCache.class);
  private final String mDiskCacheId1 = "DISK_CACHE_ID_1";
  private final BufferedDiskCache mBufferedDiskCache1 = mock(BufferedDiskCache.class);
  private final String mDiskCacheId2 = "DISK_CACHE_ID_2";
  private final BufferedDiskCache mBufferedDiskCache2 = mock(BufferedDiskCache.class);
  private final Map<String, BufferedDiskCache> mDynamicBufferedDiskCaches =
      ImmutableMap.of(mDiskCacheId1, mBufferedDiskCache1, mDiskCacheId2, mBufferedDiskCache2);
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
  private DiskCacheReadProducer mDiskCacheReadProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mDiskCacheReadProducer =
        new DiskCacheReadProducer(
            mDefaultBufferedDiskCache,
            mSmallImageBufferedDiskCache,
            mDynamicBufferedDiskCaches,
            mCacheKeyFactory,
            mInputProducer);
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

    mProducerContext =
        new SettableProducerContext(
            mImageRequest,
            mRequestId,
            mProducerListener,
            mCallerContext,
            ImageRequest.RequestLevel.FULL_FETCH,
            false,
            true,
            Priority.MEDIUM,
            mConfig);
    mLowestLevelProducerContext =
        new SettableProducerContext(
            mImageRequest,
            mRequestId,
            mProducerListener,
            mCallerContext,
            ImageRequest.RequestLevel.DISK_CACHE,
            false,
            true,
            Priority.MEDIUM,
            mConfig);
    when(mProducerListener.requiresExtraMap(mProducerContext, PRODUCER_NAME)).thenReturn(true);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, mCallerContext)).thenReturn(mCacheKey);
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(true);
  }

  private void setUpDiskCacheProducerEnabled(boolean enabled) {
    when(mImageRequest.isCacheEnabled(ImageRequest.CachesLocationsMasks.DISK_READ))
        .thenReturn(enabled);
  }

  @Test
  public void testStartInputProducerIfNotEnabled() {
    setUpDiskCacheProducerEnabled(false);
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(false);
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verifyNoMoreInteractions(
        mConsumer,
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testNotEnabledAndLowestLevel() {
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(false);
    mDiskCacheReadProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
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
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(eq(mProducerContext), eq(PRODUCER_NAME), captor.capture());
    Map<String, String> resultMap = captor.getValue();
    assertEquals("true", resultMap.get(DiskCacheReadProducer.EXTRA_CACHED_VALUE_FOUND));
    assertEquals("0", resultMap.get(DiskCacheReadProducer.ENCODED_IMAGE_SIZE));
    verify(mProducerListener).onUltimateProducerReached(mProducerContext, PRODUCER_NAME, true);
    Assert.assertFalse(EncodedImage.isValid(mFinalEncodedImage));
  }

  @Test
  public void testSmallImageDiskCacheGetSuccessful() {
    setUpDiskCacheProducerEnabled(true);
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);
    setupDiskCacheGetSuccess(mSmallImageBufferedDiskCache);
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(eq(mProducerContext), eq(PRODUCER_NAME), captor.capture());
    Map<String, String> resultMap = captor.getValue();
    assertEquals("true", resultMap.get(DiskCacheReadProducer.EXTRA_CACHED_VALUE_FOUND));
    assertEquals("0", resultMap.get(DiskCacheReadProducer.ENCODED_IMAGE_SIZE));
    verify(mProducerListener).onUltimateProducerReached(mProducerContext, PRODUCER_NAME, true);
    Assert.assertFalse(EncodedImage.isValid(mFinalEncodedImage));
  }

  @Test
  public void testDynamicImageDiskCacheGetSuccessful() {
    setUpDiskCacheProducerEnabled(true);
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DYNAMIC);
    when(mImageRequest.getDiskCacheId()).thenReturn(mDiskCacheId2);
    setupDiskCacheGetSuccess(mBufferedDiskCache2);
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(eq(mProducerContext), eq(PRODUCER_NAME), captor.capture());
    Map<String, String> resultMap = captor.getValue();
    assertEquals("true", resultMap.get(DiskCacheReadProducer.EXTRA_CACHED_VALUE_FOUND));
    assertEquals("0", resultMap.get(DiskCacheReadProducer.ENCODED_IMAGE_SIZE));
    verify(mProducerListener).onUltimateProducerReached(mProducerContext, PRODUCER_NAME, true);
    Assert.assertFalse(EncodedImage.isValid(mFinalEncodedImage));
  }

  @Test
  public void testDynamicImageDiskCacheGetFailed_NoDiskCacheIdInImageRequest() {
    setUpDiskCacheProducerEnabled(true);
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DYNAMIC);
    setupDiskCacheGetSuccess(mBufferedDiskCache2);

    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
    verify(mProducerListener, times(0))
        .onProducerFinishWithSuccess(eq(mProducerContext), eq(PRODUCER_NAME), captor.capture());
    verify(mProducerListener, times(1))
        .onProducerFinishWithFailure(
            eq(mProducerContext),
            eq(PRODUCER_NAME),
            isA(DiskCacheDecision.DiskCacheDecisionNoDiskCacheChosenException.class),
            eq(null));
  }

  @Test
  public void testDiskCacheGetSuccessfulNoExtraMap() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    when(mProducerListener.requiresExtraMap(mProducerContext, PRODUCER_NAME)).thenReturn(false);
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(mProducerContext, PRODUCER_NAME, null);
    verify(mProducerListener).onUltimateProducerReached(mProducerContext, PRODUCER_NAME, true);
    Assert.assertFalse(EncodedImage.isValid(mFinalEncodedImage));
  }

  @Test
  public void testDiskCacheGetSuccessfulLowestLevelReached() {
    setupDiskCacheGetSuccess(mDefaultBufferedDiskCache);
    when(mProducerListener.requiresExtraMap(mProducerContext, PRODUCER_NAME)).thenReturn(false);
    mDiskCacheReadProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mLowestLevelProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(mLowestLevelProducerContext, PRODUCER_NAME, null);
    verify(mProducerListener)
        .onUltimateProducerReached(mLowestLevelProducerContext, PRODUCER_NAME, true);
    Assert.assertFalse(EncodedImage.isValid(mFinalEncodedImage));
  }

  @Test
  public void testDiskCacheGetFailureInputProducerSuccess() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetFailure(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verify(mProducerListener)
        .onProducerFinishWithFailure(mProducerContext, PRODUCER_NAME, mException, null);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testDiskCacheGetFailureInputProducerNotFound() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetFailure(mDefaultBufferedDiskCache);
    setupInputProducerNotFound();
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
  }

  @Test
  public void testDiskCacheGetFailureInputProducerFailure() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetFailure(mDefaultBufferedDiskCache);
    setupInputProducerFailure();
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithFailure(mProducerContext, PRODUCER_NAME, mException, null);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testDiskCacheGetFailureLowestLevelReached() {
    setUpDiskCacheProducerEnabled(true);
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetFailure(mDefaultBufferedDiskCache);
    mDiskCacheReadProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mProducerListener).onProducerStart(mLowestLevelProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithFailure(mLowestLevelProducerContext, PRODUCER_NAME, mException, null);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mLowestLevelProducerContext), anyString(), anyBoolean());
    verify(mInputProducer).produceResults(mConsumer, mLowestLevelProducerContext);
  }

  @Test
  public void testDefaultDiskCacheGetNotFoundInputProducerSuccess() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(eq(mProducerContext), eq(PRODUCER_NAME), captor.capture());
    Map<String, String> resultMap = captor.getValue();
    assertEquals("false", resultMap.get(DiskCacheReadProducer.EXTRA_CACHED_VALUE_FOUND));
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
    assertNull(resultMap.get(DiskCacheReadProducer.ENCODED_IMAGE_SIZE));
  }

  @Test
  public void testSmallImageDiskCacheGetNotFoundInputProducerSuccess() {
    setUpDiskCacheProducerEnabled(true);
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);
    setupDiskCacheGetNotFound(mSmallImageBufferedDiskCache);
    setupInputProducerSuccess();
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(eq(mProducerContext), eq(PRODUCER_NAME), captor.capture());
    Map<String, String> resultMap = captor.getValue();
    assertEquals("false", resultMap.get(DiskCacheReadProducer.EXTRA_CACHED_VALUE_FOUND));
    assertNull(resultMap.get(DiskCacheReadProducer.ENCODED_IMAGE_SIZE));
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testDiskCacheGetNotFoundInputProducerSuccessNoExtraMap() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerSuccess();
    when(mProducerListener.requiresExtraMap(mProducerContext, PRODUCER_NAME)).thenReturn(false);
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(mProducerContext, PRODUCER_NAME, null);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testDiskCacheGetNotFoundInputProducerNotFound() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerNotFound();
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
  }

  @Test
  public void testDiskCacheGetNotFoundInputProducerFailure() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    setupInputProducerFailure();
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(eq(mProducerContext), eq(PRODUCER_NAME), captor.capture());
    Map<String, String> resultMap = captor.getValue();
    assertEquals("false", resultMap.get(DiskCacheReadProducer.EXTRA_CACHED_VALUE_FOUND));
    assertNull(resultMap.get(DiskCacheReadProducer.ENCODED_IMAGE_SIZE));
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testDiskCacheGetNotFoundLowestLevelReached() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetNotFound(mDefaultBufferedDiskCache);
    when(mProducerListener.requiresExtraMap(mLowestLevelProducerContext, PRODUCER_NAME))
        .thenReturn(false);
    mDiskCacheReadProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mProducerListener).onProducerStart(mLowestLevelProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(mLowestLevelProducerContext, PRODUCER_NAME, null);
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mLowestLevelProducerContext), anyString(), anyBoolean());
    verify(mInputProducer).produceResults(mConsumer, mLowestLevelProducerContext);
  }

  @Test
  public void testGetExtraMap() {
    when(mProducerListener.requiresExtraMap(mProducerContext, PRODUCER_NAME)).thenReturn(true);
    final Map<String, String> trueValue =
        ImmutableMap.of(
            DiskCacheReadProducer.EXTRA_CACHED_VALUE_FOUND,
            "true",
            DiskCacheReadProducer.ENCODED_IMAGE_SIZE,
            "123");
    assertEquals(
        trueValue,
        DiskCacheReadProducer.getExtraMap(mProducerListener, mProducerContext, true, 123));
    final Map<String, String> falseValue =
        ImmutableMap.of(DiskCacheReadProducer.EXTRA_CACHED_VALUE_FOUND, "false");
    assertEquals(
        falseValue,
        DiskCacheReadProducer.getExtraMap(mProducerListener, mProducerContext, false, 0));
  }

  @Test
  public void testDiskCacheGetCancelled() {
    setUpDiskCacheProducerEnabled(true);
    setupDiskCacheGetWait(mDefaultBufferedDiskCache);
    mDiskCacheReadProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer, never()).onCancellation();
    assertFalse(mIsCancelled.getValue().get());
    mProducerContext.cancel();
    assertTrue(mIsCancelled.getValue().get());
    mTaskCompletionSource.trySetCancelled();
    verify(mConsumer).onCancellation();
    verify(mInputProducer, never()).produceResults(any(Consumer.class), eq(mProducerContext));
    verify(mProducerListener).onProducerStart(mProducerContext, PRODUCER_NAME);
    verify(mProducerListener)
        .onProducerFinishWithCancellation(mProducerContext, PRODUCER_NAME, null);
    verify(mProducerListener, never())
        .onProducerFinishWithFailure(
            eq(mProducerContext), any(String.class), any(Exception.class), any(Map.class));
    verify(mProducerListener, never())
        .onProducerFinishWithSuccess(eq(mProducerContext), any(String.class), any(Map.class));
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  private void setupDiskCacheGetWait(BufferedDiskCache bufferedDiskCache) {
    mTaskCompletionSource = Task.create();
    when(bufferedDiskCache.get(eq(mCacheKey), mIsCancelled.capture()))
        .thenReturn(mTaskCompletionSource.getTask());
  }

  private void setupDiskCacheGetSuccess(BufferedDiskCache bufferedDiskCache) {
    setUpDiskCacheProducerEnabled(true);
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
              @Nullable
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Consumer consumer = (Consumer) invocation.getArguments()[0];
                consumer.onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS);
                consumer.onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
                return null;
              }
            })
        .when(mInputProducer)
        .produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerFailure() {
    doAnswer(
            new Answer<Object>() {
              @Nullable
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Consumer consumer = (Consumer) invocation.getArguments()[0];
                consumer.onFailure(mException);
                return null;
              }
            })
        .when(mInputProducer)
        .produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerNotFound() {
    doAnswer(
            new Answer<Object>() {
              @Nullable
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Consumer consumer = (Consumer) invocation.getArguments()[0];
                consumer.onNewResult(null, Consumer.IS_LAST);
                return null;
              }
            })
        .when(mInputProducer)
        .produceResults(any(Consumer.class), eq(mProducerContext));
  }
}
