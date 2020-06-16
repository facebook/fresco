/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.internal;

import com.facebook.infer.annotation.Nullsafe;

/** Additional predicates. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class AndroidPredicates {

  private AndroidPredicates() {}

  public static <T> Predicate<T> True() {
    return new Predicate<T>() {
      @Override
      public boolean apply(T t) {
        return true;
      }
    };
  }

  public static <T> Predicate<T> False() {
    return new Predicate<T>() {
      @Override
      public boolean apply(T t) {
        return false;
      }
    };
  }
}
