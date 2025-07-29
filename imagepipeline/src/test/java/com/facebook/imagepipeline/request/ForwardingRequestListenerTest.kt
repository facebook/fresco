/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request

import com.facebook.common.internal.Sets
import com.facebook.imagepipeline.listener.ForwardingRequestListener
import com.facebook.imagepipeline.listener.RequestListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests for [ForwardingRequestListener] */
@RunWith(RobolectricTestRunner::class)
class ForwardingRequestListenerTest {
  private val request: ImageRequest = mock()
  private val callerContext: Any = mock()
  private val exception: Throwable = mock()
  private val immutableMap: MutableMap<String?, String?> = mock()
  private val requestListener1: RequestListener = mock()
  private val requestListener2: RequestListener = mock()
  private val requestListener3: RequestListener = mock()
  private lateinit var listenerManager: ForwardingRequestListener
  private val requestId = "DummyRequestId"
  private val producerName = "DummyProducerName"
  private val producerEventName = "DummyProducerEventName"
  private val isPrefetch = true

  @Before
  fun setUp() {
    whenever(requestListener1.requiresExtraMap(requestId)).thenReturn(false)
    whenever(requestListener2.requiresExtraMap(requestId)).thenReturn(false)
    whenever(requestListener3.requiresExtraMap(requestId)).thenReturn(false)
    listenerManager =
        ForwardingRequestListener(
            Sets.newHashSet(requestListener1, requestListener2, requestListener3))
  }

  @Test
  fun testOnRequestStart() {
    listenerManager.onRequestStart(request, callerContext, requestId, isPrefetch)
    verify(requestListener1).onRequestStart(request, callerContext, requestId, isPrefetch)
    verify(requestListener2).onRequestStart(request, callerContext, requestId, isPrefetch)
    verify(requestListener3).onRequestStart(request, callerContext, requestId, isPrefetch)
  }

  @Test
  fun testOnRequestSuccess() {
    listenerManager.onRequestSuccess(request, requestId, isPrefetch)
    verify(requestListener1).onRequestSuccess(request, requestId, isPrefetch)
    verify(requestListener2).onRequestSuccess(request, requestId, isPrefetch)
    verify(requestListener3).onRequestSuccess(request, requestId, isPrefetch)
  }

  @Test
  fun testOnRequestFailure() {
    listenerManager.onRequestFailure(request, requestId, exception, isPrefetch)
    verify(requestListener1).onRequestFailure(request, requestId, exception, isPrefetch)
    verify(requestListener2).onRequestFailure(request, requestId, exception, isPrefetch)
    verify(requestListener3).onRequestFailure(request, requestId, exception, isPrefetch)
  }

  @Test
  fun testOnProducerStart() {
    listenerManager.onProducerStart(requestId, producerName)
    verify(requestListener1).onProducerStart(requestId, producerName)
    verify(requestListener2).onProducerStart(requestId, producerName)
    verify(requestListener3).onProducerStart(requestId, producerName)
  }

  @Test
  fun testOnProducerFinishWithSuccess() {
    listenerManager.onProducerFinishWithSuccess(requestId, producerName, immutableMap)
    verify(requestListener1).onProducerFinishWithSuccess(requestId, producerName, immutableMap)
    verify(requestListener2).onProducerFinishWithSuccess(requestId, producerName, immutableMap)
    verify(requestListener3).onProducerFinishWithSuccess(requestId, producerName, immutableMap)
  }

  @Test
  fun testOnProducerFinishWithFailure() {
    listenerManager.onProducerFinishWithFailure(requestId, producerName, exception, immutableMap)
    verify(requestListener1)
        .onProducerFinishWithFailure(requestId, producerName, exception, immutableMap)
    verify(requestListener2)
        .onProducerFinishWithFailure(requestId, producerName, exception, immutableMap)
    verify(requestListener3)
        .onProducerFinishWithFailure(requestId, producerName, exception, immutableMap)
  }

  @Test
  fun testOnProducerFinishWithCancellation() {
    listenerManager.onProducerFinishWithCancellation(requestId, producerName, immutableMap)
    verify(requestListener1).onProducerFinishWithCancellation(requestId, producerName, immutableMap)
    verify(requestListener2).onProducerFinishWithCancellation(requestId, producerName, immutableMap)
    verify(requestListener3).onProducerFinishWithCancellation(requestId, producerName, immutableMap)
  }

  @Test
  fun testOnProducerEvent() {
    listenerManager.onProducerEvent(requestId, producerName, producerEventName)
    verify(requestListener1).onProducerEvent(requestId, producerName, producerEventName)
    verify(requestListener2).onProducerEvent(requestId, producerName, producerEventName)
    verify(requestListener3).onProducerEvent(requestId, producerName, producerEventName)
  }

  @Test
  fun testRequiresExtraMap() {
    assertThat(listenerManager.requiresExtraMap(requestId)).isFalse()
    whenever(requestListener2.requiresExtraMap(requestId)).thenReturn(true)
    assertThat(listenerManager.requiresExtraMap(requestId)).isTrue()
  }
}
