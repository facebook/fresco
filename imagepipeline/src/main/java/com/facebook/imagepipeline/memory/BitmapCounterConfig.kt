/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

class BitmapCounterConfig(val maxBitmapCount: Int = DEFAULT_MAX_BITMAP_COUNT) {

  companion object {
    const val DEFAULT_MAX_BITMAP_COUNT = 384
  }
}
