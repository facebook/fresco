/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.binaryresource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.facebook.common.internal.Preconditions;

/**
 * A trivial implementation of BinaryResource that wraps a byte array
 */
public class ByteArrayBinaryResource implements BinaryResource {
  private final byte[] mBytes;

  public ByteArrayBinaryResource(byte[] bytes) {
    mBytes = Preconditions.checkNotNull(bytes);
  }

  @Override
  public long size() {
    return mBytes.length;
  }

  @Override
  public InputStream openStream() throws IOException {
    return new ByteArrayInputStream(mBytes);
  }

  /**
   * Get the underlying byte array
   * @return the underlying byte array of this resource
   */
  @Override
  public byte[] read() {
    return mBytes;
  }
}
