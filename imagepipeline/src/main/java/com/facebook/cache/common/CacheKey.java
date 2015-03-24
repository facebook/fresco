/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.common;

import com.facebook.common.internal.Preconditions;

/**
 * Strongly typed cache key that is a simple wrapper around a {@link String} object.
 *
 * <p>Users of CacheKey should construct it by providing a unique string that unambiguously
 * identifies the cached resource.
 */
public class CacheKey {
  final String mKey;

  public CacheKey(final String key) {
    mKey = Preconditions.checkNotNull(key);
  }

  @Override
  public String toString() {
    return mKey;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o instanceof CacheKey) {
      final CacheKey otherKey = (CacheKey) o;
      return mKey.equals(otherKey.mKey);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return mKey.hashCode();
  }
}
