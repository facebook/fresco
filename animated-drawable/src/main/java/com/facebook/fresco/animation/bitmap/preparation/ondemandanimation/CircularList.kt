/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

class CircularList(val size: Int) {

  fun isTargetAhead(from: Int, target: Int, lenght: Int): Boolean =
      (0 until lenght).any { getPosition(from + it) == target }

  fun getPosition(target: Int): Int = target % size

  fun sublist(from: Int, length: Int): List<Int> = (0 until length).map { getPosition(from + it) }
}
