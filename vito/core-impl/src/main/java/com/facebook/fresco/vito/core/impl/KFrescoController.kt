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
import com.facebook.common.logging.FLog
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.ImagePerfDataListener
import com.facebook.fresco.ui.common.ImagePerfDataNotifier
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.ImagePerfLoggingListener
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
import com.facebook.fresco.vito.source.DrawableImageSource
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.ImageInfoImpl
import com.facebook.imagepipeline.image.ImmutableQualityInfo
import com.facebook.imagepipeline.systrace.FrescoSystrace.traceSection
import java.util.concurrent.Executor

class KFrescoController(
    private val config: FrescoVitoConfig,
    private val vitoImagePipeline: VitoImagePipeline,
    private val uiThreadExecutor: Executor,
    private val lightweightBackgroundThreadExecutor: Executor,
    private val globalImageRequestListener: VitoImageRequestListener? = null,
    private val imagePerfLoggingListenerSupplier: Supplier<ImagePerfLoggingListener>? = null,
    private val imagePerfListener: VitoImagePerfListener = BaseVitoImagePerfListener(),
    private val drawableFactory: ImageOptionsDrawableFactory? = null,
) : FrescoController2 {

  private val imageToDataModelMapper: (Resources, CloseableImage, ImageOptions) -> ImageDataModel? =
      { r, a, b ->
        traceSection("KFrescoController#imageToDataModelMapper") {
          b.customDrawableFactory?.createDrawable(r, a, b)?.let { createDrawableModel(it, b) }
              ?: when (a) {
                is CloseableBitmap ->
                    BitmapImageDataModel(a.underlyingBitmap, true == a.getExtras()["is_rounded"])
                // TODO(T105148151): handle rotation for closeable static bitmap, handle other types
                else -> {
                  drawableFactory?.createDrawable(r, a, b)?.let { createDrawableModel(it, b) }
                }
              }
              ?: run {
                FLog.e(TAG, "Could not create Drawable for CloseableImage: $b")
                null
              }
        }
      }

  var debugOverlayHandler: DebugOverlayHandler? = null

  @Suppress("UNCHECKED_CAST")
  override fun <T> createDrawable(uiFramework: String?): T where
  T : Drawable,
  T : FrescoDrawableInterface {
    traceSection("KFrescoController#createDrawable") {
      val drawable =
          KFrescoVitoDrawable(
              imagePerfListener,
              config.experimentalResetVitoImageRequestListener(),
              config.experimentalResetLocalVitoImageRequestListener(),
              config.experimentalResetLocalImagePerfStateListener(),
              config.experimentalResetControllerListener2())
      drawable.uiFramework = uiFramework
      imagePerfLoggingListenerSupplier
          ?.get()
          ?.let(drawable.listenerManager::setImagePerfLoggingListener)
      return drawable as T
    }
  }

  override fun fetch(
      drawable: FrescoDrawableInterface,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      contextChain: ContextChain?,
      listener: ImageListener?,
      perfDataListener: ImagePerfDataListener?,
      onFadeListener: OnFadeListener?,
      viewportDimensions: Rect?,
      vitoImageRequestListener: VitoImageRequestListener?,
  ): Boolean {
    traceSection("KFrescoController#fetch") {
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
        listenerManager.setLocalVitoImageRequestListener(vitoImageRequestListener)

        // Setup local perf data listener
        val localPerfStateListener = perfDataListener?.let(::ImagePerfDataNotifier)
        listenerManager.setLocalImagePerfStateListener(localPerfStateListener)

        _imageId = imageId
        this.viewportDimensions = viewportDimensions
      }

      val options: ImageOptions = imageRequest.imageOptions

      drawable.listenerManager.onSubmit(
          imageId, imageRequest, callerContext, drawable.obtainExtras())
      drawable.imagePerfListener.onImageFetch(drawable)
      drawable.overlayImageLayer.setOverlay(imageRequest.resources, options)

      when (val source = imageRequest.imageSource) {
        // Direct bitmap available
        is BitmapImageSource -> {
          val closeableBitmap: CloseableBitmap =
              CloseableStaticBitmap.of(source.bitmap, {}, ImmutableQualityInfo.FULL_QUALITY, 0)
          val bitmapRef = CloseableReference.of<CloseableImage>(closeableBitmap)
          return drawable.setActualImage(imageRequest, bitmapRef)
        }
        // Direct Drawable available
        is DrawableImageSource -> {
          val extras = drawable.obtainExtras(null, null)
          drawable.actualImageLayer.setActualImageDrawable(
              imageRequest.imageOptions, source.drawable)
          drawable.listenerManager.onFinalImageSet(
              imageId,
              imageRequest,
              ImageOrigin.LOCAL,
              ImageInfoImpl(
                  source.drawable.intrinsicWidth,
                  source.drawable.intrinsicHeight,
                  0,
                  ImmutableQualityInfo.FULL_QUALITY,
                  extras.imageExtras ?: emptyMap()),
              extras,
              drawable.actualImageDrawable)
          debugOverlayHandler?.update(drawable)
          return true
        }
      }

      // Check if the image is in cache
      val cachedImage = vitoImagePipeline.getCachedImage(imageRequest)

      val isIntermediateImage =
          config.useIntermediateImagesAsPlaceholder() &&
              cachedImage?.get()?.qualityInfo?.isOfFullQuality != true
      val hasImage = drawable.setActualImage(imageRequest, cachedImage, isIntermediateImage)
      if (hasImage && !isIntermediateImage) {
        // Immediately return since we have the full image
        return true
      }

      if (!hasImage) {
        // The image is not in cache -> Set up layers visible until the image is available
        drawable.placeholderLayer.setPlaceholder(imageRequest.resources, options)
        drawable.listenerManager.onPlaceholderSet(
            imageId, imageRequest, drawable.placeholderLayer.getDataModel().maybeGetDrawable())

        drawable.setupProgressLayer(imageRequest.resources, options)
      }

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
            if (config.handleImageResultInBackground()) lightweightBackgroundThreadExecutor
            else uiThreadExecutor) // Keyframes require callbacks to be on the main thread.
        drawable.dataSource = dataSource
      }
      drawable.setFetchSubmitted(true)
      drawable.invalidateSelf()
      debugOverlayHandler?.update(drawable)
      return false
    }
  }

  override fun releaseDelayed(drawable: FrescoDrawableInterface) {
    traceSection("KFrescoController#releaseDelayed") {
      if (drawable !is KFrescoVitoDrawable) {
        FLog.e(TAG, "Drawable not supported $drawable")
        return
      }
      ImageReleaseScheduler.releaseDelayed(drawable)
    }
  }

  override fun release(drawable: FrescoDrawableInterface) {
    traceSection("KFrescoController#release") {
      if (drawable !is KFrescoVitoDrawable) {
        FLog.e(TAG, "Drawable not supported $drawable")
        return
      }
      ImageReleaseScheduler.releaseNextFrame(drawable)
    }
  }

  override fun releaseImmediately(drawable: FrescoDrawableInterface) {
    traceSection("KFrescoController#releaseImmediately") {
      if (drawable !is KFrescoVitoDrawable) {
        FLog.e(TAG, "Drawable not supported $drawable")
        return
      }
      ImageReleaseScheduler.releaseImmediately(drawable)
    }
  }

  /**
   * Set the actual image to be the given image reference.
   *
   * @return true if the image has been set, false if it failed (e.g. invalid/null image reference)
   */
  private fun KFrescoVitoDrawable.setActualImage(
      imageRequest: VitoImageRequest,
      imageReference: CloseableReference<CloseableImage>?,
      isIntermediateImage: Boolean = false,
  ): Boolean {
    traceSection("KFrescoController#setActualImage") {
      try {
        if (CloseableReference.isValid(imageReference)) {
          // Immediately display the actual image.
          val image = imageReference?.get() ?: return false
          setFetchSubmitted(true)
          closeable = imageReference.clone()
          actualImageLayer.setActualImage(
              imageRequest.resources, imageRequest.imageOptions, image, imageToDataModelMapper)
          // TODO(T105148151): trigger listeners
          invalidateSelf()
          val imageInfo = image.imageInfo
          if (isIntermediateImage) {
            listenerManager.onIntermediateImageSet(imageId, imageRequest, imageInfo)
          } else {
            listenerManager.onFinalImageSet(
                imageId,
                imageRequest,
                ImageOrigin.MEMORY_BITMAP_SHORTCUT,
                imageInfo,
                obtainExtras(null, imageReference),
                actualImageDrawable)
          }
          debugOverlayHandler?.update(this)
          return true
        }
      } finally {
        CloseableReference.closeSafely(imageReference)
      }
      return false
    }
  }

  private fun isAlreadyLoadingImage(
      imageRequest: VitoImageRequest,
      drawable: KFrescoVitoDrawable
  ): Boolean {
    traceSection("KFrescoController#isAlreadyLoadingImage") {
      return if (config.useSmartPropertyDiffing()) {
        imageRequest.equalsIfHasImage(drawable.imageRequest, drawable.hasImage())
      } else {
        imageRequest == drawable.imageRequest
      }
    }
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
