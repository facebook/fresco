/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.renderer

import android.graphics.Matrix
import android.graphics.Rect

class CanvasTransformationHandler(var canvasTransformation: CanvasTransformation? = null) {

  private val tempMatrix = Matrix()

  private var drawMatrix: Matrix? = null

  fun getMatrix(): Matrix? = drawMatrix

  fun configure(bounds: Rect, childWidth: Int, childHeight: Int) {
    // We only scale the model if its dimensions are > 0
    if (childWidth <= 0 || childHeight <= 0) {
      drawMatrix = null
      return
    }
    drawMatrix =
        canvasTransformation?.calculateTransformation(tempMatrix, bounds, childWidth, childHeight)
  }
}
