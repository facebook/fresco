/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable
/** Callback used to notify about drawable's visibility changes. */
interface VisibilityCallback {

  /**
   * Called when the drawable's visibility changes.
   *
   * @param visible whether or not the drawable is visible
   */
  fun onVisibilityChange(visible: Boolean)

  /** Called when the drawable gets drawn. */
  fun onDraw()
}
