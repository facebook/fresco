/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.memory.helper

object HashCode {
  @JvmStatic fun extend(current: Int, obj: Any?): Int = 31 * current + (obj?.hashCode() ?: 0)
}
