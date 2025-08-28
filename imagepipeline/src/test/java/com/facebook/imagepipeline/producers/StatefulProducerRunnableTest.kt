/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.common.internal.Supplier
import java.io.Closeable
import java.io.IOException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StatefulProducerRunnableTest {

  @Mock private lateinit var consumer: Consumer<Closeable>
  @Mock private lateinit var producerListener: ProducerListener2
  @Mock private lateinit var resultSupplier: Supplier<Closeable>
  @Mock private lateinit var result: Closeable
  @Mock private lateinit var producerContext: ProducerContext

  private lateinit var exception: RuntimeException
  private lateinit var successMap: MutableMap<String?, String?>
  private lateinit var failureMap: MutableMap<String, String>
  private lateinit var cancellationMap: MutableMap<String, String>

  private lateinit var statefulProducerRunnable: StatefulProducerRunnable<Closeable>

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    exception = ConcurrentModificationException()
    successMap = mutableMapOf<String?, String?>("state" to "success")
    failureMap = mutableMapOf("state" to "failure")
    cancellationMap = mutableMapOf("state" to "cancelled")

    statefulProducerRunnable =
        object :
            StatefulProducerRunnable<Closeable>(
                consumer,
                producerListener,
                producerContext,
                PRODUCER_NAME,
            ) {
          override fun disposeResult(result: Closeable?) {
            try {
              result?.close()
            } catch (ioe: IOException) {
              throw RuntimeException("unexpected IOException", ioe)
            }
          }

          override fun getResult(): Closeable {
            return resultSupplier.get()
          }

          override val extraMapOnCancellation: Map<String, String>
            get() = cancellationMap

          override fun getExtraMapOnFailure(exception: Exception?): Map<String, String> {
            return failureMap
          }

          override fun getExtraMapOnSuccess(result: Closeable?): Map<String?, String?> {
            return successMap
          }
        }
  }

  @Test
  fun testOnSuccess_extraMap() {
    doReturn(true).`when`(producerListener).requiresExtraMap(eq(producerContext), eq(PRODUCER_NAME))
    doReturn(result).`when`(resultSupplier).get()
    statefulProducerRunnable.run()
    verify(consumer).onNewResult(result, Consumer.IS_LAST)
    verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    verify(producerListener).onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, successMap)
    verify(producerListener, never())
        .onUltimateProducerReached(eq(producerContext), anyString(), anyBoolean())
    verify(result).close()
  }

  @Test
  fun testOnSuccess_noExtraMap() {
    doReturn(result).`when`(resultSupplier).get()
    statefulProducerRunnable.run()
    verify(consumer).onNewResult(result, Consumer.IS_LAST)
    verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    verify(producerListener).onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, null)
    verify(producerListener, never())
        .onUltimateProducerReached(eq(producerContext), anyString(), anyBoolean())
    verify(result).close()
  }

  @Test
  fun testOnCancellation_extraMap() {
    doReturn(true).`when`(producerListener).requiresExtraMap(producerContext, PRODUCER_NAME)
    statefulProducerRunnable.cancel()
    verify(consumer).onCancellation()
    verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    verify(producerListener)
        .onProducerFinishWithCancellation(producerContext, PRODUCER_NAME, cancellationMap)
    verify(producerListener, never())
        .onUltimateProducerReached(eq(producerContext), anyString(), anyBoolean())
  }

  @Test
  fun testOnCancellation_noExtraMap() {
    statefulProducerRunnable.cancel()
    verify(consumer).onCancellation()
    verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    verify(producerListener).onProducerFinishWithCancellation(producerContext, PRODUCER_NAME, null)
    verify(producerListener, never())
        .onUltimateProducerReached(eq(producerContext), anyString(), anyBoolean())
  }

  @Test
  fun testOnFailure_extraMap() {
    doReturn(true).`when`(producerListener).requiresExtraMap(producerContext, PRODUCER_NAME)
    doThrow(exception).`when`(resultSupplier).get()
    statefulProducerRunnable.run()
    verify(consumer).onFailure(exception)
    verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    verify(producerListener)
        .onProducerFinishWithFailure(producerContext, PRODUCER_NAME, exception, failureMap)
    verify(producerListener, never())
        .onUltimateProducerReached(eq(producerContext), anyString(), anyBoolean())
  }

  @Test
  fun testOnFailure_noExtraMap() {
    doThrow(exception).`when`(resultSupplier).get()
    statefulProducerRunnable.run()
    verify(consumer).onFailure(exception)
    verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    verify(producerListener)
        .onProducerFinishWithFailure(producerContext, PRODUCER_NAME, exception, null)
    verify(producerListener, never())
        .onUltimateProducerReached(eq(producerContext), anyString(), anyBoolean())
  }

  companion object {
    private const val PRODUCER_NAME = "aBitLessAwesomeButStillAwesomeProducerName"
  }
}
