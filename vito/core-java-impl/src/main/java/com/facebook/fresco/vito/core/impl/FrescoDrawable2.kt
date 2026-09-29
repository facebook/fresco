/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import com.facebook.drawee.components.DeferredReleaser.Releasable
import com.facebook.drawee.drawable.FadeDrawable
import com.facebook.drawee.drawable.ScaleTypeDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.drawable.TransformAwareDrawable
import com.facebook.drawee.drawable.TransformCallback
import com.facebook.drawee.drawable.VisibilityCallback
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import java.io.Closeable

abstract class FrescoDrawable2 :
    FadeDrawable(arrayOfNulls(LAYER_COUNT), false, IMAGE_DRAWABLE_INDEX),
    Drawable.Callback,
    TransformCallback,
    TransformAwareDrawable,
    Closeable,
    Releasable,
    FrescoDrawableInterface {

  var viewportDimensions: Rect? = null
  private var visibilityCallback: VisibilityCallback? = null

  override fun hasImage(): Boolean = getDrawable(IMAGE_DRAWABLE_INDEX) != null

  override fun setOverlayDrawable(drawable: Drawable?): Drawable? {
    val result = setDrawable(OVERLAY_DRAWABLE_INDEX, drawable)
    showLayerImmediately(OVERLAY_DRAWABLE_INDEX)
    return result
  }

  fun setProgressDrawable(drawable: Drawable?): Drawable? =
      setDrawable(PROGRESS_DRAWABLE_INDEX, drawable)

  @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
  fun setProgress(progress: Float) {
    val progressBarDrawable = getDrawable(PROGRESS_DRAWABLE_INDEX) ?: return
    // display progressbar when not fully loaded, hide otherwise
    if (progress >= 0.999f) {
      maybeStopAnimation(progressBarDrawable)
    } else {
      maybeStartAnimation(progressBarDrawable)
    }
    // set drawable level, scaled to [0, 10000] per drawable specification
    progressBarDrawable.level = Math.round(progress * 10_000)
  }

  fun setPlaceholderDrawable(drawable: Drawable?): Drawable? =
      setDrawable(PLACEHOLDER_DRAWABLE_INDEX, drawable)

  fun fadeInImage(durationMs: Int) {
    transitionDuration = durationMs
    beginBatchMode()
    fadeOutLayer(PLACEHOLDER_DRAWABLE_INDEX)
    fadeOutLayer(PROGRESS_DRAWABLE_INDEX)
    fadeInLayer(IMAGE_DRAWABLE_INDEX)
    endBatchMode()
  }

  fun showImageImmediately() {
    beginBatchMode()
    hideLayerImmediately(PLACEHOLDER_DRAWABLE_INDEX)
    hideLayerImmediately(PROGRESS_DRAWABLE_INDEX)
    showLayerImmediately(IMAGE_DRAWABLE_INDEX)
    endBatchMode()
  }

  fun showProgressImmediately() {
    showLayerImmediately(PROGRESS_DRAWABLE_INDEX)
  }

  override fun close() {
    maybeStopAnimation(getDrawable(PLACEHOLDER_DRAWABLE_INDEX))
    for (i in LAYER_RANGE) {
      setDrawable(i, null)
    }
  }

  override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
    visibilityCallback?.onVisibilityChange(visible)
    return super.setVisible(visible, restart)
  }

  override fun setVisibilityCallback(visibilityCallback: VisibilityCallback?) {
    this.visibilityCallback = visibilityCallback
  }

  val overlayDrawable: Drawable?
    get() = getDrawable(OVERLAY_DRAWABLE_INDEX)

  val actualImageScaleType: ScalingUtils.ScaleType?
    get() {
      val actual = getDrawable(IMAGE_DRAWABLE_INDEX)
      return if (actual !is ScaleTypeDrawable) null else actual.scaleType
    }

  val actualImageFocusPoint: PointF?
    get() {
      val actual = getDrawable(IMAGE_DRAWABLE_INDEX)
      return if (actual !is ScaleTypeDrawable) null else actual.focusPoint
    }

  /** @return the width of the underlying actual image or -1 if unset */
  abstract val actualImageWidthPx: Int

  /** @return the width of the underlying actual image or -1 if unset */
  abstract val actualImageHeightPx: Int
  abstract val actualImageWrapper: ScaleTypeDrawable?

  abstract fun cancelReleaseDelayed()

  abstract fun cancelReleaseNextFrame()

  override fun getActualImageBounds(outBounds: RectF) {
    val transform = Matrix()
    // actualImageWrapper is the scale type Drawable, so we retrieve the current transform matrix
    actualImageWrapper?.getTransform(transform)
    // We store the actual image bounds (if present) in outBounds.
    // IMPORTANT: {@code getBounds} should be called after {@code getTransform},
    // because the parent may have to change our bounds.
    outBounds.set(actualImageWrapper?.drawable?.bounds ?: return)
    // We map the actual image bounds according to the current transform matrix
    transform.mapRect(outBounds)
  }

  companion object {
    const val IMAGE_DRAWABLE_INDEX: Int = 1

    private const val LAYER_COUNT = 4
    private const val PLACEHOLDER_DRAWABLE_INDEX = 0
    private const val PROGRESS_DRAWABLE_INDEX = 2
    private const val OVERLAY_DRAWABLE_INDEX = 3
    private val LAYER_RANGE = 0 until LAYER_COUNT

    private fun maybeStopAnimation(drawable: Drawable?) {
      (drawable as? Animatable)?.stop()
    }

    private fun maybeStartAnimation(drawable: Drawable?) {
      (drawable as? Animatable)?.start()
    }
  }
}
