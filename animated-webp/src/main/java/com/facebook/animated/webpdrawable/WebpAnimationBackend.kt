/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.webpdrawable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.facebook.animated.webp.WebPImage
import com.facebook.fresco.animation.backend.AnimationBackend
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.annotation.concurrent.GuardedBy

/** Animation backend that is used to draw webp frames. */
class WebpAnimationBackend private constructor(private val webPImage: WebPImage) :
    AnimationBackend {

  private val renderDstRect = Rect()
  private val renderSrcRect = Rect()

  private var bounds: Rect? = null

  @GuardedBy("this") private var tempBitmap: Bitmap? = null

  override fun drawFrame(parent: Drawable, canvas: Canvas, frameNumber: Int): Boolean {
    val frame = webPImage.getFrame(frameNumber)

    val xScale = bounds!!.width().toDouble() / parent.intrinsicWidth.toDouble()
    val yScale = bounds!!.height().toDouble() / parent.intrinsicHeight.toDouble()

    val frameWidth = Math.round(frame.width * xScale).toInt()
    val frameHeight = Math.round(frame.height * yScale).toInt()
    val xOffset = (frame.xOffset * xScale).toInt()
    val yOffset = (frame.yOffset * yScale).toInt()

    synchronized(this) {
      val renderedWidth = bounds!!.width()
      val renderedHeight = bounds!!.height()
      // Update the temp bitmap to be >= rendered dimensions
      prepareTempBitmapForThisSize(renderedWidth, renderedHeight)
      if (tempBitmap == null) {
        return false
      }
      frame.renderFrame(frameWidth, frameHeight, tempBitmap!!)
      // Temporary bitmap can be bigger than frame, so we should draw only rendered area of bitmap
      renderSrcRect[0, 0, renderedWidth] = renderedHeight
      renderDstRect[xOffset, yOffset, xOffset + renderedWidth] = yOffset + renderedHeight
      canvas.drawBitmap(tempBitmap!!, renderSrcRect, renderDstRect, null)
    }
    return true
  }

  override fun setAlpha(alpha: Int) {
    // unimplemented
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    // unimplemented
  }

  @Synchronized
  override fun setBounds(bounds: Rect) {
    this.bounds = bounds
  }

  override fun getIntrinsicWidth(): Int = webPImage.width

  override fun getIntrinsicHeight(): Int = webPImage.height

  override fun getSizeInBytes(): Int = 0

  override fun clear() {
    webPImage.dispose()
  }

  override fun getFrameCount(): Int = webPImage.frameCount

  override fun getFrameDurationMs(frameNumber: Int): Int = webPImage.frameDurations[frameNumber]

  override fun getLoopDurationMs(): Int = webPImage.duration

  override fun width(): Int = webPImage.width

  override fun height(): Int = webPImage.height

  override fun getLoopCount(): Int = webPImage.loopCount

  override fun preloadAnimation() {
    // not needed as bitmaps are extracted on fly
  }

  override fun setAnimationListener(listener: AnimationBackend.Listener?) {
    // unimplementedå
  }

  @Synchronized
  private fun prepareTempBitmapForThisSize(width: Int, height: Int) {
    // Different webp frames can be different size,
    // So we need to ensure we can fit next frame to temporary bitmap
    if (tempBitmap != null && (tempBitmap!!.width < width || tempBitmap!!.height < height)) {
      clearTempBitmap()
    }
    if (tempBitmap == null) {
      tempBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
    tempBitmap!!.eraseColor(Color.TRANSPARENT)
  }

  @Synchronized
  private fun clearTempBitmap() {
    if (tempBitmap != null) {
      tempBitmap!!.recycle()
      tempBitmap = null
    }
  }

  companion object {
    @JvmStatic
    @Throws(IOException::class)
    fun create(filePath: String?): WebpAnimationBackend {
      var `is`: InputStream? = null
      try {
        `is` = BufferedInputStream(FileInputStream(filePath))
        `is`.mark(Int.MAX_VALUE)
        val targetArray = ByteArray(`is`.available())
        `is`.read(targetArray)

        val webPImage = WebPImage.createFromByteArray(targetArray, null)
        `is`.reset()

        return WebpAnimationBackend(webPImage)
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
