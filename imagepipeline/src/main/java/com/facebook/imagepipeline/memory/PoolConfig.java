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
  private final PoolParams mCommonByteArrayPoolParams;
  private final PoolStatsTracker mCommonByteArrayPoolStatsTracker;
  private final MemoryTrimmableRegistry mMemoryTrimmableRegistry;
  private final PoolParams mNativeMemoryChunkPoolParams;
  private final PoolStatsTracker mNativeMemoryChunkPoolStatsTracker;
  private final PoolParams mSharedByteArrayParams;

  private PoolConfig(Builder builder) {
    mBitmapPoolParams =
        builder.mBitmapPoolParams == null ?
            DefaultBitmapPoolParams.get() :
            builder.mBitmapPoolParams;
    mBitmapPoolStatsTracker =
        builder.mBitmapPoolStatsTracker == null ?
            NoOpPoolStatsTracker.getInstance() :
            builder.mBitmapPoolStatsTracker;
    mCommonByteArrayPoolParams =
        builder.mCommonByteArrayPoolParams == null ?
            DefaultByteArrayPoolParams.get() :
            builder.mCommonByteArrayPoolParams;
    mCommonByteArrayPoolStatsTracker =
        builder.mCommonByteArrayPoolStatsTracker == null ?
            NoOpPoolStatsTracker.getInstance() :
            builder.mCommonByteArrayPoolStatsTracker;
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
  }

  public PoolParams getBitmapPoolParams() {
    return mBitmapPoolParams;
  }

  public PoolStatsTracker getBitmapPoolStatsTracker() {
    return mBitmapPoolStatsTracker;
  }

  public PoolParams getCommonByteArrayPoolParams() {
    return mCommonByteArrayPoolParams;
  }

  public PoolStatsTracker getCommonByteArrayPoolStatsTracker() {
    return mCommonByteArrayPoolStatsTracker;
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


  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private PoolParams mBitmapPoolParams;
    private PoolStatsTracker mBitmapPoolStatsTracker;
    private PoolParams mCommonByteArrayPoolParams;
    private PoolStatsTracker mCommonByteArrayPoolStatsTracker;
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

    public Builder setCommonByteArrayPoolParams(PoolParams commonByteArrayPoolParams) {
      mCommonByteArrayPoolParams = Preconditions.checkNotNull(commonByteArrayPoolParams);
      return this;
    }

    public Builder setCommonByteArrayPoolStatsTracker(
        PoolStatsTracker commonByteArrayPoolStatsTracker) {
      mCommonByteArrayPoolStatsTracker =
          Preconditions.checkNotNull(commonByteArrayPoolStatsTracker);
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
