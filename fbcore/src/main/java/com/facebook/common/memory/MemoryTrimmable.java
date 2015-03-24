/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
  public void trim(MemoryTrimType trimType);
}
