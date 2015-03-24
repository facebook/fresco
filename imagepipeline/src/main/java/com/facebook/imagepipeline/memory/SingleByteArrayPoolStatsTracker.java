/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

/**
 * Listener that logs SingleByteArrayPool statistics.
 */
public interface SingleByteArrayPoolStatsTracker {
  public static final String BUCKETED_SIZE_REQUESTED_PREFIX = "bucketed_size_requested_";
  public static final String MEMORY_ALLOC_PREFIX = "memory_alloc_";
  public static final String MEMORY_TRIMMED_PREFIX = "memory_trimmed_";

  public void onBucketedSizeRequested(int bucketedSize);

  public void onMemoryAlloc(int byteArraySize);

  public void onMemoryTrimmed(int byteArraySize);
}
