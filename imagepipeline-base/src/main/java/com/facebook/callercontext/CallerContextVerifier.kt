/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.callercontext

/** Verifies CallerContext */
fun interface CallerContextVerifier {
  fun verifyCallerContext(callerContext: Any?, isPrefetch: Boolean)
}
