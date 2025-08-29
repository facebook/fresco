/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.graphics.Bitmap
import com.facebook.common.internal.ImmutableMap
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.image.CloseableAnimatedImage
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.producers.PostprocessorProducer.SingleUsePostprocessorConsumer
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.Postprocessor
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InOrder
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AnimatedSingleUsePostprocessorProducerTest {

  companion object {
    private const val POSTPROCESSOR_NAME = "postprocessor_name"
    private val extraMap = ImmutableMap.of(PostprocessorProducer.POSTPROCESSOR, POSTPROCESSOR_NAME)
  }

  @Mock lateinit var platformBitmapFactory: PlatformBitmapFactory
  @Mock lateinit var producerContext: ProducerContext
  @Mock lateinit var producerListener: ProducerListener2
  @Mock lateinit var inputProducer: Producer<CloseableReference<CloseableImage>>
  @Mock lateinit var consumer: Consumer<CloseableReference<CloseableImage>>
  @Mock lateinit var postprocessor: Postprocessor
  @Mock lateinit var bitmapResourceReleaser: ResourceReleaser<Bitmap>

  @Mock lateinit var imageRequest: ImageRequest

  private val requestId = "mRequestId"
  private lateinit var sourceBitmap: Bitmap
  private lateinit var sourceCloseableStaticBitmap: CloseableStaticBitmap
  private lateinit var sourceCloseableImageRef: CloseableReference<CloseableImage>
  private lateinit var destinationBitmap: Bitmap
  private lateinit var destinationCloseableBitmapRef: CloseableReference<Bitmap>
  private lateinit var testExecutorService: TestExecutorService
  private lateinit var postprocessorProducer: PostprocessorProducer
  private lateinit var results: MutableList<CloseableReference<CloseableImage>>

  private lateinit var inOrder: InOrder

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    testExecutorService = TestExecutorService(FakeClock())
    postprocessorProducer =
        PostprocessorProducer(inputProducer, platformBitmapFactory, testExecutorService)

    whenever(imageRequest.postprocessor).thenReturn(postprocessor)
    whenever(producerContext.id).thenReturn(requestId)
    whenever(producerContext.producerListener).thenReturn(producerListener)
    whenever(producerContext.imageRequest).thenReturn(imageRequest)

    results = ArrayList()
    whenever(postprocessor.name).thenReturn(POSTPROCESSOR_NAME)
    whenever(producerListener.requiresExtraMap(eq(producerContext), eq(POSTPROCESSOR_NAME)))
        .thenReturn(true)
    doAnswer { invocation ->
          results.add((invocation.arguments[0] as CloseableReference<CloseableImage>).clone())
          null
        }
        .whenever(consumer)
        .onNewResult(any(), any())
    inOrder = inOrder(postprocessor, producerListener, consumer)

    sourceBitmap = mock<Bitmap>()
    sourceCloseableStaticBitmap = mock<CloseableStaticBitmap>()
    whenever(sourceCloseableStaticBitmap.underlyingBitmap).thenReturn(sourceBitmap)
    sourceCloseableImageRef = CloseableReference.of<CloseableImage>(sourceCloseableStaticBitmap)
    destinationBitmap = mock<Bitmap>()
    destinationCloseableBitmapRef = CloseableReference.of(destinationBitmap, bitmapResourceReleaser)
  }

  @Test
  fun testNonStaticBitmapIsPassedOn() {
    val postprocessorConsumer = produceResults()
    val sourceCloseableAnimatedImage = mock<CloseableAnimatedImage>()
    val sourceCloseableImageRef =
        CloseableReference.of<CloseableImage>(sourceCloseableAnimatedImage)
    postprocessorConsumer.onNewResult(sourceCloseableImageRef, Consumer.IS_LAST)
    sourceCloseableImageRef.close()
    testExecutorService.runUntilIdle()

    inOrder
        .verify(consumer)
        .onNewResult(any<CloseableReference<CloseableImage>>(), eq(Consumer.IS_LAST))
    inOrder.verifyNoMoreInteractions()

    assertThat(results).hasSize(1)
    val res0 = results[0]
    assertThat(CloseableReference.isValid(res0)).isTrue()
    assertThat(res0.get()).isSameAs(sourceCloseableAnimatedImage)
    res0.close()

    verify(sourceCloseableAnimatedImage).close()
  }

  private fun produceResults(): SingleUsePostprocessorConsumer {
    postprocessorProducer.produceResults(consumer, producerContext)

    val consumerCaptor = argumentCaptor<Consumer<CloseableReference<CloseableImage>>>()
    verify(inputProducer).produceResults(consumerCaptor.capture(), eq(producerContext))
    return consumerCaptor.firstValue as SingleUsePostprocessorConsumer
  }
}
