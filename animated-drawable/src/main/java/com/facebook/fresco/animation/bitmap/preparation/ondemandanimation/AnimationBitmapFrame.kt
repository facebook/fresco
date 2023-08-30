/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import java.io.Closeable

class AnimationBitmapFrame(var frameNumber: Int, val bitmap: CloseableReference<Bitmap>) :
    Closeable {
  fun isValidFor(frameNumber: Int): Boolean = this.frameNumber == frameNumber && bitmap.isValid

  fun isValid(): Boolean = bitmap.isValid

  override fun close() {
    bitmap.close()
  }
}
