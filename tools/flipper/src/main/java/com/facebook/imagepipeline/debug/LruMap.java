/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A map that provides a constant-size LRU map by ordering elements by accessing order (and not in
 * insertion order) Most cases would be served better by using Android's LruCache class.
 */
public class LruMap<A, B> extends LinkedHashMap<A, B> {
  private final int mMaxEntries;

  public LruMap(final int maxEntries) {
    super(maxEntries + 1, 1.0f, true);
    mMaxEntries = maxEntries;
  }

  @Override
  protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
    return size() > mMaxEntries;
  }
}
