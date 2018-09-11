/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.producers.BaseConsumer;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;

@RunWith(RobolectricTestRunner.class)
public class ProducerToDataSourceAdapterTest {

  @Mock public RequestListener mRequestListener;

  private static final boolean FINISHED = true;
  private static final boolean NOT_FINISHED = false;
  private static final boolean WITH_RESULT = true;
  private static final boolean WITHOUT_RESULT = false;
  private static final boolean FAILED = true;
  private static final boolean NOT_FAILED = false;
  private static final boolean LAST = true;
  private static final boolean INTERMEDIATE = false;
  private static final int NO_INTERACTIONS = 0;
  private static final int ON_NEW_RESULT = 1;
  private static final int ON_FAILURE = 2;

  private static final Exception NPE = new NullPointerException();
  private static final String mRequestId = "requestId";

  private Object mResult1;
  private Object mResult2;
  private Object mResult3;
  private Exception mException;

  private DataSubscriber<Object> mDataSubscriber1;
  private DataSubscriber<Object> mDataSubscriber2;

  private SettableProducerContext mSettableProducerContext;
  private Producer<Object> mProducer;
  private Consumer<Object> mInternalConsumer;

  private DataSource<Object> mDataSource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mResult1 = new Object();
    mResult2 = new Object();
    mResult3 = new Object();
    mException = mock(Exception.class);

    mDataSubscriber1 = mock(DataSubscriber.class);
    mDataSubscriber2 = mock(DataSubscriber.class);

    mSettableProducerContext = mock(SettableProducerContext.class);
    when(mSettableProducerContext.getId()).thenReturn(mRequestId);
    when(mSettableProducerContext.isPrefetch()).thenReturn(true);
    mProducer = mock(Producer.class);
    mDataSource = ProducerToDataSourceAdapter.create(
        mProducer,
        mSettableProducerContext,
        mRequestListener);
    ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
    verify(mRequestListener).onRequestStart(
        mSettableProducerContext.getImageRequest(),
        mSettableProducerContext.getCallerContext(),
        mRequestId,
        mSettableProducerContext.isPrefetch());
    verify(mProducer).produceResults(captor.capture(), any(SettableProducerContext.class));
    mInternalConsumer = captor.getValue();

