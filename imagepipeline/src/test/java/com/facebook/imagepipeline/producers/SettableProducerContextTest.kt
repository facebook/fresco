/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.request.ImageRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks basic properties of SettableProducerContext. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettableProducerContextTest {
  @Mock private lateinit var imageRequest: ImageRequest

  @Mock private lateinit var config: ImagePipelineConfig

  private val requestId = "requestId"
  private lateinit var callbacks1: ProducerContextCallbacks
  private lateinit var callbacks2: ProducerContextCallbacks
  private lateinit var settableProducerContext: SettableProducerContext

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    settableProducerContext =
        SettableProducerContext(
            imageRequest,
            requestId,
            Mockito.mock(ProducerListener2::class.java),
            Mockito.mock(Any::class.java),
            ImageRequest.RequestLevel.FULL_FETCH,
            false,
            true,
            Priority.MEDIUM,
            config)
    callbacks1 = Mockito.mock(ProducerContextCallbacks::class.java)
    callbacks2 = Mockito.mock(ProducerContextCallbacks::class.java)
  }

  @Test
  fun testGetters() {
    assertThat(settableProducerContext.imageRequest).isEqualTo(imageRequest)
    assertThat(settableProducerContext.id).isEqualTo(requestId)
  }

  @Test
  fun testIsPrefetch() {
    assertThat(settableProducerContext.isPrefetch).isFalse()
  }

  @Test
  fun testCancellation() {
    settableProducerContext.addCallbacks(callbacks1)
    Mockito.verify(callbacks1, Mockito.never()).onCancellationRequested()
    settableProducerContext.cancel()
    Mockito.verify(callbacks1).onCancellationRequested()
    Mockito.verify(callbacks1, Mockito.never()).onIsPrefetchChanged()

    settableProducerContext.addCallbacks(callbacks2)
    Mockito.verify(callbacks2).onCancellationRequested()
    Mockito.verify(callbacks2, Mockito.never()).onIsPrefetchChanged()
  }

  @Test
  fun testSetPrefetch() {
    settableProducerContext.addCallbacks(callbacks1)
    assertThat(settableProducerContext.isPrefetch).isFalse()
    settableProducerContext.setIsPrefetch(true)
    assertThat(settableProducerContext.isPrefetch).isTrue()
    Mockito.verify(callbacks1).onIsPrefetchChanged()
    settableProducerContext.setIsPrefetch(true)
    // only one callback is expected
    Mockito.verify(callbacks1).onIsPrefetchChanged()
  }

  @Test
  fun testSetIsIntermediateResultExpected() {
    settableProducerContext.addCallbacks(callbacks1)
    assertThat(settableProducerContext.isIntermediateResultExpected).isTrue()
    settableProducerContext.setIsIntermediateResultExpected(false)
    assertThat(settableProducerContext.isIntermediateResultExpected).isFalse()
    Mockito.verify(callbacks1).onIsIntermediateResultExpectedChanged()
    settableProducerContext.setIsIntermediateResultExpected(false)
    // only one callback is expected
    Mockito.verify(callbacks1).onIsIntermediateResultExpectedChanged()
  }

  @Test
  fun testNoCallbackCalledWhenIsPrefetchDoesNotChange() {
    assertThat(settableProducerContext.isPrefetch).isFalse()
    settableProducerContext.addCallbacks(callbacks1)
    settableProducerContext.setIsPrefetch(false)
    Mockito.verify(callbacks1, Mockito.never()).onIsPrefetchChanged()
  }

  @Test
  fun testCallbackCalledWhenIsPrefetchChanges() {
    assertThat(settableProducerContext.isPrefetch).isFalse()
    settableProducerContext.addCallbacks(callbacks1)
    settableProducerContext.addCallbacks(callbacks2)
    settableProducerContext.setIsPrefetch(true)
    assertThat(settableProducerContext.isPrefetch).isTrue()
    Mockito.verify(callbacks1).onIsPrefetchChanged()
    Mockito.verify(callbacks1, Mockito.never()).onCancellationRequested()
    Mockito.verify(callbacks2).onIsPrefetchChanged()
    Mockito.verify(callbacks2, Mockito.never()).onCancellationRequested()
  }
}
