/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.Mockito.*;

import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class ThreadHandoffProducerTest {
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;

  private final String mRequestId = "mRequestId";
  private SettableProducerContext mProducerContext;
  private ThreadHandoffProducer mThreadHandoffProducer;
  private TestExecutorService mTestExecutorService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    mTestExecutorService = new TestExecutorService(new FakeClock());
    mThreadHandoffProducer = new ThreadHandoffProducer(
        mInputProducer,
        new ThreadHandoffProducerQueue(mTestExecutorService));
  }

  @Test
  public void testSuccess() {
    mThreadHandoffProducer.produceResults(mConsumer, mProducerContext);
    mTestExecutorService.runUntilIdle();
    verify(mInputProducer).produceResults(mConsumer, mProducerContext);
    verify(mProducerListener).onProducerStart(mRequestId, ThreadHandoffProducer.PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId,
        ThreadHandoffProducer.PRODUCER_NAME,
        null);
    verifyNoMoreInteractions(mProducerListener);
  }

  @Test
  public void testCancellation() {
    mThreadHandoffProducer.produceResults(mConsumer, mProducerContext);
    mProducerContext.cancel();
    mTestExecutorService.runUntilIdle();
    verify(mInputProducer, never()).produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onCancellation();
    verify(mProducerListener).onProducerStart(mRequestId, ThreadHandoffProducer.PRODUCER_NAME);
    verify(mProducerListener).requiresExtraMap(mRequestId);
    verify(mProducerListener).onProducerFinishWithCancellation(
        mRequestId,
        ThreadHandoffProducer.PRODUCER_NAME,
        null);
    verifyNoMoreInteractions(mProducerListener);
  }
}
