/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.objectpool;

import javax.annotation.Nullable;

import com.facebook.common.time.MonotonicClock;

/**
 * Builder interface for constructing a new pool
 * @param <T>
 */
public class ObjectPoolBuilder<T> {
  private Class<T> mClazz;
  private int mMinSize = DEFAULT_MIN_SIZE;
  private int mMaxSize = DEFAULT_MAX_SIZE;
  private int mIncrementSize = DEFAULT_INCREMENT_SIZE;
  private long mCompactionDelayMs = DEFAULT_COMPACTION_DELAY_MS;
  private ObjectPool.Allocator<T> mAllocator;
  private MonotonicClock mClock;
  private final ObjectPoolManager mManager;

  public static final int DEFAULT_MIN_SIZE = 16;
  public static final int DEFAULT_MAX_SIZE = 1024;
  public static final int DEFAULT_INCREMENT_SIZE = 16;
  public static final long DEFAULT_COMPACTION_DELAY_MS = 60 * 1000;

  // Generic constructor which does not attach to any specific manager
  public ObjectPoolBuilder(Class<T> clazz, MonotonicClock clock) {
    this(null, clazz, clock);
  }

  // Constructor which will attach the built pool to the specified pool manager
  /* package */ ObjectPoolBuilder(
      @Nullable ObjectPoolManager manager,
      Class<T> clazz,
      MonotonicClock clock) {
    mManager = manager;
    mClazz = clazz;
    mClock = clock;
  }

  public ObjectPoolBuilder<T> setMinimumSize(int size) {
    mMinSize = size;
    return this;
  }

  public int getMinimumSize() {
    return mMinSize;
  }

  public ObjectPoolBuilder<T> setMaximumSize(int size) {
    mMaxSize = size;
    return this;
  }

  public int getMaximumSize() {
    return mMaxSize;
  }

  public ObjectPoolBuilder<T> setIncrementSize(int size) {
    mIncrementSize = size;
    return this;
  }

  public int getIncrementSize() {
    return mIncrementSize;
  }

  public ObjectPoolBuilder<T> setCompactionDelay(long delayMs) {
    mCompactionDelayMs = delayMs;
    return this;
  }

  public long getCompactionDelay() {
    return mCompactionDelayMs;
  }

  public ObjectPoolBuilder<T> setAllocator(ObjectPool.Allocator<T> alloc) {
    mAllocator = alloc;
    return this;
  }

  public ObjectPool.Allocator<T> getAllocator() {
    return mAllocator;
  }

  public ObjectPoolBuilder<T> setClock(MonotonicClock clock) {
    mClock = clock;
    return this;
  }

  public MonotonicClock getClock() {
    return mClock;
  }

  public ObjectPool<T> build() {
    if (mClock == null) {
      throw new IllegalArgumentException("Must add a clock to the object pool builder");
    }
    ObjectPool.Allocator<T> alloc = mAllocator;
    if (alloc == null) {
      alloc = new ObjectPool.BasicAllocator<T>(mClazz);
    }
    ObjectPool<T> pool = new ObjectPool<T>(mClazz, mMinSize, mMaxSize, mIncrementSize,
        mCompactionDelayMs, alloc, mClock);
    if (mManager != null) {
      mManager.addPool(mClazz, pool);
    }
    return pool;
  }
}
