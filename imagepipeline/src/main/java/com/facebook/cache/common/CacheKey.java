/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.common;

/**
 * Strongly typed cache key to be used instead of {@link Object}.
 *
 * <p> {@link #toString}, {@link #equals} and {@link #hashCode} methods must be implemented.
 */
public interface CacheKey {

  /** This is useful for instrumentation and debugging purposes. */
  public String toString();

  /** This method must be implemented, otherwise the cache keys will be be compared by reference. */
  public boolean equals(Object o);

  /** This method must be implemented with accordance to the {@link #equals} method. */
  public int hashCode();
}
