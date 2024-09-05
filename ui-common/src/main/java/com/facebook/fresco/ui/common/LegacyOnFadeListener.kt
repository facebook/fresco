/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

@Deprecated(
    "Please use the OnFadeListener directly",
    replaceWith =
        ReplaceWith(
            "OnFadeListener",
            "import com.facebook.fresco.ui.common.OnFadeListener",
        ),
)
interface LegacyOnFadeListener {
  fun onFadeStarted(id: String)

  fun onFadeFinished(id: String)
}
