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
 * Listener that logs pool statistics.
 */
public interface PoolStatsTracker {
  public static final String BUCKETS_USED_PREFIX = "buckets_used_";
  public static final String USED_COUNT = "used_count";
  public static final String USED_BYTES = "used_bytes";
  public static final String FREE_COUNT = "free_count";
  public static final String FREE_BYTES = "free_bytes";
  public static final String SOFT_CAP = "soft_cap";
  public static final String HARD_CAP = "hard_cap";

  public void setBasePool(BasePool basePool);

  public void onValueReuse(int bucketedSize);

  public void onSoftCapReached();

  public void onHardCapReached();

  public void onAlloc(int size);

  public void onFree(int sizeInBytes);

  public void onValueRelease(int sizeInBytes);
}
