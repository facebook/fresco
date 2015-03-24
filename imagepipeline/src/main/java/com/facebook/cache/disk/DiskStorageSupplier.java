/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.disk;

import java.io.IOException;

/**
 * An interface to supply a DiskStorage instance
 */
public interface DiskStorageSupplier {

  /**
   * Get a concrete instance of DiskStorage
   * @return an instance of DiskStorage
   * @throws IOException
   */
  public DiskStorage get() throws IOException;
}
