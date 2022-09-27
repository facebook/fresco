/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import java.util.LinkedHashSet
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class BoundedLinkedHashSet<E>(maxSize: Int) {

  private val maxSize: Int = maxSize
  private val linkedHashSet: LinkedHashSet<E> = LinkedHashSet(maxSize)

  @Synchronized operator fun contains(o: E): Boolean = linkedHashSet.contains(o)

  @Synchronized
  fun add(key: E): Boolean {
    if (linkedHashSet.size == this.maxSize) {
      linkedHashSet.remove(linkedHashSet.iterator().next())
    }
    linkedHashSet.remove(key)
    return linkedHashSet.add(key)
  }
}
