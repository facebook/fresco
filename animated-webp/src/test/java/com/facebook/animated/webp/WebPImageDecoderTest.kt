/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.webp

import android.graphics.Bitmap
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.animated.base.AnimatedImageResult
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.CloseableAnimatedImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.image.ImmutableQualityInfo
import com.facebook.imagepipeline.testing.MockBitmapFactory
import com.facebook.imagepipeline.testing.TrivialBufferPooledByteBuffer
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer
import java.nio.ByteBuffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Matchers
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests for [WebPImageDecoder] */
@RunWith(RobolectricTestRunner::class)
class WebPImageDecoderTest {
  private var mockBitmapFactory: PlatformBitmapFactory? = null
  private var webPImageDecoder: WebPImageDecoder? = null
  private var webPImageMockedStatic: MockedStatic<WebPImage>? = null

  @Before
  fun setup() {
    webPImageMockedStatic = Mockito.mockStatic(WebPImage::class.java)
    mockBitmapFactory = mock<PlatformBitmapFactory>()

    val bitmapFactory = mockBitmapFactory
    if (bitmapFactory != null) {
      webPImageDecoder =
          WebPImageDecoder(
              bitmapFactory,
              isNewRenderImplementation = false,
              downscaleFrameToDrawableDimensions = false,
              treatAnimatedImagesAsStateful = true,
          )
    }
  }

  @After
  fun tearDown() {
    webPImageMockedStatic?.close()
  }

  @Test
  fun testCreateDefaultsUsingPointer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()

