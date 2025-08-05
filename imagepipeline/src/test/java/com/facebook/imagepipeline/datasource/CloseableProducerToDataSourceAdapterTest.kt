/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource

import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSubscriber
import com.facebook.imagepipeline.listener.RequestListener2
import com.facebook.imagepipeline.producers.BaseConsumer
import com.facebook.imagepipeline.producers.Consumer
import com.facebook.imagepipeline.producers.Producer
import com.facebook.imagepipeline.producers.SettableProducerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CloseableProducerToDataSourceAdapterTest {
  private val requestListener: RequestListener2 = mock()
  private lateinit var resourceReleaser: ResourceReleaser<Any>
  private lateinit var resultRef1: CloseableReference<Any>
  private lateinit var resultRef2: CloseableReference<Any>
  private lateinit var resultRef3: CloseableReference<Any>
  private lateinit var exception: Exception
  private val dataSubscriber1: DataSubscriber<CloseableReference<Any>> = mock()
  private val dataSubscriber2: DataSubscriber<CloseableReference<Any>> = mock()
  private lateinit var settableProducerContext: SettableProducerContext
  private val producer: Producer<CloseableReference<Any>> = mock()
  private lateinit var internalConsumer: Consumer<CloseableReference<Any>>
  private lateinit var dataSource: DataSource<CloseableReference<Any>>

  @Before
  fun setUp() {
    resourceReleaser = mock()
    resultRef1 = CloseableReference.of(Any(), resourceReleaser)
    resultRef2 = CloseableReference.of(Any(), resourceReleaser)
    resultRef3 = CloseableReference.of(Any(), resourceReleaser)
    exception = mock()
    settableProducerContext = mock()

    dataSource =
        CloseableProducerToDataSourceAdapter.create(
            producer, settableProducerContext, requestListener)

    val captor = argumentCaptor<Consumer<CloseableReference<Any>>>()
    verify(requestListener).onRequestStart(settableProducerContext)
    verify(producer).produceResults(captor.capture(), any())
    internalConsumer = captor.firstValue

    dataSource.subscribe(dataSubscriber1, CallerThreadExecutor.getInstance())
  }

  /* verification helpers */
  private fun verifyState(
      isFinished: Boolean,
      hasResult: Boolean,
      resultRef: CloseableReference<Any>?,
      hasFailed: Boolean,
      failureCause: Throwable?
  ) {
    assertThat(dataSource.isFinished).isEqualTo(isFinished)
    assertThat(dataSource.hasResult()).isEqualTo(hasResult)
    val dataSourceRef = dataSource.result
    assertReferencesSame("getResult", resultRef, dataSourceRef)
    CloseableReference.closeSafely(dataSourceRef)
    assertThat(dataSource.hasFailed()).isEqualTo(hasFailed)
    if (failureCause === NPE) {
      assertThat(dataSource.failureCause).isNotNull()
      assertThat(dataSource.failureCause).isInstanceOf(NullPointerException::class.java)
    } else {
      assertThat(dataSource.failureCause).isSameAs(failureCause)
    }
  }

  private fun verifyReferenceCount(resultRef: CloseableReference<Any>?) {
    // this unit test class keeps references alive, so their ref count must be 1;
    // except for the result which have ref count of 2 because it's also kept by data source
    assertReferenceCount(if (resultRef === resultRef1) 2 else 1, resultRef1)
    assertReferenceCount(if (resultRef === resultRef2) 2 else 1, resultRef2)
    assertReferenceCount(if (resultRef === resultRef3) 2 else 1, resultRef3)
  }

  private fun verifyNoMoreInteractionsAndReset() {
    verifyNoMoreInteractions(requestListener, dataSubscriber1, dataSubscriber2)
    reset(requestListener, dataSubscriber1, dataSubscriber2)
  }

  /* state verification methods */
  private fun verifyInitial() {
    verifyState(NOT_FINISHED, WITHOUT_RESULT, null, NOT_FAILED, null)
    verifyReferenceCount(null)
    verifyNoMoreInteractionsAndReset()
  }

  private fun verifyWithResult(resultRef: CloseableReference<Any>?, isLast: Boolean) {
    verifyState(isLast, resultRef != null, resultRef, NOT_FAILED, null)
    verifyReferenceCount(resultRef)
    verifyNoMoreInteractionsAndReset()
  }

  private fun verifyFailed(resultRef: CloseableReference<Any>?, throwable: Throwable?) {
    verifyState(FINISHED, resultRef != null, resultRef, FAILED, throwable)
    verifyReferenceCount(resultRef)
    verifyNoMoreInteractionsAndReset()
  }

  private fun verifyClosed(isFinished: Boolean, throwable: Throwable?) {
    verifyState(isFinished, WITHOUT_RESULT, null, throwable != null, throwable)
    verifyReferenceCount(null)
    verifyNoMoreInteractionsAndReset()
  }

  /* event testing helpers */
  private fun testSubscribe(expected: Int) {
    dataSource.subscribe(dataSubscriber2, CallerThreadExecutor.getInstance())
    when (expected) {
      NO_INTERACTIONS -> {}
      ON_NEW_RESULT -> verify(dataSubscriber2).onNewResult(dataSource)
      ON_FAILURE -> verify(dataSubscriber2).onFailure(dataSource)
    }
    verifyNoMoreInteractionsAndReset()
  }

  private fun testNewResult(
      resultRef: CloseableReference<Any>?,
      isLast: Boolean,
      numSubscribers: Int
  ) {
    internalConsumer.onNewResult(resultRef, BaseConsumer.simpleStatusForIsLast(isLast))
    if (isLast) {
      verify(requestListener).onRequestSuccess(settableProducerContext)
    }
    if (numSubscribers >= 1) {
      verify(dataSubscriber1).onNewResult(dataSource)
    }
    if (numSubscribers >= 2) {
      verify(dataSubscriber2).onNewResult(dataSource)
    }
    verifyWithResult(resultRef, isLast)
  }

  private fun testFailure(resultRef: CloseableReference<Any>?, numSubscribers: Int) {
    internalConsumer.onFailure(exception)
    verify(requestListener).onRequestFailure(settableProducerContext, exception)
    if (numSubscribers >= 1) {
      verify(dataSubscriber1).onFailure(dataSource)
    }
    if (numSubscribers >= 2) {
      verify(dataSubscriber2).onFailure(dataSource)
    }
    verifyFailed(resultRef, exception)
  }

  private fun testClose(throwable: Throwable?) {
    dataSource.close()
    verifyClosed(FINISHED, throwable)
  }

  private fun testClose(isFinished: Boolean, numSubscribers: Int) {
    dataSource.close()
    if (!isFinished) {
      verify(requestListener).onRequestCancellation(settableProducerContext)
      if (numSubscribers >= 1) {
        verify(dataSubscriber1).onCancellation(dataSource)
      }
      if (numSubscribers >= 2) {
        verify(dataSubscriber2).onCancellation(dataSource)
      }
    }
    verifyClosed(isFinished, null)
  }

  @Test
  fun testInitialState() {
    verifyInitial()
  }

  @Test
  fun test_C_a() {
    testClose(NOT_FINISHED, 1)
    testSubscribe(NO_INTERACTIONS)
  }

  @Test
  fun test_C_I_a() {
    testClose(NOT_FINISHED, 1)
    internalConsumer.onNewResult(resultRef2, Consumer.NO_FLAGS)
    verifyClosed(NOT_FINISHED, null)
    testSubscribe(NO_INTERACTIONS)
  }

  @Test
  fun test_C_L_a() {
    testClose(NOT_FINISHED, 1)
    internalConsumer.onNewResult(resultRef2, Consumer.IS_LAST)
    verifyClosed(NOT_FINISHED, null)
    testSubscribe(NO_INTERACTIONS)
  }

  @Test
  fun testC_F_a() {
    testClose(NOT_FINISHED, 1)
    internalConsumer.onFailure(exception)
    verifyClosed(NOT_FINISHED, null)
    testSubscribe(NO_INTERACTIONS)
  }

  @Test
  fun test_I_a_C() {
    testNewResult(resultRef1, INTERMEDIATE, 1)
    testSubscribe(ON_NEW_RESULT)
    testClose(NOT_FINISHED, 2)
  }

  @Test
  fun test_I_I_a_C() {
    testNewResult(resultRef1, INTERMEDIATE, 1)
    testNewResult(resultRef2, INTERMEDIATE, 1)
    testSubscribe(ON_NEW_RESULT)
    testClose(NOT_FINISHED, 2)
  }

  @Test
  fun test_I_I_L_a_C() {
    testNewResult(resultRef1, INTERMEDIATE, 1)
    testNewResult(resultRef2, INTERMEDIATE, 1)
    testNewResult(resultRef3, LAST, 1)
    testSubscribe(ON_NEW_RESULT)
    testClose(FINISHED, 2)
  }

  @Test
  fun test_I_I_F_a_C() {
    testNewResult(resultRef1, INTERMEDIATE, 1)
    testNewResult(resultRef2, INTERMEDIATE, 1)
    testFailure(resultRef2, 1)
    testSubscribe(ON_FAILURE)
    testClose(exception)
  }

  @Test
  fun test_I_L_a_C() {
    testNewResult(resultRef1, INTERMEDIATE, 1)
    testNewResult(resultRef2, LAST, 1)
    testSubscribe(ON_NEW_RESULT)
    testClose(FINISHED, 2)
  }

  @Test
  fun test_I_F_a_C() {
    testNewResult(resultRef1, INTERMEDIATE, 1)
    testFailure(resultRef1, 1)
    testSubscribe(ON_FAILURE)
    testClose(exception)
  }

  @Test
  fun test_L_a_C() {
    testNewResult(resultRef1, LAST, 1)
    testSubscribe(ON_NEW_RESULT)
    testClose(FINISHED, 2)
  }

  @Test
  fun test_L_I_a_C() {
    testNewResult(resultRef1, LAST, 1)
    internalConsumer.onNewResult(resultRef2, Consumer.NO_FLAGS)
    verifyWithResult(resultRef1, LAST)
    testSubscribe(ON_NEW_RESULT)
    testClose(FINISHED, 2)
  }

  @Test
  fun test_L_L_a_C() {
    testNewResult(resultRef1, LAST, 1)
    internalConsumer.onNewResult(resultRef2, Consumer.IS_LAST)
    verifyWithResult(resultRef1, LAST)
    testSubscribe(ON_NEW_RESULT)
    testClose(FINISHED, 2)
  }

  @Test
  fun test_L_F_a_C() {
    testNewResult(resultRef1, LAST, 1)
    internalConsumer.onFailure(exception)
    verifyWithResult(resultRef1, LAST)
    testSubscribe(ON_NEW_RESULT)
    testClose(FINISHED, 2)
  }

  @Test
  fun test_F_a_C() {
    testFailure(null, 1)
    testSubscribe(ON_FAILURE)
    testClose(exception)
  }

  @Test
  fun test_F_I_a_C() {
    testFailure(null, 1)
    internalConsumer.onNewResult(resultRef1, Consumer.NO_FLAGS)
    verifyFailed(null, exception)
    testSubscribe(ON_FAILURE)
    testClose(exception)
  }

  @Test
  fun test_F_L_a_C() {
    testFailure(null, 1)
    internalConsumer.onNewResult(resultRef1, Consumer.IS_LAST)
    verifyFailed(null, exception)
    testSubscribe(ON_FAILURE)
    testClose(exception)
  }

  @Test
  fun test_F_F_a_C() {
    testFailure(null, 1)
    val anotherException: Throwable = mock()
    internalConsumer.onFailure(anotherException)
    verifyFailed(null, exception)
    testSubscribe(ON_FAILURE)
    testClose(exception)
  }

  @Test
  fun test_NI_S_a_C() {
    internalConsumer.onNewResult(null, Consumer.NO_FLAGS)
    verify(dataSubscriber1).onNewResult(dataSource)
    verifyWithResult(null, INTERMEDIATE)

    testNewResult(resultRef1, LAST, 1)
    testSubscribe(ON_NEW_RESULT)
    testClose(FINISHED, 2)
  }

  @Test
  fun test_NI_a_NL_C() {
    internalConsumer.onNewResult(null, Consumer.NO_FLAGS)
    verify(dataSubscriber1).onNewResult(dataSource)
    verifyWithResult(null, INTERMEDIATE)

    testSubscribe(NO_INTERACTIONS)

    internalConsumer.onNewResult(null, Consumer.IS_LAST)
    verify(requestListener).onRequestSuccess(settableProducerContext)
    verify(dataSubscriber1).onNewResult(dataSource)
    verify(dataSubscriber2).onNewResult(dataSource)
    verifyWithResult(null, LAST)

    testClose(FINISHED, 2)
  }

  @Test
  fun test_I_NL_a_C() {
    testNewResult(resultRef1, INTERMEDIATE, 1)

    internalConsumer.onNewResult(null, Consumer.IS_LAST)
    verify(requestListener).onRequestSuccess(settableProducerContext)
    verify(dataSubscriber1).onNewResult(dataSource)
    verifyWithResult(null, LAST)

    testSubscribe(ON_NEW_RESULT)
    testClose(FINISHED, 2)
  }

  companion object {
    private const val FINISHED = true
    private const val NOT_FINISHED = false
    private const val WITH_RESULT = true
    private const val WITHOUT_RESULT = false
    private const val FAILED = true
    private const val NOT_FAILED = false
    private const val LAST = true
    private const val INTERMEDIATE = false
    private const val NO_INTERACTIONS = 0
    private const val ON_NEW_RESULT = 1
    private const val ON_FAILURE = 2

    private val NPE: Exception = NullPointerException()

    /* reference assertions */
    private fun <T> assertReferenceCount(expectedCount: Int, ref: CloseableReference<T>) {
      assertThat(ref.underlyingReferenceTestOnly.refCountTestOnly.toLong())
          .isEqualTo(expectedCount.toLong())
    }

    private fun <T> assertReferencesSame(
        errorMessage: String,
        expectedRef: CloseableReference<T>?,
        actualRef: CloseableReference<T>?
    ) {
      if (expectedRef == null) {
        assertThat(actualRef).withFailMessage(errorMessage).isNull()
      } else {
        assertThat(actualRef?.get()).withFailMessage(errorMessage).isSameAs(expectedRef.get())
      }
    }
  }
}
