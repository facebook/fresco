/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

class CircularList(val size: Int) {

  fun isTargetAhead(from: Int, target: Int, length: Int): Boolean {
    val endPosition = getPosition(from + length)

    return if (from < endPosition) {
      target in from..endPosition
    } else {
      target in from..size || target in 0..endPosition
    }
  }

  fun getPosition(target: Int): Int {
    val circularPosition = target % size

    return circularPosition.takeIf { it >= 0 } ?: (circularPosition + size)
  }

  fun sublist(from: Int, length: Int): List<Int> = (0 until length).map { getPosition(from + it) }
}
