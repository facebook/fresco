/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.RequiresApi
import com.facebook.common.internal.Preconditions

object RenderScriptBlurFilter {

  const val BLUR_MAX_RADIUS = 25

  /**
   * Not-in-place intrinsic Gaussian blur filter using [ScriptIntrinsicBlur] and [ ]. This require
   * an Android versions >= 4.2.
   *
   * @param dest The [Bitmap] where the blurred image is written to.
   * @param src The [Bitmap] containing the original image.
   * @param context The [Context] necessary to use [RenderScript]
   * @param radius The radius of the blur with a supported range 0 < radius <= [ ][.BLUR_MAX_RADIUS]
   */
  @JvmStatic
  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  fun blurBitmap(dest: Bitmap, src: Bitmap, context: Context, radius: Int) {
    checkNotNull(dest)
    checkNotNull(src)
    checkNotNull(context)
    Preconditions.checkArgument(radius > 0 && radius <= BLUR_MAX_RADIUS)
    var rs: RenderScript? = null
    try {
      rs = checkNotNull(RenderScript.create(context))

      // Create an Intrinsic Blur Script using the Renderscript
      val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

      // Create the input/output allocations with Renderscript and the src/dest bitmaps
      val allIn = checkNotNull(Allocation.createFromBitmap(rs, src))
      val allOut = checkNotNull(Allocation.createFromBitmap(rs, dest))

      // Set the radius of the blur
      blurScript.setRadius(radius.toFloat())
      blurScript.setInput(allIn)
      blurScript.forEach(allOut)
      allOut.copyTo(dest)
      blurScript.destroy()
      allIn.destroy()
      allOut.destroy()
    } finally {
      rs?.destroy()
    }
  }

  @JvmStatic
  fun canUseRenderScript(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
}
