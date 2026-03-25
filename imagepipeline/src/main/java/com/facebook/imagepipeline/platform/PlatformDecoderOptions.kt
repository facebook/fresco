/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

class PlatformDecoderOptions(
    val avoidPoolGet: Boolean = false,
    val avoidPoolRelease: Boolean = false,
    val enableDecodeDimensionValidation: Boolean = false,
    val catchNativeDecoderErrors: Boolean = false,
    val errorReporter: DecoderErrorReporter? = null,
) {
  fun interface DecoderErrorReporter {
    fun reportError(category: String, message: String, cause: Throwable?)
  }
}
