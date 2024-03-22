/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core.impl

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import com.facebook.common.callercontext.ContextChain
import com.facebook.common.internal.ImmutableMap
import com.facebook.common.internal.Supplier
import com.facebook.common.logging.FLog
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.fresco.middleware.MiddlewareUtils.obtainExtras
import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.ui.common.ImagePerfDataListener
import com.facebook.fresco.ui.common.ImagePerfDataNotifier
import com.facebook.fresco.ui.common.ImagePerfNotifier
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.ui.common.VitoUtils.generateIdentifier
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.NopDrawable
import com.facebook.fresco.vito.core.VitoImagePerfListener
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.VitoImageRequestListener
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayFactory2
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.source.BitmapImageSource
import com.facebook.fresco.vito.source.DrawableImageSource
import com.facebook.fresco.vito.source.EmptyImageSource
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.image.ImageInfoImpl
import com.facebook.imagepipeline.image.ImmutableQualityInfo
import java.util.concurrent.Executor

open class FrescoController2Impl(
    private val mConfig: FrescoVitoConfig,
    private val mHierarcher: Hierarcher,
    private val mLightweightBackgroundThreadExecutor: Executor,
    private val mUiThreadExecutor: Executor,
    private val mImagePipeline: VitoImagePipeline,
    private val mGlobalImageListener: VitoImageRequestListener?,
    private val mDebugOverlayFactory: DebugOverlayFactory2,
    private val mImagePerfListenerSupplier: Supplier<ControllerListener2<ImageInfo>>?,
    private val mVitoImagePerfListener: VitoImagePerfListener
) : DrawableDataSubscriber, FrescoController2 {
  override fun <T : Drawable> createDrawable(): T where T : FrescoDrawableInterface {
    return FrescoDrawable2Impl(
        mConfig.useNewReleaseCallback(), mImagePerfListenerSupplier?.get(), mVitoImagePerfListener)
        as T
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
      vitoImageRequestListener: VitoImageRequestListener?
  ): Boolean {
    if (drawable !is FrescoDrawable2Impl) {
      FLog.e(TAG, "Drawable not supported $drawable")
      return false
    }
    val frescoDrawable = drawable

    // Fast path for null-URIs
    if (mConfig.fastPathForEmptyRequests() && imageRequest.imageSource is EmptyImageSource) {
      emptyRequestFastPath(frescoDrawable, imageRequest, callerContext)
      return true
    }

    // Save viewport dimension for future use
    frescoDrawable.viewportDimensions = viewportDimensions

    // Check if we already fetched the image
    if (isAlreadyLoadingImage(frescoDrawable, imageRequest)) {
      frescoDrawable.cancelReleaseNextFrame()
      frescoDrawable.cancelReleaseDelayed()
      return true // already set
    }
    if (frescoDrawable.isFetchSubmitted) {
      frescoDrawable.imagePerfListener.onDrawableReconfigured(frescoDrawable)
    }
    // We didn't -> Reset everything
    frescoDrawable.close()
    // Basic setup
    frescoDrawable.drawableDataSubscriber = this
    frescoDrawable.imageRequest = imageRequest
    frescoDrawable.callerContext = callerContext
    frescoDrawable.imageListener = listener
    frescoDrawable.setVitoImageRequestListener(mGlobalImageListener)
    frescoDrawable.setLocalVitoImageRequestListener(vitoImageRequestListener)

    // Setup local perf data listener
    if (perfDataListener != null) {
      val localPerfStateListener: ImagePerfNotifier = ImagePerfDataNotifier(perfDataListener)
      frescoDrawable.internalListener.setLocalImagePerfStateListener(localPerfStateListener)
    } else {
      frescoDrawable.internalListener.setLocalImagePerfStateListener(null)
    }
    frescoDrawable.setOnFadeListener(onFadeListener)

    // Set layers that are always visible
    frescoDrawable.setOverlayDrawable(
        mHierarcher.buildOverlayDrawable(imageRequest.resources, imageRequest.imageOptions))

    // We're fetching a new image, so we're updating the ID
    val imageId = generateIdentifier()
    frescoDrawable.setImageId(imageId)
    val extras = obtainExtras(null, null, frescoDrawable)

    // Notify listeners that we're about to fetch an image
    frescoDrawable.internalListener.onSubmit(imageId, imageRequest, callerContext, extras)
    frescoDrawable.imagePerfListener.onImageFetch(frescoDrawable)

    // Direct bitmap available
    if (imageRequest.imageSource is BitmapImageSource) {
      val bitmap = (imageRequest.imageSource as BitmapImageSource).bitmap
      val closeableBitmap: CloseableBitmap =
          CloseableStaticBitmap.of(
              bitmap, { noOpReleaser: Bitmap? -> }, ImmutableQualityInfo.FULL_QUALITY, 0)
      val bitmapRef = CloseableReference.of<CloseableImage>(closeableBitmap)
      return try {
        frescoDrawable.imageOrigin = ImageOrigin.MEMORY_BITMAP
        // Immediately display the actual image.
        setActualImage(frescoDrawable, imageRequest, bitmapRef, true, null)
        frescoDrawable.setFetchSubmitted(true)
        mDebugOverlayFactory.update(frescoDrawable, extras)
        true
      } finally {
        CloseableReference.closeSafely(bitmapRef)
      }
    } else if (imageRequest.imageSource is DrawableImageSource) {
      val actualImageDrawable = (imageRequest.imageSource as DrawableImageSource).drawable
      val actualImageWrapperDrawable = frescoDrawable.actualImageWrapper
      mHierarcher.setupActualImageWrapper(
          actualImageWrapperDrawable, imageRequest.imageOptions, drawable.callerContext)
      val actualDrawable =
          mHierarcher.applyRoundingOptions(
              imageRequest.resources, actualImageDrawable, imageRequest.imageOptions)
      actualImageWrapperDrawable.setCurrent(actualDrawable)
      frescoDrawable.setImage(actualImageWrapperDrawable, null)
      frescoDrawable.showImageImmediately()
      frescoDrawable.imageOrigin = ImageOrigin.LOCAL
      frescoDrawable.setFetchSubmitted(true)
      if (imageRequest.imageOptions.shouldAutoPlay() && actualImageDrawable is Animatable) {
        (actualDrawable as Animatable).start()
      }
      val imageInfoExtras: Map<String, Any> = extras.imageExtras ?: HashMap()
      frescoDrawable.internalListener.onFinalImageSet(
          drawable.imageId,
          imageRequest,
          ImageOrigin.LOCAL,
          ImageInfoImpl(
              actualImageDrawable.intrinsicWidth,
              actualImageDrawable.intrinsicHeight,
              0,
              ImmutableQualityInfo.FULL_QUALITY,
              imageInfoExtras),
          extras,
          actualDrawable)
      drawable.imagePerfListener.onImageSuccess(drawable, true)
      mDebugOverlayFactory.update(frescoDrawable, extras)
      return true
    }

    // Check if the image is in cache
    val cachedImage = mImagePipeline.getCachedImage(imageRequest)
    try {
      if (CloseableReference.isValid(cachedImage)) {
        frescoDrawable.imageOrigin = ImageOrigin.MEMORY_BITMAP_SHORTCUT
        // Immediately display the actual image.
        setActualImage(frescoDrawable, imageRequest, cachedImage, true, null)
        frescoDrawable.setFetchSubmitted(true)
        mDebugOverlayFactory.update(frescoDrawable, extras)
        return true
      }
    } finally {
      CloseableReference.closeSafely(cachedImage)
    }

    // The image is not in cache -> Set up layers visible until the image is available
    frescoDrawable.setProgressDrawable(
        mHierarcher.buildProgressDrawable(imageRequest.resources, imageRequest.imageOptions))
    // Immediately show the progress image and set progress to 0
    frescoDrawable.setProgress(0f)
    frescoDrawable.showProgressImmediately()
    setUpPlaceholder(frescoDrawable, imageRequest, imageId)

    // Fetch the image
    val fetchRunnable = Runnable {
      if (imageId != frescoDrawable.imageId) {
        return@Runnable // We're trying to load a different image -> ignore
      }
      val dataSource =
          mImagePipeline.fetchDecodedImage(
              imageRequest, callerContext, frescoDrawable.imageOriginListener, imageId)
      frescoDrawable.setDataSource(imageId, dataSource)
      dataSource.subscribe(frescoDrawable, mUiThreadExecutor)
    }
    if (mConfig.submitFetchOnBgThread()) {
      mLightweightBackgroundThreadExecutor.execute(fetchRunnable)
    } else {
      fetchRunnable.run()
    }
    frescoDrawable.setFetchSubmitted(true)
    mDebugOverlayFactory.update(frescoDrawable, null)
    return false
  }

  private fun emptyRequestFastPath(
      frescoDrawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest,
      callerContext: Any?
  ) {
    frescoDrawable.close()
    frescoDrawable.setVitoImageRequestListener(mGlobalImageListener)
    frescoDrawable.internalListener.onEmptyEvent(callerContext)
    frescoDrawable.setOverlayDrawable(
        mHierarcher.buildOverlayDrawable(imageRequest.resources, imageRequest.imageOptions))
    setUpPlaceholder(frescoDrawable, imageRequest, EMPTY_IMAGE_ID)
  }

  private fun setUpPlaceholder(
      frescoDrawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest,
      imageId: Long
  ) {
    val placeholder =
        mHierarcher.buildPlaceholderDrawable(imageRequest.resources, imageRequest.imageOptions)
    frescoDrawable.setPlaceholderDrawable(placeholder)
    frescoDrawable.setImageDrawable(null)
    frescoDrawable.internalListener.onPlaceholderSet(imageId, imageRequest, placeholder)
  }

  override fun releaseDelayed(drawable: FrescoDrawableInterface) {
    if (drawable !is FrescoDrawable2Impl) {
      FLog.e(TAG, "Drawable not supported $drawable")
      return
    }
    val frescoDrawable = drawable
    frescoDrawable.imagePerfListener.onScheduleReleaseDelayed(frescoDrawable)
    frescoDrawable.scheduleReleaseDelayed()
  }

  override fun release(drawable: FrescoDrawableInterface) {
    if (drawable !is FrescoDrawable2Impl) {
      FLog.e(TAG, "Drawable not supported $drawable")
      return
    }
    val frescoDrawable = drawable
    frescoDrawable.imagePerfListener.onScheduleReleaseNextFrame(frescoDrawable)
    frescoDrawable.scheduleReleaseNextFrame()
  }

  override fun releaseImmediately(drawable: FrescoDrawableInterface) {
    if (drawable !is FrescoDrawable2Impl) {
      FLog.e(TAG, "Drawable not supported $drawable")
      return
    }
    val frescoDrawable = drawable
    frescoDrawable.imagePerfListener.onReleaseImmediately(frescoDrawable)
    frescoDrawable.releaseImmediately()
  }

  private fun setActualImage(
      drawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest,
      image: CloseableReference<CloseableImage>?,
      isImmediate: Boolean,
      dataSource: DataSource<CloseableReference<CloseableImage>>?
  ) {
    val actualImageWrapperDrawable = drawable.actualImageWrapper
    mHierarcher.setupActualImageWrapper(
        actualImageWrapperDrawable, imageRequest.imageOptions, drawable.callerContext)
    val actualDrawable =
        mHierarcher.buildActualImageDrawable(
            imageRequest.resources, imageRequest.imageOptions, image!!)
    actualImageWrapperDrawable.setCurrent(actualDrawable ?: NopDrawable)
    drawable.setImage(actualImageWrapperDrawable, image)
    if (isImmediate || imageRequest.imageOptions.fadeDurationMs <= 0) {
      drawable.showImageImmediately()
    } else {
      drawable.fadeInImage(imageRequest.imageOptions.fadeDurationMs)
    }
    if (imageRequest.imageOptions.shouldAutoPlay() && actualDrawable is Animatable) {
      (actualDrawable as Animatable).start()
    }
    val extras = obtainExtras(dataSource, image, drawable)
    val imageInfo = image.get().imageInfo
    if (notifyFinalResult(dataSource)) {
      drawable.internalListener.onFinalImageSet(
          drawable.imageId, imageRequest, drawable.imageOrigin, imageInfo, extras, actualDrawable)
    } else {
      drawable.internalListener.onIntermediateImageSet(drawable.imageId, imageRequest, imageInfo)
    }
    drawable.imagePerfListener.onImageSuccess(drawable, isImmediate)
    var progress = 1f
    if (dataSource != null && !dataSource.isFinished) {
      progress = dataSource.progress
    }
    drawable.setProgress(progress)
    mDebugOverlayFactory.update(drawable, extras)
  }

  override fun onNewResult(
      drawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest,
      dataSource: DataSource<CloseableReference<CloseableImage>>
  ) {
    if (!dataSource.hasResult()) {
      return
    }
    val image = dataSource.result
    try {
      if (!CloseableReference.isValid(image)) {
        onFailure(drawable, imageRequest, dataSource)
      } else {
        setActualImage(drawable, imageRequest, image, false, dataSource)
      }
    } finally {
      CloseableReference.closeSafely(image)
    }
  }

  override fun onFailure(
      drawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest,
      dataSource: DataSource<CloseableReference<CloseableImage>>
  ) {
    val errorDrawable =
        mHierarcher.buildErrorDrawable(imageRequest.resources, imageRequest.imageOptions)
    drawable.setProgress(1f)
    drawable.setImageDrawable(errorDrawable)
    if (!drawable.isDefaultLayerIsOn) {
      if (imageRequest.imageOptions.fadeDurationMs <= 0) {
        drawable.showImageImmediately()
      } else {
        drawable.fadeInImage(imageRequest.imageOptions.fadeDurationMs)
      }
    } else {
      drawable.setPlaceholderDrawable(null)
      drawable.setProgressDrawable(null)
    }
    val extras = obtainExtras(dataSource, dataSource.result, drawable)
    if (notifyFinalResult(dataSource)) {
      drawable.internalListener.onFailure(
          drawable.imageId, imageRequest, errorDrawable, dataSource.failureCause, extras)
    } else {
      drawable.internalListener.onIntermediateImageFailed(
          drawable.imageId, imageRequest, dataSource.failureCause)
    }
    drawable.imagePerfListener.onImageError(drawable)
    mDebugOverlayFactory.update(drawable, extras)
  }

  override fun onProgressUpdate(
      drawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest,
      dataSource: DataSource<CloseableReference<CloseableImage>>
  ) {
    val isFinished = dataSource.isFinished
    val progress = dataSource.progress
    if (!isFinished) {
      drawable.setProgress(progress)
    }
  }

  override fun onRelease(drawable: FrescoDrawable2Impl) {
    val imageRequest = drawable.imageRequest
    if (imageRequest != null) {
      // Notify listeners
      drawable.internalListener.onRelease(
          drawable.imageId, imageRequest, obtainExtras(null, null, drawable))
      if (mConfig.stopAnimationInOnRelease()) {
        // We automatically stop the animation if it was automatically started
        if (!mConfig.onlyStopAnimationWhenAutoPlayEnabled() ||
            imageRequest.imageOptions.shouldAutoPlay()) {
          (drawable.actualImageDrawable as? Animatable)?.stop()
        }
      }
    }
    drawable.imagePerfListener.onImageRelease(drawable)
  }

  private fun isAlreadyLoadingImage(
      drawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest
  ): Boolean {
    if (drawable.drawableDataSubscriber !== this || !drawable.isFetchSubmitted) {
      return false
    }
    return if (mConfig.useSmartPropertyDiffing()) {
      imageRequest.equalsIfHasImage(drawable.imageRequest, drawable.hasImage())
    } else {
      imageRequest.equals(drawable.imageRequest)
    }
  }

  companion object {
    private val COMPONENT_EXTRAS = ImmutableMap.of<String, Any>("component_tag", "vito2")
    private val SHORTCUT_EXTRAS =
        ImmutableMap.of<String, Any>("origin", "memory_bitmap", "origin_sub", "shortcut")
    private const val TAG = "FrescoController2Impl"
    private const val EMPTY_IMAGE_ID = Long.MAX_VALUE

    private fun obtainExtras(
        dataSource: DataSource<CloseableReference<CloseableImage>>?,
        image: CloseableReference<CloseableImage>?,
        drawable: FrescoDrawable2
    ): Extras {
      var imageExtras: Map<String, Any?>? = null
      if (image != null) {
        imageExtras = image.get().extras
      }
      var sourceUri: Uri? = null
      val vitoImageRequest = drawable.imageRequest
      var imageSourceExtras: Map<String, Any>? = null
      var logWithHighSamplingRate = false
      if (vitoImageRequest != null) {
        logWithHighSamplingRate = vitoImageRequest.logWithHighSamplingRate
        vitoImageRequest.finalImageRequest?.let { finalImageRequest ->
          sourceUri = finalImageRequest.sourceUri
        }
        imageSourceExtras =
            vitoImageRequest.extras[HasExtraData.KEY_IMAGE_SOURCE_EXTRAS] as Map<String, Any>?
      }
      return obtainExtras(
          COMPONENT_EXTRAS,
          SHORTCUT_EXTRAS,
          dataSource?.extras,
          imageSourceExtras,
          drawable.viewportDimensions,
          drawable.actualImageScaleType.toString(),
          drawable.actualImageFocusPoint,
          imageExtras,
          drawable.callerContext,
          logWithHighSamplingRate,
          sourceUri)
    }

    private fun notifyFinalResult(
        dataSource: DataSource<CloseableReference<CloseableImage>>?
    ): Boolean {
      return dataSource == null || dataSource.isFinished || dataSource.hasMultipleResults()
    }
  }
}
