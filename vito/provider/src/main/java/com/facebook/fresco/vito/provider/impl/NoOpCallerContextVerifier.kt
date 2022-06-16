/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider.impl

import com.facebook.callercontext.CallerContextVerifier

object NoOpCallerContextVerifier : CallerContextVerifier {
  override fun verifyCallerContext(callerContext: Any?, isPrefetch: Boolean) {
    // No-op
  }
}
