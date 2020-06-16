/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.internal;

import com.facebook.infer.annotation.Nullsafe;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to create immutable maps.
 *
 * <p>We do not replicate Guava's ImmutableMap class here. This class merely returns standard {@link
 * HashMap}s wrapped so that they throw UnsupportedOperationExceptions on any write method.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class ImmutableMap<K, V> extends HashMap<K, V> {

  private ImmutableMap(Map<? extends K, ? extends V> map) {
    super(map);
  }

  public static <K, V> Map<K, V> of() {
    return Collections.unmodifiableMap(new HashMap<K, V>());
  }

  public static <K, V> Map<K, V> of(K k1, V v1) {
    Map<K, V> map = new HashMap<>(1);
    map.put(k1, v1);
    return Collections.unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
    Map<K, V> map = new HashMap<>(2);
    map.put(k1, v1);
    map.put(k2, v2);
    return Collections.unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
    Map<K, V> map = new HashMap<>(3);
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    return Collections.unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    Map<K, V> map = new HashMap<>(4);
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    map.put(k4, v4);
    return Collections.unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    Map<K, V> map = new HashMap<>(5);
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    map.put(k4, v4);
    map.put(k5, v5);
    return Collections.unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
    Map<K, V> map = new HashMap<>(6);
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    map.put(k4, v4);
    map.put(k5, v5);
    map.put(k6, v6);
    return Collections.unmodifiableMap(map);
  }

  // looking for of() with > 6 entries? Use the put method instead

  // Dummy method at the moment to help us enforce types.
  public static <K, V> ImmutableMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
    return new ImmutableMap<>(map);
  }
}
