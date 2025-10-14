/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.ContentResolver
import android.net.Uri
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import java.io.File
import java.io.InputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Basic tests for LocalContentUriFetchProducer */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalContentUriFetchProducerTest {
  @Mock lateinit var pooledByteBufferFactory: PooledByteBufferFactory
  @Mock lateinit var contentResolver: ContentResolver
  @Mock lateinit var consumer: Consumer<EncodedImage?>
  @Mock lateinit var imageRequest: ImageRequest
  @Mock lateinit var producerListener: ProducerListener2
  @Mock lateinit var exception: Exception
  @Mock lateinit var config: ImagePipelineConfig
  private lateinit var executor: TestExecutorService
  private lateinit var producerContext: SettableProducerContext
  private val requestId = "requestId"
  private lateinit var contentUri: Uri
  private lateinit var localContentUriFetchProducer: LocalContentUriFetchProducer
  private var capturedEncodedImage: EncodedImage? = null

  @Before
  @Throws(Exception::class)
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    executor = TestExecutorService(FakeClock())
    localContentUriFetchProducer =
        LocalContentUriFetchProducer(executor, pooledByteBufferFactory, contentResolver)
    contentUri = Uri.fromFile(Mockito.mock(File::class.java))

    producerContext =
        SettableProducerContext(
            imageRequest,
            requestId,
            producerListener,
            Mockito.mock(Any::class.java),
            ImageRequest.RequestLevel.FULL_FETCH,
            false,
            true,
            Priority.MEDIUM,
            config,
        )
    Mockito.`when`(imageRequest.getSourceUri()).thenReturn(contentUri)
    Mockito.doAnswer(
            object : Answer<Any?> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): Any? {
                capturedEncodedImage =
                    EncodedImage.cloneOrNull(invocation.getArguments()[0] as EncodedImage?)
                return null
              }
            }
        )
        .`when`(consumer)
        .onNewResult(ArgumentMatchers.notNull(EncodedImage::class.java), ArgumentMatchers.anyInt())
  }

  @Test
  fun testLocalContentUriFetchCancelled() {
    localContentUriFetchProducer.produceResults(consumer, producerContext)
    producerContext.cancel()
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithCancellation(producerContext, PRODUCER_NAME, null)
    Mockito.verify(consumer).onCancellation()
    executor.runUntilIdle()
    verifyNoMoreInteractions(pooledByteBufferFactory)
  }

  @Test
  @Throws(Exception::class)
  fun testFetchLocalContentUri() {
    val pooledByteBuffer = Mockito.mock(PooledByteBuffer::class.java)
    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(ArgumentMatchers.any(InputStream::class.java))
        )
        .thenReturn(pooledByteBuffer)

    Mockito.`when`(contentResolver.openInputStream(contentUri))
        .thenReturn(Mockito.mock(InputStream::class.java))
    localContentUriFetchProducer.produceResults(consumer, producerContext)

    executor.runUntilIdle()

    assertThat(
            capturedEncodedImage
                ?.getByteBufferRef()
                ?.getUnderlyingReferenceTestOnly()
                ?.getRefCountTestOnly()
        )
        .isEqualTo(2)
    assertThat(capturedEncodedImage?.getByteBufferRef()?.get()).isSameAs(pooledByteBuffer)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener).onUltimateProducerReached(producerContext, PRODUCER_NAME, true)
  }

  @Test(expected = RuntimeException::class)
  @Throws(Exception::class)
  fun testFetchLocalContentUriFailsByThrowing() {
    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(ArgumentMatchers.any(InputStream::class.java))
        )
        .thenThrow(exception)
    Mockito.verify(consumer).onFailure(exception)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithFailure(producerContext, PRODUCER_NAME, exception, null)
    Mockito.verify(producerListener)
        .onUltimateProducerReached(producerContext, PRODUCER_NAME, false)
  }

  companion object {
    private val PRODUCER_NAME = LocalContentUriFetchProducer.PRODUCER_NAME
  }
}
