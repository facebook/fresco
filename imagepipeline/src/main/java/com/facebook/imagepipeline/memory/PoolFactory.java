/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import javax.annotation.concurrent.NotThreadSafe;

import com.facebook.common.internal.Preconditions;

/**
 * Factory class for pools.
 */
@NotThreadSafe
public class PoolFactory {

  private final PoolConfig mConfig;

  private BitmapPool mBitmapPool;
  private ByteArrayPool mCommonByteArrayPool;
  private NativeMemoryChunkPool mNativeMemoryChunkPool;
  private PooledByteBufferFactory mPooledByteBufferFactory;
  private PooledByteStreams mPooledByteStreams;
  private SharedByteArray mSharedByteArray;

  public PoolFactory(PoolConfig config) {
    mConfig = Preconditions.checkNotNull(config);
  }

  public BitmapPool getBitmapPool() {
    if (mBitmapPool == null) {
      mBitmapPool = new BitmapPool(
          mConfig.getMemoryTrimmableRegistry(),
          mConfig.getBitmapPoolParams(),
          mConfig.getBitmapPoolStatsTracker());
    }
    return mBitmapPool;
  }

  public ByteArrayPool getCommonByteArrayPool() {
    if (mCommonByteArrayPool == null) {
      mCommonByteArrayPool = new GenericByteArrayPool(
          mConfig.getMemoryTrimmableRegistry(),
          mConfig.getCommonByteArrayPoolParams(),
          mConfig.getCommonByteArrayPoolStatsTracker());
    }
    return mCommonByteArrayPool;
  }

  public NativeMemoryChunkPool getNativeMemoryChunkPool() {
    if (mNativeMemoryChunkPool == null) {
      mNativeMemoryChunkPool = new NativeMemoryChunkPool(
          mConfig.getMemoryTrimmableRegistry(),
          mConfig.getNativeMemoryChunkPoolParams(),
          mConfig.getNativeMemoryChunkPoolStatsTracker());
    }
    return mNativeMemoryChunkPool;
  }

  public PooledByteBufferFactory getPooledByteBufferFactory() {
    if (mPooledByteBufferFactory == null) {
      mPooledByteBufferFactory = new NativePooledByteBufferFactory(
          getNativeMemoryChunkPool(),
          getPooledByteStreams());
    }
    return mPooledByteBufferFactory;
  }

  public PooledByteStreams getPooledByteStreams() {
    if (mPooledByteStreams == null) {
      mPooledByteStreams = new PooledByteStreams(getCommonByteArrayPool());
    }
    return mPooledByteStreams;
  }

  public SharedByteArray getSharedByteArray() {
    if (mSharedByteArray == null) {
      mSharedByteArray = new SharedByteArray(
          mConfig.getMemoryTrimmableRegistry(),
          mConfig.getSharedByteArrayParams());
    }
    return mSharedByteArray;
  }
}
