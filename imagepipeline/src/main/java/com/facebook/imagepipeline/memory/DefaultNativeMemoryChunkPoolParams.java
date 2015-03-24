/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import android.util.SparseIntArray;

import com.facebook.common.util.ByteConstants;

/**
 * Provides pool parameters ({@link PoolParams}) for {@link NativeMemoryChunkPool}
 *
 */
public class DefaultNativeMemoryChunkPoolParams {
  /**
   * Length of 'small' sized buckets. Bucket lengths for these buckets are larger because
   * they're smaller in size
   */
  private static final int SMALL_BUCKET_LENGTH = 5;

  /**
   * Bucket lengths for 'large' (> 256KB) buckets
   */
  private static final int LARGE_BUCKET_LENGTH = 2;

  public static PoolParams get() {
    SparseIntArray DEFAULT_BUCKETS = new SparseIntArray();
    DEFAULT_BUCKETS.put(1 * ByteConstants.KB, SMALL_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(2 * ByteConstants.KB, SMALL_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(4 * ByteConstants.KB, SMALL_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(8 * ByteConstants.KB, SMALL_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(16 * ByteConstants.KB, SMALL_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(32 * ByteConstants.KB, SMALL_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(64 * ByteConstants.KB, SMALL_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(128 * ByteConstants.KB, SMALL_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(256 * ByteConstants.KB, LARGE_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(512 * ByteConstants.KB, LARGE_BUCKET_LENGTH);
    DEFAULT_BUCKETS.put(1024 * ByteConstants.KB, LARGE_BUCKET_LENGTH);
    return new PoolParams(
        getMaxSizeSoftCap(),
        getMaxSizeHardCap(),
        DEFAULT_BUCKETS);
  }

  /**
   * {@link NativeMemoryChunkPool} manages memory on the native heap, so we don't need as strict
   * caps as we would if we were on the Dalvik heap. However, since native memory OOMs are
   * significantly more problematic than Dalvik OOMs, we would like to stay conservative.
   */
  private static int getMaxSizeSoftCap() {
    final int maxMemory = (int)Math.min(Runtime.getRuntime().maxMemory(), Integer.MAX_VALUE);
    if (maxMemory < 16 * ByteConstants.MB) {
      return 3 * ByteConstants.MB;
    } else if (maxMemory < 32 * ByteConstants.MB) {
      return 6 * ByteConstants.MB;
    } else {
      return 12 * ByteConstants.MB;
    }
  }

  /**
   * We need a smaller cap for devices with less then 16 MB so that we don't run the risk of
   * evicting other processes from the native heap.
   */
  private static int getMaxSizeHardCap() {
    final int maxMemory = (int) Math.min(Runtime.getRuntime().maxMemory(), Integer.MAX_VALUE);
    if (maxMemory < 16 * ByteConstants.MB) {
      return maxMemory / 2;
    } else {
      return maxMemory / 4 * 3;
    }
  }
}
