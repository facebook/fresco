/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug

import java.util.LinkedHashMap

/**
 * A map that provides a constant-size LRU map by ordering elements by accessing order (and not in
 * insertion order) Most cases would be served better by using Android's LruCache class.
 */
class LruMap<A, B>(private val maxEntries: Int) : LinkedHashMap<A, B>(maxEntries + 1, 1.0f, true) {
  override fun removeEldestEntry(eldest: Map.Entry<A, B>): Boolean = size > maxEntries
}
