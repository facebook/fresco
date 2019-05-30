/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static com.facebook.imagepipeline.core.MemoryChunkType.BUFFER_MEMORY;
import static com.facebook.imagepipeline.core.MemoryChunkType.NATIVE_MEMORY;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteStreams;
import com.facebook.imagepipeline.core.MemoryChunkType;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Factory class for pools.
 */
@NotThreadSafe
public class PoolFactory {

  private final PoolConfig mConfig;

  private BitmapPool mBitmapPool;
  private BufferMemoryChunkPool mBufferMemoryChunkPool;
  private FlexByteArrayPool mFlexByteArrayPool;
  private NativeMemoryChunkPool mNativeMemoryChunkPool;
  private PooledByteBufferFactory mPooledByteBufferFactory;
  private PooledByteStreams mPooledByteStreams;
  private SharedByteArray mSharedByteArray;
  private ByteArrayPool mSmallByteArrayPool;

  public PoolFactory(PoolConfig config) {
    mConfig = Preconditions.checkNotNull(config);
  }

  public BitmapPool getBitmapPool() {
    if (mBitmapPool == null) {
      final String bitmapPoolType = mConfig.getBitmapPoolType();
      switch (bitmapPoolType) {
        case BitmapPoolType.DUMMY:
          mBitmapPool = new DummyBitmapPool();
          break;
        case BitmapPoolType.DUMMY_WITH_TRACKING:
          mBitmapPool = new DummyTrackingInUseBitmapPool();
          break;
        case BitmapPoolType.EXPERIMENTAL:
          mBitmapPool =
              new LruBitmapPool(
                  mConfig.getBitmapPoolMaxPoolSize(),
                  mConfig.getBitmapPoolMaxBitmapSize(),
                  NoOpPoolStatsTracker.getInstance(),
                  mConfig.isRegisterLruBitmapPoolAsMemoryTrimmable()
                      ? mConfig.getMemoryTrimmableRegistry()
                      : null);
          break;
        case BitmapPoolType.LEGACY_DEFAULT_PARAMS:
          mBitmapPool =
              new BucketsBitmapPool(
                  mConfig.getMemoryTrimmableRegistry(),
                  DefaultBitmapPoolParams.get(),
                  mConfig.getBitmapPoolStatsTracker());
          break;
        case BitmapPoolType.LEGACY:
          // fall through
        default:
          mBitmapPool = new BucketsBitmapPool(
              mConfig.getMemoryTrimmableRegistry(),
              mConfig.getBitmapPoolParams(),
              mConfig.getBitmapPoolStatsTracker());
      }
    }
    return mBitmapPool;
  }

  public BufferMemoryChunkPool getBufferMemoryChunkPool() {
    if (mBufferMemoryChunkPool == null) {
      mBufferMemoryChunkPool =
          new BufferMemoryChunkPool(
              mConfig.getMemoryTrimmableRegistry(),
              mConfig.getMemoryChunkPoolParams(),
              mConfig.getMemoryChunkPoolStatsTracker());
    }
    return mBufferMemoryChunkPool;
  }

  public FlexByteArrayPool getFlexByteArrayPool() {
    if (mFlexByteArrayPool == null) {
      mFlexByteArrayPool = new FlexByteArrayPool(
          mConfig.getMemoryTrimmableRegistry(),
          mConfig.getFlexByteArrayPoolParams());
    }
    return mFlexByteArrayPool;
  }

  public int getFlexByteArrayPoolMaxNumThreads() {
    return mConfig.getFlexByteArrayPoolParams().maxNumThreads;
  }

  public NativeMemoryChunkPool getNativeMemoryChunkPool() {
    if (mNativeMemoryChunkPool == null) {
      mNativeMemoryChunkPool =
          new NativeMemoryChunkPool(
              mConfig.getMemoryTrimmableRegistry(),
              mConfig.getMemoryChunkPoolParams(),
              mConfig.getMemoryChunkPoolStatsTracker());
    }
    return mNativeMemoryChunkPool;
  }

  public PooledByteBufferFactory getPooledByteBufferFactory() {
    return getPooledByteBufferFactory(NATIVE_MEMORY);
  }

  public PooledByteBufferFactory getPooledByteBufferFactory(@MemoryChunkType int memoryChunkType) {
    if (mPooledByteBufferFactory == null) {
      mPooledByteBufferFactory =
          new MemoryPooledByteBufferFactory(
              getMemoryChunkPool(memoryChunkType), getPooledByteStreams());
    }
    return mPooledByteBufferFactory;
  }

  public PooledByteStreams getPooledByteStreams() {
    if (mPooledByteStreams == null) {
      mPooledByteStreams = new PooledByteStreams(getSmallByteArrayPool());
    }
    return mPooledByteStreams;
  }

  public SharedByteArray getSharedByteArray() {
    if (mSharedByteArray == null) {
      mSharedByteArray = new SharedByteArray(
          mConfig.getMemoryTrimmableRegistry(),
          mConfig.getFlexByteArrayPoolParams());
    }
    return mSharedByteArray;
  }

  public ByteArrayPool getSmallByteArrayPool() {
    if (mSmallByteArrayPool == null) {
      mSmallByteArrayPool = new GenericByteArrayPool(
          mConfig.getMemoryTrimmableRegistry(),
          mConfig.getSmallByteArrayPoolParams(),
          mConfig.getSmallByteArrayPoolStatsTracker());
    }
    return mSmallByteArrayPool;
  }

  private MemoryChunkPool getMemoryChunkPool(@MemoryChunkType int memoryChunkType) {
    switch (memoryChunkType) {
      case NATIVE_MEMORY:
        return getNativeMemoryChunkPool();
      case BUFFER_MEMORY:
        return getBufferMemoryChunkPool();
      default:
        throw new IllegalArgumentException("Invalid MemoryChunkType");
    }
  }
}
