/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie

/**
 * Pronounced Draw-er Draws frames of a [Movie] to a bitmap. All methods are synchronized, so can be
 * used in parallel. The underlying [mMovie] is not threadsafe, and should therefore not be accessed
 * outside of [MovieDrawer]. Attempts to optimize work done by the drawing [Canvas] by detecting if
 * the underlying [Bitmap] has changed.
 */
class MovieDrawer(private val movie: Movie) {

  private val scaleHolder: MovieScaleHolder = MovieScaleHolder(movie.width(), movie.height())
  private val canvas: Canvas = Canvas()
  private var previousBitmap: Bitmap? = null

  @Synchronized
  fun drawFrame(movieTime: Int, w: Int, h: Int, bitmap: Bitmap) {
    movie.setTime(movieTime)
    if (previousBitmap?.isRecycled == true) {
      previousBitmap = null
    }
    if (previousBitmap != bitmap) {
      previousBitmap = bitmap
      canvas.setBitmap(bitmap)
    }
    scaleHolder.updateViewPort(w, h)
    canvas.save()
    canvas.scale(scaleHolder.scale, scaleHolder.scale)
    movie.draw(canvas, scaleHolder.left, scaleHolder.top)
    canvas.restore()
  }
}
