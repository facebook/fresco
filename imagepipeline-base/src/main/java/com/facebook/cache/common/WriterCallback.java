/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common;

import com.facebook.infer.annotation.Nullsafe;
import java.io.IOException;
import java.io.OutputStream;

/** Callback that writes to an {@link OutputStream}. */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface WriterCallback {
  void write(OutputStream os) throws IOException;
}
