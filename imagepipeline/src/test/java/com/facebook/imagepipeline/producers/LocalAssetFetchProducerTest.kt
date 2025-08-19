/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
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

/** Basic tests for LocalAssetFetchProducer */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalAssetFetchProducerTest {
  @Mock lateinit var assetManager: AssetManager
  @Mock lateinit var pooledByteBuffer: PooledByteBuffer
  @Mock lateinit var assetFileDescriptor: AssetFileDescriptor
  @Mock lateinit var pooledByteBufferFactory: PooledByteBufferFactory
  @Mock lateinit var consumer: Consumer<EncodedImage?>
  @Mock lateinit var imageRequest: ImageRequest
  @Mock lateinit var producerListener: ProducerListener2
  @Mock lateinit var exception: Exception
  @Mock lateinit var config: ImagePipelineConfig

  private lateinit var executor: TestExecutorService
  private lateinit var producerContext: SettableProducerContext
  private val requestId = "requestId"
  private lateinit var localAssetFetchProducer: LocalAssetFetchProducer
  private var capturedEncodedImage: EncodedImage? = null

  @Before
  @Throws(Exception::class)
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    Mockito.`when`(assetManager.openFd(ArgumentMatchers.eq(TEST_FILENAME)))
        .thenReturn(assetFileDescriptor)
    Mockito.`when`(assetFileDescriptor.length).thenReturn(TEST_DATA_LENGTH.toLong())
    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(
                ArgumentMatchers.any(InputStream::class.java),
                ArgumentMatchers.eq(TEST_DATA_LENGTH),
            ))
        .thenReturn(pooledByteBuffer)

    executor = TestExecutorService(FakeClock())
    localAssetFetchProducer =
        LocalAssetFetchProducer(executor, pooledByteBufferFactory, assetManager)

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
    Mockito.`when`(imageRequest.sourceUri).thenReturn(Uri.parse("asset:///$TEST_FILENAME"))
    Mockito.doAnswer(
            object : Answer<Any?> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): Any? {
                capturedEncodedImage =
                    EncodedImage.cloneOrNull(invocation.arguments[0] as EncodedImage?)
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
  fun testFetchAssetResource() {
    val pooledByteBuffer = Mockito.mock(PooledByteBuffer::class.java)
    Mockito.`when`(
            assetManager.open(
                ArgumentMatchers.eq(TEST_FILENAME),
                ArgumentMatchers.eq(AssetManager.ACCESS_STREAMING),
            ))
        .thenReturn(ByteArrayInputStream(ByteArray(TEST_DATA_LENGTH)))
    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(
                ArgumentMatchers.any(InputStream::class.java),
                ArgumentMatchers.eq(TEST_DATA_LENGTH),
            ))
        .thenReturn(pooledByteBuffer)

    localAssetFetchProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()

    assertThat(
            capturedEncodedImage
                ?.byteBufferRef
                ?.getUnderlyingReferenceTestOnly()
                ?.refCountTestOnly
                ?.toLong())
        .isEqualTo(2)
    assertThat(capturedEncodedImage?.byteBufferRef?.get()).isSameAs(pooledByteBuffer)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, null)
    Mockito.verify(producerListener).onUltimateProducerReached(producerContext, PRODUCER_NAME, true)
  }

  @Test(expected = RuntimeException::class)
  @Throws(Exception::class)
  fun testFetchLocalResourceFailsByThrowing() {
    Mockito.`when`(
            assetManager.open(
                ArgumentMatchers.eq(TEST_FILENAME),
                ArgumentMatchers.eq(AssetManager.ACCESS_STREAMING),
            ))
        .thenThrow(exception)
    localAssetFetchProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()

    Mockito.verify(consumer).onFailure(exception)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithFailure(producerContext, PRODUCER_NAME, exception, null)
    Mockito.verify(producerListener)
        .onUltimateProducerReached(producerContext, PRODUCER_NAME, false)
  }

  companion object {
    private const val PRODUCER_NAME = LocalAssetFetchProducer.PRODUCER_NAME
    private const val TEST_FILENAME = "dummy_asset.jpg"
    private const val TEST_DATA_LENGTH = 337
  }
}
