/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.factory

import android.graphics.Bitmap
import android.graphics.Rect
import com.facebook.animated.webp.WebPImage
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

/** Tests for [AnimatedImageFactory] */
@RunWith(RobolectricTestRunner::class)
class AnimatedImageFactoryWebPImplTest {
  private var mockAnimatedDrawableBackendProvider: AnimatedDrawableBackendProvider? = null
  private var mockBitmapFactory: PlatformBitmapFactory? = null
  private var animatedImageFactory: AnimatedImageFactory? = null
  private var webPImageMockedStatic: MockedStatic<WebPImage>? = null
  private var webPImageMock: WebPImage? = null

  @Before
  fun setup() {
    webPImageMockedStatic = Mockito.mockStatic(WebPImage::class.java)
    webPImageMock = mock<WebPImage>()
    mockAnimatedDrawableBackendProvider = mock<AnimatedDrawableBackendProvider>()
    mockBitmapFactory = mock<PlatformBitmapFactory>()

    val backendProvider = mockAnimatedDrawableBackendProvider
    val bitmapFactory = mockBitmapFactory
    if (backendProvider != null && bitmapFactory != null) {
      animatedImageFactory = AnimatedImageFactoryImpl(backendProvider, bitmapFactory, false)
    }
    AnimatedImageFactoryImpl.webpAnimatedImageDecoder = webPImageMock
  }

  @After
  fun tearDown() {
    webPImageMockedStatic?.close()
  }

  @Test
  fun testCreateDefaultsUsingPointer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()

