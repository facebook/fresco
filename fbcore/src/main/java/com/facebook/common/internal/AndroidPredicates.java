/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.internal;

/**
 * Additional predicates.
 */
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
