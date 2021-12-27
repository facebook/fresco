/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.facebook.drawee.drawable.VisibilityCallback
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImagePerfListener
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import java.io.Closeable
import java.io.IOException

class KFrescoVitoDrawable(val _imagePerfListener: VitoImagePerfListener = NopImagePerfListener()) :
    Drawable(), FrescoDrawableInterface {

  var _imageId: Long = 0
  var _isLoading: Boolean = false
  var _callerContext: Any? = null
  var _visibilityCallback: VisibilityCallback? = null
  var _fetchSubmitted: Boolean = false
  val listenerManager: CombinedImageListenerImpl = CombinedImageListenerImpl()
  var _extras: Any? = null
  var viewportDimensions: Rect? = null

  val releaseState = ImageReleaseScheduler.createReleaseState(this)
  private var hasBoundsSet = false

  private var _imageRequest: VitoImageRequest? = null

  private var _closeable: Closeable? = null

  override fun getImageId(): Long = _imageId

  override fun setCallerContext(callerContext: Any?) {
    _callerContext = callerContext
  }

  override fun getCallerContext(): Any? = _callerContext

  override fun getImagePerfListener(): VitoImagePerfListener = _imagePerfListener

  override fun setMutateDrawables(mutateDrawables: Boolean) {
    // No-op since we never mutate Drawables
  }

  override fun getActualImageDrawable(): Drawable? {
    return when (val model = actualImageLayer.getDataModel()) {
      is DrawableImageDataModel -> model.drawable
      else -> null
    }
  }

  override fun hasImage(): Boolean = actualImageLayer.getDataModel() != null

  fun setFetchSubmitted(fetchSubmitted: Boolean) {
    _fetchSubmitted = fetchSubmitted
  }

  override fun isFetchSubmitted(): Boolean = _fetchSubmitted

  override fun getImageRequest(): VitoImageRequest? = _imageRequest

  override fun setImageRequest(imageRequest: VitoImageRequest?) {
    _imageRequest = imageRequest
  }

  override fun setVisibilityCallback(visibilityCallback: VisibilityCallback?) {
    _visibilityCallback = visibilityCallback
  }

  override fun setImageListener(imageListener: ImageListener?) {
    listenerManager.imageListener = imageListener
  }

  override fun getImageListener(): ImageListener? = listenerManager.imageListener

  override fun setOverlayDrawable(drawable: Drawable?): Drawable? {
    overlayImageLayer.apply {
      configure(
          dataModel = if (drawable == null) null else DrawableImageDataModel(drawable),
          roundingOptions = null,
          borderOptions = null)
    }
    return drawable
  }

  override fun getExtras(): Any? = _extras

  override fun setExtras(extras: Any?) {
    _extras = extras
  }

  override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
    _visibilityCallback?.onVisibilityChange(visible)
    return super.setVisible(visible, restart)
  }

  fun reset() {
    imageRequest?.let { listenerManager.onRelease(imageId, it, obtainExtras()) }
    imagePerfListener.onImageRelease(this)
    ImageReleaseScheduler.cancelAllReleasing(this)
    _imageId = 0
    _isLoading = false
    _callerContext = null

    placeholderLayer.reset()
    actualImageLayer.reset()
    overlayImageLayer.reset()
    hasBoundsSet = false
    closeCloseable()

    listenerManager.onReset()
    listenerManager.imageListener = null
  }

  @Synchronized
  fun setCloseable(closeable: Closeable?) {
    ImageReleaseScheduler.cancelAllReleasing(this)
    closeCloseable()
    _closeable = closeable
  }

  @Synchronized
  private fun closeCloseable() {
    val current = _closeable
    if (current != null) {
      _closeable = null
      try {
        current.close()
      } catch (e: IOException) {
        // swallow
      }
    }
  }

  private var drawableAlpha: Int = 255
  private var drawableColorFilter: ColorFilter? = null

  val placeholderLayer = ImageLayerDataModel()
  val actualImageLayer = ImageLayerDataModel()
  val overlayImageLayer = ImageLayerDataModel()

  override fun draw(canvas: Canvas) {
    if (!hasBoundsSet) {
      setLayerBounds(bounds)
    }
    placeholderLayer.draw(canvas)
    actualImageLayer.draw(canvas)
    overlayImageLayer.draw(canvas)
  }

  override fun onBoundsChange(bounds: Rect?) {
    super.onBoundsChange(bounds)
    setLayerBounds(bounds)
  }

  private fun setLayerBounds(bounds: Rect?) {
    if (bounds != null) {
      placeholderLayer.configure(bounds = bounds)
      actualImageLayer.configure(bounds = bounds)
      overlayImageLayer.configure(bounds = bounds)
      hasBoundsSet = true
    }
  }

  override fun setAlpha(alpha: Int) {
    drawableAlpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    drawableColorFilter = colorFilter
  }

  // TODO(T105148151) Calculate opacity based on layers
  override fun getOpacity(): Int = PixelFormat.TRANSPARENT
}
