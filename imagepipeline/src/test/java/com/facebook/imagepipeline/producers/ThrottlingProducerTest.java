/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.facebook.common.executors.CallerThreadExecutor;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class ThrottlingProducerTest {
  private static final String PRODUCER_NAME = ThrottlingProducer.PRODUCER_NAME;
  private static final int MAX_SIMULTANEOUS_REQUESTS = 2;

  @Mock public Producer<Object> mInputProducer;
  @Mock public Exception mException;

  private final Consumer<Object>[] mConsumers = new Consumer[5];
  private final ProducerContext[] mProducerContexts = new ProducerContext[5];
  private final ProducerListener[] mProducerListeners = new ProducerListener[5];
  private final String[] mRequestIds = new String[5];
  private final Consumer<Object>[] mThrottlerConsumers = new Consumer[5];
  private final Object[] mResults = new Object[5];
  private ThrottlingProducer<Object> mThrottlingProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mThrottlingProducer = new ThrottlingProducer<Object>(
        MAX_SIMULTANEOUS_REQUESTS,
        CallerThreadExecutor.getInstance(),
        mInputProducer);
    for (int i = 0; i < 5; i++) {
      mConsumers[i] = mock(Consumer.class);
      mProducerContexts[i] = mock(ProducerContext.class);
      mProducerListeners[i] = mock(ProducerListener.class);
      mRequestIds[i] = Integer.toString(i);
      mResults[i] = mock(Object.class);
      when(mProducerContexts[i].getListener()).thenReturn(mProducerListeners[i]);
      when(mProducerContexts[i].getId()).thenReturn(mRequestIds[i]);
      final int iFinal = i;
      doAnswer(
          new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
              mThrottlerConsumers[iFinal] =
                  (Consumer<Object>) invocation.getArguments()[0];
              return null;
            }
          }).when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContexts[i]));
    }
  }

  @Test
  public void testThrottling() {
    // First two requests are passed on immediately
    mThrottlingProducer.produceResults(mConsumers[0], mProducerContexts[0]);
    assertNotNull(mThrottlerConsumers[0]);
    verify(mProducerListeners[0]).onProducerStart(mRequestIds[0], PRODUCER_NAME);
    verify(mProducerListeners[0]).onProducerFinishWithSuccess(mRequestIds[0], PRODUCER_NAME, null);

    mThrottlingProducer.produceResults(mConsumers[1], mProducerContexts[1]);
    assertNotNull(mThrottlerConsumers[1]);
    verify(mProducerListeners[1]).onProducerStart(mRequestIds[1], PRODUCER_NAME);
    verify(mProducerListeners[1]).onProducerFinishWithSuccess(mRequestIds[1], PRODUCER_NAME, null);

    // Third and fourth requests are queued up
    mThrottlingProducer.produceResults(mConsumers[2], mProducerContexts[2]);
    assertNull(mThrottlerConsumers[2]);
    verify(mProducerListeners[2]).onProducerStart(mRequestIds[2], PRODUCER_NAME);
    verify(mProducerListeners[2], never())
        .onProducerFinishWithSuccess(mRequestIds[2], PRODUCER_NAME, null);

    mThrottlingProducer.produceResults(mConsumers[3], mProducerContexts[3]);
    assertNull(mThrottlerConsumers[3]);
    verify(mProducerListeners[3]).onProducerStart(mRequestIds[3], PRODUCER_NAME);
    verify(mProducerListeners[3], never())
        .onProducerFinishWithSuccess(mRequestIds[3], PRODUCER_NAME, null);

    // First request fails, third request is kicked off, fourth request remains in queue
    mThrottlerConsumers[0].onFailure(mException);
    verify(mConsumers[0]).onFailure(mException);
    assertNotNull(mThrottlerConsumers[2]);
    verify(mProducerListeners[2]).onProducerFinishWithSuccess(mRequestIds[2], PRODUCER_NAME, null);
    assertNull(mThrottlerConsumers[3]);
    verify(mProducerListeners[3], never())
        .onProducerFinishWithSuccess(mRequestIds[3], PRODUCER_NAME, null);

    // Fifth request is queued up
    mThrottlingProducer.produceResults(mConsumers[4], mProducerContexts[4]);
    assertNull(mThrottlerConsumers[4]);
    verify(mProducerListeners[4]).onProducerStart(mRequestIds[4], PRODUCER_NAME);
    verify(mProducerListeners[4], never())
        .onProducerFinishWithSuccess(mRequestIds[4], PRODUCER_NAME, null);

    // Second request gives intermediate result, no new request is kicked off
    Object intermediateResult = mock(Object.class);
    mThrottlerConsumers[1].onNewResult(intermediateResult, Consumer.NO_FLAGS);
    verify(mConsumers[1]).onNewResult(intermediateResult, Consumer.NO_FLAGS);
    assertNull(mThrottlerConsumers[3]);
    assertNull(mThrottlerConsumers[4]);

    // Third request finishes, fourth request is kicked off
    mThrottlerConsumers[2].onNewResult(mResults[2], Consumer.IS_LAST);
    verify(mConsumers[2]).onNewResult(mResults[2], Consumer.IS_LAST);
    assertNotNull(mThrottlerConsumers[3]);
    verify(mProducerListeners[3]).onProducerFinishWithSuccess(mRequestIds[3], PRODUCER_NAME, null);
    assertNull(mThrottlerConsumers[4]);

    // Second request is cancelled, fifth request is kicked off
    mThrottlerConsumers[1].onCancellation();
    verify(mConsumers[1]).onCancellation();
    assertNotNull(mThrottlerConsumers[4]);
    verify(mProducerListeners[4]).onProducerFinishWithSuccess(mRequestIds[4], PRODUCER_NAME, null);

    // Fourth and fifth requests finish
    mThrottlerConsumers[3].onNewResult(mResults[3], Consumer.IS_LAST);
    mThrottlerConsumers[4].onNewResult(mResults[4], Consumer.IS_LAST);
  }

  @Test
  public void testNoThrottlingAfterRequestsFinish() {
    // First two requests are passed on immediately
    mThrottlingProducer.produceResults(mConsumers[0], mProducerContexts[0]);
    assertNotNull(mThrottlerConsumers[0]);
    verify(mProducerListeners[0]).onProducerStart(mRequestIds[0], PRODUCER_NAME);
    verify(mProducerListeners[0]).onProducerFinishWithSuccess(mRequestIds[0], PRODUCER_NAME, null);

    mThrottlingProducer.produceResults(mConsumers[1], mProducerContexts[1]);
    assertNotNull(mThrottlerConsumers[1]);
    verify(mProducerListeners[1]).onProducerStart(mRequestIds[1], PRODUCER_NAME);
    verify(mProducerListeners[1]).onProducerFinishWithSuccess(mRequestIds[1], PRODUCER_NAME, null);

    // First two requests finish
    mThrottlerConsumers[0].onNewResult(mResults[3], Consumer.IS_LAST);
    mThrottlerConsumers[1].onNewResult(mResults[4], Consumer.IS_LAST);

    // Next two requests are passed on immediately
    mThrottlingProducer.produceResults(mConsumers[2], mProducerContexts[2]);
    assertNotNull(mThrottlerConsumers[2]);
    verify(mProducerListeners[2]).onProducerStart(mRequestIds[2], PRODUCER_NAME);
    verify(mProducerListeners[2]).onProducerFinishWithSuccess(mRequestIds[2], PRODUCER_NAME, null);

    mThrottlingProducer.produceResults(mConsumers[3], mProducerContexts[3]);
    assertNotNull(mThrottlerConsumers[3]);
    verify(mProducerListeners[3]).onProducerStart(mRequestIds[3], PRODUCER_NAME);
    verify(mProducerListeners[3]).onProducerFinishWithSuccess(mRequestIds[3], PRODUCER_NAME, null);

    // Next two requests finish
    mThrottlerConsumers[2].onNewResult(mResults[3], Consumer.IS_LAST);
    mThrottlerConsumers[3].onNewResult(mResults[4], Consumer.IS_LAST);
  }
}
