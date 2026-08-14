/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.Drawable

/** Drawable that can adjust underlying drawable based on specified [Matrix]. */
class MatrixDrawable
/**
 * Creates a new MatrixDrawable with given underlying drawable and matrix.
 *
 * @param drawable underlying drawable to apply the matrix to
 * @param matrix matrix to be applied to the drawable
 */
(
    drawable: Drawable, // Specified matrix.
    private var _matrix: Matrix,
) : ForwardingDrawable(checkNotNull(drawable) { "Drawable cannot be null" }) {

  // Matrix that is actually being used for drawing. In case underlying drawable doesn't have
  // intrinsic dimensions, this will be null (i.e. no matrix will be applied).
  private var drawMatrix: Matrix? = null

  // Last known dimensions of the underlying drawable. Used to avoid computing bounds every time
  // if underlying size hasn't changed.
  private var underlyingWidth = 0
  private var underlyingHeight = 0

  override fun setCurrent(newDelegate: Drawable?): Drawable? {
    val previousDelegate = super.setCurrent(newDelegate)
    configureBounds()

    return previousDelegate
  }

  var matrix: Matrix
    /**
     * Gets the current matrix.
     *
     * @return matrix
     */
    get() = _matrix
    /**
     * Sets the matrix.
     *
     * @param matrix matrix to set
     */
    set(matrix) {
      this._matrix = matrix
      configureBounds()
      invalidateSelf()
    }

  override fun draw(canvas: Canvas) {
    configureBoundsIfUnderlyingChanged()
    if (drawMatrix != null) {
      val saveCount = canvas.save()
      canvas.clipRect(bounds)
      canvas.concat(drawMatrix)
      super.draw(canvas)
      canvas.restoreToCount(saveCount)
    } else {
      // mDrawMatrix == null means our bounds match and we can take fast path
      super.draw(canvas)
    }
  }

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    configureBounds()
  }

  private fun configureBoundsIfUnderlyingChanged() {
    val current = current ?: return

    if (underlyingWidth != current.intrinsicWidth || underlyingHeight != current.intrinsicHeight) {
      configureBounds()
    }
  }

  /** Determines bounds for the underlying drawable and a matrix that should be applied on it. */
  private fun configureBounds() {
    val underlyingDrawable = current ?: return

    val bounds = bounds
    this.underlyingWidth = underlyingDrawable.intrinsicWidth
    val underlyingWidth = this.underlyingWidth
    this.underlyingHeight = underlyingDrawable.intrinsicHeight
    val underlyingHeight = this.underlyingHeight

    // In case underlying drawable doesn't have intrinsic dimensions, we cannot set its bounds to
    // -1 so we use our bounds and discard specified matrix. In normal case we use drawable's
    // intrinsic dimensions for its bounds and apply specified matrix to it.
    if (underlyingWidth <= 0 || underlyingHeight <= 0) {
      underlyingDrawable.bounds = bounds
      drawMatrix = null
    } else {
      underlyingDrawable.setBounds(0, 0, underlyingWidth, underlyingHeight)
      drawMatrix = _matrix
    }
  }

  /** TransformationCallback method */
  override fun getTransform(transform: Matrix) {
    super.getTransform(transform)
    if (drawMatrix != null) {
      transform.preConcat(drawMatrix)
    }
  }
}
