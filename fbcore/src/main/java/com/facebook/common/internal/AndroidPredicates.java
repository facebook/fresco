/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.internal;

import com.android.internal.util.Predicate;

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
