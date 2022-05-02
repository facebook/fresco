/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.textspan

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.widget.text.span.BetterImageSpan

open class VitoSpan(
    val resources: Resources,
    val drawableInterface: FrescoDrawableInterface,
    @BetterImageSpan.BetterImageSpanAlignment verticalAlignment: Int
) : BetterImageSpan(drawableInterface as Drawable, verticalAlignment), Drawable.Callback {

  var parentView: View? = null

  var imageFetchCommand: (() -> Boolean)? = null

  init {
    (drawableInterface as Drawable).callback = this
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
