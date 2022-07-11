/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.nativecode

import android.graphics.Bitmap
import com.facebook.imagepipeline.nativecode.NativeRoundingFilter
import com.facebook.imagepipeline.transformation.BitmapTransformation
import com.facebook.imagepipeline.transformation.CircularTransformation

data class CircularBitmapTransformation(
    val isAntiAliased: Boolean = true,
    private val useFastNativeRounding: Boolean = false
) : BitmapTransformation, CircularTransformation {

  override fun transform(bitmap: Bitmap) {
    if (useFastNativeRounding) {
      NativeRoundingFilter.toCircleFast(bitmap, isAntiAliased)
    } else {
      NativeRoundingFilter.toCircle(bitmap, isAntiAliased)
    }
  }

  override fun modifiesTransparency(): Boolean = true
}
