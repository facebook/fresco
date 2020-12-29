/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import android.util.SparseIntArray;
import com.facebook.common.util.ByteConstants;
import com.facebook.infer.annotation.Nullsafe;

/** Provides pool parameters ({@link PoolParams}) for common {@link ByteArrayPool} */
@Nullsafe(Nullsafe.Mode.STRICT)
public class DefaultByteArrayPoolParams {
  private static final int DEFAULT_IO_BUFFER_SIZE = 16 * ByteConstants.KB;

  /*
   * There are up to 5 simultaneous IO operations in new pipeline performed by:
   * - 3 image-fetch threads
   * - 2 image-cache threads
   * We should be able to satisfy these requirements without any allocations
   */
  private static final int DEFAULT_BUCKET_SIZE = 5;
  private static final int MAX_SIZE_SOFT_CAP = 5 * DEFAULT_IO_BUFFER_SIZE;

  /** We don't need hard cap here. */
  private static final int MAX_SIZE_HARD_CAP = 1 * ByteConstants.MB;

  /** Get default {@link PoolParams}. */
  public static PoolParams get() {
    // This pool supports only one bucket size: DEFAULT_IO_BUFFER_SIZE
    SparseIntArray defaultBuckets = new SparseIntArray();
    defaultBuckets.put(DEFAULT_IO_BUFFER_SIZE, DEFAULT_BUCKET_SIZE);
    return new PoolParams(MAX_SIZE_SOFT_CAP, MAX_SIZE_HARD_CAP, defaultBuckets);
  }
}
