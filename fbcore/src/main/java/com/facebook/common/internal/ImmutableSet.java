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
import java.util.HashSet;
import java.util.Set;

/**
 * A dummy representation of an immutable set. This can be used temporarily as a type until we have
 * an actual non-gauva implementation.
 */
public class ImmutableSet<E> extends HashSet<E> {

  // Prevent direct instantiation.
  private ImmutableSet(Set<E> set) {
    super(set);
  }

  public static <E> ImmutableSet<E> copyOf(Set<E> set) {
    return new ImmutableSet<>(set);
  }

  public static <E> ImmutableSet<E> of(E... elements) {
    HashSet<E> set = new HashSet<>();
    Collections.addAll(set, elements);
    return new ImmutableSet<>(set);
  }
}
