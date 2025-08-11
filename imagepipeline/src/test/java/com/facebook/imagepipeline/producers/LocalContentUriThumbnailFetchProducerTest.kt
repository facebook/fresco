/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.ContentResolver
import android.database.Cursor
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Basic tests for LocalContentUriThumbnailFetchProducer */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalContentUriThumbnailFetchProducerTest {
  @Mock lateinit var pooledByteBufferFactory: PooledByteBufferFactory
  @Mock lateinit var contentResolver: ContentResolver
  @Mock lateinit var consumer: Consumer<EncodedImage?>
  @Mock lateinit var imageRequest: ImageRequest
  @Mock lateinit var producerListener: ProducerListener2
  @Mock lateinit var exception: Exception
  @Mock lateinit var cursor: Cursor
  @Mock lateinit var thumbnailFile: File
  @Mock lateinit var config: ImagePipelineConfig

  private lateinit var executor: TestExecutorService
  private lateinit var producerContext: SettableProducerContext
  private val requestId = "requestId"
  private lateinit var contentUri: Uri
  private lateinit var localContentUriThumbnailFetchProducer: LocalContentUriThumbnailFetchProducer
  private lateinit var mockedMediaStoreImagesThumbnails: MockedStatic<MediaStore.Images.Thumbnails>
  private lateinit var mockedFileUtil: MockedStatic<LocalContentUriThumbnailFetchProducer.FileUtil>
  private lateinit var mockedFileInputStream: MockedConstruction<FileInputStream>
  private lateinit var mockedExifInterface: MockedConstruction<ExifInterface>
  private lateinit var mockedEncodedImage: MockedConstruction<EncodedImage>

  @Before
  @Throws(Exception::class)
  fun setUp() {
    mockedMediaStoreImagesThumbnails = Mockito.mockStatic(MediaStore.Images.Thumbnails::class.java)
    MockitoAnnotations.initMocks(this)

    executor = TestExecutorService(FakeClock())
    localContentUriThumbnailFetchProducer =
        LocalContentUriThumbnailFetchProducer(executor, pooledByteBufferFactory, contentResolver)
    contentUri = Uri.parse("content://media/external/images/media/1")

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
            config)
    Mockito.`when`(imageRequest.sourceUri).thenReturn(contentUri)

    mockMediaStoreCursor()
    mockContentResolver()
    mockThumbnailFile()
  }

  @After
  fun tearDown() {
    mockedMediaStoreImagesThumbnails.close()
    mockedFileUtil.close()
    mockedFileInputStream.close()
    mockedExifInterface.close()
    mockedEncodedImage.close()
  }

  private fun mockMediaStoreCursor() {
    Mockito.`when`(
            MediaStore.Images.Thumbnails.queryMiniThumbnail(
                ArgumentMatchers.any(ContentResolver::class.java),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.any()))
        .thenAnswer { cursor }
    val dataColumnIndex = 5
    Mockito.`when`(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA))
        .thenReturn(dataColumnIndex)
    Mockito.`when`(cursor.getString(dataColumnIndex)).thenReturn(THUMBNAIL_FILE_NAME)
    Mockito.`when`(cursor.moveToFirst()).thenReturn(true)
  }

  @Throws(Exception::class)
  private fun mockThumbnailFile() {
    mockedFileUtil = Mockito.mockStatic(LocalContentUriThumbnailFetchProducer.FileUtil::class.java)
    Mockito.`when`(LocalContentUriThumbnailFetchProducer.FileUtil.exists(ArgumentMatchers.any()))
        .thenReturn(true)
    Mockito.`when`(LocalContentUriThumbnailFetchProducer.FileUtil.length(ArgumentMatchers.any()))
        .thenReturn(THUMBNAIL_FILE_SIZE)
    mockedFileInputStream = Mockito.mockConstruction(FileInputStream::class.java)
    mockedExifInterface = Mockito.mockConstruction(ExifInterface::class.java)
    mockedEncodedImage =
        Mockito.mockConstruction(
            EncodedImage::class.java,
            MockedConstruction.MockInitializer {
                mock: EncodedImage,
                context: MockedConstruction.Context ->
              Mockito.`when`(mock.getSize()).thenReturn(THUMBNAIL_FILE_SIZE.toInt())
            })
  }

  @Throws(Exception::class)
  private fun mockContentResolver() {
    Mockito.`when`(
            contentResolver.query(
                ArgumentMatchers.eq(contentUri),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()))
        .thenReturn(cursor)
    Mockito.`when`(contentResolver.openInputStream(contentUri))
        .thenReturn(Mockito.mock(InputStream::class.java))
  }

  @Test
  fun testLocalContentUriFetchCancelled() {
    mockResizeOptions(512, 384)

    produceResults()

    producerContext.cancel()
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithCancellation(producerContext, PRODUCER_NAME, null)
    Mockito.verify(consumer).onCancellation()
    executor.runUntilIdle()
    verifyZeroInteractions(pooledByteBufferFactory)
  }

  @Test
  @Throws(Exception::class)
  fun testFetchLocalContentUri() {
    mockResizeOptions(512, 384)

    val pooledByteBuffer = Mockito.mock(PooledByteBuffer::class.java)
    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(ArgumentMatchers.any(InputStream::class.java)))
        .thenReturn(pooledByteBuffer)

    produceResultsAndRunUntilIdle()

    assertConsumerReceivesImage()
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, null)
    Mockito.verify(producerListener).onUltimateProducerReached(producerContext, PRODUCER_NAME, true)
  }

  @Test(expected = RuntimeException::class)
  @Throws(Exception::class)
  fun testFetchLocalContentUriFailsByThrowing() {
    mockResizeOptions(512, 384)

    Mockito.`when`(
            pooledByteBufferFactory.newByteBuffer(ArgumentMatchers.any(InputStream::class.java)))
        .thenThrow(exception)
    Mockito.verify(consumer).onFailure(exception)
    Mockito.verify(producerListener).onProducerStart(producerContext, PRODUCER_NAME)
    Mockito.verify(producerListener)
        .onProducerFinishWithFailure(producerContext, PRODUCER_NAME, exception, null)
    Mockito.verify(producerListener)
        .onUltimateProducerReached(producerContext, PRODUCER_NAME, false)
  }

  @Test
  fun testIsLargerThanThumbnailMaxSize() {
    mockResizeOptions(1000, 384)

    produceResultsAndRunUntilIdle()

    assertConsumerReceivesNull()
  }

  @Test
  fun testWithoutResizeOptions() {
    produceResultsAndRunUntilIdle()

    assertConsumerReceivesNull()
  }

  private fun mockResizeOptions(width: Int, height: Int) {
    val resizeOptions = ResizeOptions(width, height)
    Mockito.`when`(imageRequest.resizeOptions).thenReturn(resizeOptions)
  }

  private fun produceResults() {
    localContentUriThumbnailFetchProducer.produceResults(consumer, producerContext)
  }

  private fun produceResultsAndRunUntilIdle() {
    localContentUriThumbnailFetchProducer.produceResults(consumer, producerContext)
    executor.runUntilIdle()
  }

  private fun assertConsumerReceivesNull() {
    Mockito.verify(consumer).onNewResult(null, Consumer.IS_LAST)
    Mockito.verifyNoMoreInteractions(consumer)

    verifyZeroInteractions(pooledByteBufferFactory)
  }

  private fun assertConsumerReceivesImage() {
    val resultCaptor = ArgumentCaptor.forClass(EncodedImage::class.java)
    Mockito.verify(consumer)
        .onNewResult(resultCaptor.capture(), ArgumentMatchers.eq(Consumer.IS_LAST))

    assertThat(resultCaptor.getValue()).isNotNull()
    assertThat(resultCaptor.getValue()?.getSize()).isEqualTo(THUMBNAIL_FILE_SIZE.toInt())

    Mockito.verifyNoMoreInteractions(consumer)
  }

  companion object {
    private const val PRODUCER_NAME = LocalContentUriThumbnailFetchProducer.PRODUCER_NAME
    private const val THUMBNAIL_FILE_NAME = "////sdcard/thumb.jpg"
    private const val THUMBNAIL_FILE_SIZE: Long = 1374
  }
}
