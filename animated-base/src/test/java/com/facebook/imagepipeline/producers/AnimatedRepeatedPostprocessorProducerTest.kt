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
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.image.CloseableAnimatedImage
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.producers.PostprocessorProducer.RepeatedPostprocessorConsumer
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.RepeatedPostprocessor
import com.facebook.imagepipeline.request.RepeatedPostprocessorRunner
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AnimatedRepeatedPostprocessorProducerTest {

  companion object {
    private const val POSTPROCESSOR_NAME = "postprocessor_name"
    private val extraMap = ImmutableMap.of(PostprocessorProducer.POSTPROCESSOR, POSTPROCESSOR_NAME)
  }

  @Mock lateinit var platformBitmapFactory: PlatformBitmapFactory
  @Mock lateinit var producerListener: ProducerListener2
  @Mock lateinit var inputProducer: Producer<CloseableReference<CloseableImage>>
  @Mock lateinit var consumer: Consumer<CloseableReference<CloseableImage>>
  @Mock lateinit var postprocessor: RepeatedPostprocessor
  @Mock lateinit var bitmapResourceReleaser: ResourceReleaser<Bitmap>

  @Mock lateinit var imageRequest: ImageRequest

  @Mock lateinit var config: ImagePipelineConfig

  private lateinit var producerContext: SettableProducerContext
  private val requestId = "requestId"
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
    producerContext =
        SettableProducerContext(
            imageRequest,
            requestId,
            producerListener,
            mock<Object>(),
            ImageRequest.RequestLevel.FULL_FETCH,
            false /* isPrefetch */,
            false /* isIntermediateResultExpected */,
            Priority.MEDIUM,
            config,
        )
    whenever(imageRequest.postprocessor).thenReturn(postprocessor)
    results = ArrayList()
    whenever(postprocessor.name).thenReturn(POSTPROCESSOR_NAME)
    whenever(producerListener.requiresExtraMap(producerContext, POSTPROCESSOR_NAME))
        .thenReturn(true)
    doAnswer { invocation ->
          results.add((invocation.arguments[0] as CloseableReference<CloseableImage>).clone())
          null
        }
        .whenever(consumer)
        .onNewResult(any(), any<Int>())
    inOrder = inOrder(postprocessor, producerListener, consumer)
  }

  @Test
  fun testNonStaticBitmapIsPassedOn() {
    val postprocessorConsumer = produceResults()
    val repeatedPostprocessorRunner = getRunner()

    val sourceCloseableAnimatedImage = mock<CloseableAnimatedImage>()
    val sourceCloseableImageRef =
        CloseableReference.of<CloseableImage>(sourceCloseableAnimatedImage)
    postprocessorConsumer.onNewResult(sourceCloseableImageRef, Consumer.IS_LAST)
    sourceCloseableImageRef.close()
    testExecutorService.runUntilIdle()

    inOrder
        .verify(consumer)
        .onNewResult(any<CloseableReference<CloseableImage>>(), eq(Consumer.NO_FLAGS))
    inOrder.verifyNoMoreInteractions()

    assertThat(results).hasSize(1)
    val res0 = results[0]
    assertThat(CloseableReference.isValid(res0)).isTrue()
    assertThat(res0.get()).isSameAs(sourceCloseableAnimatedImage)
    res0.close()

    performCancelAndVerifyOnCancellation()
    verify(sourceCloseableAnimatedImage).close()
  }

  private fun setupNewSourceImage() {
    sourceBitmap = mock<Bitmap>()
    sourceCloseableStaticBitmap = mock<CloseableStaticBitmap>()
    whenever(sourceCloseableStaticBitmap.underlyingBitmap).thenReturn(sourceBitmap)
    sourceCloseableImageRef = CloseableReference.of<CloseableImage>(sourceCloseableStaticBitmap)
  }

  private fun setupNewDestinationImage() {
    destinationBitmap = mock<Bitmap>()
    destinationCloseableBitmapRef = CloseableReference.of(destinationBitmap, bitmapResourceReleaser)
    doReturn(destinationCloseableBitmapRef)
        .whenever(postprocessor)
        .process(sourceBitmap, platformBitmapFactory)
  }

  private fun produceResults(): RepeatedPostprocessorConsumer {
    postprocessorProducer.produceResults(consumer, producerContext)

    // Use argumentCaptor from Mockito Kotlin
    val consumerCaptor = argumentCaptor<Consumer<CloseableReference<CloseableImage>>>()
    verify(inputProducer).produceResults(consumerCaptor.capture(), eq(producerContext))
    return consumerCaptor.firstValue as RepeatedPostprocessorConsumer
  }

  private fun getRunner(): RepeatedPostprocessorRunner {
    val captor = argumentCaptor<RepeatedPostprocessorRunner>()
    inOrder.verify(postprocessor).setCallback(captor.capture())
    return captor.firstValue
  }

  private fun performNewResult(postprocessorConsumer: RepeatedPostprocessorConsumer, run: Boolean) {
    setupNewSourceImage()
    setupNewDestinationImage()
    postprocessorConsumer.onNewResult(sourceCloseableImageRef, Consumer.IS_LAST)
    sourceCloseableImageRef.close()
    if (run) {
      testExecutorService.runUntilIdle()
    }
  }

  private fun performUpdate(
      repeatedPostprocessorRunner: RepeatedPostprocessorRunner,
      run: Boolean,
  ) {
    setupNewDestinationImage()
    repeatedPostprocessorRunner.update()
    if (run) {
      testExecutorService.runUntilIdle()
    }
  }

  private fun performUpdateDuringTheNextPostprocessing(
      repeatedPostprocessorRunner: RepeatedPostprocessorRunner
  ) {
    doAnswer {
          val destBitmapRef = destinationCloseableBitmapRef
          performUpdate(repeatedPostprocessorRunner, false)
          // the following call should be ignored
          performUpdate(repeatedPostprocessorRunner, false)
          destBitmapRef
        }
        .whenever(postprocessor)
        .process(sourceBitmap, platformBitmapFactory)
  }

  private fun performFailure(repeatedPostprocessorRunner: RepeatedPostprocessorRunner) {
    setupNewDestinationImage()
    doThrow(RuntimeException()).whenever(postprocessor).process(sourceBitmap, platformBitmapFactory)
    repeatedPostprocessorRunner.update()
    testExecutorService.runUntilIdle()
  }

  private fun performCancelAndVerifyOnCancellation() {
    performCancel()
    inOrder.verify(consumer).onCancellation()
  }

  private fun performCancelAfterFinished() {
    performCancel()
    inOrder.verify(consumer, never()).onCancellation()
  }

  private fun performCancel() {
    producerContext.cancel()
    testExecutorService.runUntilIdle()
  }

  private fun verifyNewResultProcessed(index: Int) {
    verifyNewResultProcessed(index, destinationBitmap)
  }

  private fun verifyNewResultProcessed(index: Int, destBitmap: Bitmap) {
    inOrder.verify(producerListener).onProducerStart(producerContext, PostprocessorProducer.NAME)
    inOrder.verify(postprocessor).process(sourceBitmap, platformBitmapFactory)
    inOrder.verify(producerListener).requiresExtraMap(producerContext, PostprocessorProducer.NAME)
    inOrder
        .verify(producerListener)
        .onProducerFinishWithSuccess(producerContext, PostprocessorProducer.NAME, extraMap)
    inOrder
        .verify(consumer)
        .onNewResult(any<CloseableReference<CloseableImage>>(), eq(Consumer.NO_FLAGS))
    inOrder.verifyNoMoreInteractions()

    assertThat(results).hasSize(index + 1)
    val res0 = results[index]
    assertThat(CloseableReference.isValid(res0)).isTrue()
    assertThat((res0.get() as CloseableStaticBitmap).underlyingBitmap).isSameAs(destBitmap)
    res0.close()
    verify(bitmapResourceReleaser).release(destBitmap)
  }
}
