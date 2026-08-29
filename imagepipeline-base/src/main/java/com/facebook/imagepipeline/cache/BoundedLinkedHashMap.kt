/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import java.util.LinkedHashMap
import javax.annotation.concurrent.ThreadSafe

/** Map that evicts its least recently accessed entry once [maxSize] entries are exceeded. */
@ThreadSafe
class BoundedLinkedHashMap<K, V>(maxSize: Int) {

  private val linkedHashMap: LinkedHashMap<K, V> =
      object : LinkedHashMap<K, V>(maxSize, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean =
            size > maxSize
      }

  @Synchronized operator fun get(key: K): V? = linkedHashMap[key]

  @Synchronized
  fun put(key: K, value: V) {
    linkedHashMap[key] = value
  }

  @Synchronized fun remove(key: K): V? = linkedHashMap.remove(key)

  companion object {
    private const val LOAD_FACTOR = 0.75f
  }
}
