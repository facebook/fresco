/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

class BitmapCounterConfig(builder: Builder) {

  var maxBitmapCount = builder.maxBitmapCount

  class Builder internal constructor() {
    var maxBitmapCount = DEFAULT_MAX_BITMAP_COUNT
      private set

    fun setMaxBitmapCount(maxBitmapCount: Int): Builder = apply {
      this.maxBitmapCount = maxBitmapCount
    }

    fun build(): BitmapCounterConfig = BitmapCounterConfig(this)
  }

  companion object {
    const val DEFAULT_MAX_BITMAP_COUNT = 384
    @JvmStatic fun newBuilder(): Builder = Builder()
  }
}
