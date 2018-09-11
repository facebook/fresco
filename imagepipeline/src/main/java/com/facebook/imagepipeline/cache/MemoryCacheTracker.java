/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

public interface MemoryCacheTracker<K> {
  void onCacheHit(K cacheKey);
  void onCacheMiss();
  void onCachePut();
}
