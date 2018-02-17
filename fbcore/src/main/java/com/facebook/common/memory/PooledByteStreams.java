/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper class for interacting with java streams, similar to guava's ByteSteams.
 * To prevent numerous allocations of temp buffers pool of byte arrays is used.
 */
public class PooledByteStreams {
  /**
   * Size of temporary buffer to use for copying (16 kb)
   */
  private static final int DEFAULT_TEMP_BUF_SIZE = 16 * 1024;

  private final int mTempBufSize;
  private final ByteArrayPool mByteArrayPool;

  public PooledByteStreams(ByteArrayPool byteArrayPool) {
    this(byteArrayPool, DEFAULT_TEMP_BUF_SIZE);
  }

  @VisibleForTesting
  public PooledByteStreams(ByteArrayPool byteArrayPool, int tempBufSize) {
    Preconditions.checkArgument(tempBufSize > 0);
    mTempBufSize = tempBufSize;
    mByteArrayPool = byteArrayPool;
  }

  /**
   * Copy all bytes from InputStream to OutputStream.
   * @param from InputStream
   * @param to OutputStream
   * @return number of copied bytes
   * @throws IOException
   */
  public long copy(final InputStream from, final OutputStream to) throws IOException {
    long count = 0;
    byte[] tmp = mByteArrayPool.get(mTempBufSize);

    try {
      while (true) {
        int read = from.read(tmp, 0, mTempBufSize);
        if (read == -1) {
          return count;
        }
        to.write(tmp, 0, read);
        count += read;
      }
    } finally {
      mByteArrayPool.release(tmp);
    }
  }

  /**
   * Copy at most number of bytes from InputStream to OutputStream.
   * @param from InputStream
   * @param to OutputStream
   * @param bytesToCopy bytes to copy
   * @return number of copied bytes
   * @throws IOException
   */
  public long copy(
      final InputStream from,
      final OutputStream to,
      final long bytesToCopy) throws IOException {
    Preconditions.checkState(bytesToCopy > 0);
    long copied = 0;
    byte[] tmp = mByteArrayPool.get(mTempBufSize);

    try {
      while (copied < bytesToCopy) {
        int read = from.read(tmp, 0, (int) Math.min(mTempBufSize, bytesToCopy - copied));
        if (read == -1) {
          return copied;
        }
        to.write(tmp, 0, read);
        copied += read;
      }
      return copied;
    } finally {
      mByteArrayPool.release(tmp);
    }
  }
}
