/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import com.facebook.common.internal.ImmutableMap
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Basic tests for [LocalVideoThumbnailProducer] */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalVideoThumbnailProducerTest {
  @Mock lateinit var pooledByteBufferFactory: PooledByteBufferFactory
  @Mock lateinit var consumer: Consumer<CloseableReference<CloseableImage>>
  @Mock lateinit var imageRequest: ImageRequest
  @Mock lateinit var producerListener: ProducerListener2
  @Mock lateinit var exception: Exception
  @Mock lateinit var bitmap: Bitmap
  @Mock lateinit var config: ImagePipelineConfig

  private lateinit var executor: TestExecutorService
  private lateinit var producerContext: SettableProducerContext
  private val requestId = "requestId"
  private lateinit var localVideoThumbnailProducer: LocalVideoThumbnailProducer
  private var closeableReference: CloseableReference<CloseableStaticBitmap>? = null
  private lateinit var localVideoUri: Uri
  private lateinit var mockedThumbnailUtils: MockedStatic<ThumbnailUtils>

  @Before
  @Throws(Exception::class)
  fun setUp() {
    mockedThumbnailUtils = Mockito.mockStatic(ThumbnailUtils::class.java)
    MockitoAnnotations.openMocks(this)
    executor = TestExecutorService(FakeClock())
    localVideoThumbnailProducer =
        LocalVideoThumbnailProducer(executor, RuntimeEnvironment.application.contentResolver)

    producerContext =
        SettableProducerContext(
            imageRequest,
            requestId,
            producerListener,
            Mockito.mock(Any::class.java),
            ImageRequest.RequestLevel.FULL_FETCH,
            false,
            false,
            Priority.MEDIUM,
            config)
    localVideoUri = Uri.parse(TEST_FILEPATH)
  }

  @After
  fun tearDownStaticMocks() {
    mockedThumbnailUtils.close()
  }

  @Test
  fun testLocalVideoThumbnailCancelled() {
    localVideoThumbnailProducer.produceResults(consumer, producerContext)
    producerContext.cancel()
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithCancellation(producerContext, PRODUCER_NAME, null)
    Mockito.verify(producerListener, Mockito.never())
        .onUltimateProducerReached(
            ArgumentMatchers.eq(producerContext),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyBoolean())
    Mockito.verify(consumer).onCancellation()
  }

  @Test
  @Throws(Exception::class)
  fun testLocalVideoMiniThumbnailSuccess() {
    Mockito.`when`(imageRequest.preferredWidth).thenReturn(100)
    Mockito.`when`(imageRequest.preferredHeight).thenReturn(100)
    Mockito.`when`(imageRequest.sourceUri).thenReturn(localVideoUri)
    Mockito.`when`(
            ThumbnailUtils.createVideoThumbnail(
                TEST_FILENAME, MediaStore.Images.Thumbnails.MINI_KIND))
        .thenReturn(bitmap)
    Mockito.doAnswer(
            object : Answer<Any?> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): Any? {
                closeableReference =
                    (invocation.getArguments()[0] as CloseableReference<*>).clone()
                        as CloseableReference<CloseableStaticBitmap>
                return null
              }
            })
        .`when`(consumer)
        .onNewResult(
            ArgumentMatchers.any<CloseableReference<CloseableImage>>(),
            ArgumentMatchers.eq(Consumer.IS_LAST))
    localVideoThumbnailProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()
    assertThat(closeableReference?.underlyingReferenceTestOnly?.refCountTestOnly).isEqualTo(1)
    assertThat(closeableReference?.underlyingReferenceTestOnly?.get()?.underlyingBitmap)
        .isEqualTo(bitmap)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, null)
    Mockito.verify(producerListener).onUltimateProducerReached(producerContext, PRODUCER_NAME, true)
  }

  @Test
  @Throws(Exception::class)
  fun testLocalVideoMicroThumbnailSuccess() {
    Mockito.`when`(imageRequest.sourceUri).thenReturn(localVideoUri)
    Mockito.`when`(producerListener.requiresExtraMap(producerContext, PRODUCER_NAME))
        .thenReturn(true)
    Mockito.`when`(
            ThumbnailUtils.createVideoThumbnail(
                TEST_FILENAME, MediaStore.Images.Thumbnails.MICRO_KIND))
        .thenReturn(bitmap)
    Mockito.doAnswer(
            object : Answer<Any?> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): Any? {
                closeableReference =
                    (invocation.getArguments()[0] as CloseableReference<*>).clone()
                        as CloseableReference<CloseableStaticBitmap>
                return null
              }
            })
        .`when`(consumer)
        .onNewResult(
            ArgumentMatchers.any<CloseableReference<CloseableImage>>(),
            ArgumentMatchers.eq(Consumer.IS_LAST))
    localVideoThumbnailProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()
    assertThat(closeableReference?.underlyingReferenceTestOnly?.refCountTestOnly).isEqualTo(1)
    assertThat(closeableReference?.underlyingReferenceTestOnly?.get()?.underlyingBitmap)
        .isEqualTo(bitmap)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    val thumbnailFoundMap = ImmutableMap.of(LocalVideoThumbnailProducer.CREATED_THUMBNAIL, "true")
    Mockito.verify(producerListener)
        .onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, thumbnailFoundMap)
    Mockito.verify(producerListener).onUltimateProducerReached(producerContext, PRODUCER_NAME, true)
  }

  @Test
  @Throws(Exception::class)
  fun testLocalVideoMicroThumbnailReturnsNull() {
    Mockito.`when`(imageRequest.sourceUri).thenReturn(localVideoUri)
    Mockito.`when`(producerListener.requiresExtraMap(producerContext, PRODUCER_NAME))
        .thenReturn(true)
    Mockito.`when`(
            ThumbnailUtils.createVideoThumbnail(
                TEST_FILENAME, MediaStore.Images.Thumbnails.MICRO_KIND))
        .thenReturn(null)
    localVideoThumbnailProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()
    Mockito.verify(consumer).onNewResult(null, Consumer.IS_LAST)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    val thumbnailNotFoundMap =
        ImmutableMap.of(LocalVideoThumbnailProducer.CREATED_THUMBNAIL, "false")
    Mockito.verify(producerListener)
        .onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, thumbnailNotFoundMap)
    Mockito.verify(producerListener)
        .onUltimateProducerReached(producerContext, PRODUCER_NAME, false)
  }

  @Test(expected = RuntimeException::class)
  @Throws(Exception::class)
  fun testFetchLocalFileFailsByThrowing() {
    Mockito.`when`(imageRequest.sourceUri).thenReturn(localVideoUri)
    Mockito.`when`(
            ThumbnailUtils.createVideoThumbnail(
                TEST_FILENAME, MediaStore.Images.Thumbnails.MICRO_KIND))
        .thenThrow(exception)
    localVideoThumbnailProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()
    Mockito.verify(consumer).onFailure(exception)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithFailure(producerContext, PRODUCER_NAME, exception, null)
    Mockito.verify(producerListener)
        .onUltimateProducerReached(producerContext, PRODUCER_NAME, false)
  }

  companion object {
    private val PRODUCER_NAME = LocalVideoThumbnailProducer.PRODUCER_NAME
    private const val TEST_FILENAME = "/dancing_hotdog.mp4"
    private val TEST_FILEPATH = "file://$TEST_FILENAME"
  }
}
