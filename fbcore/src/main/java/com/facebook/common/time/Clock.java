/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.time;

/**
 * Interface for getting the current time.
 */
public interface Clock {

  /**
   * The maximum time.
   */
  public static final long MAX_TIME = Long.MAX_VALUE;

  /**
   * Gets the current time in milliseconds.
   *
   * @return the current time in milliseconds.
   */
  long now();
}
