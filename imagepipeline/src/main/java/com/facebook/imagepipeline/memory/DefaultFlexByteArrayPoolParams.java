/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import android.util.SparseIntArray;
import com.facebook.common.util.ByteConstants;

/**
 * Provides pool parameters ({@link PoolParams}) for {@link SharedByteArray}
 */
public class DefaultFlexByteArrayPoolParams {
  // the default max buffer size we'll use
  public static final int DEFAULT_MAX_BYTE_ARRAY_SIZE = 4 * ByteConstants.MB;
  // the min buffer size we'll use
  private static final int DEFAULT_MIN_BYTE_ARRAY_SIZE = 128 * ByteConstants.KB;
  // the maximum number of threads permitted to touch this pool
  public static final int DEFAULT_MAX_NUM_THREADS = Runtime.getRuntime().availableProcessors();

  private DefaultFlexByteArrayPoolParams() {
  }

  public static SparseIntArray generateBuckets(int min, int max, int numThreads) {
    SparseIntArray buckets = new SparseIntArray();
    for (int i = min; i <= max; i*=2) {
      buckets.put(i, numThreads);
    }
    return buckets;
  }

  public static PoolParams get() {
    return new PoolParams(
        /* maxSizeSoftCap */ DEFAULT_MAX_BYTE_ARRAY_SIZE,
        /* maxSizeHardCap */ DEFAULT_MAX_NUM_THREADS * DEFAULT_MAX_BYTE_ARRAY_SIZE,
        /* bucketSizes */ generateBuckets(
            DEFAULT_MIN_BYTE_ARRAY_SIZE,
            DEFAULT_MAX_BYTE_ARRAY_SIZE,
            DEFAULT_MAX_NUM_THREADS),
        /* minBucketSize */  DEFAULT_MIN_BYTE_ARRAY_SIZE,
        /* maxBucketSize */  DEFAULT_MAX_BYTE_ARRAY_SIZE,
        /* maxNumThreads */  DEFAULT_MAX_NUM_THREADS);
  }
}
