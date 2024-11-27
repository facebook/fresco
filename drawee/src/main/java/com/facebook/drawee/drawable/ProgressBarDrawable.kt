/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.min

/** Drawable that displays a progress bar based on the level. */
class ProgressBarDrawable : Drawable(), CloneableDrawable {

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val path = Path()
  private val rect = RectF()
  private var _backgroundColor = -0x80000000
  private var _color = -0x7fff7f01
  private var _padding = 10
  private var _barWidth = 20
  private var level = 0
  private var _radius = 0

  /** Gets whether the progress bar should be hidden when the progress is 0. */
  /** Sets whether the progress bar should be hidden when the progress is 0. */
  var hideWhenZero: Boolean = false
  private var _isVertical = false

  var color: Int
    /** Gets the progress bar color. */
    get() = _color
    /** Sets the progress bar color. */
    set(color) {
      if (this._color != color) {
        this._color = color
        invalidateSelf()
      }
    }

  var backgroundColor: Int
    /** Gets the progress bar background color. */
    get() = _backgroundColor
    /** Sets the progress bar background color. */
    set(backgroundColor) {
      if (this._backgroundColor != backgroundColor) {
        this._backgroundColor = backgroundColor
        invalidateSelf()
      }
    }

  /** Sets the progress bar padding. */
  fun setPadding(padding: Int) {
    if (this._padding != padding) {
      this._padding = padding
      invalidateSelf()
    }
  }

  /** Gets the progress bar padding. */
  override fun getPadding(padding: Rect): Boolean {
    padding[_padding, _padding, _padding] = _padding
    return this._padding != 0
  }

  var barWidth: Int
    /** Gets the progress bar width. */
    get() = _barWidth
    /** Sets the progress bar width. */
    set(barWidth) {
      if (this._barWidth != barWidth) {
        this._barWidth = barWidth
        invalidateSelf()
      }
    }

  var radius: Int
    /** Gets the radius of the progress bar. */
    get() = _radius
    /** The progress bar will be displayed as a rounded corner rectangle, sets the radius here. */
    set(radius) {
      if (this._radius != radius) {
        this._radius = radius
        invalidateSelf()
      }
    }

  var isVertical: Boolean
    /** Gets if the progress bar is vertical. */
    get() = _isVertical
    /** Sets if the progress bar should be vertical. */
    set(isVertical) {
      if (this._isVertical != isVertical) {
        this._isVertical = isVertical
        invalidateSelf()
      }
    }

  // automatically generated for Java compatibility, please inline it as soon as possible
  fun setIsVertical(value: Boolean) {
    this.isVertical = value
  }

  override fun onLevelChange(level: Int): Boolean {
    this.level = level
    invalidateSelf()
    return true
  }

  override fun setAlpha(alpha: Int) {
    paint.alpha = alpha
  }

  override fun setColorFilter(cf: ColorFilter?) {
    paint.setColorFilter(cf)
  }

  override fun getOpacity(): Int = DrawableUtils.getOpacityFromColor(paint.color)

  override fun draw(canvas: Canvas) {
    if (hideWhenZero && level == 0) {
      return
    }
    if (_isVertical) {
      drawVerticalBar(canvas, 10_000, _backgroundColor)
      drawVerticalBar(canvas, level, _color)
    } else {
      drawHorizontalBar(canvas, 10_000, _backgroundColor)
      drawHorizontalBar(canvas, level, _color)
    }
  }

  override fun cloneDrawable(): Drawable? {
    val copy = ProgressBarDrawable()
    copy._backgroundColor = _backgroundColor
    copy._color = _color
    copy._padding = _padding
    copy._barWidth = _barWidth
    copy.level = level
    copy._radius = _radius
    copy.hideWhenZero = hideWhenZero
    copy._isVertical = _isVertical
    return copy
  }

  private fun drawHorizontalBar(canvas: Canvas, level: Int, color: Int) {
    val bounds = bounds
    val length = (bounds.width() - 2 * _padding) * level / 10_000
    val xpos = bounds.left + _padding
    val ypos = bounds.bottom - _padding - _barWidth
    rect[xpos.toFloat(), ypos.toFloat(), (xpos + length).toFloat()] = (ypos + _barWidth).toFloat()
    drawBar(canvas, color)
  }

  private fun drawVerticalBar(canvas: Canvas, level: Int, color: Int) {
    val bounds = bounds
    val length = (bounds.height() - 2 * _padding) * level / 10_000
    val xpos = bounds.left + _padding
    val ypos = bounds.top + _padding
    rect[xpos.toFloat(), ypos.toFloat(), (xpos + _barWidth).toFloat()] = (ypos + length).toFloat()
    drawBar(canvas, color)
  }

  private fun drawBar(canvas: Canvas, color: Int) {
    paint.color = color
    paint.style = Paint.Style.FILL_AND_STROKE
    path.reset()
    path.fillType = Path.FillType.EVEN_ODD
    path.addRoundRect(
        rect,
        min(_radius.toDouble(), (_barWidth / 2).toDouble()).toFloat(),
        min(_radius.toDouble(), (_barWidth / 2).toDouble()).toFloat(),
        Path.Direction.CW)
    canvas.drawPath(path, paint)
  }
}
