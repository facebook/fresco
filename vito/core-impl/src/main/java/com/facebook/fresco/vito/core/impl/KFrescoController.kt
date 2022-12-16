/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import com.facebook.common.callercontext.ContextChain
import com.facebook.common.internal.Supplier
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.VitoImagePerfListener
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.VitoImageRequestListener
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory
import com.facebook.fresco.vito.renderer.AnimatedDrawableImageDataModel
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import com.facebook.fresco.vito.renderer.ImageDataModel
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.ImageInfo
import java.util.concurrent.Executor

class KFrescoController(
    private val config: FrescoVitoConfig,
    private val vitoImagePipeline: VitoImagePipeline,
    private val uiThreadExecutor: Executor,
    private val lightweightBackgroundThreadExecutor: Executor,
    private val globalImageRequestListener: VitoImageRequestListener? = null,
    private val imagePerfControllerListenerSupplier: Supplier<ControllerListener2<ImageInfo>>? =
        null,
    private val imagePerfListener: VitoImagePerfListener = BaseVitoImagePerfListener(),
    private val drawableFactory: ImageOptionsDrawableFactory? = null
) : FrescoController2 {

  private val imageToDataModelMapper: (Resources, CloseableImage, ImageOptions) -> ImageDataModel? =
      { r, a, b ->
        b.customDrawableFactory?.createDrawable(r, a, b)?.let { createDrawableModel(it, b) }
            ?: when (a) {
              is CloseableBitmap ->
                  BitmapImageDataModel(
                      a.underlyingBitmap,
                      java.lang.Boolean.TRUE.equals(a.getExtras()["is_rounded"]))
              // TODO(T105148151): handle rotation for closeable static bitmap, handle other types
              else -> {
                drawableFactory?.createDrawable(r, a, b)?.let { createDrawableModel(it, b) }
              }
            }
      }

  var debugOverlayHandler: DebugOverlayHandler? = null

  override fun <T> createDrawable(): T where T : Drawable, T : FrescoDrawableInterface {
    val drawable = KFrescoVitoDrawable(imagePerfListener)
    imagePerfControllerListenerSupplier?.get()?.let {
      drawable.listenerManager.setImagePerfControllerListener(it)
    }
    return drawable as T
  }

  override fun fetch(
      frescoDrawable: FrescoDrawableInterface,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      contextChain: ContextChain?,
      listener: ImageListener?,
      onFadeListener: OnFadeListener?,
      viewportDimensions: Rect?
  ): Boolean {
    val drawable = frescoDrawable as KFrescoVitoDrawable

    // Check if we already fetched that image
    if (isAlreadyLoadingImage(imageRequest, drawable)) {
      ImageReleaseScheduler.cancelAllReleasing(drawable)
      return true
    }
    if (drawable.isFetchSubmitted) {
      drawable.imagePerfListener.onDrawableReconfigured(drawable)
    }
    // We didn't -> reset everything and set up new fetch
    // TODO(T105148151): move to new package so that no legacy impl dep
    val imageId: Long = VitoUtils.generateIdentifier()
    drawable.apply {
      reset()
      this.imageRequest = imageRequest
      this.callerContext = callerContext
      imageListener = listener
      listenerManager.setVitoImageRequestListener(globalImageRequestListener)
      _imageId = imageId
      this.viewportDimensions = viewportDimensions
    }

    drawable.listenerManager.onSubmit(imageId, imageRequest, callerContext, drawable.obtainExtras())
    drawable.imagePerfListener.onImageFetch(drawable)

    val options = imageRequest.imageOptions

    drawable.overlayImageLayer.setOverlay(imageRequest.resources, options)

    // Check if the image is in cache
    val cachedImage = vitoImagePipeline.getCachedImage(imageRequest)
    try {
      if (CloseableReference.isValid(cachedImage)) {
        // Immediately display the actual image.
        val image = cachedImage?.get()
        if (image != null) {
          drawable.setFetchSubmitted(true)
          drawable.closeable = cachedImage.clone()
          drawable.actualImageLayer.setActualImage(
              imageRequest.resources, options, image, imageToDataModelMapper)
          // TODO(T105148151): trigger listeners
          drawable.invalidateSelf()
          drawable.listenerManager.onFinalImageSet(
              imageId,
              imageRequest,
              ImageOrigin.MEMORY_BITMAP_SHORTCUT,
              image,
              drawable.obtainExtras(null, cachedImage),
              drawable.actualImageDrawable)
          debugOverlayHandler?.update(drawable)
          return true
        }
      }
    } finally {
      CloseableReference.closeSafely(cachedImage)
    }

    // The image is not in cache -> Set up layers visible until the image is available
    drawable.placeholderLayer.setPlaceholder(imageRequest.resources, options)
    drawable.listenerManager.onPlaceholderSet(
        imageId, imageRequest, drawable.placeholderLayer.getDataModel().maybeGetDrawable())

    drawable.setupProgressLayer(imageRequest.resources, options)

    // Fetch the image
    lightweightBackgroundThreadExecutor.execute {
      if (imageId != drawable.imageId) {
        return@execute
      }
      val dataSource: DataSource<CloseableReference<CloseableImage>> =
          vitoImagePipeline.fetchDecodedImage(imageRequest, callerContext, null, imageId)
      dataSource.subscribe(
          ImageFetchSubscriber(
              imageId, drawable, imageToDataModelMapper, debugOverlayHandler, uiThreadExecutor),
          uiThreadExecutor) // Keyframes require callbacks to be on the main thread.
      drawable.dataSource = dataSource
    }
    drawable.setFetchSubmitted(true)
    drawable.invalidateSelf()
    debugOverlayHandler?.update(drawable)
    return false
  }

  override fun releaseDelayed(drawable: FrescoDrawableInterface) {
    ImageReleaseScheduler.releaseDelayed(drawable as KFrescoVitoDrawable)
  }

  override fun release(drawable: FrescoDrawableInterface) {
    ImageReleaseScheduler.releaseNextFrame(drawable as KFrescoVitoDrawable)
  }

  override fun releaseImmediately(drawable: FrescoDrawableInterface) {
    ImageReleaseScheduler.releaseImmediately(drawable as KFrescoVitoDrawable)
  }

  private fun isAlreadyLoadingImage(
      imageRequest: VitoImageRequest,
      drawable: KFrescoVitoDrawable
  ): Boolean =
      if (config.useSmartPropertyDiffing()) {
        imageRequest.equalsIfHasImage(drawable.imageRequest, drawable.hasImage())
      } else {
        imageRequest == drawable.imageRequest
      }

  private fun createDrawableModel(drawable: Drawable, options: ImageOptions): ImageDataModel =
      if (drawable is Animatable) {
        AnimatedDrawableImageDataModel(drawable, drawable, options.shouldAutoPlay())
      } else {
        DrawableImageDataModel(drawable)
      }
}
