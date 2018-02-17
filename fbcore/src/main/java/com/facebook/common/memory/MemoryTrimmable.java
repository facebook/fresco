/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory;

/**
 * A class can implement this interface to react to a {@link MemoryTrimmableRegistry}'s request to
 * trim memory.
 */

public interface MemoryTrimmable {

  /**
   * Trim memory.
   */
  void trim(MemoryTrimType trimType);
}
