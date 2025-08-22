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
import android.graphics.Rect
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend
import com.facebook.imagepipeline.animated.base.AnimatedImage
import com.facebook.imagepipeline.animated.base.AnimatedImageResult
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendImpl
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.CloseableAnimatedImage
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.ImmutableQualityInfo

/**
 * Base class for animated image decoders that provides common functionality for decoding animated
 * images. This class contains the shared logic for creating preview bitmaps, decoding all frames,
 * and creating closeable images.
 */
abstract class AnimatedImageDecoderBase(
    protected val platformBitmapFactory: PlatformBitmapFactory,
    protected val downscaleFrameToDrawableDimensions: Boolean,
    protected val isNewRenderImplementation: Boolean,
    protected val treatAnimatedImagesAsStateful: Boolean = true,
) {

  protected val animatedDrawableBackendProvider: AnimatedDrawableBackendProvider =
      createAnimatedDrawableBackendProvider(downscaleFrameToDrawableDimensions)

  companion object {
    /** Creates an AnimatedDrawableBackendProvider for animated image decoding. */
    fun createAnimatedDrawableBackendProvider(
        downscaleFrameToDrawableDimensions: Boolean
    ): AnimatedDrawableBackendProvider {
      return object : AnimatedDrawableBackendProvider {
        override fun get(
            animatedImageResult: AnimatedImageResult,
            bounds: Rect?,
        ): AnimatedDrawableBackend {
          return AnimatedDrawableBackendImpl(
              AnimatedDrawableUtil(),
              animatedImageResult,
              bounds,
              downscaleFrameToDrawableDimensions,
          )
        }
      }
    }
  }

  protected fun getCloseableImage(
      sourceUri: String?,
      options: ImageDecodeOptions,
      image: AnimatedImage,
      bitmapConfig: Bitmap.Config,
  ): CloseableImage {
    var decodedFrames: List<CloseableReference<Bitmap>?>? = null
    var previewBitmap: CloseableReference<Bitmap>? = null
    try {
      val frameForPreview = if (options.useLastFrameForPreview) (image.frameCount - 1) else 0
      if (options.forceStaticImage) {
        return CloseableStaticBitmap.of(
            createPreviewBitmap(image, bitmapConfig, frameForPreview),
            ImmutableQualityInfo.FULL_QUALITY,
            0,
        )
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

  protected fun createPreviewBitmap(
      image: AnimatedImage,
      bitmapConfig: Bitmap.Config,
      frameForPreview: Int,
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
            },
        )
    animatedImageCompositor.renderFrame(frameForPreview, bitmap.get())
    return bitmap
  }

  protected fun decodeAllFrames(
      image: AnimatedImage,
      bitmapConfig: Bitmap.Config,
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
            },
        )
    for (i in 0..<drawableBackend.frameCount) {
      val bitmap = createBitmap(drawableBackend.width, drawableBackend.height, bitmapConfig)
      animatedImageCompositor.renderFrame(i, bitmap.get())
      bitmaps.add(bitmap)
    }
    return bitmaps
  }

  @SuppressLint("NewApi")
  protected fun createBitmap(
      width: Int,
      height: Int,
      bitmapConfig: Bitmap.Config,
  ): CloseableReference<Bitmap> {
    val bitmap = platformBitmapFactory.createBitmapInternal(width, height, bitmapConfig)
    bitmap.get().eraseColor(Color.TRANSPARENT)
    bitmap.get().setHasAlpha(true)
    return bitmap
  }
}
