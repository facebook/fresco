/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.backend

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.IntRange

/** Animation backend delegate that forwards all calls to a given [AnimationBackend] */
open class AnimationBackendDelegate<T : AnimationBackend?>(
    /** Current animation backend in use */
    private var _animationBackend: T?
) : AnimationBackend {

  // Animation backend parameters
  @IntRange(from = -1, to = 255) private var alpha = ALPHA_UNSET

  private var colorFilter: ColorFilter? = null
  private var bounds: Rect? = null

  override fun getFrameCount(): Int =
      if (_animationBackend == null) 0 else _animationBackend!!.frameCount

  override fun getFrameDurationMs(frameNumber: Int): Int =
      if (_animationBackend == null) 0 else _animationBackend!!.getFrameDurationMs(frameNumber)

  override fun getLoopDurationMs(): Int =
      if (_animationBackend == null) 0 else _animationBackend!!.loopDurationMs

  override fun width(): Int = if (_animationBackend == null) 0 else _animationBackend!!.width()

  override fun height(): Int = if (_animationBackend == null) 0 else _animationBackend!!.height()

  override fun getLoopCount(): Int =
      if (_animationBackend == null) AnimationInformation.LOOP_COUNT_INFINITE
      else _animationBackend!!.loopCount

  override fun drawFrame(parent: Drawable, canvas: Canvas, frameNumber: Int): Boolean =
      _animationBackend?.drawFrame(parent, canvas, frameNumber) == true

  override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
    _animationBackend?.setAlpha(alpha)
    this.alpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    _animationBackend?.setColorFilter(colorFilter)
    this.colorFilter = colorFilter
  }

  override fun setBounds(bounds: Rect) {
    _animationBackend?.setBounds(bounds)
    this.bounds = bounds
  }

  override fun getSizeInBytes(): Int =
      if (_animationBackend == null) 0 else _animationBackend!!.sizeInBytes

  override fun clear() {
    _animationBackend?.clear()
  }

  override fun preloadAnimation() {
    _animationBackend?.preloadAnimation()
  }

  override fun setAnimationListener(listener: AnimationBackend.Listener?) {
    _animationBackend?.setAnimationListener(listener)
  }

  override fun getIntrinsicWidth(): Int =
      if (_animationBackend == null) AnimationBackend.INTRINSIC_DIMENSION_UNSET
      else _animationBackend!!.intrinsicWidth

  override fun getIntrinsicHeight(): Int =
      if (_animationBackend == null) AnimationBackend.INTRINSIC_DIMENSION_UNSET
      else _animationBackend!!.intrinsicHeight

  var animationBackend: T?
    /**
     * Get the current animation backend.
     *
     * @return the current animation backend in use or null if not set
     */
    get() = _animationBackend
    /**
     * Set the animation backend to forward calls to. If called with null, the current backend will
     * be removed.
     *
     * @param animationBackend the backend to use or null to remove the current backend
     */
    set(animationBackend) {
      this._animationBackend = animationBackend
      if (this._animationBackend != null) {
        applyBackendProperties(_animationBackend!!)
      }
    }

  @SuppressLint("Range")
  private fun applyBackendProperties(backend: AnimationBackend) {
    if (bounds != null) {
      backend.setBounds(bounds)
    }
    if (alpha >= 0 && alpha <= 255) {
      backend.setAlpha(alpha)
    }
    if (colorFilter != null) {
      backend.setColorFilter(colorFilter)
    }
  }

  companion object {
    private const val ALPHA_UNSET = -1
  }
}
