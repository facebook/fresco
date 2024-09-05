/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

/**
 * Any component that wants to be able to log visibility changes, should implement this interface.
 * This is meant to be used initially for logging purposes only.
 */
interface VisibilityAware {
  /** Make the component aware that it's visibility might have been updated. */
  fun reportVisible(visible: Boolean)
}