    // Expect a call to WebPImage.decodeFromNativeMemory
    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            webPImageMock?.decodeFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.getNativePtr()),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            ))
        .thenReturn(mockWebPImage)

    testCreateDefaults(mockWebPImage, byteBuffer)
  }

  @Test
  fun testCreateDefaultsUsingByteBuffer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()

    // Expect a call to WebPImage.decodeFromByteBuffer
    val byteBuffer: TrivialBufferPooledByteBuffer = createDirectByteBuffer()
    whenever(
            webPImageMock?.decodeFromByteBuffer(
                ArgumentMatchers.any<ByteBuffer>(),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            ))
        .thenReturn(mockWebPImage)

    testCreateDefaults(mockWebPImage, byteBuffer)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithPreviewBitmapUsingPointer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()
    val mockBitmap: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to WebPImage.decodeFromNativeMemory
    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            webPImageMock?.decodeFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.nativePtr),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            ))
        .thenReturn(mockWebPImage)
    whenever(mockWebPImage.width).thenReturn(50)
    whenever(mockWebPImage.height).thenReturn(50)

    testCreateWithPreviewBitmap(mockWebPImage, byteBuffer, mockBitmap)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithPreviewBitmapUsingByteBuffer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()
    val mockBitmap: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to WebPImage.decodeFromByteBuffer
    val byteBuffer: TrivialBufferPooledByteBuffer = createDirectByteBuffer()
    whenever(
            webPImageMock?.decodeFromByteBuffer(
                ArgumentMatchers.any<ByteBuffer>(),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            ))
        .thenReturn(mockWebPImage)
    whenever(mockWebPImage.width).thenReturn(50)
    whenever(mockWebPImage.height).thenReturn(50)

    testCreateWithPreviewBitmap(mockWebPImage, byteBuffer, mockBitmap)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithDecodeAlFramesUsingPointer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()

    val mockBitmap1: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)
    val mockBitmap2: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to WebPImage.decodeFromNativeMemory
    val byteBuffer: TrivialPooledByteBuffer = createByteBuffer()
    whenever(
            webPImageMock?.decodeFromNativeMemory(
                ArgumentMatchers.eq(byteBuffer.nativePtr),
                ArgumentMatchers.eq(byteBuffer.size()),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            ))
        .thenReturn(mockWebPImage)
    whenever(mockWebPImage.width).thenReturn(50)
    whenever(mockWebPImage.height).thenReturn(50)

    testCreateWithDecodeAlFrames(mockWebPImage, byteBuffer, mockBitmap1, mockBitmap2)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateWithDecodeAlFramesUsingByteBuffer() {
    val mockWebPImage: WebPImage = mock<WebPImage>()

    val mockBitmap1: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)
    val mockBitmap2: Bitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG)

    // Expect a call to WebPImage.decodeFromByteBuffer
    val byteBuffer: TrivialBufferPooledByteBuffer = createDirectByteBuffer()
    whenever(
            webPImageMock?.decodeFromByteBuffer(
                ArgumentMatchers.any<ByteBuffer>(),
                Matchers.any<ImageDecodeOptions?>(ImageDecodeOptions::class.java),
            ))
        .thenReturn(mockWebPImage)
    whenever(mockWebPImage.width).thenReturn(50)
    whenever(mockWebPImage.height).thenReturn(50)

    testCreateWithDecodeAlFrames(mockWebPImage, byteBuffer, mockBitmap1, mockBitmap2)
  }

  private fun testCreateDefaults(mockWebPImage: WebPImage, byteBuffer: PooledByteBuffer) {
    val encodedImage: EncodedImage =
        EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER))
    encodedImage.imageFormat = ImageFormat.UNKNOWN

    val closeableImage: CloseableAnimatedImage? =
        animatedImageFactory?.decodeWebP(
            encodedImage,
            ImageDecodeOptions.defaults(),
            DEFAULT_BITMAP_CONFIG,
        ) as? CloseableAnimatedImage

    // Verify we got the right result
    val imageResult: AnimatedImageResult? = closeableImage?.imageResult
    assertThat(imageResult?.image).isSameAs(mockWebPImage)
    assertThat(imageResult?.previewBitmap).isNull()
    assertThat(imageResult?.hasDecodedFrame(0) == true).isFalse()

    // Should not have interacted with these.
    mockAnimatedDrawableBackendProvider?.let { Mockito.verifyNoInteractions(it) }
    mockBitmapFactory?.let { Mockito.verifyNoInteractions(it) }
  }

  @Throws(Exception::class)
  private fun testCreateWithPreviewBitmap(
      mockWebPImage: WebPImage,
      byteBuffer: PooledByteBuffer,
      mockBitmap: Bitmap,
  ) {
    // For decoding preview frame, expect some calls.
    val mockAnimatedDrawableBackend: AnimatedDrawableBackend = createAnimatedDrawableBackendMock(1)

    whenever(
            mockAnimatedDrawableBackendProvider?.get(
                Matchers.any<AnimatedImageResult?>(AnimatedImageResult::class.java),
                Matchers.isNull<Rect?>(Rect::class.java),
            ))
        .thenReturn(mockAnimatedDrawableBackend)
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
          animatedImageFactory?.decodeWebP(encodedImage, imageDecodeOptions, DEFAULT_BITMAP_CONFIG)
              as? CloseableAnimatedImage

      // Verify we got the right result
      val imageResult: AnimatedImageResult? = closeableImage?.imageResult
      assertThat(imageResult?.image).isSameAs(mockWebPImage)
      assertThat(imageResult?.previewBitmap).isNotNull()
      assertThat(imageResult?.hasDecodedFrame(0) == true).isFalse()

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
    // For decoding preview frame, expect some calls.
    val mockAnimatedDrawableBackend: AnimatedDrawableBackend = createAnimatedDrawableBackendMock(2)
    whenever(
            mockAnimatedDrawableBackendProvider?.get(
                Matchers.any<AnimatedImageResult?>(AnimatedImageResult::class.java),
                Matchers.isNull<Rect?>(Rect::class.java),
            ))
        .thenReturn(mockAnimatedDrawableBackend)

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
          animatedImageFactory?.decodeWebP(encodedImage, imageDecodeOptions, DEFAULT_BITMAP_CONFIG)
              as? CloseableAnimatedImage

      // Verify we got the right result
      val imageResult: AnimatedImageResult? = closeableImage?.imageResult
      assertThat(imageResult?.image).isSameAs(mockWebPImage)
      assertThat(imageResult?.getDecodedFrame(0)).isNotNull()
      assertThat(imageResult?.getDecodedFrame(1)).isNotNull()
      assertThat(imageResult?.previewBitmap).isNotNull()

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
      val mockCompositor = animatedImageCompositorConstruction.constructed().getOrNull(0)
      mockCompositor?.let { compositor ->
        verify(compositor).renderFrame(0, mockBitmap1)
        verify(compositor).renderFrame(1, mockBitmap2)
      }
    }
  }

  /**
   * Creates the mock for the AnimatedDrawableBackend with the number of frame
   *
   * @param frameCount The number of frame to mock
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
