/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import com.facebook.common.internal.Preconditions;

public class MemoryChunkUtil {

  /**
   * Computes number of bytes that can be safely read/written starting at given offset, but no more
   * than count.
   */
  static int adjustByteCount(final int offset, final int count, final int memorySize) {
    final int available = Math.max(0, memorySize - offset);
    return Math.min(available, count);
  }

  /** Check that copy/read/write operation won't access memory it should not */
  static void checkBounds(
      final int offset,
      final int otherLength,
      final int otherOffset,
      final int count,
      final int memorySize) {
    Preconditions.checkArgument(count >= 0);
    Preconditions.checkArgument(offset >= 0);
    Preconditions.checkArgument(otherOffset >= 0);
    Preconditions.checkArgument(offset + count <= memorySize);
    Preconditions.checkArgument(otherOffset + count <= otherLength);
  }
}
