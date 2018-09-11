/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

/**
 * Basic tests for LocalFileFetchProducer
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class LocalFileFetchProducerTest {
  private static final String PRODUCER_NAME = LocalFileFetchProducer.PRODUCER_NAME;
  private static final int INPUT_STREAM_LENGTH = 100;
  private static final String TEST_FILENAME = "dummy.jpg";
  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  private TestExecutorService mExecutor;
  private SettableProducerContext mProducerContext;
  private final String mRequestId = "mRequestId";
  private File mFile;
  private LocalFileFetchProducer mLocalFileFetchProducer;
  private EncodedImage mCapturedEncodedImage;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mExecutor = new TestExecutorService(new FakeClock());
    mLocalFileFetchProducer =
        new LocalFileFetchProducer(mExecutor, mPooledByteBufferFactory);
    mFile = new File(RuntimeEnvironment.application.getExternalFilesDir(null), TEST_FILENAME);
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mFile));
    bos.write(new byte[INPUT_STREAM_LENGTH], 0 , INPUT_STREAM_LENGTH);
    bos.close();

    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    when(mImageRequest.getSourceFile()).thenReturn(mFile);
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
  public void testLocalFileFetchCancelled() {
    mLocalFileFetchProducer.produceResults(mConsumer, mProducerContext);
    mProducerContext.cancel();
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithCancellation(mRequestId, PRODUCER_NAME, null);
    verify(mConsumer).onCancellation();
    mExecutor.runUntilIdle();
    verifyZeroInteractions(mPooledByteBufferFactory);
  }

  @Test
  public void testFetchLocalFile() throws Exception {
    PooledByteBuffer pooledByteBuffer = mock(PooledByteBuffer.class);
    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class), eq(INPUT_STREAM_LENGTH)))
        .thenReturn(pooledByteBuffer);
    mLocalFileFetchProducer.produceResults(mConsumer, mProducerContext);
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
  public void testFetchLocalFileFailsByThrowing() throws Exception {
    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class), eq(INPUT_STREAM_LENGTH)))
        .thenThrow(mException);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        mRequestId, PRODUCER_NAME, mException, null);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, false);
  }

  @After
  public void tearDown() throws Exception {
    mFile.delete();
  }
}