    mDataSource.subscribe(mDataSubscriber1, CallerThreadExecutor.getInstance());
  }

  /* verification helpers */

  private void verifyState(
      boolean isFinished,
      boolean hasResult,
      Object result,
      boolean hasFailed,
      Throwable failureCause) {
    DataSource<Object> dataSource = mDataSource;
    assertEquals("isFinished", isFinished, dataSource.isFinished());
    assertEquals("hasResult", hasResult, dataSource.hasResult());
    assertSame("getResult", result, dataSource.getResult());
    assertEquals("hasFailed", hasFailed, dataSource.hasFailed());
    if (failureCause == NPE) {
      assertNotNull("failure", dataSource.getFailureCause());
      assertSame("failure", NullPointerException.class, dataSource.getFailureCause().getClass());
    } else {
      assertSame("failure", failureCause, dataSource.getFailureCause());
    }
  }

  private void verifyNoMoreInteractionsAndReset() {
    verifyNoMoreInteractions(mRequestListener, mDataSubscriber1, mDataSubscriber2);
    reset(mRequestListener, mDataSubscriber1, mDataSubscriber2);
  }

  /* state verification methods */

  private void verifyInitial() {
    verifyState(NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
    verifyNoMoreInteractionsAndReset();
  }

  private void verifyWithResult(Object result, boolean isLast) {
    verifyState(isLast, result != null, result, NOT_FAILED, null);
    verifyNoMoreInteractionsAndReset();
  }

  private void verifyFailed(Object result, Throwable throwable) {
    verifyState(FINISHED, result != null, result, FAILED, throwable);
    verifyNoMoreInteractionsAndReset();
  }

  private void verifyClosed(boolean isFinished, Throwable throwable) {
    verifyState(isFinished, WITHOUT_RESULT, null, throwable != null, throwable);
    verifyNoMoreInteractionsAndReset();
  }

  /* event testing helpers */

  private void testSubscribe(int expected) {
    mDataSource.subscribe(mDataSubscriber2, CallerThreadExecutor.getInstance());
    switch (expected) {
      case NO_INTERACTIONS:
        break;
      case ON_NEW_RESULT:
        verify(mDataSubscriber2).onNewResult(mDataSource);
        break;
      case ON_FAILURE:
        verify(mDataSubscriber2).onFailure(mDataSource);
        break;
    }
    verifyNoMoreInteractionsAndReset();
  }

  private void testNewResult(
      Object result,
      boolean isLast,
      int numSubscribers) {
    mInternalConsumer.onNewResult(result, BaseConsumer.simpleStatusForIsLast(isLast));
    if (isLast) {
      verify(mRequestListener).onRequestSuccess(
          mSettableProducerContext.getImageRequest(),
          mRequestId,
          mSettableProducerContext.isPrefetch());
    }
    if (numSubscribers >= 1) {
      verify(mDataSubscriber1).onNewResult(mDataSource);
    }
    if (numSubscribers >= 2) {
      verify(mDataSubscriber2).onNewResult(mDataSource);
    }
    verifyWithResult(result, isLast);
  }

  private void testFailure(Object result, int numSubscribers) {
    mInternalConsumer.onFailure(mException);
    verify(mRequestListener).onRequestFailure(
        mSettableProducerContext.getImageRequest(),
        mRequestId,
        mException,
        mSettableProducerContext.isPrefetch());
    if (numSubscribers >= 1) {
      verify(mDataSubscriber1).onFailure(mDataSource);
    }
    if (numSubscribers >= 2) {
      verify(mDataSubscriber2).onFailure(mDataSource);
    }
    verifyFailed(result, mException);
  }

  private void testClose(Throwable throwable) {
    mDataSource.close();
    verifyClosed(FINISHED, throwable);
  }

  private void testClose(boolean isFinished, int numSubscribers) {
    mDataSource.close();
    if (!isFinished) {
      verify(mRequestListener).onRequestCancellation(mRequestId);
      if (numSubscribers >= 1) {
        verify(mDataSubscriber1).onCancellation(mDataSource);
      }
      if (numSubscribers >= 2) {
        verify(mDataSubscriber2).onCancellation(mDataSource);
      }
    }
    verifyClosed(isFinished, null);
  }

  @Test
  public void testInitialState() {
    verifyInitial();
  }

  @Test
  public void test_C_a() {
    testClose(NOT_FINISHED, 1);
    testSubscribe(NO_INTERACTIONS);
  }

  @Test
  public void test_C_I_a() {
    testClose(NOT_FINISHED, 1);
    mInternalConsumer.onNewResult(mResult2, Consumer.NO_FLAGS);
    verifyClosed(NOT_FINISHED, null);
    testSubscribe(NO_INTERACTIONS);
  }

  @Test
  public void test_C_L_a() {
    testClose(NOT_FINISHED, 1);
    mInternalConsumer.onNewResult(mResult2, Consumer.IS_LAST);
    verifyClosed(NOT_FINISHED, null);
    testSubscribe(NO_INTERACTIONS);
  }

  @Test
  public void testC_F_a() {
    testClose(NOT_FINISHED, 1);
    mInternalConsumer.onFailure(mException);
    verifyClosed(NOT_FINISHED, null);
    testSubscribe(NO_INTERACTIONS);
  }

  @Test
  public void test_I_a_C() {
    testNewResult(mResult1, INTERMEDIATE, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(NOT_FINISHED, 2);
  }

  @Test
  public void test_I_I_a_C() {
    testNewResult(mResult1, INTERMEDIATE, 1);
    testNewResult(mResult2, INTERMEDIATE, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(NOT_FINISHED, 2);
  }

  @Test
  public void test_I_I_L_a_C() {
    testNewResult(mResult1, INTERMEDIATE, 1);
    testNewResult(mResult2, INTERMEDIATE, 1);
    testNewResult(mResult3, LAST, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_I_I_F_a_C() {
    testNewResult(mResult1, INTERMEDIATE, 1);
    testNewResult(mResult2, INTERMEDIATE, 1);
    testFailure(mResult2, 1);
    testSubscribe(ON_FAILURE);
    testClose(mException);
  }

  @Test
  public void test_I_L_a_C() {
    testNewResult(mResult1, INTERMEDIATE, 1);
    testNewResult(mResult2, LAST, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_I_F_a_C() {
    testNewResult(mResult1, INTERMEDIATE, 1);
    testFailure(mResult1, 1);
    testSubscribe(ON_FAILURE);
    testClose(mException);
  }

  @Test
  public void test_L_a_C() {
    testNewResult(mResult1, LAST, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_L_I_a_C() {
    testNewResult(mResult1, LAST, 1);
    mInternalConsumer.onNewResult(mResult2, Consumer.NO_FLAGS);
    verifyWithResult(mResult1, LAST);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_L_L_a_C() {
    testNewResult(mResult1, LAST, 1);
    mInternalConsumer.onNewResult(mResult2, Consumer.IS_LAST);
    verifyWithResult(mResult1, LAST);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_L_F_a_C() {
    testNewResult(mResult1, LAST, 1);
    mInternalConsumer.onFailure(mException);
    verifyWithResult(mResult1, LAST);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_F_a_C() {
    testFailure(null, 1);
    testSubscribe(ON_FAILURE);
    testClose(mException);
  }

  @Test
  public void test_F_I_a_C() {
    testFailure(null, 1);
    mInternalConsumer.onNewResult(mResult1, Consumer.NO_FLAGS);
    verifyFailed(null, mException);
    testSubscribe(ON_FAILURE);
    testClose(mException);
  }

  @Test
  public void test_F_L_a_C() {
    testFailure(null, 1);
    mInternalConsumer.onNewResult(mResult1, Consumer.IS_LAST);
    verifyFailed(null, mException);
    testSubscribe(ON_FAILURE);
    testClose(mException);
  }

  @Test
  public void test_F_F_a_C() {
    testFailure(null, 1);
    mInternalConsumer.onFailure(mock(Throwable.class));
    verifyFailed(null, mException);
    testSubscribe(ON_FAILURE);
    testClose(mException);
  }

  @Test
  public void test_NI_S_a_C() {
    mInternalConsumer.onNewResult(null, Consumer.NO_FLAGS);
    verify(mDataSubscriber1).onNewResult(mDataSource);
    verifyWithResult(null, INTERMEDIATE);

    testNewResult(mResult1, LAST, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_NI_a_NL_C() {
    mInternalConsumer.onNewResult(null, Consumer.NO_FLAGS);
    verify(mDataSubscriber1).onNewResult(mDataSource);
    verifyWithResult(null, INTERMEDIATE);

    testSubscribe(NO_INTERACTIONS);

    mInternalConsumer.onNewResult(null, Consumer.IS_LAST);
    verify(mRequestListener).onRequestSuccess(
        mSettableProducerContext.getImageRequest(),
        mRequestId,
        mSettableProducerContext.isPrefetch());
    verify(mDataSubscriber1).onNewResult(mDataSource);
    verify(mDataSubscriber2).onNewResult(mDataSource);
    verifyWithResult(null, LAST);

    testClose(FINISHED, 2);
  }

  @Test
  public void test_I_NL_a_C() {
    testNewResult(mResult1, INTERMEDIATE, 1);

    mInternalConsumer.onNewResult(null, Consumer.IS_LAST);
    verify(mRequestListener).onRequestSuccess(
        mSettableProducerContext.getImageRequest(),
        mRequestId,
        mSettableProducerContext.isPrefetch());
    verify(mDataSubscriber1).onNewResult(mDataSource);
    verifyWithResult(null, LAST);

    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }
}
