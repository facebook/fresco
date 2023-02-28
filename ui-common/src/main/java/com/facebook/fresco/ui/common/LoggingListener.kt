/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

interface LoggingListener {
  fun onFadeStarted(id: String)

  fun onFadeFinished(id: String)
}
