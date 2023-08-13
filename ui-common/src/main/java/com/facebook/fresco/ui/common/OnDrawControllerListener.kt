/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

fun interface OnDrawControllerListener<INFO> {
  /**
   * Called when the image is drawn
   *
   * @param id controller id
   * @param imageInfo image info
   * @param dimensionsInfo viewport and encoded image dimensions
   */
  fun onImageDrawn(id: String, imageInfo: INFO, dimensionsInfo: DimensionsInfo)
}
