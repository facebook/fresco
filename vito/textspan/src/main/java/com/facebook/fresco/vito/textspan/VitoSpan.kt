/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.textspan

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.text.style.ReplacementSpan
import android.view.View
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.widget.text.span.BetterImageSpan

open class VitoSpan(
    val resources: Resources,
    val drawableInterface: FrescoDrawableInterface,
    @BetterImageSpan.BetterImageSpanAlignment
    verticalAlignment: Int = BetterImageSpan.ALIGN_BASELINE,
    private val imageSpan: BetterImageSpan =
        BetterImageSpan(drawableInterface as Drawable, verticalAlignment),
    private val customCallback: Drawable.Callback? = null,
) : ReplacementSpan(), Drawable.Callback {

  var parentView: View? = null

  var imageFetchCommand: (() -> Boolean)? = null

  init {
    (drawableInterface as Drawable).callback = customCallback ?: this
  }

  override fun getSize(
      paint: Paint,
      text: CharSequence?,
      start: Int,
      end: Int,
      fm: Paint.FontMetricsInt?
  ): Int {
    return imageSpan.getSize(paint, text, start, end, fm)
  }

  override fun draw(
      canvas: Canvas,
      text: CharSequence?,
      start: Int,
      end: Int,
      x: Float,
      top: Int,
      y: Int,
      bottom: Int,
      paint: Paint
  ) {
    imageSpan.draw(canvas, text, start, end, x, top, y, bottom, paint)
  }

  override fun invalidateDrawable(who: Drawable) {
    parentView?.invalidate()
  }

  override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
    parentView?.apply {
      // 'parentView.scheduleDrawable(who, what, when)' wouldn't work because
      // it cannot determine the 'who' drawable with 'verifyDrawable(who)'.
      // So we're re-implementing 'scheduleDrawable' manually.
      val delay = `when` - SystemClock.uptimeMillis()
      postDelayed(what, delay)
    }
  }

  override fun unscheduleDrawable(who: Drawable, what: Runnable) {
    parentView?.removeCallbacks(what)
  }
}
