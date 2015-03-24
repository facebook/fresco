/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.disk;

/**
 * Any class that uses a lot of disk space and should implement this interface.
 */
public interface DiskTrimmable {
  /**
   * Called when there is very little disk space left.
   */
  public void trimToMinimum();

  /**
   * Called when there is almost no disk space left and the app is likely to crash soon
   */
  public void trimToNothing();
}
