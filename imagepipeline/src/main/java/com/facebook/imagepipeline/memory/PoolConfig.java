/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry;

/**
 * Configuration class for pools.
 */
public class PoolConfig {

  // There are a lot of parameters in this class. Please follow strict alphabetical order.

  private final PoolParams mBitmapPoolParams;
  private final PoolStatsTracker mBitmapPoolStatsTracker;
  private final MemoryTrimmableRegistry mMemoryTrimmableRegistry;
  private final PoolParams mNativeMemoryChunkPoolParams;
  private final PoolStatsTracker mNativeMemoryChunkPoolStatsTracker;
  private final PoolParams mSharedByteArrayParams;
  private final PoolParams mSmallByteArrayPoolParams;
  private final PoolStatsTracker mSmallByteArrayPoolStatsTracker;

  private PoolConfig(Builder builder) {
    mBitmapPoolParams =
        builder.mBitmapPoolParams == null ?
            DefaultBitmapPoolParams.get() :
            builder.mBitmapPoolParams;
    mBitmapPoolStatsTracker =
        builder.mBitmapPoolStatsTracker == null ?
            NoOpPoolStatsTracker.getInstance() :
            builder.mBitmapPoolStatsTracker;
    mMemoryTrimmableRegistry =
        builder.mMemoryTrimmableRegistry == null ?
            NoOpMemoryTrimmableRegistry.getInstance() :
            builder.mMemoryTrimmableRegistry;
    mNativeMemoryChunkPoolParams =
        builder.mNativeMemoryChunkPoolParams == null ?
            DefaultNativeMemoryChunkPoolParams.get() :
            builder.mNativeMemoryChunkPoolParams;
    mNativeMemoryChunkPoolStatsTracker =
        builder.mNativeMemoryChunkPoolStatsTracker == null ?
            NoOpPoolStatsTracker.getInstance() :
            builder.mNativeMemoryChunkPoolStatsTracker;
    mSharedByteArrayParams =
        builder.mSharedByteArrayParams == null ?
            DefaultSharedByteArrayParams.get() :
            builder.mSharedByteArrayParams;
    mSmallByteArrayPoolParams =
        builder.mSmallByteArrayPoolParams == null ?
            DefaultByteArrayPoolParams.get() :
            builder.mSmallByteArrayPoolParams;
    mSmallByteArrayPoolStatsTracker =
        builder.mSmallByteArrayPoolStatsTracker == null ?
            NoOpPoolStatsTracker.getInstance() :
            builder.mSmallByteArrayPoolStatsTracker;
  }

  public PoolParams getBitmapPoolParams() {
    return mBitmapPoolParams;
  }

  public PoolStatsTracker getBitmapPoolStatsTracker() {
    return mBitmapPoolStatsTracker;
  }

  public MemoryTrimmableRegistry getMemoryTrimmableRegistry() {
    return mMemoryTrimmableRegistry;
  }

  public PoolParams getNativeMemoryChunkPoolParams() {
    return mNativeMemoryChunkPoolParams;
  }

  public PoolStatsTracker getNativeMemoryChunkPoolStatsTracker() {
    return mNativeMemoryChunkPoolStatsTracker;
  }

  public PoolParams getSharedByteArrayParams() {
    return mSharedByteArrayParams;
  }

  public PoolParams getSmallByteArrayPoolParams() {
    return mSmallByteArrayPoolParams;
  }

  public PoolStatsTracker getSmallByteArrayPoolStatsTracker() {
    return mSmallByteArrayPoolStatsTracker;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private PoolParams mBitmapPoolParams;
    private PoolStatsTracker mBitmapPoolStatsTracker;
    private PoolParams mSmallByteArrayPoolParams;
    private PoolStatsTracker mSmallByteArrayPoolStatsTracker;
    private MemoryTrimmableRegistry mMemoryTrimmableRegistry;
    private PoolParams mNativeMemoryChunkPoolParams;
    private PoolStatsTracker mNativeMemoryChunkPoolStatsTracker;
    private PoolParams mSharedByteArrayParams;

    private Builder() {
    }

    public Builder setBitmapPoolParams(PoolParams bitmapPoolParams) {
      mBitmapPoolParams = Preconditions.checkNotNull(bitmapPoolParams);
      return this;
    }

    public Builder setBitmapPoolStatsTracker(
        PoolStatsTracker bitmapPoolStatsTracker) {
      mBitmapPoolStatsTracker = Preconditions.checkNotNull(bitmapPoolStatsTracker);
      return this;
    }

    public Builder setSmallByteArrayPoolParams(PoolParams commonByteArrayPoolParams) {
      mSmallByteArrayPoolParams = Preconditions.checkNotNull(commonByteArrayPoolParams);
      return this;
    }

    public Builder setSmallByteArrayPoolStatsTracker(
        PoolStatsTracker smallByteArrayPoolStatsTracker) {
      mSmallByteArrayPoolStatsTracker =
          Preconditions.checkNotNull(smallByteArrayPoolStatsTracker);
      return this;
    }

    public Builder setMemoryTrimmableRegistry(MemoryTrimmableRegistry memoryTrimmableRegistry) {
      mMemoryTrimmableRegistry = memoryTrimmableRegistry;
      return this;
    }

    public Builder setNativeMemoryChunkPoolParams(PoolParams nativeMemoryChunkPoolParams) {
      mNativeMemoryChunkPoolParams = Preconditions.checkNotNull(nativeMemoryChunkPoolParams);
      return this;
    }

    public Builder setNativeMemoryChunkPoolStatsTracker(
        PoolStatsTracker nativeMemoryChunkPoolStatsTracker) {
      mNativeMemoryChunkPoolStatsTracker =
          Preconditions.checkNotNull(nativeMemoryChunkPoolStatsTracker);
      return this;
    }

    public Builder setSharedByteArrayParams(PoolParams sharedByteArrayParams) {
      mSharedByteArrayParams = sharedByteArrayParams;
      return this;
    }

    public PoolConfig build() {
      return new PoolConfig(this);
    }
  }
}
