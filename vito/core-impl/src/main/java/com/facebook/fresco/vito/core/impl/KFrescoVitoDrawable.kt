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
import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.facebook.common.closeables.AutoCleanupDelegate
import com.facebook.datasource.DataSource
import com.facebook.drawee.drawable.VisibilityCallback
import com.facebook.fresco.vito.core.CombinedImageListener
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.ImagePerfLoggingListener
import com.facebook.fresco.vito.core.VitoImagePerfListener
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import java.io.Closeable
import java.io.IOException

class KFrescoVitoDrawable(
    private val _imagePerfListener: VitoImagePerfListener = NopImagePerfListener(),
    private val resetVitoImageRequestListener: Boolean = false,
    private val resetLocalVitoImageRequestListener: Boolean = false,
    private val resetLocalImagePerfStateListener: Boolean = false,
    private val resetControllerListener2: Boolean = false,
) : Drawable(), FrescoDrawableInterface, Drawable.Callback {

  var _imageId: Long = 0
  var _isLoading: Boolean = false
  override var callerContext: Any? = null
  override var uiFramework: String? = null
  var _visibilityCallback: VisibilityCallback? = null
  var _fetchSubmitted: Boolean = false
  val listenerManager: CombinedImageListener = CombinedImageListenerImpl()
  override var extras: Any? = null
  var viewportDimensions: Rect? = null
  var dataSource: DataSource<out Any>? by DataSourceCleanupDelegate()

  val releaseState = ImageReleaseScheduler.createReleaseState(this)
  private var hasBoundsSet = false

  override var imageRequest: VitoImageRequest? = null

  var _intrinsicWidth: Int = -1
  var _intrinsicHeight: Int = -1

  private val closeableCleanupFunction: (Closeable) -> Unit = {
    ImageReleaseScheduler.cancelAllReleasing(this)
    try {
      it.close()
    } catch (e: IOException) {
      // swallow
    }
  }

  var closeable: Closeable? by AutoCleanupDelegate(null, closeableCleanupFunction)

  override var refetchRunnable: Runnable? = null

  override fun getImagePerfLoggingListener(): ImagePerfLoggingListener? =
      listenerManager.getImagePerfLoggingListener()

  override val imageId: Long
    get() = _imageId

  override val imagePerfListener: VitoImagePerfListener
    get() = _imagePerfListener

  override fun setMutateDrawables(mutateDrawables: Boolean) {
    // No-op since we never mutate Drawables
  }

  override val actualImageDrawable: Drawable?
    get() {
      return when (val model = actualImageLayer.getDataModel()) {
        is DrawableImageDataModel -> model.drawable
        else -> null
      }
    }

  override fun hasImage(): Boolean = actualImageLayer.getDataModel() != null

  override fun setFetchSubmitted(fetchSubmitted: Boolean) {
    _fetchSubmitted = fetchSubmitted
  }

  override val isFetchSubmitted: Boolean
    get() = _fetchSubmitted

  override fun setVisibilityCallback(visibilityCallback: VisibilityCallback?) {
    _visibilityCallback = visibilityCallback
  }

  override var imageListener: ImageListener?
    get() = listenerManager.imageListener
    set(value) {
      listenerManager.imageListener = value
    }

  override fun setOverlayDrawable(drawable: Drawable?): Drawable? {
    overlayImageLayer.apply {
      configure(
          dataModel = if (drawable == null) null else DrawableImageDataModel(drawable),
          roundingOptions = null,
          borderOptions = null)
    }
    return drawable
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
    closeable = null
    dataSource = null
    imageRequest = null
    _isLoading = false
    callerContext = null
    _intrinsicWidth = -1
    _intrinsicHeight = -1

    placeholderLayer.reset()
    actualImageLayer.reset()
    progressLayer?.reset()
    overlayImageLayer.reset()
    debugOverlayImageLayer?.reset()
    hasBoundsSet = false

    listenerManager.onReset(
        resetVitoImageRequestListener,
        resetLocalVitoImageRequestListener,
        resetLocalImagePerfStateListener,
        resetControllerListener2)
  }

  private var drawableColorFilter: ColorFilter? = null

  val callbackProvider: (() -> Callback?) = { this }
  val invalidateLayerCallback: (() -> Unit) = { invalidateSelf() }

  val placeholderLayer = createLayer()
  val actualImageLayer = createLayer()
  var progressLayer: ImageLayerDataModel? = null
  val overlayImageLayer = createLayer()
  var debugOverlayImageLayer: ImageLayerDataModel? = null

  override fun draw(canvas: Canvas) {
    if (!hasBoundsSet) {
      setLayerBounds(bounds)
    }
    placeholderLayer.draw(canvas)
    actualImageLayer.draw(canvas)
    progressLayer?.draw(canvas)
    overlayImageLayer.draw(canvas)
    debugOverlayImageLayer?.draw(canvas)
  }

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    setLayerBounds(bounds)
  }

  private fun setLayerBounds(bounds: Rect?) {
    if (bounds != null) {
      placeholderLayer.configure(bounds = bounds)
      actualImageLayer.configure(bounds = bounds)
      progressLayer?.configure(bounds = bounds)
      overlayImageLayer.configure(bounds = bounds)
      debugOverlayImageLayer?.configure(bounds = bounds)
      hasBoundsSet = true
    }
  }

  override fun setAlpha(alpha: Int) {
    placeholderLayer.setAlpha(alpha)
    actualImageLayer.setAlpha(alpha)
    progressLayer?.setAlpha(alpha)
    overlayImageLayer.setAlpha(alpha)
    debugOverlayImageLayer?.setAlpha(alpha)
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    drawableColorFilter = colorFilter
  }

  // TODO(T105148151) Calculate opacity based on layers
  override fun getOpacity(): Int = PixelFormat.TRANSPARENT

  internal fun createLayer() = ImageLayerDataModel(callbackProvider, invalidateLayerCallback)

  override fun invalidateDrawable(who: Drawable) {
    invalidateSelf()
  }

  override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
    scheduleSelf(what, `when`)
  }

  override fun unscheduleDrawable(who: Drawable, what: Runnable) {
    unscheduleSelf(what)
  }

  override fun setIntrinsicSize(width: Int, height: Int) {
    _intrinsicWidth = width
    _intrinsicHeight = height
  }

  override fun configureWhenUnderlyingChanged() {
    actualImageLayer.configure()
  }

  override fun getActualImageBounds(outBounds: RectF) {
    // TODO
    throw UnsupportedOperationException("Not implemented for KVito")
  }

  override fun reportVisible(visible: Boolean) {
    getImagePerfLoggingListener()?.reportVisible(visible)
  }

  override fun getIntrinsicWidth(): Int {
    return if (_intrinsicWidth !== -1) {
      _intrinsicWidth
    } else super.getIntrinsicWidth()
  }

  override fun getIntrinsicHeight(): Int {
    return if (_intrinsicHeight !== -1) {
      _intrinsicHeight
    } else super.getIntrinsicHeight()
  }
}
