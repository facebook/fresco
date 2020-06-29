/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static com.facebook.imagepipeline.core.MemoryChunkType.ASHMEM_MEMORY;
import static com.facebook.imagepipeline.core.MemoryChunkType.BUFFER_MEMORY;
import static com.facebook.imagepipeline.core.MemoryChunkType.NATIVE_MEMORY;

import android.os.Build;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteStreams;
import com.facebook.imagepipeline.core.MemoryChunkType;
import com.facebook.imagepipeline.core.NativeCodeSetup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/** Factory class for pools. */
@NotThreadSafe
public class PoolFactory {

  private final PoolConfig mConfig;

  private @Nullable MemoryChunkPool mAshmemMemoryChunkPool;
  private BitmapPool mBitmapPool;
  private @Nullable MemoryChunkPool mBufferMemoryChunkPool;
  private FlexByteArrayPool mFlexByteArrayPool;
  private @Nullable MemoryChunkPool mNativeMemoryChunkPool;
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
                  mConfig.getBitmapPoolStatsTracker(),
                  mConfig.isIgnoreBitmapPoolHardCap());
          break;
        case BitmapPoolType.LEGACY:
          // fall through
        default:
          if (Build.VERSION.SDK_INT >= 21) {
            mBitmapPool =
                new BucketsBitmapPool(
                    mConfig.getMemoryTrimmableRegistry(),
                    mConfig.getBitmapPoolParams(),
                    mConfig.getBitmapPoolStatsTracker(),
                    mConfig.isIgnoreBitmapPoolHardCap());
          } else {
            mBitmapPool = new DummyBitmapPool();
          }
      }
    }
    return mBitmapPool;
  }

  @Nullable
  public MemoryChunkPool getBufferMemoryChunkPool() {
    if (mBufferMemoryChunkPool == null) {
      try {
        Class<?> clazz = Class.forName("com.facebook.imagepipeline.memory.BufferMemoryChunkPool");
        Constructor<?> cons =
            clazz.getConstructor(
                MemoryTrimmableRegistry.class, PoolParams.class, PoolStatsTracker.class);
        mBufferMemoryChunkPool =
            (MemoryChunkPool)
                cons.newInstance(
                    mConfig.getMemoryTrimmableRegistry(),
                    mConfig.getMemoryChunkPoolParams(),
                    mConfig.getMemoryChunkPoolStatsTracker());
      } catch (ClassNotFoundException e) {
        mBufferMemoryChunkPool = null;
      } catch (IllegalAccessException e) {
        mBufferMemoryChunkPool = null;
      } catch (InstantiationException e) {
        mBufferMemoryChunkPool = null;
      } catch (NoSuchMethodException e) {
        mBufferMemoryChunkPool = null;
      } catch (InvocationTargetException e) {
        mBufferMemoryChunkPool = null;
      }
    }
    return mBufferMemoryChunkPool;
  }

  public FlexByteArrayPool getFlexByteArrayPool() {
    if (mFlexByteArrayPool == null) {
      mFlexByteArrayPool =
          new FlexByteArrayPool(
              mConfig.getMemoryTrimmableRegistry(), mConfig.getFlexByteArrayPoolParams());
    }
    return mFlexByteArrayPool;
  }

  public int getFlexByteArrayPoolMaxNumThreads() {
    return mConfig.getFlexByteArrayPoolParams().maxNumThreads;
  }

  @Nullable
  public MemoryChunkPool getNativeMemoryChunkPool() {
    if (mNativeMemoryChunkPool == null) {
      try {
        Class<?> clazz = Class.forName("com.facebook.imagepipeline.memory.NativeMemoryChunkPool");
        Constructor<?> cons =
            clazz.getConstructor(
                MemoryTrimmableRegistry.class, PoolParams.class, PoolStatsTracker.class);
        mNativeMemoryChunkPool =
            (MemoryChunkPool)
                cons.newInstance(
                    mConfig.getMemoryTrimmableRegistry(),
                    mConfig.getMemoryChunkPoolParams(),
                    mConfig.getMemoryChunkPoolStatsTracker());
      } catch (ClassNotFoundException e) {
        FLog.e("PoolFactory", "", e);
        mNativeMemoryChunkPool = null;
      } catch (IllegalAccessException e) {
        FLog.e("PoolFactory", "", e);
        mNativeMemoryChunkPool = null;
      } catch (InstantiationException e) {
        FLog.e("PoolFactory", "", e);
        mNativeMemoryChunkPool = null;
      } catch (NoSuchMethodException e) {
        FLog.e("PoolFactory", "", e);
        mNativeMemoryChunkPool = null;
      } catch (InvocationTargetException e) {
        FLog.e("PoolFactory", "", e);
        mNativeMemoryChunkPool = null;
      }
    }
    return mNativeMemoryChunkPool;
  }

  @Nullable
  private MemoryChunkPool getAshmemMemoryChunkPool() {
    if (mAshmemMemoryChunkPool == null) {
      try {
        Class<?> clazz = Class.forName("com.facebook.imagepipeline.memory.AshmemMemoryChunkPool");
        Constructor<?> cons =
            clazz.getConstructor(
                MemoryTrimmableRegistry.class, PoolParams.class, PoolStatsTracker.class);
        mAshmemMemoryChunkPool =
            (MemoryChunkPool)
                cons.newInstance(
                    mConfig.getMemoryTrimmableRegistry(),
                    mConfig.getMemoryChunkPoolParams(),
                    mConfig.getMemoryChunkPoolStatsTracker());
      } catch (ClassNotFoundException e) {
        mAshmemMemoryChunkPool = null;
      } catch (IllegalAccessException e) {
        mAshmemMemoryChunkPool = null;
      } catch (InstantiationException e) {
        mAshmemMemoryChunkPool = null;
      } catch (NoSuchMethodException e) {
        mAshmemMemoryChunkPool = null;
      } catch (InvocationTargetException e) {
        mAshmemMemoryChunkPool = null;
      }
    }
    return mAshmemMemoryChunkPool;
  }

  public PooledByteBufferFactory getPooledByteBufferFactory() {
    return getPooledByteBufferFactory(
        NativeCodeSetup.getUseNativeCode() ? NATIVE_MEMORY : BUFFER_MEMORY);
  }

  public PooledByteBufferFactory getPooledByteBufferFactory(@MemoryChunkType int memoryChunkType) {
    if (mPooledByteBufferFactory == null) {
      MemoryChunkPool memoryChunkPool = getMemoryChunkPool(memoryChunkType);
      Preconditions.checkNotNull(
          memoryChunkPool, "failed to get pool for chunk type: " + memoryChunkType);
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
      mSharedByteArray =
          new SharedByteArray(
              mConfig.getMemoryTrimmableRegistry(), mConfig.getFlexByteArrayPoolParams());
    }
    return mSharedByteArray;
  }

  public ByteArrayPool getSmallByteArrayPool() {
    if (mSmallByteArrayPool == null) {
      mSmallByteArrayPool =
          new GenericByteArrayPool(
              mConfig.getMemoryTrimmableRegistry(),
              mConfig.getSmallByteArrayPoolParams(),
              mConfig.getSmallByteArrayPoolStatsTracker());
    }
    return mSmallByteArrayPool;
  }

  @Nullable
  private MemoryChunkPool getMemoryChunkPool(@MemoryChunkType int memoryChunkType) {
    switch (memoryChunkType) {
      case NATIVE_MEMORY:
        return getNativeMemoryChunkPool();
      case BUFFER_MEMORY:
        return getBufferMemoryChunkPool();
      case ASHMEM_MEMORY:
        return getAshmemMemoryChunkPool();
      default:
        throw new IllegalArgumentException("Invalid MemoryChunkType");
    }
  }
}
