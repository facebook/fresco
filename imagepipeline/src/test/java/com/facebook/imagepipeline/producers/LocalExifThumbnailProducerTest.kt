/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.ContentResolver
import android.media.ExifInterface
import android.net.Uri
import android.util.Pair
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import com.facebook.imageutils.BitmapUtil
import com.facebook.imageutils.JfifUtil
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executor
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalExifThumbnailProducerTest {
  @Mock lateinit var exifInterface: ExifInterface
  @Mock lateinit var imageRequest: ImageRequest
  @Mock lateinit var producerListener: ProducerListener2
  @Mock lateinit var consumer: Consumer<EncodedImage?>
  @Mock lateinit var producerContext: ProducerContext
  @Mock lateinit var pooledByteBufferFactory: PooledByteBufferFactory
  @Mock lateinit var thumbnailByteBuffer: PooledByteBuffer
  @Mock lateinit var file: File
  @Mock lateinit var contentResolver: ContentResolver

  private val uri: Uri = Uri.parse("/dummy/path")
  private lateinit var thumbnailBytes: ByteArray
  private lateinit var testExecutorService: TestExecutorService
  private lateinit var testLocalExifThumbnailProducer: TestLocalExifThumbnailProducer
  private var capturedEncodedImage: EncodedImage? = null
  private lateinit var mockedJfifUtil: MockedStatic<JfifUtil>
  private lateinit var mockedBitmapUtil: MockedStatic<BitmapUtil>

  @Before
  @Throws(IOException::class)
  fun setUp() {
    mockedBitmapUtil = Mockito.mockStatic(BitmapUtil::class.java)
    mockedJfifUtil = Mockito.mockStatic(JfifUtil::class.java)
    MockitoAnnotations.initMocks(this)
    testExecutorService = TestExecutorService(FakeClock())

    testLocalExifThumbnailProducer =
        TestLocalExifThumbnailProducer(
            testExecutorService, pooledByteBufferFactory, contentResolver)

    Mockito.`when`(producerContext.imageRequest).thenReturn(imageRequest)
    Mockito.`when`(imageRequest.getSourceUri()).thenReturn(uri)
    Mockito.`when`(producerContext.producerListener).thenReturn(producerListener)

    thumbnailBytes = ByteArray(100)
    Mockito.`when`(exifInterface.hasThumbnail()).thenReturn(true)
    Mockito.`when`(exifInterface.getThumbnail()).thenReturn(thumbnailBytes)
    Mockito.`when`(pooledByteBufferFactory.newByteBuffer(thumbnailBytes))
        .thenReturn(thumbnailByteBuffer)

    Mockito.`when`(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION))
        .thenReturn(ORIENTATION.toString())
    mockedJfifUtil
        .`when`<Int> { JfifUtil.getAutoRotateAngleFromOrientation(ORIENTATION) }
        .thenAnswer { ANGLE }
    mockedBitmapUtil
        .`when`<Pair<Int, Int>> {
          BitmapUtil.decodeDimensions(ArgumentMatchers.any(InputStream::class.java))
        }
        .thenAnswer { Pair(WIDTH, HEIGHT) }

    Mockito.doAnswer(
            object : Answer<Any?> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): Any? {
                capturedEncodedImage =
                    EncodedImage.cloneOrNull(invocation.getArguments()[0] as EncodedImage?)
                return null
              }
            })
        .`when`(consumer)
        .onNewResult(ArgumentMatchers.notNull(EncodedImage::class.java), ArgumentMatchers.anyInt())
  }

  @After
  fun tearDownStaticMocks() {
    mockedJfifUtil.close()
    mockedBitmapUtil.close()
  }

  @Test
  fun testFindExifThumbnail() {
    testLocalExifThumbnailProducer.produceResults(consumer, producerContext)
    testExecutorService.runUntilIdle()
    // Should have 2 references open: The cloned reference when the argument is
    // captured by EncodedImage and the one that is created when
    // getByteBufferRef is called on EncodedImage
    assertThat(
            capturedEncodedImage
                ?.getByteBufferRef()
                ?.getUnderlyingReferenceTestOnly()
                ?.getRefCountTestOnly()
                ?.toLong())
        .isEqualTo(2)
    assertThat(capturedEncodedImage?.getByteBufferRef()?.get()).isSameAs(thumbnailByteBuffer)
    assertThat(capturedEncodedImage?.getImageFormat()).isEqualTo(DefaultImageFormats.JPEG)
    assertThat(capturedEncodedImage?.getWidth()).isEqualTo(WIDTH)
    assertThat(capturedEncodedImage?.getHeight()).isEqualTo(HEIGHT)
    assertThat(capturedEncodedImage?.getRotationAngle()).isEqualTo(ANGLE)
  }

  @Test
  fun testNoExifThumbnail() {
    Mockito.`when`(exifInterface.hasThumbnail()).thenReturn(false)
    testLocalExifThumbnailProducer.produceResults(consumer, producerContext)
    testExecutorService.runUntilIdle()
    Mockito.verify(consumer).onNewResult(null, Consumer.IS_LAST)
  }

  private inner class TestLocalExifThumbnailProducer(
      executor: Executor,
      pooledByteBufferFactory: PooledByteBufferFactory,
      contentResolver: ContentResolver
  ) : LocalExifThumbnailProducer(executor, pooledByteBufferFactory, contentResolver) {
    override fun getExifInterface(uri: Uri): ExifInterface? {
      if (uri == this@LocalExifThumbnailProducerTest.uri) {
        return exifInterface
      }
      return null
    }
  }

  companion object {
    private const val WIDTH = 10
    private const val HEIGHT = 20
    private const val ORIENTATION = 8
    private const val ANGLE = 270
  }
}