    // Expect a call to WebPImage.createFromNativeMemory
    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            WebPImage.createFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.getNativePtr()),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockWebPImage)

    testCreateDefaults(mockWebPImage, byteBuffer)
  }

  @Test
  fun testCreateDefaultsUsingByteBuffer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()

    // Expect a call to WebPImage.createFromByteBuffer
    val byteBuffer: TrivialBufferPooledByteBuffer = createDirectByteBuffer()
    whenever(
            WebPImage.createFromByteBuffer(
                ArgumentMatchers.any<ByteBuffer>(),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockWebPImage)

    testCreateDefaults(mockWebPImage, byteBuffer)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithPreviewBitmapUsingPointer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()
    val mockBitmap: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to WebPImage.createFromNativeMemory
    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            WebPImage.createFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.getNativePtr()),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockWebPImage)
    whenever(mockWebPImage.width).thenReturn(50)
    whenever(mockWebPImage.height).thenReturn(50)
    whenever(mockWebPImage.frameCount).thenReturn(1)
    whenever(mockWebPImage.frameDurations).thenReturn(intArrayOf(100))

    testCreateWithPreviewBitmap(mockWebPImage, byteBuffer, mockBitmap)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithPreviewBitmapUsingByteBuffer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()
    val mockBitmap: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to WebPImage.createFromByteBuffer
    val byteBuffer: TrivialBufferPooledByteBuffer = createDirectByteBuffer()
    whenever(
            WebPImage.createFromByteBuffer(
                ArgumentMatchers.any<ByteBuffer>(),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockWebPImage)
    whenever(mockWebPImage.width).thenReturn(50)
    whenever(mockWebPImage.height).thenReturn(50)
    whenever(mockWebPImage.frameCount).thenReturn(1)
    whenever(mockWebPImage.frameDurations).thenReturn(intArrayOf(100))

    testCreateWithPreviewBitmap(mockWebPImage, byteBuffer, mockBitmap)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithDecodeAlFramesUsingPointer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()

    val mockBitmap1: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)
    val mockBitmap2: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to WebPImage.createFromNativeMemory
    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            WebPImage.createFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.getNativePtr()),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockWebPImage)
    whenever(mockWebPImage.width).thenReturn(50)
    whenever(mockWebPImage.height).thenReturn(50)
    whenever(mockWebPImage.frameCount).thenReturn(2)
    whenever(mockWebPImage.frameDurations).thenReturn(intArrayOf(100, 150))

    testCreateWithDecodeAlFrames(mockWebPImage, byteBuffer, mockBitmap1, mockBitmap2)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithDecodeAlFramesUsingByteBuffer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()

    val mockBitmap1: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)
    val mockBitmap2: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to WebPImage.createFromByteBuffer
    val byteBuffer: TrivialBufferPooledByteBuffer = createDirectByteBuffer()
    whenever(
            WebPImage.createFromByteBuffer(
                ArgumentMatchers.any<ByteBuffer>(),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockWebPImage)
    whenever(mockWebPImage.width).thenReturn(50)
    whenever(mockWebPImage.height).thenReturn(50)
    whenever(mockWebPImage.frameCount).thenReturn(2)
    whenever(mockWebPImage.frameDurations).thenReturn(intArrayOf(100, 150))

    testCreateWithDecodeAlFrames(mockWebPImage, byteBuffer, mockBitmap1, mockBitmap2)
  }

  private fun testCreateDefaults(mockWebPImage: WebPImage, byteBuffer: PooledByteBuffer) {
    val encodedImage: EncodedImage =
        EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER))
    encodedImage.imageFormat = ImageFormat.UNKNOWN

    val closeableImage: CloseableAnimatedImage? =
        webPImageDecoder?.decode(
            encodedImage,
            byteBuffer.size(),
            ImmutableQualityInfo.FULL_QUALITY,
            ImageDecodeOptions.defaults(),
        ) as? CloseableAnimatedImage

    // Verify we got the right result
    val imageResult: AnimatedImageResult? = closeableImage?.imageResult
    assertThat(imageResult?.image).isSameAs(mockWebPImage)
    assertThat(imageResult?.previewBitmap).isNull()
    assertThat(imageResult?.hasDecodedFrame(0) == true).isFalse()

    // Should not have interacted with bitmap factory for basic decoding
    mockBitmapFactory?.let { Mockito.verifyNoInteractions(it) }
  }

  @Throws(Exception::class)
  private fun testCreateWithPreviewBitmap(
      mockWebPImage: WebPImage,
      byteBuffer: PooledByteBuffer,
      mockBitmap: Bitmap,
  ) {
    whenever(mockBitmapFactory?.createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG))
        .thenReturn(CloseableReference.of(mockBitmap, FAKE_BITMAP_RESOURCE_RELEASER))

    Mockito.mockConstruction(AnimatedImageCompositor::class.java).use {
        animatedImageCompositorConstruction: MockedConstruction<AnimatedImageCompositor> ->
      val imageDecodeOptions: ImageDecodeOptions =
          ImageDecodeOptions.newBuilder().setDecodePreviewFrame(true).build()
      val encodedImage: EncodedImage =
          EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER))
      encodedImage.imageFormat = ImageFormat.UNKNOWN
      val closeableImage: CloseableAnimatedImage? =
          webPImageDecoder?.decode(
              encodedImage,
              byteBuffer.size(),
              ImmutableQualityInfo.FULL_QUALITY,
              imageDecodeOptions,
          ) as? CloseableAnimatedImage

      // Verify we got the right result
      val imageResult: AnimatedImageResult? = closeableImage?.imageResult
      assertThat(imageResult?.image).isSameAs(mockWebPImage)
      assertThat(imageResult?.previewBitmap).isNotNull()
      assertThat(imageResult?.hasDecodedFrame(0) == true).isFalse()

      mockBitmapFactory?.let { factory ->
        verify(factory).createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG)
        verifyNoMoreInteractions(factory)
      }
      animatedImageCompositorConstruction.constructed().getOrNull(0)?.let { compositor ->
        verify(compositor).renderFrame(0, mockBitmap)
      }
    }
  }

  @Throws(Exception::class)
  private fun testCreateWithDecodeAlFrames(
      mockWebPImage: WebPImage,
      byteBuffer: PooledByteBuffer,
      mockBitmap1: Bitmap,
      mockBitmap2: Bitmap,
  ) {
    whenever(mockBitmapFactory?.createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG))
        .thenReturn(CloseableReference.of(mockBitmap1, FAKE_BITMAP_RESOURCE_RELEASER))
        .thenReturn(CloseableReference.of(mockBitmap2, FAKE_BITMAP_RESOURCE_RELEASER))

    Mockito.mockConstruction(AnimatedImageCompositor::class.java).use {
        animatedImageCompositorConstruction: MockedConstruction<AnimatedImageCompositor> ->
      val imageDecodeOptions: ImageDecodeOptions =
          ImageDecodeOptions.newBuilder()
              .setDecodePreviewFrame(true)
              .setDecodeAllFrames(true)
              .build()

      val encodedImage: EncodedImage =
          EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER))
      encodedImage.imageFormat = ImageFormat.UNKNOWN

      val closeableImage: CloseableAnimatedImage? =
          webPImageDecoder?.decode(
              encodedImage,
              byteBuffer.size(),
              ImmutableQualityInfo.FULL_QUALITY,
              imageDecodeOptions,
          ) as? CloseableAnimatedImage

      // Verify we got the right result
      val imageResult: AnimatedImageResult? = closeableImage?.imageResult
      assertThat(imageResult?.image).isSameAs(mockWebPImage)
      assertThat(imageResult?.getDecodedFrame(0)).isNotNull()
      assertThat(imageResult?.getDecodedFrame(1)).isNotNull()
      assertThat(imageResult?.previewBitmap).isNotNull()

      mockBitmapFactory?.let { factory ->
        verify(factory, times(2)).createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG)
        verifyNoMoreInteractions(factory)
      }
      val mockCompositor = animatedImageCompositorConstruction.constructed().getOrNull(0)
      mockCompositor?.let { compositor ->
        verify(compositor).renderFrame(0, mockBitmap1)
        verify(compositor).renderFrame(1, mockBitmap2)
      }
    }
  }

  @Test
  fun testDecodeForceStaticImage() {
    val mockWebPImage: WebPImage = mock<WebPImage>()
    val mockBitmap: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            WebPImage.createFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.getNativePtr()),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockWebPImage)
    whenever(mockWebPImage.width).thenReturn(50)
    whenever(mockWebPImage.height).thenReturn(50)
    whenever(mockWebPImage.frameCount).thenReturn(1)
    whenever(mockWebPImage.frameDurations).thenReturn(intArrayOf(100))
    whenever(mockBitmapFactory?.createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG))
        .thenReturn(CloseableReference.of(mockBitmap, FAKE_BITMAP_RESOURCE_RELEASER))

    Mockito.mockConstruction(AnimatedImageCompositor::class.java).use {
        animatedImageCompositorConstruction: MockedConstruction<AnimatedImageCompositor> ->
      val imageDecodeOptions: ImageDecodeOptions =
          ImageDecodeOptions.newBuilder().setForceStaticImage(true).build()
      val encodedImage: EncodedImage =
          EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER))
      encodedImage.imageFormat = ImageFormat.UNKNOWN

      val closeableImage: CloseableStaticBitmap? =
          webPImageDecoder?.decode(
              encodedImage,
              byteBuffer.size(),
              ImmutableQualityInfo.FULL_QUALITY,
              imageDecodeOptions,
          ) as? CloseableStaticBitmap

      // Verify we got a static bitmap instead of animated image
      assertThat(closeableImage).isNotNull()
      assertThat(closeableImage?.underlyingBitmap).isNotNull()

      mockBitmapFactory?.let { factory ->
        verify(factory).createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG)
        verifyNoMoreInteractions(factory)
      }
      verify(animatedImageCompositorConstruction.constructed()[0]).renderFrame(0, mockBitmap)
    }
  }

  @Test
  fun testDecodeWithInvalidWebP() {
    // Create an invalid WebP by using an empty byte buffer
    val invalidWebPData = ByteArray(0) // Empty array will cause validation to fail
    val byteBuffer = TrivialPooledByteBuffer(invalidWebPData)

    val encodedImage: EncodedImage =
        EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER))
    encodedImage.imageFormat = ImageFormat.UNKNOWN

    try {
      webPImageDecoder?.decode(
          encodedImage,
          byteBuffer.size(),
          ImmutableQualityInfo.FULL_QUALITY,
          ImageDecodeOptions.defaults(),
      )
      assertThat(false).`as`("Expected exception to be thrown").isTrue()
    } catch (e: Exception) {
      // Expected - invalid WebP should cause an exception
      assertThat(e).isNotNull()
    }
  }

  companion object {
    private val DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888

    private val FAKE_RESOURCE_RELEASER: ResourceReleaser<PooledByteBuffer> =
        object : ResourceReleaser<PooledByteBuffer> {
          override fun release(value: PooledByteBuffer) = Unit
        }

    private val FAKE_BITMAP_RESOURCE_RELEASER: ResourceReleaser<Bitmap> =
        object : ResourceReleaser<Bitmap> {
          override fun release(value: Bitmap) = Unit
        }

    private fun createByteBuffer(): TrivialPooledByteBuffer {
      val buf = ByteArray(16)
      return TrivialPooledByteBuffer(buf)
    }

    private fun createDirectByteBuffer(): TrivialBufferPooledByteBuffer {
      val buf = ByteArray(16)
      return TrivialBufferPooledByteBuffer(buf)
    }
  }
}
