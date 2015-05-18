/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to create immutable maps.
 *
 * <p>We do not replicate Guava's ImmutableMap class here. This class merely returns standard
 * {@link HashMap}s wrapped so that they throw UnsupportedOperationExceptions on any write method.
 */
public class ImmutableMap {
  private ImmutableMap() {}

  public static <K, V> Map<K, V> of() {
    return Collections.unmodifiableMap(new HashMap<K, V>());
  }

  public static <K, V> Map<K, V> of(K k1, V v1) {
    Map<K, V> map = new HashMap<>();
    map.put(k1, v1);
    return Collections.unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
    Map<K, V> map = new HashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    return Collections.unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3) {
    Map<K, V> map = new HashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    return Collections.unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    Map<K, V> map = new HashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    map.put(k4, v4);
    return Collections.unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    Map<K, V> map = new HashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    map.put(k4, v4);
    map.put(k5, v5);
    return Collections.unmodifiableMap(map);
  }

  // looking for of() with > 5 entries? Use the put method instead
}
