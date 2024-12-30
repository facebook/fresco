/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.graphics.Canvas
import android.graphics.drawable.NinePatchDrawable
import com.facebook.imagepipeline.systrace.FrescoSystrace

class RoundedNinePatchDrawable(ninePatchDrawable: NinePatchDrawable) :
    RoundedDrawable(ninePatchDrawable) {
  override fun draw(canvas: Canvas) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("RoundedNinePatchDrawable#draw")
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
    canvas.clipPath(mPath)
    super.draw(canvas)
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection()
    }
  }
}
