/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import com.facebook.common.callercontext.ContextChain
import com.facebook.common.internal.Supplier
import com.facebook.common.logging.FLog
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.fresco.ui.common.ImagePerfDataListener
import com.facebook.fresco.ui.common.ImagePerfDataNotifier
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
import com.facebook.fresco.vito.source.BitmapImageSource
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.image.ImmutableQualityInfo
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
      drawable: FrescoDrawableInterface,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      contextChain: ContextChain?,
      listener: ImageListener?,
      perfDataListener: ImagePerfDataListener?,
      onFadeListener: OnFadeListener?,
      viewportDimensions: Rect?
  ): Boolean {
    if (drawable !is KFrescoVitoDrawable) {
      FLog.e(TAG, "Drawable not supported $drawable")
      return false
    }

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

      // Setup local perf data listener
      perfDataListener
          ?.let { ImagePerfDataNotifier(it) }
          ?.let { listenerManager.setLocalImagePerfStateListener(it) }

      _imageId = imageId
      this.viewportDimensions = viewportDimensions
    }

    val options: ImageOptions = imageRequest.imageOptions
    val resources: Resources = imageRequest.resources

    drawable.listenerManager.onSubmit(imageId, imageRequest, callerContext, drawable.obtainExtras())
    drawable.imagePerfListener.onImageFetch(drawable)
    drawable.overlayImageLayer.setOverlay(imageRequest.resources, options)

    // Direct bitmap available
    if (imageRequest.imageSource is BitmapImageSource) {
      val bitmap: Bitmap = (imageRequest.imageSource as BitmapImageSource).bitmap
      val closeableBitmap: CloseableBitmap =
          CloseableStaticBitmap.of(bitmap, {}, ImmutableQualityInfo.FULL_QUALITY, 0)
      val bitmapRef = CloseableReference.of<CloseableImage>(closeableBitmap)
      return try {
        // Immediately display the actual image.
        drawable.setFetchSubmitted(true)
        drawable.actualImageLayer.setActualImage(
            resources, options, closeableBitmap, imageToDataModelMapper)
        drawable.invalidateSelf()
        drawable.listenerManager.onFinalImageSet(
            imageId,
            imageRequest,
            ImageOrigin.MEMORY_BITMAP_SHORTCUT,
            closeableBitmap.imageInfo,
            drawable.obtainExtras(null, bitmapRef),
            drawable.actualImageDrawable)
        debugOverlayHandler?.update(drawable)
        true
      } finally {
        CloseableReference.closeSafely(bitmapRef)
      }
    }

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
              resources, options, image, imageToDataModelMapper)
          // TODO(T105148151): trigger listeners
          drawable.invalidateSelf()
          val imageInfo = image.imageInfo
          drawable.listenerManager.onFinalImageSet(
              imageId,
              imageRequest,
              ImageOrigin.MEMORY_BITMAP_SHORTCUT,
              imageInfo,
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
    if (drawable !is KFrescoVitoDrawable) {
      FLog.e(TAG, "Drawable not supported $drawable")
      return
    }
    ImageReleaseScheduler.releaseDelayed(drawable)
  }

  override fun release(drawable: FrescoDrawableInterface) {
    if (drawable !is KFrescoVitoDrawable) {
      FLog.e(TAG, "Drawable not supported $drawable")
      return
    }
    ImageReleaseScheduler.releaseNextFrame(drawable)
  }

  override fun releaseImmediately(drawable: FrescoDrawableInterface) {
    if (drawable !is KFrescoVitoDrawable) {
      FLog.e(TAG, "Drawable not supported $drawable")
      return
    }
    ImageReleaseScheduler.releaseImmediately(drawable)
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

  companion object {
    private const val TAG = "KFrescoController"
  }
}
