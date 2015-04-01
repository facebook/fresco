/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.internal;

/**
 * Wrapper for creating a Supplier.
 */
public class Suppliers {
  /**
   * Returns a Supplier which always returns {@code instance}.
   *
   * @param instance the instance that should always be provided.
   */
  public static <T> Supplier<T> of(final T instance) {
    return new Supplier<T>() {
      @Override
      public T get() {
        return instance;
      }
    };
  }
}
