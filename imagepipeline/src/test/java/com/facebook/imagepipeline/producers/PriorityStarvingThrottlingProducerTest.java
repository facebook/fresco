/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.producers.PriorityStarvingThrottlingProducer.Item;
import com.facebook.imagepipeline.producers.PriorityStarvingThrottlingProducer.PriorityComparator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PriorityStarvingThrottlingProducerTest {
  private static final String PRODUCER_NAME = PriorityStarvingThrottlingProducer.PRODUCER_NAME;
  private static final int MAX_SIMULTANEOUS_REQUESTS = 2;
  private final Consumer<Object>[] mConsumers = new Consumer[7];
  private final ProducerContext[] mProducerContexts = new ProducerContext[7];
  private final ProducerListener2[] mProducerListeners = new ProducerListener2[7];
  private final String[] mRequestIds = new String[7];
  private final Consumer<Object>[] mThrottlerConsumers = new Consumer[7];
  private final Object[] mResults = new Object[7];
  @Mock public Producer<Object> mInputProducer;
  @Mock public Exception mException;
  private PriorityStarvingThrottlingProducer<Object> mThrottlingProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mThrottlingProducer =
        new PriorityStarvingThrottlingProducer<>(
            MAX_SIMULTANEOUS_REQUESTS, CallerThreadExecutor.getInstance(), mInputProducer);
    for (int i = 0; i < 7; i++) {
      mConsumers[i] = mock(Consumer.class);
      mProducerContexts[i] = mock(ProducerContext.class);
      mProducerListeners[i] = mock(ProducerListener2.class);
      mResults[i] = mock(Object.class);
      when(mProducerContexts[i].getProducerListener()).thenReturn(mProducerListeners[i]);
      when(mProducerContexts[i].getId()).thenReturn(mRequestIds[i]);
      final int iFinal = i;
      doAnswer(
              new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                  mThrottlerConsumers[iFinal] = (Consumer<Object>) invocation.getArguments()[0];
                  return null;
                }
              })
          .when(mInputProducer)
          .produceResults(any(Consumer.class), eq(mProducerContexts[i]));
    }
  }

  @Test
  public void testThrottling() {
    // First two requests are passed on immediately
    mThrottlingProducer.produceResults(mConsumers[0], mProducerContexts[0]);
    assertNotNull(mThrottlerConsumers[0]);
    verify(mProducerListeners[0]).onProducerStart(mProducerContexts[0], PRODUCER_NAME);
    verify(mProducerListeners[0])
        .onProducerFinishWithSuccess(mProducerContexts[0], PRODUCER_NAME, null);

    mThrottlingProducer.produceResults(mConsumers[1], mProducerContexts[1]);
    assertNotNull(mThrottlerConsumers[1]);
    verify(mProducerListeners[1]).onProducerStart(mProducerContexts[1], PRODUCER_NAME);
    verify(mProducerListeners[1])
        .onProducerFinishWithSuccess(mProducerContexts[1], PRODUCER_NAME, null);

    // Third and fourth requests are queued up
    mThrottlingProducer.produceResults(mConsumers[2], mProducerContexts[2]);
    assertNull(mThrottlerConsumers[2]);
    verify(mProducerListeners[2]).onProducerStart(mProducerContexts[2], PRODUCER_NAME);
    verify(mProducerListeners[2], never())
        .onProducerFinishWithSuccess(mProducerContexts[2], PRODUCER_NAME, null);

    mThrottlingProducer.produceResults(mConsumers[3], mProducerContexts[3]);
    assertNull(mThrottlerConsumers[3]);
    verify(mProducerListeners[3]).onProducerStart(mProducerContexts[3], PRODUCER_NAME);
    verify(mProducerListeners[3], never())
        .onProducerFinishWithSuccess(mProducerContexts[3], PRODUCER_NAME, null);

    // First request fails, third request is kicked off, fourth request remains in queue
    mThrottlerConsumers[0].onFailure(mException);
    verify(mConsumers[0]).onFailure(mException);
    assertNotNull(mThrottlerConsumers[2]);
    verify(mProducerListeners[2])
        .onProducerFinishWithSuccess(mProducerContexts[2], PRODUCER_NAME, null);
    assertNull(mThrottlerConsumers[3]);
    verify(mProducerListeners[3], never())
        .onProducerFinishWithSuccess(mProducerContexts[3], PRODUCER_NAME, null);

    // Fifth request is queued up
    mThrottlingProducer.produceResults(mConsumers[4], mProducerContexts[4]);
    assertNull(mThrottlerConsumers[4]);
    verify(mProducerListeners[4]).onProducerStart(mProducerContexts[4], PRODUCER_NAME);
    verify(mProducerListeners[4], never())
        .onProducerFinishWithSuccess(mProducerContexts[4], PRODUCER_NAME, null);

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
    verify(mProducerListeners[3])
        .onProducerFinishWithSuccess(mProducerContexts[3], PRODUCER_NAME, null);
    assertNull(mThrottlerConsumers[4]);

    // Second request is cancelled, fifth request is kicked off
    mThrottlerConsumers[1].onCancellation();
    verify(mConsumers[1]).onCancellation();
    assertNotNull(mThrottlerConsumers[4]);
    verify(mProducerListeners[4])
        .onProducerFinishWithSuccess(mProducerContexts[4], PRODUCER_NAME, null);

    // Fourth and fifth requests finish
    mThrottlerConsumers[3].onNewResult(mResults[3], Consumer.IS_LAST);
    mThrottlerConsumers[4].onNewResult(mResults[4], Consumer.IS_LAST);
  }

  @Test
  public void testNoThrottlingAfterRequestsFinish() {
    // First two requests are passed on immediately
    mThrottlingProducer.produceResults(mConsumers[0], mProducerContexts[0]);
    assertNotNull(mThrottlerConsumers[0]);
    verify(mProducerListeners[0]).onProducerStart(mProducerContexts[0], PRODUCER_NAME);
    verify(mProducerListeners[0])
        .onProducerFinishWithSuccess(mProducerContexts[0], PRODUCER_NAME, null);

    mThrottlingProducer.produceResults(mConsumers[1], mProducerContexts[1]);
    assertNotNull(mThrottlerConsumers[1]);
    verify(mProducerListeners[1]).onProducerStart(mProducerContexts[1], PRODUCER_NAME);
    verify(mProducerListeners[1])
        .onProducerFinishWithSuccess(mProducerContexts[1], PRODUCER_NAME, null);

    // First two requests finish
    mThrottlerConsumers[0].onNewResult(mResults[3], Consumer.IS_LAST);
    mThrottlerConsumers[1].onNewResult(mResults[4], Consumer.IS_LAST);

    // Next two requests are passed on immediately
    mThrottlingProducer.produceResults(mConsumers[2], mProducerContexts[2]);
    assertNotNull(mThrottlerConsumers[2]);
    verify(mProducerListeners[2]).onProducerStart(mProducerContexts[2], PRODUCER_NAME);
    verify(mProducerListeners[2])
        .onProducerFinishWithSuccess(mProducerContexts[2], PRODUCER_NAME, null);

    mThrottlingProducer.produceResults(mConsumers[3], mProducerContexts[3]);
    assertNotNull(mThrottlerConsumers[3]);
    verify(mProducerListeners[3]).onProducerStart(mProducerContexts[3], PRODUCER_NAME);
    verify(mProducerListeners[3])
        .onProducerFinishWithSuccess(mProducerContexts[3], PRODUCER_NAME, null);

    // Next two requests finish
    mThrottlerConsumers[2].onNewResult(mResults[3], Consumer.IS_LAST);
    mThrottlerConsumers[3].onNewResult(mResults[4], Consumer.IS_LAST);
  }

  @Test
  public void testPriority() {

    // first two, priority order by insertion
    // 0, 1, 4, 6, 2, 3, 5
    when(mProducerContexts[0].getPriority()).thenReturn(Priority.MEDIUM);
    when(mProducerContexts[1].getPriority()).thenReturn(Priority.MEDIUM);
    when(mProducerContexts[2].getPriority()).thenReturn(Priority.MEDIUM);
    when(mProducerContexts[3].getPriority()).thenReturn(Priority.LOW);
    when(mProducerContexts[4].getPriority()).thenReturn(Priority.HIGH);
    when(mProducerContexts[5].getPriority()).thenReturn(Priority.LOW);
    when(mProducerContexts[6].getPriority()).thenReturn(Priority.HIGH);

    for (int i = 0; i < 7; i++) {
      mThrottlingProducer.produceResults(mConsumers[i], mProducerContexts[i]);
    }
    // First two requests finish
    mThrottlerConsumers[0].onNewResult(mResults[0], Consumer.IS_LAST);
    mThrottlerConsumers[1].onNewResult(mResults[1], Consumer.IS_LAST);

    verifyNextCalledForIndex(4);
    mThrottlerConsumers[4].onNewResult(mResults[1], Consumer.IS_LAST);

    verifyNextCalledForIndex(6);
    mThrottlerConsumers[6].onNewResult(mResults[1], Consumer.IS_LAST);
    verifyNextCalledForIndex(2);
    mThrottlerConsumers[2].onNewResult(mResults[1], Consumer.IS_LAST);
    verifyNextCalledForIndex(3);
    mThrottlerConsumers[3].onNewResult(mResults[1], Consumer.IS_LAST);
    verifyNextCalledForIndex(5);
    mThrottlerConsumers[5].onNewResult(mResults[1], Consumer.IS_LAST);
  }

  @Test
  public void testSamePriorityLowerTimeGoesFirst() {
    int result = compareItems(Priority.MEDIUM, 0, Priority.MEDIUM, 1);
    // pq is a min heap. smaller goes first, so if compareItems < 0, item 1 runs before item 2
    assertTrue(result < 0);
  }

  @Test
  public void testSamePriorityLowerTimeGoesFirstBothOrder() {
    int result = compareItems(Priority.MEDIUM, 1, Priority.MEDIUM, 0);
    assertTrue(result > 0);
  }

  @Test
  public void testHigherPrioritySameTimeGoesFirst() {
    int result = compareItems(Priority.MEDIUM, 0, Priority.HIGH, 0);
    assertTrue(result > 0);
  }

  @Test
  public void testHigherPriorityLaterTimeGoesFirst() {
    int result = compareItems(Priority.MEDIUM, 0, Priority.HIGH, 10);
    assertTrue(result > 0);
  }

  @Test
  public void testHigherPriorityEarlierTimeGoesFirst() {
    int result = compareItems(Priority.MEDIUM, 10, Priority.HIGH, 0);
    assertTrue(result > 0);
  }

  @Test
  public void testHigherPriorityEarlierTimeGoesFirstBothOrder() {
    int result = compareItems(Priority.HIGH, 0, Priority.MEDIUM, 10);
    assertTrue(result < 0);
  }

  @Test
  public void testSamePrioritySameOrderIsEqual() {
    int result = compareItems(Priority.HIGH, 0, Priority.HIGH, 0);
    assertEquals(result, 0);
  }

  private static int compareItems(Priority pri1, long time1, Priority pri2, long time2) {
    PriorityComparator pc = new PriorityComparator();
    Consumer<Object> consumer1 = mock(Consumer.class);
    ProducerContext context1 = mock(ProducerContext.class);
    when(context1.getPriority()).thenReturn(pri1);
    Consumer<Object> consumer2 = mock(Consumer.class);
    ProducerContext context2 = mock(ProducerContext.class);
    when(context2.getPriority()).thenReturn(pri2);
    Item<Object> item1 = new Item<>(consumer1, context1, time1);
    Item<Object> item2 = new Item<>(consumer2, context2, time2);
    return pc.compare(item1, item2);
  }

  private void verifyNextCalledForIndex(int val) {
    verify(mProducerListeners[val])
        .onProducerFinishWithSuccess(mProducerContexts[val], PRODUCER_NAME, null);
  }
}
