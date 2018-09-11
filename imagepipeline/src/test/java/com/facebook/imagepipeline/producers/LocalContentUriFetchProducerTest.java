/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

import android.content.ContentResolver;
import android.net.Uri;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.io.File;
import java.io.InputStream;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;

/**
 * Basic tests for LocalContentUriFetchProducer
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class LocalContentUriFetchProducerTest {
  private static final String PRODUCER_NAME = LocalContentUriFetchProducer.PRODUCER_NAME;
  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public ContentResolver mContentResolver;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  private TestExecutorService mExecutor;
  private SettableProducerContext mProducerContext;
  private final String mRequestId = "mRequestId";
  private Uri mContentUri;
  private LocalContentUriFetchProducer mLocalContentUriFetchProducer;
  private EncodedImage mCapturedEncodedImage;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mExecutor = new TestExecutorService(new FakeClock());
    mLocalContentUriFetchProducer = new LocalContentUriFetchProducer(
        mExecutor,
        mPooledByteBufferFactory,
        mContentResolver
    );
    mContentUri = Uri.fromFile(mock(File.class));

    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    when(mImageRequest.getSourceUri()).thenReturn(mContentUri);
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mCapturedEncodedImage =
                EncodedImage.cloneOrNull((EncodedImage) invocation.getArguments()[0]);
            return null;
          }
        })
        .when(mConsumer)
        .onNewResult(notNull(EncodedImage.class), anyInt());
  }

  @Test
  public void testLocalContentUriFetchCancelled() {
    mLocalContentUriFetchProducer.produceResults(mConsumer, mProducerContext);
    mProducerContext.cancel();
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithCancellation(mRequestId, PRODUCER_NAME, null);
    verify(mConsumer).onCancellation();
    mExecutor.runUntilIdle();
    verifyZeroInteractions(mPooledByteBufferFactory);
  }

  @Test
  public void testFetchLocalContentUri() throws Exception {
    PooledByteBuffer pooledByteBuffer = mock(PooledByteBuffer.class);
    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class)))
        .thenReturn(pooledByteBuffer);

    when(mContentResolver.openInputStream(mContentUri)).thenReturn(mock(InputStream.class));
    mLocalContentUriFetchProducer.produceResults(mConsumer, mProducerContext);

    mExecutor.runUntilIdle();

    assertEquals(
        2,
        mCapturedEncodedImage.getByteBufferRef()
            .getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertSame(pooledByteBuffer, mCapturedEncodedImage.getByteBufferRef().get());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, true);
  }

  @Test(expected = RuntimeException.class)
  public void testFetchLocalContentUriFailsByThrowing() throws Exception {
    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class))).thenThrow(mException);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        mRequestId, PRODUCER_NAME, mException, null);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, false);
  }
}
