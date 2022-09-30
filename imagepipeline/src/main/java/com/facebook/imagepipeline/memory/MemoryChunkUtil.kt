/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import com.facebook.common.internal.Preconditions

object MemoryChunkUtil {

  /**
   * Computes number of bytes that can be safely read/written starting at given offset, but no more
   * than count.
   */
  @JvmStatic
  fun adjustByteCount(offset: Int, count: Int, memorySize: Int): Int {
    val available = Math.max(0, memorySize - offset)
    return Math.min(available, count)
  }

  /** Check that copy/read/write operation won't access memory it should not */
  @JvmStatic
  fun checkBounds(offset: Int, otherLength: Int, otherOffset: Int, count: Int, memorySize: Int) {
    Preconditions.checkArgument(count >= 0, "count (%d) ! >= 0", count)
    Preconditions.checkArgument(offset >= 0, "offset (%d) ! >= 0", offset)
    Preconditions.checkArgument(otherOffset >= 0, "otherOffset (%d) ! >= 0", otherOffset)
    Preconditions.checkArgument(
        offset + count <= memorySize, "offset (%d) + count (%d) ! <= %d", offset, count, memorySize)
    Preconditions.checkArgument(
        otherOffset + count <= otherLength,
        "otherOffset (%d) + count (%d) ! <= %d",
        otherOffset,
        count,
        otherLength)
  }
}
