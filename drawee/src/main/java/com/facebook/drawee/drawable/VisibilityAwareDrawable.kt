/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

/** Interface that enables setting a visibility callback. */
fun interface VisibilityAwareDrawable {
  /**
   * Sets a visibility callback.
   *
   * @param visibilityCallback the visibility callback to be set
   */
  fun setVisibilityCallback(visibilityCallback: VisibilityCallback?)
}
