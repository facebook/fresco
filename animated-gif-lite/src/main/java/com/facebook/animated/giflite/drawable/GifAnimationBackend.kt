/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Movie
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.facebook.animated.giflite.decoder.GifMetadataDecoder
import com.facebook.fresco.animation.backend.AnimationBackend
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class GifAnimationBackend
private constructor(private val gifDecoder: GifMetadataDecoder, private val movie: Movie) :
    AnimationBackend {

  private val frameStartTimes = IntArray(gifDecoder.frameCount)

  private var midX = 0f
  private var midY = 0f

  override fun drawFrame(parent: Drawable, canvas: Canvas, frameNumber: Int): Boolean {
    movie.setTime(getFrameStartTime(frameNumber))
    movie.draw(canvas, midX, midY)
    return true
  }

  override fun setAlpha(alpha: Int) {
    // unimplemented
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    // unimplemented
  }

  override fun setBounds(bounds: Rect) {
    scale(
        bounds.right - bounds.left, /* viewPortWidth */
        bounds.bottom - bounds.top, /* viewPortHeight */
        movie.width(), /* sourceWidth */
        movie.height() /* sourceHeight */)
  }

  override fun getIntrinsicWidth(): Int = movie.width()

  override fun getIntrinsicHeight(): Int = movie.height()

  override fun getSizeInBytes(): Int = 0 // no cached data

  override fun clear() {
    // unimplemented
  }

  override fun preloadAnimation() {
    // unimplemented
  }

  override fun setAnimationListener(listener: AnimationBackend.Listener?) {
    // unimplemented
  }

  override fun width(): Int = movie.width()

  override fun height(): Int = movie.height()

  override fun getLoopDurationMs(): Int {
    var total = 0
    for (i in 0..<frameCount) {
      total += getFrameDurationMs(i)
    }
    return total
  }

  override fun getFrameCount(): Int = gifDecoder.frameCount

  override fun getFrameDurationMs(frameNumber: Int): Int =
      gifDecoder.getFrameDurationMs(frameNumber)

  override fun getLoopCount(): Int = gifDecoder.loopCount

  private fun getFrameStartTime(frameNumber: Int): Int {
    if (frameNumber == 0 || frameNumber >= frameStartTimes.size) {
      return 0
    }
    if (frameStartTimes[frameNumber] != 0) {
      return frameStartTimes[frameNumber]
    }
    for (i in 0..<frameNumber) {
      frameStartTimes[frameNumber] += gifDecoder.getFrameDurationMs(i)
    }
    return frameStartTimes[frameNumber]
  }

  /**
   * Measures the source, and sets the size based on them. Maintains aspect ratio of source, and
   * ensures that screen is filled in at least one dimension.
   *
   * Adapted from com.facebook.cameracore.common.RenderUtil#calculateFitRect
   *
   * @param viewPortWidth the width of the display
   * @param viewPortHeight the height of the display
   * @param sourceWidth the width of the video
   * @param sourceHeight the height of the video
   */
  private fun scale(viewPortWidth: Int, viewPortHeight: Int, sourceWidth: Int, sourceHeight: Int) {
    val inputRatio = (sourceWidth.toFloat()) / sourceHeight
    val outputRatio = (viewPortWidth.toFloat()) / viewPortHeight

    var scaledWidth = viewPortWidth
    var scaledHeight = viewPortHeight
    if (outputRatio > inputRatio) {
      // Not enough width to fill the output. (Black bars on left and right.)
      scaledWidth = (viewPortHeight * inputRatio).toInt()
      scaledHeight = viewPortHeight
    } else if (outputRatio < inputRatio) {
      // Not enough height to fill the output. (Black bars on top and bottom.)
      scaledHeight = (viewPortWidth / inputRatio).toInt()
      scaledWidth = viewPortWidth
    }
    val scale = scaledWidth / sourceWidth.toFloat()

    midX = ((viewPortWidth - scaledWidth) / 2f) / scale
    midY = ((viewPortHeight - scaledHeight) / 2f) / scale
  }

  companion object {
    @JvmStatic
    @Throws(IOException::class)
    fun create(filePath: String?): GifAnimationBackend {
      var `is`: InputStream? = null
      try {
        `is` = BufferedInputStream(FileInputStream(filePath))
        `is`.mark(Int.MAX_VALUE)

        val decoder = GifMetadataDecoder.create(`is`, null)
        `is`.reset()

        val movie = Movie.decodeStream(`is`)
        return GifAnimationBackend(decoder, movie)
      } finally {
        closeSilently(`is`)
      }
    }

    private fun closeSilently(closeable: Closeable?) {
      if (closeable == null) {
        return
      }
      try {
        closeable.close()
      } catch (ignored: IOException) {
        // ignore
      }
    }
  }
}
