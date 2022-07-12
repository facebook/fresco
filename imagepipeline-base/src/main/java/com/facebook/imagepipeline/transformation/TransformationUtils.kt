/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transformation

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference

object TransformationUtils {
  @JvmStatic
  fun maybeApplyTransformation(
      transformation: BitmapTransformation?,
      bitmapReference: CloseableReference<Bitmap>?
  ): Boolean {

    if (transformation == null || bitmapReference == null) {
      return false
    }
    val bitmap = bitmapReference.get()
    if (transformation.modifiesTransparency()) {
      bitmap.setHasAlpha(true)
    }
    transformation.transform(bitmap)
    return true
  }
}
