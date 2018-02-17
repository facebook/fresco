/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.disk;

/**
 * Any class that uses a lot of disk space and should implement this interface.
 */
public interface DiskTrimmable {
  /**
   * Called when there is very little disk space left.
   */
  void trimToMinimum();

  /**
   * Called when there is almost no disk space left and the app is likely to crash soon
   */
  void trimToNothing();
}
