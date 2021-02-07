/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.internal;

import com.facebook.infer.annotation.Nullsafe;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A dummy representation of an immutable set. This can be used temporarily as a type until we have
 * an actual non-gauva implementation.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class ImmutableSet<E> extends HashSet<E> {

  // Prevent direct instantiation.
  private ImmutableSet(Set<E> set) {
    super(set);
  }

  public static <E> ImmutableSet<E> copyOf(Set<E> set) {
    return new ImmutableSet<>(set);
  }

  public static <E> ImmutableSet<E> of(E... elements) {
    HashSet<E> set = new HashSet<>(elements.length);
    Collections.addAll(set, elements);
    return new ImmutableSet<>(set);
  }
}
