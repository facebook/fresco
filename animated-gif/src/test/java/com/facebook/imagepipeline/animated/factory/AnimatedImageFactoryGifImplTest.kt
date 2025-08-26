/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.factory

import android.graphics.Bitmap
import android.graphics.Rect
import com.facebook.animated.gif.GifImage
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend
import com.facebook.imagepipeline.animated.base.AnimatedImageResult
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.CloseableAnimatedImage
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.testing.MockBitmapFactory
import com.facebook.imagepipeline.testing.TrivialBufferPooledByteBuffer
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import org.junit.After
import org.junit.Assert
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

/** Tests for [AnimatedImageFactory] */
@RunWith(RobolectricTestRunner::class)
class AnimatedImageFactoryGifImplTest {
  private var mockAnimatedDrawableBackendProvider: AnimatedDrawableBackendProvider? = null
  private var mockBitmapFactory: PlatformBitmapFactory? = null
  private var animatedImageFactory: AnimatedImageFactory? = null

  private var gifImageMock: GifImage? = null
  private var gifImageStaticMock: MockedStatic<GifImage?>? = null

  @Before
  fun setup() {
    gifImageStaticMock = Mockito.mockStatic(GifImage::class.java)
    gifImageMock = mock<GifImage>()

    mockAnimatedDrawableBackendProvider = mock<AnimatedDrawableBackendProvider>()
    mockBitmapFactory = mock<PlatformBitmapFactory>()

    val backendProvider = mockAnimatedDrawableBackendProvider
    val bitmapFactory = mockBitmapFactory
    if (backendProvider != null && bitmapFactory != null) {
      animatedImageFactory = AnimatedImageFactoryImpl(backendProvider, bitmapFactory, false)
    }

    AnimatedImageFactoryImpl.gifAnimatedImageDecoder = gifImageMock
  }

  @After
  fun tearDown() {
    gifImageStaticMock?.close()
  }

