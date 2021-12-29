/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.facebook.common.callercontext.ContextChain
import com.facebook.common.internal.Supplier
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSubscriber
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.vito.core.*
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import com.facebook.fresco.vito.renderer.ImageDataModel
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.ImageInfo
import java.util.concurrent.Executor

class KFrescoController(
    private val vitoImagePipeline: VitoImagePipeline,
    private val uiThreadExecutor: Executor,
    private val lightweightBackgroundThreadExecutor: Executor,
    private val globalImageRequestListener: VitoImageRequestListener? = null,
    private val imagePerfControllerListenerSupplier: Supplier<ControllerListener2<ImageInfo>>? =
        null,
    private val imagePerfListener: VitoImagePerfListener = BaseVitoImagePerfListener(),
    private val drawableFactory: ImageOptionsDrawableFactory? = null
) : FrescoController2 {

  private val imageToDataModelMapper: (CloseableImage, ImageOptions) -> ImageDataModel? = { a, b ->
    b.customDrawableFactory?.createDrawable(a, b)?.let { DrawableImageDataModel(it) }
        ?: when (a) {
          is CloseableBitmap ->
              BitmapImageDataModel(
                  a.underlyingBitmap, java.lang.Boolean.TRUE.equals(a.getExtras()["is_rounded"]))
          // TODO(T105148151): handle rotation for closeable static bitmap, handle other types
          else -> drawableFactory?.createDrawable(a, b)?.let { DrawableImageDataModel(it) }
        }
  }

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
    if (drawable.imageRequest == imageRequest) {
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
      setImageRequest(imageRequest)
      setCallerContext(callerContext)
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
          drawable.isFetchSubmitted = true
          drawable.setCloseable(cachedImage.clone())
          drawable.actualImageLayer.setActualImage(options, image)
          // TODO(T105148151): trigger listeners
          drawable.invalidateSelf()
          drawable.listenerManager.onFinalImageSet(
              imageId,
              imageRequest,
              ImageOrigin.MEMORY_BITMAP_SHORTCUT,
              image,
              drawable.obtainExtras(null, cachedImage),
              drawable.actualImageDrawable)
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
          object : DataSubscriber<CloseableReference<CloseableImage>> {
            override fun onNewResult(dataSource: DataSource<CloseableReference<CloseableImage>>) {
              if (imageId != drawable.imageId) {
                return
              }

              val result: CloseableReference<CloseableImage>? = dataSource.result

              if (result == null || !result.isValid) {
                onFailure(dataSource)
                result?.close()
                return
              }

              // We avoid cloning result and closing the original for performance reasons
              drawable.setCloseable(result)
              val image = result.get()
              drawable.actualImageLayer.setActualImage(options, image)
              if (dataSource.isFinished) {
                drawable.hideProgressLayer()
              }
              if (notifyFinalResult(dataSource)) {
                drawable.listenerManager.onFinalImageSet(
                    imageId,
                    imageRequest,
                    ImageOrigin.MEMORY_BITMAP_SHORTCUT,
                    image,
                    drawable.obtainExtras(dataSource, cachedImage),
                    drawable.actualImageDrawable)
              } else {
                drawable.listenerManager.onIntermediateImageSet(imageId, imageRequest, image)
              }
              uiThreadExecutor.execute { drawable.invalidateSelf() }
            }

            override fun onFailure(dataSource: DataSource<CloseableReference<CloseableImage>>) {
              if (imageId != drawable.imageId) {
                return
              }
              drawable.actualImageLayer.setError(imageRequest.resources, options)
              if (dataSource.isFinished) {
                drawable.hideProgressLayer()
              }
              if (notifyFinalResult(dataSource)) {
                dataSource.result.use { result ->
                  drawable.listenerManager.onFailure(
                      imageId,
                      imageRequest,
                      drawable.actualImageLayer.getDataModel().maybeGetDrawable(),
                      dataSource.failureCause,
                      drawable.obtainExtras(dataSource, result))
                }
              } else {
                drawable.listenerManager.onIntermediateImageFailed(
                    imageId,
                    imageRequest,
                    dataSource.failureCause,
                )
              }
              drawable.imagePerfListener.onImageError(drawable)
              uiThreadExecutor.execute { drawable.invalidateSelf() }
            }

            override fun onCancellation(
                dataSource: DataSource<CloseableReference<CloseableImage>>
            ) = Unit

            override fun onProgressUpdate(
                dataSource: DataSource<CloseableReference<CloseableImage>>
            ) {
              drawable.updateProgress(dataSource)
            }
          },
          uiThreadExecutor) // Keyframes require callbacks to be on the main thread.
    }
    drawable.isFetchSubmitted = true
    drawable.invalidateSelf()

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

  private fun ImageLayerDataModel.setActualImage(
      imageOptions: ImageOptions,
      closeableImage: CloseableImage
  ) {
    configure(
        dataModel = imageToDataModelMapper(closeableImage, imageOptions),
        canvasTransformation = imageOptions.createActualImageCanvasTransformation(),
        roundingOptions = imageOptions.roundingOptions,
        borderOptions = imageOptions.borderOptions,
        colorFilter = imageOptions.actualImageColorFilter)
  }

  private fun ImageLayerDataModel.setPlaceholder(resources: Resources, imageOptions: ImageOptions) {
    val model = imageOptions.createPlaceholderModel(resources)
    if (model == null) {
      reset()
      return
    }
    configure(
        dataModel = model,
        canvasTransformation = imageOptions.createPlaceholderCanvasTransformation(),
        roundingOptions =
            if (imageOptions.placeholderApplyRoundingOptions) imageOptions.roundingOptions
            else null,
        borderOptions =
            if (imageOptions.placeholderApplyRoundingOptions) imageOptions.borderOptions else null)
  }

  private fun ImageLayerDataModel.setOverlay(resources: Resources, imageOptions: ImageOptions) {
    configure(dataModel = imageOptions.createOverlayModel(resources))
  }

  private fun ImageLayerDataModel.setError(resources: Resources, imageOptions: ImageOptions) {
    val model = imageOptions.createErrorModel(resources)
    if (model == null) {
      reset()
      return
    }
    configure(
        dataModel = model, canvasTransformation = imageOptions.createErrorCanvasTransformation())
  }

  private fun notifyFinalResult(
      dataSource: DataSource<CloseableReference<CloseableImage>>?
  ): Boolean {
    return dataSource == null || dataSource.isFinished || dataSource.hasMultipleResults()
  }
}
