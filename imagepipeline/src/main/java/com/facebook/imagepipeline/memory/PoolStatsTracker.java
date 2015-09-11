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
  String BUCKETS_USED_PREFIX = "buckets_used_";
  String USED_COUNT = "used_count";
  String USED_BYTES = "used_bytes";
  String FREE_COUNT = "free_count";
  String FREE_BYTES = "free_bytes";
  String SOFT_CAP = "soft_cap";
  String HARD_CAP = "hard_cap";

  void setBasePool(BasePool basePool);

  void onValueReuse(int bucketedSize);

  void onSoftCapReached();

  void onHardCapReached();

  void onAlloc(int size);

  void onFree(int sizeInBytes);

  void onValueRelease(int sizeInBytes);
}
