/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import com.facebook.drawee.view.AspectRatioMeasure

class ImageViewWithAspectRatio : ImageView {
  constructor(
      context: Context,
      attrSet: AttributeSet?,
      defStyleAttr: Int,
  ) : super(context, attrSet, defStyleAttr)

  constructor(context: Context, attrSet: AttributeSet?) : super(context, attrSet)

  constructor(context: Context) : super(context)

  private var currentAspectRatio: Float = 1f

  private val measureSpec = AspectRatioMeasure.Spec()

  /** Sets the desired aspect ratio (w/h). */
  fun setAspectRatio(aspectRatio: Float) {
    if (currentAspectRatio == aspectRatio) {
      return
    }
    currentAspectRatio = aspectRatio
    requestLayout()
  }

  fun getAspectRatio(): Float {
    return currentAspectRatio
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    measureSpec.width = widthMeasureSpec
    measureSpec.height = heightMeasureSpec
    AspectRatioMeasure.updateMeasureSpec(
        measureSpec,
        currentAspectRatio,
        layoutParams,
        paddingLeft + paddingRight,
        paddingTop + paddingBottom,
    )
    super.onMeasure(measureSpec.width, measureSpec.height)
  }
}
