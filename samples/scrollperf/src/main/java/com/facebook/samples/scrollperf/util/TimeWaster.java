/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.util;

/** Class with utility method which spend time */
public final class TimeWaster {

  /**
   * A not efficient calculation of the fibonacci sequence
   *
   * @param n The position for the fibonacci sequence
   * @return The n-th fibonacci number
   */
  public static long Fib(int n) {
    if (n < 2) {
      return n;
    } else {
      return Fib(n - 1) + Fib(n - 2);
    }
  }
}
