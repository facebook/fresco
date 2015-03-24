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
 * Empty implementation of PoolStatsTracker that does not perform any tracking.
 */
public class NoOpPoolStatsTracker implements PoolStatsTracker {
  private static NoOpPoolStatsTracker sInstance = null;

  private NoOpPoolStatsTracker() {
  }

  public static synchronized NoOpPoolStatsTracker getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpPoolStatsTracker();
    }
    return sInstance;
  }

  @Override
  public void setBasePool(BasePool basePool) {
  }

  @Override
  public void onValueReuse(int bucketedSize) {
  }

  @Override
  public void onSoftCapReached() {
  }

  @Override
  public void onHardCapReached() {
  }

  @Override
  public void onAlloc(int size) {
  }

  @Override
  public void onFree(int sizeInBytes) {
  }

  @Override
  public void onValueRelease(int sizeInBytes) {
  }
}
