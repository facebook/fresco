/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
  private static final String PRODUCER_NAME = DiskCacheWriteProducer.PRODUCER_NAME;

  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public Object mCallerContext;
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
  private EncodedImage mFinalEncodedImageFormatUnknown;
  private EncodedImage mFinalEncodedImage;
  private DiskCacheWriteProducer mDiskCacheWriteProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mDiskCacheWriteProducer = new DiskCacheWriteProducer(
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        mInputProducer
    );
    List<CacheKey> keys = new ArrayList<>(1);
    keys.add(new SimpleCacheKey("http://dummy.uri"));
    mCacheKey = new MultiCacheKey(keys);
    mIntermediatePooledByteBuffer = mock(PooledByteBuffer.class);
    mFinalPooledByteBuffer = mock(PooledByteBuffer.class);
    mIntermediateImageReference = CloseableReference.of(mIntermediatePooledByteBuffer);
    mFinalImageReference = CloseableReference.of(mFinalPooledByteBuffer);
    mIntermediateEncodedImage = new EncodedImage(mIntermediateImageReference);
    mFinalEncodedImageFormatUnknown = new EncodedImage(mFinalImageReference);

    mFinalEncodedImage = new EncodedImage(mFinalImageReference);
    mFinalEncodedImage.setImageFormat(new ImageFormat("jpeg", null));
    mFinalEncodedImage.setWidth(100);
    mFinalEncodedImage.setHeight(100);

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
  public void testDefaultDiskCacheInputProducerSuccess() {
    setupInputProducerSuccess();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mDefaultBufferedDiskCache, never()).put(mCacheKey, mIntermediateEncodedImage);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mDefaultBufferedDiskCache).put(eq(mCacheKey), argumentCaptor.capture());
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertSame(
        encodedImage.getByteBufferRef().getUnderlyingReferenceTestOnly(),
        mFinalImageReference.getUnderlyingReferenceTestOnly());
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verifyZeroInteractions(mProducerListener);
  }

  @Test
  public void testSmallImageDiskCacheInputProducerSuccess() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);
    setupInputProducerSuccess();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mSmallImageBufferedDiskCache, never()).put(mCacheKey, mIntermediateEncodedImage);
    verify(mSmallImageBufferedDiskCache).put(mCacheKey, mFinalEncodedImage);
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    verifyZeroInteractions(mProducerListener);
  }

  @Test
  public void testSmallImageDiskCacheInputProducerUnknownFormat() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);
    setupInputProducerSuccessFormatUnknown();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mSmallImageBufferedDiskCache, never()).put(mCacheKey, mIntermediateEncodedImage);
    verify(mSmallImageBufferedDiskCache, never()).put(mCacheKey, mFinalEncodedImageFormatUnknown);
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer).onNewResult(mFinalEncodedImageFormatUnknown, Consumer.IS_LAST);
    verifyZeroInteractions(mProducerListener);
  }

  @Test
  public void testDoesNotWriteToCacheIfPartialResult() {
    setupInputProducerSuccessWithStatusFlags(
        Consumer.IS_PARTIAL_RESULT, mFinalEncodedImageFormatUnknown);

    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);

    verify(mConsumer).onNewResult(mIntermediateEncodedImage, Consumer.IS_PARTIAL_RESULT);
    verify(mConsumer)
        .onNewResult(
            mFinalEncodedImageFormatUnknown, Consumer.IS_LAST | Consumer.IS_PARTIAL_RESULT);

    verifyZeroInteractions(mDefaultBufferedDiskCache, mSmallImageBufferedDiskCache);
  }

  @Test
  public void testInputProducerNotFound() {
    setupInputProducerNotFound();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verifyZeroInteractions(
        mProducerListener,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testInputProducerFailure() {
    setupInputProducerFailure();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verifyZeroInteractions(
        mProducerListener,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testDoesNotWriteResultToCacheIfNotEnabled() {
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(false);
    setupInputProducerSuccessFormatUnknown();
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer).onNewResult(mFinalEncodedImageFormatUnknown, Consumer.IS_LAST);
    verifyNoMoreInteractions(
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testDoesNotWriteResultToCacheIfResultStatusSaysNotTo() {
    setupInputProducerSuccessWithStatusFlags(
        Consumer.DO_NOT_CACHE_ENCODED, mFinalEncodedImageFormatUnknown);
    mDiskCacheWriteProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, Consumer.DO_NOT_CACHE_ENCODED);
    verify(mConsumer)
        .onNewResult(
            mFinalEncodedImageFormatUnknown, Consumer.IS_LAST | Consumer.DO_NOT_CACHE_ENCODED);
    verifyNoMoreInteractions(
        mProducerListener,
        mCacheKeyFactory,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache);
  }

  @Test
  public void testLowestLevelReached() {
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(false);
    mDiskCacheWriteProducer.produceResults(mConsumer, mLowestLevelProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verifyZeroInteractions(
        mInputProducer,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mProducerListener);
  }

  private void setupInputProducerSuccessFormatUnknown() {
    setupInputProducerSuccessWithStatusFlags(0, mFinalEncodedImageFormatUnknown);
  }

  private void setupInputProducerSuccess() {
    setupInputProducerSuccessWithStatusFlags(0, mFinalEncodedImage);
  }

  private void setupInputProducerSuccessWithStatusFlags(
      final @Consumer.Status int statusFlags, final EncodedImage finalEncodedImage) {
    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Consumer consumer = (Consumer) invocation.getArguments()[0];
                consumer.onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS | statusFlags);
                consumer.onNewResult(finalEncodedImage, Consumer.IS_LAST | statusFlags);
                return null;
              }
            })
        .when(mInputProducer)
        .produceResults(any(Consumer.class), eq(mProducerContext));
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
            consumer.onNewResult(null, Consumer.IS_LAST);
            return null;
          }
        }).when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }
}
