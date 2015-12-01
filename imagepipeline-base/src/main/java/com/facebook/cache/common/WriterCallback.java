/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.common;

import java.io.IOException;
import java.io.OutputStream;

/**
* Callback that writes to an {@link OutputStream}.
*/
public interface WriterCallback {
  void write(OutputStream os) throws IOException;
}
