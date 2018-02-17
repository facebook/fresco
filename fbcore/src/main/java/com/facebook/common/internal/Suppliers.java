/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.internal;

/** Wrapper for creating a Supplier and default Suppliers for convenience. */
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

  /** Boolean supplier that always returns true. */
  public static final Supplier<Boolean> BOOLEAN_TRUE =
      new Supplier<Boolean>() {
        @Override
        public Boolean get() {
          return true;
        }
      };

  /** Boolean supplier that always returns false. */
  public static final Supplier<Boolean> BOOLEAN_FALSE =
      new Supplier<Boolean>() {
        @Override
        public Boolean get() {
          return false;
        }
      };
}
