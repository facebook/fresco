/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.Uri;

import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Basic tests for LocalResourceFetchProducer
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class LocalAssetFetchProducerTest {

  private static final String PRODUCER_NAME = LocalAssetFetchProducer.PRODUCER_NAME;
  private static final String TEST_FILENAME = "dummy_asset.jpg";
  private static final int TEST_DATA_LENGTH = 337;

  @Mock public AssetManager mAssetManager;
  @Mock public PooledByteBuffer mPooledByteBuffer;
  @Mock public AssetFileDescriptor mAssetFileDescriptor;
  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;

  private TestExecutorService mExecutor;
  private SettableProducerContext mProducerContext;
  private final String mRequestId = "mRequestId";
  private LocalAssetFetchProducer mLocalAssetFetchProducer;
  private EncodedImage mCapturedEncodedImage;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(mAssetManager.openFd(eq(TEST_FILENAME))).thenReturn(mAssetFileDescriptor);
    when(mAssetFileDescriptor.getLength()).thenReturn((long) TEST_DATA_LENGTH);
    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class), eq(TEST_DATA_LENGTH)))
        .thenReturn(mPooledByteBuffer);

    mExecutor = new TestExecutorService(new FakeClock());
    mLocalAssetFetchProducer = new LocalAssetFetchProducer(
        mExecutor,
        mPooledByteBufferFactory,
        mAssetManager,
        false);

    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    when(mImageRequest.getSourceUri()).thenReturn(Uri.parse("asset:///" + TEST_FILENAME));
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
        .onNewResult(notNull(EncodedImage.class), anyBoolean());
  }

  @After
  public void tearDown() throws Exception {
    verify(mPooledByteBufferFactory, atMost(1))
        .newByteBuffer(any(InputStream.class), eq(TEST_DATA_LENGTH));
  }

  @Test
  public void testFetchAssetResource() throws Exception {
    PooledByteBuffer pooledByteBuffer = mock(PooledByteBuffer.class);
    when(mAssetManager.open(eq(TEST_FILENAME), eq(AssetManager.ACCESS_STREAMING)))
        .thenReturn(new ByteArrayInputStream(new byte[TEST_DATA_LENGTH]));
    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class), eq(TEST_DATA_LENGTH)))
        .thenReturn(pooledByteBuffer);

    mLocalAssetFetchProducer.produceResults(mConsumer, mProducerContext);
    mExecutor.runUntilIdle();
  }

  @Test(expected = RuntimeException.class)
  public void testFetchLocalResourceFailsByThrowing() throws Exception {
    when(mAssetManager.open(eq(TEST_FILENAME), eq(AssetManager.ACCESS_STREAMING)))
        .thenThrow(mException);
    mLocalAssetFetchProducer.produceResults(mConsumer, mProducerContext);
    mExecutor.runUntilIdle();
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        mRequestId, PRODUCER_NAME, mException, null);
  }

}
