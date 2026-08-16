/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.VisibleForTesting
import com.facebook.imagepipeline.systrace.FrescoSystrace
import java.lang.ref.WeakReference

class RoundedBitmapDrawable
@JvmOverloads
constructor(
    res: Resources,
    val bitmap: Bitmap?,
    paint: Paint? = null,
    repeatEdgePixels: Boolean = defaultRepeatEdgePixels,
) : RoundedDrawable(BitmapDrawable(res, bitmap)) {

  val paint: Paint = Paint()
  private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private var lastBitmap: WeakReference<Bitmap?>? = null
  private var _repeatEdgePixels: Boolean
  private var bitmapClipRect: RectF? = null

  init {
    if (paint != null) {
      this.paint.set(paint)
    }

    this.paint.flags = Paint.ANTI_ALIAS_FLAG
    borderPaint.style = Paint.Style.STROKE
    this._repeatEdgePixels = repeatEdgePixels
  }

  override fun updateTransform() {
    super.updateTransform()
    if (!_repeatEdgePixels) {
      if (bitmapClipRect == null) {
        bitmapClipRect = RectF()
      }
      mTransform.mapRect(bitmapClipRect, mBitmapBounds)
    }
  }

  override fun draw(canvas: Canvas) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("RoundedBitmapDrawable#draw")
    }
    if (!shouldRound()) {
      super.draw(canvas)
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection()
      }
      return
    }
    updateTransform()
    updatePath()
    updatePaint()
    val saveCount = canvas.save()
    canvas.concat(mInverseParentTransform)
    if (!_repeatEdgePixels && bitmapClipRect != null) {
      val saveCount2 = canvas.save()
      canvas.clipRect(bitmapClipRect!!)
      canvas.drawPath(mPath, paint)
      canvas.restoreToCount(saveCount2)
    } else {
      canvas.drawPath(mPath, paint)
    }
    if (mBorderWidth > 0) {
      borderPaint.strokeWidth = mBorderWidth
      borderPaint.color = DrawableUtils.multiplyColorAlpha(mBorderColor, paint.alpha)
      canvas.drawPath(mBorderPath, borderPaint)
    }
    canvas.restoreToCount(saveCount)
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection()
    }
  }

  private fun updatePaint() {
    if (lastBitmap == null || lastBitmap!!.get() != bitmap) {
      lastBitmap = WeakReference(bitmap)

      if (bitmap != null && !bitmap.isRecycled) {
        paint.setShader(BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        mIsShaderTransformDirty = true
      }
    }
    if (mIsShaderTransformDirty) {
      val shader = paint.shader
      if (shader != null) {
        shader.setLocalMatrix(mTransform)
        mIsShaderTransformDirty = false
      }
    }
    paint.isFilterBitmap = paintFilterBitmap
  }

  /** If both the radii and border width are zero or bitmap is null, there is nothing to round. */
  @VisibleForTesting override fun shouldRound(): Boolean = super.shouldRound() && bitmap != null

  override fun setAlpha(alpha: Int) {
    super.setAlpha(alpha)
    if (alpha != paint.alpha) {
      paint.alpha = alpha
      super.setAlpha(alpha)
      invalidateSelf()
    }
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    super.setColorFilter(colorFilter)
    paint.setColorFilter(colorFilter)
  }

  override fun setRepeatEdgePixels(repeatEdgePixels: Boolean) {
    this._repeatEdgePixels = repeatEdgePixels
  }

  val repeatEdgePixels: Boolean
    get() = _repeatEdgePixels

  companion object {
    @JvmStatic var defaultRepeatEdgePixels: Boolean = false

    /**
     * Creates a new RoundedBitmapDrawable from the given BitmapDrawable.
     *
     * @param res resources to use for this drawable
     * @param bitmapDrawable bitmap drawable containing the bitmap to be used for this drawable
     * @return the RoundedBitmapDrawable that is created
     */
    @JvmStatic
    fun fromBitmapDrawable(
        res: Resources,
        bitmapDrawable: BitmapDrawable,
    ): RoundedBitmapDrawable =
        RoundedBitmapDrawable(res, bitmapDrawable.bitmap, bitmapDrawable.paint)
  }
}
