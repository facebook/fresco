/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry;
import javax.annotation.concurrent.Immutable;

/**
 * Configuration class for pools.
 */
@Immutable
public class PoolConfig {

  public static final String DEFAULT_BITMAP_POOL_TYPE = "legacy";

  // There are a lot of parameters in this class. Please follow strict alphabetical order.

  private final PoolParams mBitmapPoolParams;
  private final PoolStatsTracker mBitmapPoolStatsTracker;
  private final PoolParams mFlexByteArrayPoolParams;
  private final MemoryTrimmableRegistry mMemoryTrimmableRegistry;
  private final PoolParams mMemoryChunkPoolParams;
  private final PoolStatsTracker mMemoryChunkPoolStatsTracker;
  private final PoolParams mSmallByteArrayPoolParams;
  private final PoolStatsTracker mSmallByteArrayPoolStatsTracker;
  private final String mBitmapPoolType;

  private PoolConfig(Builder builder) {
    mBitmapPoolParams =
        builder.mBitmapPoolParams == null ?
            DefaultBitmapPoolParams.get() :
            builder.mBitmapPoolParams;
    mBitmapPoolStatsTracker =
        builder.mBitmapPoolStatsTracker == null ?
            NoOpPoolStatsTracker.getInstance() :
            builder.mBitmapPoolStatsTracker;
    mFlexByteArrayPoolParams =
        builder.mFlexByteArrayPoolParams == null ?
            DefaultFlexByteArrayPoolParams.get() :
            builder.mFlexByteArrayPoolParams;
    mMemoryTrimmableRegistry =
        builder.mMemoryTrimmableRegistry == null ?
            NoOpMemoryTrimmableRegistry.getInstance() :
            builder.mMemoryTrimmableRegistry;
    mMemoryChunkPoolParams =
        builder.mMemoryChunkPoolParams == null
            ? DefaultNativeMemoryChunkPoolParams.get()
            : builder.mMemoryChunkPoolParams;
    mMemoryChunkPoolStatsTracker =
        builder.mMemoryChunkPoolStatsTracker == null
            ? NoOpPoolStatsTracker.getInstance()
            : builder.mMemoryChunkPoolStatsTracker;
    mSmallByteArrayPoolParams =
        builder.mSmallByteArrayPoolParams == null ?
            DefaultByteArrayPoolParams.get() :
            builder.mSmallByteArrayPoolParams;
    mSmallByteArrayPoolStatsTracker =
        builder.mSmallByteArrayPoolStatsTracker == null ?
            NoOpPoolStatsTracker.getInstance() :
            builder.mSmallByteArrayPoolStatsTracker;

    mBitmapPoolType = builder.mBitmapPoolType == null ? DEFAULT_BITMAP_POOL_TYPE : builder.mBitmapPoolType;
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

  public PoolParams getMemoryChunkPoolParams() {
    return mMemoryChunkPoolParams;
  }

  public PoolStatsTracker getMemoryChunkPoolStatsTracker() {
    return mMemoryChunkPoolStatsTracker;
  }

  public PoolParams getFlexByteArrayPoolParams() {
    return mFlexByteArrayPoolParams;
  }

  public PoolParams getSmallByteArrayPoolParams() {
    return mSmallByteArrayPoolParams;
  }

  public PoolStatsTracker getSmallByteArrayPoolStatsTracker() {
    return mSmallByteArrayPoolStatsTracker;
  }

  public String getBitmapPoolType() {
    return mBitmapPoolType;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private PoolParams mBitmapPoolParams;
    private PoolStatsTracker mBitmapPoolStatsTracker;
    private PoolParams mFlexByteArrayPoolParams;
    private MemoryTrimmableRegistry mMemoryTrimmableRegistry;
    private PoolParams mMemoryChunkPoolParams;
    private PoolStatsTracker mMemoryChunkPoolStatsTracker;
    private PoolParams mSmallByteArrayPoolParams;
    private PoolStatsTracker mSmallByteArrayPoolStatsTracker;
    private String mBitmapPoolType;

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

    public Builder setFlexByteArrayPoolParams(PoolParams flexByteArrayPoolParams) {
      mFlexByteArrayPoolParams = flexByteArrayPoolParams;
      return this;
    }

    public Builder setMemoryTrimmableRegistry(MemoryTrimmableRegistry memoryTrimmableRegistry) {
      mMemoryTrimmableRegistry = memoryTrimmableRegistry;
      return this;
    }

    public Builder setNativeMemoryChunkPoolParams(PoolParams memoryChunkPoolParams) {
      mMemoryChunkPoolParams = Preconditions.checkNotNull(memoryChunkPoolParams);
      return this;
    }

    public Builder setNativeMemoryChunkPoolStatsTracker(
        PoolStatsTracker memoryChunkPoolStatsTracker) {
      mMemoryChunkPoolStatsTracker = Preconditions.checkNotNull(memoryChunkPoolStatsTracker);
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

    public PoolConfig build() {
      return new PoolConfig(this);
    }

    public void setBitmapPoolType(String bitmapPoolType) {
      mBitmapPoolType = bitmapPoolType;
    }
  }
}
