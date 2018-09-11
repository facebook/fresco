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

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.net.Uri;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.Config;

/**
 * Basic tests for LocalResourceFetchProducer
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class LocalResourceFetchProducerTest {

  private static final String PRODUCER_NAME = LocalResourceFetchProducer.PRODUCER_NAME;
  private static final int TEST_ID = 1337;
  private static final int TEST_DATA_LENGTH = 337;

  @Mock public Resources mResources;
  @Mock public AssetFileDescriptor mAssetFileDescriptor;
  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;

  private TestExecutorService mExecutor;
  private SettableProducerContext mProducerContext;
  private final String mRequestId = "mRequestId";
  private LocalResourceFetchProducer mLocalResourceFetchProducer;
  private EncodedImage mCapturedEncodedImage;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(mResources.openRawResourceFd(eq(TEST_ID))).thenReturn(mAssetFileDescriptor);
    when(mAssetFileDescriptor.getLength()).thenReturn((long) TEST_DATA_LENGTH);

    mExecutor = new TestExecutorService(new FakeClock());
    mLocalResourceFetchProducer = new LocalResourceFetchProducer(
        mExecutor,
        mPooledByteBufferFactory,
        mResources
    );

    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    when(mImageRequest.getSourceUri()).thenReturn(Uri.parse("res:///" + TEST_ID));
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mCapturedEncodedImage =
                EncodedImage.cloneOrNull(((EncodedImage) invocation.getArguments()[0]));
            return null;
          }
        })
        .when(mConsumer)
        .onNewResult(notNull(EncodedImage.class), anyInt());
  }

  @After
  public void tearDown() throws Exception {
    verify(mPooledByteBufferFactory, atMost(1))
        .newByteBuffer(any(InputStream.class), eq(TEST_DATA_LENGTH));
  }

  @Test
  public void testFetchLocalResource() throws Exception {
    PooledByteBuffer pooledByteBuffer = mock(PooledByteBuffer.class);
    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class), eq(TEST_DATA_LENGTH)))
        .thenReturn(pooledByteBuffer);
    when(mResources.openRawResource(eq(TEST_ID)))
        .thenReturn(new ByteArrayInputStream(new byte[TEST_DATA_LENGTH]));

    mLocalResourceFetchProducer.produceResults(mConsumer, mProducerContext);
    mExecutor.runUntilIdle();

    assertEquals(
        2,
        mCapturedEncodedImage.getByteBufferRef()
            .getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertSame(pooledByteBuffer, mCapturedEncodedImage.getByteBufferRef().get());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, null);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, true);
  }

  @Test(expected = RuntimeException.class)
  public void testFetchLocalResourceFailsByThrowing() throws Exception {
    when(mResources.openRawResource(eq(TEST_ID)))
        .thenThrow(mException);
    mLocalResourceFetchProducer.produceResults(mConsumer, mProducerContext);
    mExecutor.runUntilIdle();
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        mRequestId, PRODUCER_NAME, mException, null);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, false);
  }
}
