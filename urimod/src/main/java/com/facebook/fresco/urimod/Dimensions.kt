/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.urimod

data class Dimensions(val w: Int, val h: Int) {
  override fun toString() = "${w}x${h}"
}
