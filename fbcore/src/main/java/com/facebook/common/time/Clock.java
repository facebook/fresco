/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.time;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Interface for getting the current time.
 */
@ThreadSafe
public interface Clock {

  /**
   * The maximum time.
   */
  long MAX_TIME = Long.MAX_VALUE;

  /**
   * Gets the current time in milliseconds.
   *
   * @return the current time in milliseconds.
   */
  long now();
}
