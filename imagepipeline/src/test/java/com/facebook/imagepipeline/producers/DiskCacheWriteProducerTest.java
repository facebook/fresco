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
import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.DiskCachePolicy;
import com.facebook.imagepipeline.common.Priority;
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
public class DiskCacheWriteProducerTest {

  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public Object mCallerContext;
  @Mock public DiskCachePolicy mDiskCachePolicy;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  @Mock public AtomicBoolean mIsCancelled;
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
  private DiskCacheWriteProducer mDiskCacheWriteProducer;
  private DiskCacheWriteProducer mForceSmallCacheProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mDiskCacheWriteProducer = new DiskCacheWriteProducer(
        mInputProducer,
        mDiskCachePolicy);
    mForceSmallCacheProducer = new DiskCacheWriteProducer(
        mInputProducer,
        mDiskCachePolicy);
    List<CacheKey> keys = new ArrayList<>(1);
    keys.add(new SimpleCacheKey("http://dummy.uri"));
    mCacheKey = new MultiCacheKey(keys);
    mIntermediatePooledByteBuffer = mock(PooledByteBuffer.class);
    mFinalPooledByteBuffer = mock(PooledByteBuffer.class);
    mIntermediateImageReference = CloseableReference.of(mIntermediatePooledByteBuffer);
    mFinalImageReference = CloseableReference.of(mFinalPooledByteBuffer);
    mIntermediateEncodedImage = new EncodedImage(mIntermediateImageReference);
    mFinalEncodedImage = new EncodedImage(mFinalImageReference);

    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mCallerContext,
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    mLowestLevelProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mCallerContext,
        ImageRequest.RequestLevel.DISK_CACHE,
        false,
        true,
        Priority.MEDIUM);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, mCallerContext)).thenReturn(mCacheKey);
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(true);
  }

  @Test
  public void testInputProducerSuccess() {
    setupInputProducerSuccess();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mDiskCachePolicy, never())
        .writeToCache(mIntermediateEncodedImage, mImageRequest, mCallerContext);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mDiskCachePolicy)
        .writeToCache(argumentCaptor.capture(), eq(mImageRequest), eq(mCallerContext));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertSame(
        encodedImage.getByteBufferRef().getUnderlyingReferenceTestOnly(),
        mFinalImageReference.getUnderlyingReferenceTestOnly());
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, false);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verifyZeroInteractions(mProducerListener);
  }

  @Test
  public void testInputProducerNotFound() {
    setupInputProducerNotFound();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, true);
    verifyZeroInteractions(
        mProducerListener,
        mDiskCachePolicy);
  }

  @Test
  public void testInputProducerFailure() {
    setupInputProducerFailure();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verifyZeroInteractions(
        mProducerListener,
        mDiskCachePolicy);
  }

  @Test
  public void testDoesNotWriteResultToCacheIfNotEnabled() {
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(false);
    setupInputProducerSuccess();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, false);
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
    verify(mDiskCachePolicy, never())
        .writeToCache(any(EncodedImage.class), any(ImageRequest.class), anyObject());
    verifyNoMoreInteractions(
        mProducerListener,
        mCacheKeyFactory,
        mDiskCachePolicy);
  }

  @Test
  public void testLowestLevelReached() {
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(false);
    mDiskCacheWriteProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mConsumer).onNewResult(null, true);
    verifyZeroInteractions(
        mInputProducer,
        mDiskCachePolicy,
        mProducerListener);
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