  @Test
  fun testCreateDefaultsUsingPointer() {
    val mockGifImage: GifImage? = mock<GifImage>()

    // Expect a call to GifImage.createFromByteBuffer
    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            gifImageMock?.decodeFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.getNativePtr()),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockGifImage)

    testCreateDefaults(mockGifImage, byteBuffer)
  }

  @Test
  fun testCreateDefaultsUsingByteBuffer() {
    val mockGifImage: GifImage? = mock<GifImage>()

    // Expect a call to GifImage.createFromByteBuffer
    val byteBuffer: TrivialBufferPooledByteBuffer = createDirectByteBuffer()
    whenever(
            gifImageMock?.decodeFromByteBuffer(
                ArgumentMatchers.eq<ByteBuffer?>(byteBuffer.getByteBuffer()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockGifImage)

    testCreateDefaults(mockGifImage, byteBuffer)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithPreviewBitmapUsingPointer() {
    val mockGifImage: GifImage = mock<GifImage>()
    val mockBitmap: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to GifImage.createFromByteBuffer
    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            gifImageMock?.decodeFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.getNativePtr()),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockGifImage)
    whenever(mockGifImage.getWidth()).thenReturn(50)
    whenever(mockGifImage.getHeight()).thenReturn(50)

    testCreateWithPreviewBitmap(mockGifImage, mockBitmap, byteBuffer)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithPreviewBitmapUsingByteBuffer() {
    val mockGifImage: GifImage = mock<GifImage>()
    val mockBitmap: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to GifImage.createFromByteBuffer
    val byteBuffer: TrivialBufferPooledByteBuffer = createDirectByteBuffer()
    whenever(
            gifImageMock?.decodeFromByteBuffer(
                ArgumentMatchers.eq<ByteBuffer?>(byteBuffer.getByteBuffer()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockGifImage)
    whenever(mockGifImage.getWidth()).thenReturn(50)
    whenever(mockGifImage.getHeight()).thenReturn(50)

    testCreateWithPreviewBitmap(mockGifImage, mockBitmap, byteBuffer)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithDecodeAlFramesUsingPointer() {
    val mockGifImage: GifImage = mock<GifImage>()

    val mockBitmap1: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)
    val mockBitmap2: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to GifImage.createFromByteBuffer
    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            gifImageMock?.decodeFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.getNativePtr()),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockGifImage)
    whenever(mockGifImage.getWidth()).thenReturn(50)
    whenever(mockGifImage.getHeight()).thenReturn(50)

    testCreateWithDecodeAllFrames(mockGifImage, mockBitmap1, mockBitmap2, byteBuffer)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithDecodeAlFramesUsingByteBuffer() {
    val mockGifImage: GifImage = mock<GifImage>()

    val mockBitmap1: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)
    val mockBitmap2: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to GifImage.createFromByteBuffer
    val byteBuffer: TrivialBufferPooledByteBuffer = createDirectByteBuffer()
    whenever(
            gifImageMock?.decodeFromByteBuffer(
                ArgumentMatchers.eq<ByteBuffer?>(byteBuffer.getByteBuffer()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            )
        )
        .thenReturn(mockGifImage)
    whenever(mockGifImage.getWidth()).thenReturn(50)
    whenever(mockGifImage.getHeight()).thenReturn(50)

    testCreateWithDecodeAllFrames(mockGifImage, mockBitmap1, mockBitmap2, byteBuffer)
  }

  private fun testCreateDefaults(mockGifImage: GifImage?, byteBuffer: PooledByteBuffer) {
    val encodedImage: EncodedImage =
        EncodedImage(CloseableReference.of<PooledByteBuffer?>(byteBuffer, FAKE_RESOURCE_RELEASER))
    encodedImage.setImageFormat(ImageFormat.UNKNOWN)

    val closeableImage: CloseableAnimatedImage? =
        animatedImageFactory?.decodeGif(
            encodedImage,
            ImageDecodeOptions.defaults(),
            DEFAULT_BITMAP_CONFIG,
        ) as? CloseableAnimatedImage

    // Verify we got the right result
    val imageResult: AnimatedImageResult? = closeableImage?.getImageResult()
    Assert.assertSame(mockGifImage, imageResult?.getImage())
    Assert.assertNull(imageResult?.getPreviewBitmap())
    Assert.assertFalse(imageResult?.hasDecodedFrame(0) ?: false)

    // Should not have interacted with these.
    mockAnimatedDrawableBackendProvider?.let { Mockito.verifyNoInteractions(it) }
    mockBitmapFactory?.let { Mockito.verifyNoInteractions(it) }
  }

  @Throws(Exception::class)
  private fun testCreateWithPreviewBitmap(
      mockGifImage: GifImage?,
      mockBitmap: Bitmap,
      byteBuffer: PooledByteBuffer,
  ) {
    // For decoding preview frame, expect some calls.
    val mockAnimatedDrawableBackend: AnimatedDrawableBackend = createAnimatedDrawableBackendMock(1)
    whenever(
            mockAnimatedDrawableBackendProvider?.get(
                Matchers.any<AnimatedImageResult?>(AnimatedImageResult::class.java),
                Matchers.isNull<Rect?>(Rect::class.java),
            )
        )
        .thenReturn(mockAnimatedDrawableBackend)
    whenever(mockBitmapFactory?.createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG))
        .thenReturn(CloseableReference.of<Bitmap?>(mockBitmap, FAKE_BITMAP_RESOURCE_RELEASER))

    Mockito.mockConstruction(AnimatedImageCompositor::class.java).use {
        animatedImageCompositorConstruction: MockedConstruction<AnimatedImageCompositor> ->
      val imageDecodeOptions: ImageDecodeOptions =
          ImageDecodeOptions.newBuilder().setDecodePreviewFrame(true).build()
      val encodedImage: EncodedImage =
          EncodedImage(CloseableReference.of<PooledByteBuffer?>(byteBuffer, FAKE_RESOURCE_RELEASER))
      encodedImage.setImageFormat(ImageFormat.UNKNOWN)
      val closeableImage: CloseableAnimatedImage? =
          animatedImageFactory?.decodeGif(encodedImage, imageDecodeOptions, DEFAULT_BITMAP_CONFIG)
              as? CloseableAnimatedImage

      // Verify we got the right result
      val imageResult: AnimatedImageResult? = closeableImage?.getImageResult()
      Assert.assertSame(mockGifImage, imageResult?.getImage())
      Assert.assertNotNull(imageResult?.getPreviewBitmap())
      Assert.assertFalse(imageResult?.hasDecodedFrame(0) ?: false)

      // Should not have interacted with these.
      mockAnimatedDrawableBackendProvider?.let { provider ->
        verify(provider)
            .get(
                Matchers.any<AnimatedImageResult?>(AnimatedImageResult::class.java),
                Matchers.isNull<Rect?>(Rect::class.java),
            )
        verifyNoMoreInteractions(provider)
      }
      mockBitmapFactory?.let { factory ->
        verify(factory).createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG)
        verifyNoMoreInteractions(factory)
      }
      verify(animatedImageCompositorConstruction.constructed()[0]).renderFrame(0, mockBitmap)
    }
  }

  @Throws(Exception::class)
  private fun testCreateWithDecodeAllFrames(
      mockGifImage: GifImage?,
      mockBitmap1: Bitmap,
      mockBitmap2: Bitmap,
      byteBuffer: PooledByteBuffer,
  ) {
    // For decoding preview frame, expect some calls.
    val mockAnimatedDrawableBackend: AnimatedDrawableBackend = createAnimatedDrawableBackendMock(2)

    whenever(
            mockAnimatedDrawableBackendProvider?.get(
                Matchers.any<AnimatedImageResult?>(AnimatedImageResult::class.java),
                Matchers.isNull<Rect?>(Rect::class.java),
            )
        )
        .thenReturn(mockAnimatedDrawableBackend)

    whenever(mockBitmapFactory?.createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG))
        .thenReturn(CloseableReference.of<Bitmap?>(mockBitmap1, FAKE_BITMAP_RESOURCE_RELEASER))
        .thenReturn(CloseableReference.of<Bitmap?>(mockBitmap2, FAKE_BITMAP_RESOURCE_RELEASER))

    Mockito.mockConstruction(AnimatedImageCompositor::class.java).use {
        animatedImageCompositorConstruction: MockedConstruction<AnimatedImageCompositor> ->
      val imageDecodeOptions: ImageDecodeOptions =
          ImageDecodeOptions.newBuilder()
              .setDecodePreviewFrame(true)
              .setDecodeAllFrames(true)
              .build()
      val encodedImage: EncodedImage =
          EncodedImage(CloseableReference.of<PooledByteBuffer?>(byteBuffer, FAKE_RESOURCE_RELEASER))
      encodedImage.setImageFormat(ImageFormat.UNKNOWN)

      val closeableImage: CloseableAnimatedImage? =
          animatedImageFactory?.decodeGif(encodedImage, imageDecodeOptions, DEFAULT_BITMAP_CONFIG)
              as? CloseableAnimatedImage

      // Verify we got the right result
      val imageResult: AnimatedImageResult? = closeableImage?.getImageResult()
      Assert.assertSame(mockGifImage, imageResult?.getImage())
      Assert.assertNotNull(imageResult?.getDecodedFrame(0))
      Assert.assertNotNull(imageResult?.getDecodedFrame(1))
      Assert.assertNotNull(imageResult?.getPreviewBitmap())

      // Should not have interacted with these.
      mockAnimatedDrawableBackendProvider?.let { provider ->
        verify(provider)
            .get(
                Matchers.any<AnimatedImageResult?>(AnimatedImageResult::class.java),
                Matchers.isNull<Rect?>(Rect::class.java),
            )
        verifyNoMoreInteractions(provider)
      }
      mockBitmapFactory?.let { factory ->
        verify(factory, times(2)).createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG)
        verifyNoMoreInteractions(factory)
      }
      val mockCompositor: AnimatedImageCompositor? =
          animatedImageCompositorConstruction.constructed().get(0)
      mockCompositor?.let { compositor ->
        verify(compositor).renderFrame(0, mockBitmap1)
        verify(compositor).renderFrame(1, mockBitmap2)
      }
    }
  }

  private fun createByteBuffer(): TrivialPooledByteBuffer {
    val gifData: ByteArray = createValidGif()
    return TrivialPooledByteBuffer(gifData)
  }

  /**
   * Creates the mock for the AnimatedDrawableBackend with the number of frame * * @param frameCount
   * The number of frame to mock
   */
  private fun createAnimatedDrawableBackendMock(frameCount: Int): AnimatedDrawableBackend {
    // For decoding preview frame, expect some calls.
    val mockAnimatedDrawableBackend: AnimatedDrawableBackend = mock<AnimatedDrawableBackend>()
    whenever(mockAnimatedDrawableBackend.getFrameCount()).thenReturn(frameCount)
    whenever(mockAnimatedDrawableBackend.getWidth()).thenReturn(50)
    whenever(mockAnimatedDrawableBackend.getHeight()).thenReturn(50)
    return mockAnimatedDrawableBackend
  }

  companion object {
    private val DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888

    private val FAKE_RESOURCE_RELEASER: ResourceReleaser<PooledByteBuffer> =
        object : ResourceReleaser<PooledByteBuffer> {
          override fun release(value: PooledByteBuffer) {}
        }

    private val FAKE_BITMAP_RESOURCE_RELEASER: ResourceReleaser<Bitmap> =
        object : ResourceReleaser<Bitmap> {
          override fun release(value: Bitmap) {}
        }

    private fun createDirectByteBuffer(): TrivialBufferPooledByteBuffer {
      val gifData: ByteArray = createValidGif()
      return TrivialBufferPooledByteBuffer(gifData)
    }

    /**
     * Creates a valid GIF Structure: Header + Logical Screen Descriptor + Image Descriptor +
     * Image * Data + Trailer
     */
    private fun createValidGif(): ByteArray {
      val gif = ByteArrayOutputStream()

      try {
        // GIF Header (6 bytes) with GIF89a signature
        gif.write("GIF89a".toByteArray(charset("ASCII")))

        // Logical Screen Descriptor (7 bytes)
        writeShort(gif, 1) // width = 1
        writeShort(gif, 1) // height = 1
        gif.write(0x00) // Packed field (no global color table)
        gif.write(0x00) // Background color index
        gif.write(0x00) // Pixel aspect ratio

        // Image Descriptor (10 bytes)
        gif.write(0x2C) // Image separator
        writeShort(gif, 0) // Left position
        writeShort(gif, 0) // Top position
        writeShort(gif, 1) // width = 1
        writeShort(gif, 1) // height = 1
        gif.write(0x00) // Packed field (no local color table)

        // Image Data
        gif.write(0x02) // LZW minimum code size
        gif.write(0x02) // Sub-block size
        gif.write(0x4C) // Minimal LZW data
        gif.write(0x01) // Minimal LZW data
        gif.write(0x00) // Sub-block terminator

        // GIF Trailer
        gif.write(0x3B)

        return gif.toByteArray()
      } catch (e: IOException) {
        throw RuntimeException("Failed to create test GIF data", e)
      }
    }

    /** Helper method to write a 16-bit value in little-endian format */
    private fun writeShort(stream: ByteArrayOutputStream, value: Int) {
      stream.write(value and 0xFF) // Low byte
      stream.write((value shr 8) and 0xFF) // High byte
    }
  }
}
