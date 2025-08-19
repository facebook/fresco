/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BaseConsumerTest {
  @Mock private lateinit var delegatedConsumer: Consumer<Any>
  private lateinit var result: Any
  private lateinit var result2: Any
  private lateinit var exception: Exception
  private lateinit var baseConsumer: BaseConsumer<Any>

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    result = Any()
    result2 = Any()
    exception = RuntimeException()
    baseConsumer =
        object : BaseConsumer<Any>() {
          override fun onNewResultImpl(newResult: Any?, @Consumer.Status status: Int) {
            delegatedConsumer.onNewResult(newResult, status)
          }

          override fun onFailureImpl(t: Throwable) {
            delegatedConsumer.onFailure(t)
          }

          override fun onCancellationImpl() {
            delegatedConsumer.onCancellation()
          }
        }
  }

  @Test
  fun testOnNewResultDoesNotThrow() {
    doThrow(RuntimeException()).`when`(delegatedConsumer).onNewResult(any(), anyInt())
    baseConsumer.onNewResult(result, 0)
    verify(delegatedConsumer).onNewResult(result, 0)
  }

  @Test
  fun testOnFailureDoesNotThrow() {
    doThrow(RuntimeException()).`when`(delegatedConsumer).onFailure(any(Throwable::class.java))
    baseConsumer.onFailure(exception)
    verify(delegatedConsumer).onFailure(exception)
  }

  @Test
  fun testOnCancellationDoesNotThrow() {
    doThrow(RuntimeException()).`when`(delegatedConsumer).onCancellation()
    baseConsumer.onCancellation()
    verify(delegatedConsumer).onCancellation()
  }

  @Test
  fun testDoesNotForwardAfterFinalResult() {
    baseConsumer.onNewResult(result, Consumer.IS_LAST)
    baseConsumer.onFailure(exception)
    baseConsumer.onCancellation()
    verify(delegatedConsumer).onNewResult(result, Consumer.IS_LAST)
    verifyNoMoreInteractions(delegatedConsumer)
  }

  @Test
  fun testDoesNotForwardAfterOnFailure() {
    baseConsumer.onFailure(exception)
    baseConsumer.onNewResult(result, Consumer.IS_LAST)
    baseConsumer.onCancellation()
    verify(delegatedConsumer).onFailure(exception)
    verifyNoMoreInteractions(delegatedConsumer)
  }

  @Test
  fun testDoesNotForwardAfterOnCancellation() {
    baseConsumer.onCancellation()
    baseConsumer.onNewResult(result, Consumer.IS_LAST)
    baseConsumer.onFailure(exception)
    verify(delegatedConsumer).onCancellation()
    verifyNoMoreInteractions(delegatedConsumer)
  }

  @Test
  fun testDoesForwardAfterIntermediateResult() {
    baseConsumer.onNewResult(result, 0)
    baseConsumer.onNewResult(result2, Consumer.IS_LAST)
    verify(delegatedConsumer).onNewResult(result2, Consumer.IS_LAST)
  }

  @Test
  fun testIsLast() {
    assertThat(BaseConsumer.isLast(Consumer.IS_LAST)).isTrue()
    assertThat(BaseConsumer.isLast(Consumer.NO_FLAGS)).isFalse()
  }

  @Test
  fun testIsNotLast() {
    assertThat(BaseConsumer.isNotLast(Consumer.IS_LAST)).isFalse()
    assertThat(BaseConsumer.isNotLast(Consumer.NO_FLAGS)).isTrue()
  }

  @Test
  fun testTurnOnStatusFlag() {
    val turnedOn = BaseConsumer.turnOnStatusFlag(Consumer.NO_FLAGS, Consumer.IS_LAST)
    assertThat(BaseConsumer.isLast(turnedOn)).isTrue()
  }

  @Test
  fun testTurnOffStatusFlag() {
    val turnedOff = BaseConsumer.turnOffStatusFlag(Consumer.IS_LAST, Consumer.IS_LAST)
    assertThat(BaseConsumer.isNotLast(turnedOff)).isTrue()
  }

  @Test
  fun testStatusHasFlag() {
    assertThat(
            BaseConsumer.statusHasFlag(
                Consumer.IS_PLACEHOLDER or Consumer.IS_LAST,
                Consumer.IS_PLACEHOLDER,
            ))
        .isTrue()

    assertThat(
            BaseConsumer.statusHasFlag(
                Consumer.DO_NOT_CACHE_ENCODED or Consumer.IS_LAST,
                Consumer.IS_PLACEHOLDER,
            ))
        .isFalse()
  }

  @Test
  fun testStatusHasAnyFlag() {
    assertThat(
            BaseConsumer.statusHasAnyFlag(
                Consumer.IS_PLACEHOLDER or Consumer.IS_LAST,
                Consumer.IS_PLACEHOLDER or Consumer.DO_NOT_CACHE_ENCODED,
            ))
        .isTrue()

    assertThat(
            BaseConsumer.statusHasAnyFlag(
                Consumer.IS_PLACEHOLDER or Consumer.IS_LAST,
                Consumer.IS_PARTIAL_RESULT or Consumer.DO_NOT_CACHE_ENCODED,
            ))
        .isFalse()
  }
}
