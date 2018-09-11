/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common;

import android.net.Uri;

/**
 * Strongly typed cache key to be used instead of {@link Object}.
 *
 * <p> {@link #toString}, {@link #equals} and {@link #hashCode} methods must be implemented.
 */
public interface CacheKey {

  /** This is useful for instrumentation and debugging purposes. */
  String toString();

  /** This method must be implemented, otherwise the cache keys will be be compared by reference. */
  boolean equals(Object o);

  /** This method must be implemented with accordance to the {@link #equals} method. */
  int hashCode();

  /**
   * Returns true if this key was constructed from this {@link Uri}.
   *
   * Used for cases like deleting all keys for a given uri.
   */
  boolean containsUri(Uri uri);

  /**
   * Returns a string representation of the URI at the heart of the cache key. In cases of multiple
   * keys being contained, the first is returned.
   */
  String getUriString();
}
