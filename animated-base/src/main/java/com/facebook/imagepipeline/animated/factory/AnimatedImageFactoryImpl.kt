/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.animated.factory

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.animated.base.AnimatedImage
import com.facebook.imagepipeline.animated.base.AnimatedImageResult
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.CloseableAnimatedImage
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.image.ImmutableQualityInfo

/** Decoder for animated images. */
class AnimatedImageFactoryImpl
@JvmOverloads
constructor(
    private val animatedDrawableBackendProvider: AnimatedDrawableBackendProvider,
    private val bitmapFactory: PlatformBitmapFactory,
    private val isNewRenderImplementation: Boolean,
    private val treatAnimatedImagesAsStateful: Boolean = true
) : AnimatedImageFactory {

  /**
   * Decodes a GIF into a CloseableImage.
   *
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @param bitmapConfig the Bitmap.Config used to generate the output bitmaps
   * @return a [CloseableImage] for the GIF image
   */
  override fun decodeGif(
      encodedImage: EncodedImage,
      options: ImageDecodeOptions,
      bitmapConfig: Bitmap.Config
  ): CloseableImage {
    if (GifAnimatedImageDecoder == null) {
      throw UnsupportedOperationException(
          "To encode animated gif please add the dependency to the animated-gif module")
    }
    val bytesRef = encodedImage.byteBufferRef
    checkNotNull(bytesRef)

    val validationResult = validateGif(encodedImage)
    if (!validationResult.isValid) {
      Log.w("AnimatedImageFactory", "GIF validation failed: ${validationResult.message}")
      throw UnsupportedOperationException("Invalid GIF: ${validationResult.message}")
    }
    try {
      val input = bytesRef.get()
      val gifImage =
          if (input.byteBuffer != null) {
            GifAnimatedImageDecoder!!.decodeFromByteBuffer(input.byteBuffer!!, options)
          } else {
            GifAnimatedImageDecoder!!.decodeFromNativeMemory(input.nativePtr, input.size(), options)
          }

      return getCloseableImage(encodedImage.source, options, checkNotNull(gifImage), bitmapConfig)
    } finally {
      CloseableReference.closeSafely(bytesRef)
    }
  }

  /**
   * Decode a WebP into a CloseableImage.
   *
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @param bitmapConfig the Bitmap.Config used to generate the output bitmaps
   * @return a [CloseableImage] for the WebP image
   */
  override fun decodeWebP(
      encodedImage: EncodedImage,
      options: ImageDecodeOptions,
      bitmapConfig: Bitmap.Config
  ): CloseableImage {
    if (WebpAnimatedImageDecoder == null) {
      throw UnsupportedOperationException(
          "To encode animated webp please add the dependency to the animated-webp module")
    }
    val bytesRef = encodedImage.byteBufferRef
    checkNotNull(bytesRef)
    try {
      val input = bytesRef.get()
      val webPImage =
          if (input.byteBuffer != null) {
            WebpAnimatedImageDecoder!!.decodeFromByteBuffer(input.byteBuffer!!, options)
          } else {
            WebpAnimatedImageDecoder!!.decodeFromNativeMemory(
                input.nativePtr, input.size(), options)
          }
      return getCloseableImage(encodedImage.source, options, checkNotNull(webPImage), bitmapConfig)
    } finally {
      CloseableReference.closeSafely(bytesRef)
    }
  }

  private fun getCloseableImage(
      sourceUri: String?,
      options: ImageDecodeOptions,
      image: AnimatedImage,
      bitmapConfig: Bitmap.Config
  ): CloseableImage {
    var decodedFrames: List<CloseableReference<Bitmap>?>? = null
    var previewBitmap: CloseableReference<Bitmap>? = null
    try {
      val frameForPreview = if (options.useLastFrameForPreview) (image.frameCount - 1) else 0
      if (options.forceStaticImage) {
        return CloseableStaticBitmap.of(
            createPreviewBitmap(image, bitmapConfig, frameForPreview),
            ImmutableQualityInfo.FULL_QUALITY,
            0)
      }

      if (options.decodeAllFrames) {
        decodedFrames = decodeAllFrames(image, bitmapConfig)
        previewBitmap = CloseableReference.cloneOrNull(decodedFrames[frameForPreview])
      }

      if (options.decodePreviewFrame && previewBitmap == null) {
        previewBitmap = createPreviewBitmap(image, bitmapConfig, frameForPreview)
      }
      val animatedImageResult =
          AnimatedImageResult.newBuilder(image)
              .setPreviewBitmap(previewBitmap)
              .setFrameForPreview(frameForPreview)
              .setDecodedFrames(decodedFrames)
              .setBitmapTransformation(options.bitmapTransformation)
              .setSource(sourceUri)
              .build()
      return CloseableAnimatedImage(animatedImageResult, treatAnimatedImagesAsStateful)
    } finally {
      CloseableReference.closeSafely(previewBitmap)
      CloseableReference.closeSafely(decodedFrames)
    }
  }

  private fun createPreviewBitmap(
      image: AnimatedImage,
      bitmapConfig: Bitmap.Config,
      frameForPreview: Int
  ): CloseableReference<Bitmap> {
    val bitmap = createBitmap(image.width, image.height, bitmapConfig)
    val tempResult = AnimatedImageResult.forAnimatedImage(image)
    val drawableBackend = animatedDrawableBackendProvider.get(tempResult, null)
    val animatedImageCompositor =
        AnimatedImageCompositor(
            drawableBackend,
            isNewRenderImplementation,
            object : AnimatedImageCompositor.Callback {
              override fun onIntermediateResult(frameNumber: Int, bitmap: Bitmap) {
                // Don't care.
              }

              override fun getCachedBitmap(frameNumber: Int): CloseableReference<Bitmap>? = null
            })
    animatedImageCompositor.renderFrame(frameForPreview, bitmap.get())
    return bitmap
  }

  private fun decodeAllFrames(
      image: AnimatedImage,
      bitmapConfig: Bitmap.Config
  ): List<CloseableReference<Bitmap>?> {
    val tempResult = AnimatedImageResult.forAnimatedImage(image)
    val drawableBackend = animatedDrawableBackendProvider.get(tempResult, null)
    val bitmaps: MutableList<CloseableReference<Bitmap>?> = ArrayList(drawableBackend.frameCount)
    val animatedImageCompositor =
        AnimatedImageCompositor(
            drawableBackend,
            isNewRenderImplementation,
            object : AnimatedImageCompositor.Callback {
              override fun onIntermediateResult(frameNumber: Int, bitmap: Bitmap) {
                // Don't care.
              }

              override fun getCachedBitmap(frameNumber: Int): CloseableReference<Bitmap>? =
                  CloseableReference.cloneOrNull(bitmaps[frameNumber])
            })
    for (i in 0..<drawableBackend.frameCount) {
      val bitmap = createBitmap(drawableBackend.width, drawableBackend.height, bitmapConfig)
      animatedImageCompositor.renderFrame(i, bitmap.get())
      bitmaps.add(bitmap)
    }
    return bitmaps
  }

  @SuppressLint("NewApi")
  private fun createBitmap(
      width: Int,
      height: Int,
      bitmapConfig: Bitmap.Config
  ): CloseableReference<Bitmap> {
    val bitmap = bitmapFactory.createBitmapInternal(width, height, bitmapConfig)
    bitmap.get().eraseColor(Color.TRANSPARENT)
    bitmap.get().setHasAlpha(true)
    return bitmap
  }

  sealed class ValidationResult(val isValid: Boolean, val message: String) {
    object Success : ValidationResult(true, "GIF is valid and safe")

    class Failure(message: String) : ValidationResult(false, message)
  }

  companion object {
    @JvmField var GifAnimatedImageDecoder: AnimatedImageDecoder? = null

    @JvmField var WebpAnimatedImageDecoder: AnimatedImageDecoder? = null

    // GIF validation constants
    private const val HEADER_SIZE = 6
    private const val LSD_SIZE = 7
    private val GIF87A_SIGNATURE = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x37, 0x61)
    private val GIF89A_SIGNATURE = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61)
    private val GIF_TRAILER = 0x3B.toByte()

    private fun loadIfPresent(className: String): AnimatedImageDecoder? {
      try {
        val clazz = Class.forName(className)
        return clazz.newInstance() as AnimatedImageDecoder
      } catch (e: Throwable) {
        return null
      }
    }

    init {
      GifAnimatedImageDecoder = loadIfPresent("com.facebook.animated.gif.GifImage")
      WebpAnimatedImageDecoder = loadIfPresent("com.facebook.animated.webp.WebPImage")
    }
  }

  private fun validateGif(encodedImage: EncodedImage): ValidationResult {
    val inputStream =
        encodedImage.inputStream ?: return ValidationResult.Failure("No input stream available")

    try {
      inputStream.use { stream ->
        val header = ByteArray(HEADER_SIZE)

        if (stream.read(header) != HEADER_SIZE) {
          return ValidationResult.Failure("Header too short")
        }

        if (!header.contentEquals(GIF87A_SIGNATURE) && !header.contentEquals(GIF89A_SIGNATURE)) {
          return ValidationResult.Failure("Invalid GIF header")
        }

        val lsd = ByteArray(LSD_SIZE)
        if (stream.read(lsd) != LSD_SIZE) {
          return ValidationResult.Failure("Logical Screen Descriptor too short")
        }

        val width = (lsd[1].toInt() and 0xFF shl 8) or (lsd[0].toInt() and 0xFF)
        val height = (lsd[3].toInt() and 0xFF shl 8) or (lsd[2].toInt() and 0xFF)

        if (width <= 0 || height <= 0) {
          return ValidationResult.Failure("Invalid logical screen size")
        }

        var b = stream.read()
        while (b != -1) {
          if (b == 0x2C) { // Image Descriptor
            val desc = ByteArray(9)
            if (stream.read(desc) != 9) {
              return ValidationResult.Failure("Incomplete image descriptor")
            }

            val frameWidth = (desc[5].toInt() and 0xFF shl 8) or (desc[4].toInt() and 0xFF)
            val frameHeight = (desc[7].toInt() and 0xFF shl 8) or (desc[6].toInt() and 0xFF)

            if (frameWidth == 0 || frameHeight == 0) {
              return ValidationResult.Failure("Frame with 0x0 size found")
            }
          } else if (b == GIF_TRAILER.toInt()) {
            break
          }
          b = stream.read()
        }
      }
      return ValidationResult.Success
    } catch (e: Exception) {
      return ValidationResult.Failure("Error parsing GIF: ${e.message}")
    }
  }
}
