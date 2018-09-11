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
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
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
public class CloseableProducerToDataSourceAdapterTest {

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

  private ResourceReleaser mResourceReleaser;
  private CloseableReference<Object> mResultRef1;
  private CloseableReference<Object> mResultRef2;
  private CloseableReference<Object> mResultRef3;
  private Exception mException;

  private DataSubscriber<CloseableReference<Object>> mDataSubscriber1;
  private DataSubscriber<CloseableReference<Object>> mDataSubscriber2;

  private SettableProducerContext mSettableProducerContext;
  private Producer<CloseableReference<Object>> mProducer;
  private Consumer<CloseableReference<Object>> mInternalConsumer;

  private DataSource<CloseableReference<Object>> mDataSource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mResourceReleaser = mock(ResourceReleaser.class);
    mResultRef1 = CloseableReference.of(new Object(), mResourceReleaser);
    mResultRef2 = CloseableReference.of(new Object(), mResourceReleaser);
    mResultRef3 = CloseableReference.of(new Object(), mResourceReleaser);
    mException = mock(Exception.class);

    mDataSubscriber1 = mock(DataSubscriber.class);
    mDataSubscriber2 = mock(DataSubscriber.class);

    mSettableProducerContext = mock(SettableProducerContext.class);
    when(mSettableProducerContext.getId()).thenReturn(mRequestId);
    when(mSettableProducerContext.isPrefetch()).thenReturn(false);
    mProducer = mock(Producer.class);
    mDataSource = CloseableProducerToDataSourceAdapter.create(
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

  /* reference assertions */

  private static <T> void assertReferenceCount(int expectedCount, CloseableReference<T> ref) {
    assertEquals(expectedCount, ref.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  private static <T> void assertReferencesSame(
      String errorMessage,
      CloseableReference<T> expectedRef,
      CloseableReference<T> actualRef) {
    if (expectedRef == null) {
      assertNull(errorMessage, actualRef);
    } else {
      assertSame(errorMessage, expectedRef.get(), actualRef.get());
    }
  }

  /* verification helpers */

  private void verifyState(
      boolean isFinished,
      boolean hasResult,
      CloseableReference<Object> resultRef,
      boolean hasFailed,
      Throwable failureCause) {
    DataSource<CloseableReference<Object>> dataSource = mDataSource;
    assertEquals("isFinished", isFinished, dataSource.isFinished());
    assertEquals("hasResult", hasResult, dataSource.hasResult());
    CloseableReference<Object> dataSourceRef = dataSource.getResult();
    assertReferencesSame("getResult", resultRef, dataSourceRef);
    CloseableReference.closeSafely(dataSourceRef);
    assertEquals("hasFailed", hasFailed, dataSource.hasFailed());
    if (failureCause == NPE) {
      assertNotNull("failure", dataSource.getFailureCause());
      assertSame("failure", NullPointerException.class, dataSource.getFailureCause().getClass());
    } else {
      assertSame("failure", failureCause, dataSource.getFailureCause());
    }
  }

  private void verifyReferenceCount(CloseableReference<Object> resultRef) {
    // this unit test class keeps references alive, so their ref count must be 1;
    // except for the result which have ref count of 2 because it's also kept by data source
    assertReferenceCount((resultRef == mResultRef1) ? 2 : 1, mResultRef1);
    assertReferenceCount((resultRef == mResultRef2) ? 2 : 1, mResultRef2);
    assertReferenceCount((resultRef == mResultRef3) ? 2 : 1, mResultRef3);
  }

  private void verifyNoMoreInteractionsAndReset() {
    verifyNoMoreInteractions(mRequestListener, mDataSubscriber1, mDataSubscriber2);
    reset(mRequestListener, mDataSubscriber1, mDataSubscriber2);
  }

  /* state verification methods */

  private void verifyInitial() {
    verifyState(NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null);
    verifyReferenceCount(null);
    verifyNoMoreInteractionsAndReset();
  }

  private void verifyWithResult(CloseableReference<Object> resultRef, boolean isLast) {
    verifyState(isLast, resultRef != null, resultRef, NOT_FAILED, null);
    verifyReferenceCount(resultRef);
    verifyNoMoreInteractionsAndReset();
  }

  private void verifyFailed(CloseableReference<Object> resultRef, Throwable throwable) {
    verifyState(FINISHED, resultRef != null, resultRef, FAILED, throwable);
    verifyReferenceCount(resultRef);
    verifyNoMoreInteractionsAndReset();
  }

  private void verifyClosed(boolean isFinished, Throwable throwable) {
    verifyState(isFinished, WITHOUT_RESULT, null, throwable != null, throwable);
    verifyReferenceCount(null);
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
      CloseableReference<Object> resultRef,
      boolean isLast,
      int numSubscribers) {
    mInternalConsumer.onNewResult(resultRef, BaseConsumer.simpleStatusForIsLast(isLast));
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
    verifyWithResult(resultRef, isLast);
  }

  private void testFailure(CloseableReference<Object> resultRef, int numSubscribers) {
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
    verifyFailed(resultRef, mException);
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
    mInternalConsumer.onNewResult(mResultRef2, Consumer.NO_FLAGS);
    verifyClosed(NOT_FINISHED, null);
    testSubscribe(NO_INTERACTIONS);
  }

  @Test
  public void test_C_L_a() {
    testClose(NOT_FINISHED, 1);
    mInternalConsumer.onNewResult(mResultRef2, Consumer.IS_LAST);
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
    testNewResult(mResultRef1, INTERMEDIATE, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(NOT_FINISHED, 2);
  }

  @Test
  public void test_I_I_a_C() {
    testNewResult(mResultRef1, INTERMEDIATE, 1);
    testNewResult(mResultRef2, INTERMEDIATE, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(NOT_FINISHED, 2);
  }

  @Test
  public void test_I_I_L_a_C() {
    testNewResult(mResultRef1, INTERMEDIATE, 1);
    testNewResult(mResultRef2, INTERMEDIATE, 1);
    testNewResult(mResultRef3, LAST, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_I_I_F_a_C() {
    testNewResult(mResultRef1, INTERMEDIATE, 1);
    testNewResult(mResultRef2, INTERMEDIATE, 1);
    testFailure(mResultRef2, 1);
    testSubscribe(ON_FAILURE);
    testClose(mException);
  }

  @Test
  public void test_I_L_a_C() {
    testNewResult(mResultRef1, INTERMEDIATE, 1);
    testNewResult(mResultRef2, LAST, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_I_F_a_C() {
    testNewResult(mResultRef1, INTERMEDIATE, 1);
    testFailure(mResultRef1, 1);
    testSubscribe(ON_FAILURE);
    testClose(mException);
  }

  @Test
  public void test_L_a_C() {
    testNewResult(mResultRef1, LAST, 1);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_L_I_a_C() {
    testNewResult(mResultRef1, LAST, 1);
    mInternalConsumer.onNewResult(mResultRef2, Consumer.NO_FLAGS);
    verifyWithResult(mResultRef1, LAST);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_L_L_a_C() {
    testNewResult(mResultRef1, LAST, 1);
    mInternalConsumer.onNewResult(mResultRef2, Consumer.IS_LAST);
    verifyWithResult(mResultRef1, LAST);
    testSubscribe(ON_NEW_RESULT);
    testClose(FINISHED, 2);
  }

  @Test
  public void test_L_F_a_C() {
    testNewResult(mResultRef1, LAST, 1);
    mInternalConsumer.onFailure(mException);
    verifyWithResult(mResultRef1, LAST);
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
    mInternalConsumer.onNewResult(mResultRef1, Consumer.NO_FLAGS);
    verifyFailed(null, mException);
    testSubscribe(ON_FAILURE);
    testClose(mException);
  }

  @Test
  public void test_F_L_a_C() {
    testFailure(null, 1);
    mInternalConsumer.onNewResult(mResultRef1, Consumer.IS_LAST);
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

    testNewResult(mResultRef1, LAST, 1);
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
    testNewResult(mResultRef1, INTERMEDIATE, 1);

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
