/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

annotation class BitmapPoolType {
  companion object {
    const val LEGACY = "legacy"
    const val LEGACY_DEFAULT_PARAMS = "legacy_default_params"
    const val DUMMY = "dummy"
    const val DUMMY_WITH_TRACKING = "dummy_with_tracking"
    const val EXPERIMENTAL = "experimental"
    const val DEFAULT = LEGACY
  }
}
