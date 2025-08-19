/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.net.Uri
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Basic tests for LocalResourceFetchProducer */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalResourceFetchProducerTest {
  @Mock lateinit var resources: Resources
  @Mock lateinit var assetFileDescriptor: AssetFileDescriptor
  @Mock lateinit var pooledByteBufferFactory: PooledByteBufferFactory
  @Mock lateinit var consumer: Consumer<EncodedImage>
  @Mock lateinit var imageRequest: ImageRequest
  @Mock lateinit var producerListener: ProducerListener2
  @Mock lateinit var exception: Exception
  @Mock lateinit var config: ImagePipelineConfig

  private lateinit var executor: TestExecutorService
  private lateinit var producerContext: SettableProducerContext
  private val requestId = "requestId"
  private lateinit var localResourceFetchProducer: LocalResourceFetchProducer
  private var capturedEncodedImage: EncodedImage? = null

  @Before
  @Throws(Exception::class)
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    Mockito.`when`(resources.openRawResourceFd(ArgumentMatchers.eq(TEST_ID)))
        .thenReturn(assetFileDescriptor)
    Mockito.`when`(assetFileDescriptor.length).thenReturn(TEST_DATA_LENGTH.toLong())

    executor = TestExecutorService(FakeClock())
    localResourceFetchProducer =
        LocalResourceFetchProducer(executor, pooledByteBufferFactory, resources)

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
    Mockito.`when`(imageRequest.sourceUri).thenReturn(Uri.parse("res:///$TEST_ID"))
    Mockito.doAnswer(
            object : Answer<Any?> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): Any? {
                capturedEncodedImage =
                    EncodedImage.cloneOrNull(invocation.getArguments()[0] as EncodedImage)
                return null
              }
            })
        .`when`(consumer)
        .onNewResult(ArgumentMatchers.notNull(EncodedImage::class.java), ArgumentMatchers.anyInt())
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    Mockito.verify(pooledByteBufferFactory, Mockito.atMost(1))
        .newByteBuffer(
            ArgumentMatchers.any(InputStream::class.java),
            ArgumentMatchers.eq(TEST_DATA_LENGTH),
        )
  }

  @Test
  @Throws(Exception::class)
  fun testFetchLocalResource() {
    val pooledByteBuffer = Mockito.mock(PooledByteBuffer::class.java)
    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(
                ArgumentMatchers.any(InputStream::class.java),
                ArgumentMatchers.eq(TEST_DATA_LENGTH),
            ))
        .thenReturn(pooledByteBuffer)
    Mockito.`when`(resources.openRawResource(ArgumentMatchers.eq(TEST_ID)))
        .thenReturn(ByteArrayInputStream(ByteArray(TEST_DATA_LENGTH)))

    localResourceFetchProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()

    val encodedImage = capturedEncodedImage
    assertThat(encodedImage).isNotNull()
    assertThat(encodedImage?.byteBufferRef?.underlyingReferenceTestOnly?.refCountTestOnly)
        .isEqualTo(2)
    assertThat(encodedImage?.byteBufferRef?.get()).isSameAs(pooledByteBuffer)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, null)
    Mockito.verify(producerListener).onUltimateProducerReached(producerContext, PRODUCER_NAME, true)
  }

  @Test(expected = RuntimeException::class)
  @Throws(Exception::class)
  fun testFetchLocalResourceFailsByThrowing() {
    Mockito.`when`(resources.openRawResource(ArgumentMatchers.eq(TEST_ID))).thenThrow(exception)
    localResourceFetchProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()
    Mockito.verify(consumer).onFailure(exception)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithFailure(producerContext, PRODUCER_NAME, exception, null)
    Mockito.verify(producerListener)
        .onUltimateProducerReached(producerContext, PRODUCER_NAME, false)
  }

  companion object {
    private val PRODUCER_NAME = LocalResourceFetchProducer.PRODUCER_NAME
    private const val TEST_ID = 1337
    private const val TEST_DATA_LENGTH = 337
  }
}
