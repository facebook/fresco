/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transformation

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.QualityInfo

object TransformationUtils {
  @JvmStatic
  fun maybeApplyTransformation(
      transformation: BitmapTransformation?,
      bitmapReference: CloseableReference<Bitmap>?,
  ): Boolean {
    return maybeApplyTransformation(transformation, bitmapReference, null)
  }

  @JvmStatic
  fun maybeApplyTransformation(
      transformation: BitmapTransformation?,
      bitmapReference: CloseableReference<Bitmap>?,
      qualityInfo: QualityInfo?,
  ): Boolean {
    if (transformation == null || bitmapReference == null) {
      return false
    }
    val bitmap = bitmapReference.get()
    if (transformation.modifiesTransparency()) {
      bitmap.setHasAlpha(true)
    }
    if (qualityInfo != null) {
      transformation.transform(bitmap, qualityInfo)
    } else {
      transformation.transform(bitmap)
    }
    return true
  }
}
