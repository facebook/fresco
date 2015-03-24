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
 * Empty implementation of {@link SingleByteArrayPoolStatsTracker} that does not perform any
 * tracking.
 */
public class NoOpSingleByteArrayPoolStatsTracker implements SingleByteArrayPoolStatsTracker {
  private static NoOpSingleByteArrayPoolStatsTracker sInstance = null;

  private NoOpSingleByteArrayPoolStatsTracker() {
  }

  public static synchronized NoOpSingleByteArrayPoolStatsTracker getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpSingleByteArrayPoolStatsTracker();
    }
    return sInstance;
  }

  @Override
  public void onBucketedSizeRequested(int bucketedSize) {
  }

  @Override
  public void onMemoryAlloc(int byteArraySize) {
  }

  @Override
  public void onMemoryTrimmed(int byteArraySize) {
  }
}
