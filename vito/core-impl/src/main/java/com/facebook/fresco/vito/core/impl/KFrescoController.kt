/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.facebook.common.callercontext.ContextChain
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSubscriber
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
import java.util.concurrent.Executor

class KFrescoController(
    private val vitoImagePipeline: VitoImagePipeline,
    private val uiThreadExecutor: Executor,
    private val lightweightBackgroundThreadExecutor: Executor,
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
    return KFrescoVitoDrawable() as T
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
      _imageId = imageId
    }

    // TODO(T105148151): use extras
    // val extras = obtainExtras(null, null, drawable)

    // TODO(T105148151): fix internal listeners
    val internalListener = listener
    internalListener?.onSubmit(imageId, callerContext)
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
          return true
        }
      }
    } finally {
      CloseableReference.closeSafely(cachedImage)
    }

    // The image is not in cache -> Set up layers visible until the image is available
    drawable.placeholderLayer.setPlaceholder(imageRequest.resources, options)
    // TODO(T105148151): notify with placeholder drawable parameter
    internalListener?.onPlaceholderSet(imageId, null)

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
              if (dataSource.hasResult()) {
                val result: CloseableReference<CloseableImage> = dataSource.result ?: return
                if (CloseableReference.isValid(result)) {
                  // We avoid cloning result and closing the original for performance reasons
                  drawable.setCloseable(result)
                  val image = result.get()
                  drawable.actualImageLayer.setActualImage(options, image)
                } else {
                  result.close()
                }
              }
              uiThreadExecutor.execute { drawable.invalidateSelf() }
            }
            override fun onFailure(dataSource: DataSource<CloseableReference<CloseableImage>>) {
              if (imageId != drawable.imageId) {
                return
              }
              drawable.actualImageLayer.setError(imageRequest.resources, options)
              uiThreadExecutor.execute { drawable.invalidateSelf() }
            }

            override fun onCancellation(
                dataSource: DataSource<CloseableReference<CloseableImage>>
            ) = Unit

            override fun onProgressUpdate(
                dataSource: DataSource<CloseableReference<CloseableImage>>
            ) {
              // TODO(T105148151): set progress
            }
          },
          lightweightBackgroundThreadExecutor)
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
        borderOptions = imageOptions.borderOptions)
  }

  private fun ImageLayerDataModel.setPlaceholder(resources: Resources, imageOptions: ImageOptions) {
    val model = imageOptions.createPlaceholderModel(resources)
    if (model == null) {
      configure(dataModel = model)
      return
    }
    configure(
        dataModel = model,
        canvasTransformation = imageOptions.createPlaceholderCanvasTransformation(),
        roundingOptions = imageOptions.roundingOptions,
        borderOptions = imageOptions.borderOptions)
  }

  private fun ImageLayerDataModel.setOverlay(resources: Resources, imageOptions: ImageOptions) {
    configure(dataModel = imageOptions.createOverlayModel(resources))
  }

  private fun ImageLayerDataModel.setError(resources: Resources, imageOptions: ImageOptions) {
    val model = imageOptions.createErrorModel(resources)
    if (model == null) {
      configure(dataModel = model)
      return
    }
    configure(
        dataModel = model, canvasTransformation = imageOptions.createErrorCanvasTransformation())
  }
}
