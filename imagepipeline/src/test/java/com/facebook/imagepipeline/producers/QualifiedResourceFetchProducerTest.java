/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.net.Uri;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Basic tests for QualifiedResourceFetchProducer
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class QualifiedResourceFetchProducerTest {

  private static final String PRODUCER_NAME = QualifiedResourceFetchProducer.PRODUCER_NAME;

  private static final String PACKAGE_NAME = "com.myapp.myplugin";
  private static final int RESOURCE_ID = 42;

  private static final String REQUEST_ID = "requestId";
  private static final String CALLER_CONTEXT = "callerContext";

  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public ContentResolver mContentResolver;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;

  private TestExecutorService mExecutor;
  private SettableProducerContext mProducerContext;
  private Uri mContentUri;
  private QualifiedResourceFetchProducer mQualifiedResourceFetchProducer;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mExecutor = new TestExecutorService(new FakeClock());
    mQualifiedResourceFetchProducer = new QualifiedResourceFetchProducer(
        mExecutor,
        mPooledByteBufferFactory,
        mContentResolver);
    mContentUri = UriUtil.getUriForQualifiedResource(PACKAGE_NAME, RESOURCE_ID);

    mProducerContext = new SettableProducerContext(
        mImageRequest,
        REQUEST_ID,
        mProducerListener,
        CALLER_CONTEXT,
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    when(mImageRequest.getSourceUri()).thenReturn(mContentUri);
  }

  @Test
  public void testQualifiedResourceUri() throws Exception {
    PooledByteBuffer pooledByteBuffer = mock(PooledByteBuffer.class);
    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class)))
        .thenReturn(pooledByteBuffer);

    when(mContentResolver.openInputStream(mContentUri))
        .thenReturn(mock(InputStream.class));

    mQualifiedResourceFetchProducer.produceResults(mConsumer, mProducerContext);
    mExecutor.runUntilIdle();

    verify(mPooledByteBufferFactory, times(1)).newByteBuffer(any(InputStream.class));
    verify(mContentResolver, times(1)).openInputStream(mContentUri);

    verify(mProducerListener).onProducerStart(REQUEST_ID, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(REQUEST_ID, PRODUCER_NAME, null);
  }
}
