/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info

import androidx.annotation.IntDef

/**
 * Image origin that indicates whether an image has been loaded from cache, network or other source.
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    ImageOrigin.UNKNOWN,
    ImageOrigin.NETWORK,
    ImageOrigin.DISK,
    ImageOrigin.MEMORY_ENCODED,
    ImageOrigin.MEMORY_BITMAP,
    ImageOrigin.MEMORY_BITMAP_SHORTCUT,
    ImageOrigin.LOCAL)
annotation class ImageOrigin {
  companion object {
    const val UNKNOWN: Int = 1
    const val NETWORK: Int = 2
    const val DISK: Int = 3
    const val MEMORY_ENCODED: Int = 4
    const val MEMORY_BITMAP: Int = 5
    const val MEMORY_BITMAP_SHORTCUT: Int = 6
    const val LOCAL: Int = 7
  }
}
