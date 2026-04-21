/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
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
    MockitoAnnotations.openMocks(this).close()
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

  @Test
  @Throws(Exception::class)
  fun testFetchContactPhotoUri_usesOpenInputStream() {
    val contactPhotoUri = Uri.parse("content://com.android.contacts/contacts/1/photo")
    Mockito.`when`(imageRequest.getSourceUri()).thenReturn(contactPhotoUri)

    val testData = byteArrayOf(10, 20, 30, 40, 50)
    Mockito.`when`(contentResolver.openInputStream(contactPhotoUri))
        .thenReturn(ByteArrayInputStream(testData))
    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(ArgumentMatchers.any(InputStream::class.java))
        )
        .thenReturn(TrivialPooledByteBuffer(testData))

    localContentUriFetchProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()

    assertThat(capturedEncodedImage?.getByteBufferRef()?.get()?.size()).isEqualTo(5)
    Mockito.verify(contentResolver).openInputStream(contactPhotoUri)
    Mockito.verify(contentResolver, Mockito.never())
        .openAssetFileDescriptor(ArgumentMatchers.any(), ArgumentMatchers.anyString())
  }

  @Test
  @Throws(Exception::class)
  fun testFetchContactDisplayPhotoUri_usesAssetFileDescriptor() {
    val displayPhotoUri = Uri.parse("content://com.android.contacts/contacts/1/display_photo")
    Mockito.`when`(imageRequest.getSourceUri()).thenReturn(displayPhotoUri)

    val testData = byteArrayOf(1, 2, 3)
    val mockAssetFd = Mockito.mock(AssetFileDescriptor::class.java)
    Mockito.`when`(contentResolver.openAssetFileDescriptor(displayPhotoUri, "r"))
        .thenReturn(mockAssetFd)
    Mockito.`when`(mockAssetFd.createInputStream())
        .thenReturn(Mockito.mock(FileInputStream::class.java))
    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(ArgumentMatchers.any(InputStream::class.java))
        )
        .thenReturn(TrivialPooledByteBuffer(testData))

    localContentUriFetchProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()

    assertThat(capturedEncodedImage?.getByteBufferRef()?.get()?.size()).isEqualTo(3)
    Mockito.verify(contentResolver).openAssetFileDescriptor(displayPhotoUri, "r")
  }

  @Test
  @Throws(Exception::class)
  fun testFetchContactDisplayPhotoUri_ioException_reportsFailure() {
    val displayPhotoUri = Uri.parse("content://com.android.contacts/contacts/1/display_photo")
    Mockito.`when`(imageRequest.getSourceUri()).thenReturn(displayPhotoUri)

    Mockito.`when`(contentResolver.openAssetFileDescriptor(displayPhotoUri, "r"))
        .thenThrow(FileNotFoundException("original error"))

    localContentUriFetchProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()

    Mockito.verify(consumer).onFailure(ArgumentMatchers.any(IOException::class.java))
  }

  @Test
  @Throws(Exception::class)
  fun testFetchCameraUri_fileNotFound_fallsBackToContentResolver() {
    val cameraUri = Uri.parse("content://media/external/images/media/1")
    Mockito.`when`(imageRequest.getSourceUri()).thenReturn(cameraUri)

    Mockito.`when`(contentResolver.openFileDescriptor(cameraUri, "r"))
        .thenThrow(FileNotFoundException("not found"))

    val testData = byteArrayOf(7, 8, 9, 10)
    Mockito.`when`(contentResolver.openInputStream(cameraUri))
        .thenReturn(ByteArrayInputStream(testData))
    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(ArgumentMatchers.any(InputStream::class.java))
        )
        .thenReturn(TrivialPooledByteBuffer(testData))

    localContentUriFetchProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()

    assertThat(capturedEncodedImage?.getByteBufferRef()?.get()?.size()).isEqualTo(4)
    Mockito.verify(contentResolver).openFileDescriptor(cameraUri, "r")
    Mockito.verify(contentResolver).openInputStream(cameraUri)
  }

  companion object {
    private val PRODUCER_NAME = LocalContentUriFetchProducer.PRODUCER_NAME
  }
}
