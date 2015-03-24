/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.objectpool;

import java.lang.reflect.Array;

import com.facebook.common.logging.FLog;
import com.facebook.common.time.MonotonicClock;

/**
 * Generic object pool implementation for arbitrary types.  Provides an interface for performing
 * actions when allocating from / releasing to the pool, as well as a simple compaction strategy
 * based upon the last time we needed the pool to be as large as it is.
 * @param <T> type of the object to pool
 */
public class ObjectPool<T> {

  private static final Class<?> TAG = ObjectPool.class;

  private final Class<T> mClazz;
  // minimum and maximum size for the pool
  private final int mMinSize;
  private final int mMaxSize;

  // increment size the pool size is increased by when there is not enough space
  private final int mIncrementSize;
  private final Allocator<T> mAllocator;

  private final MonotonicClock mClock;

  // amount of time in ms since the last 'low capacity' event before we reduce the size of the pool
  private final long mCompactionDelayMs;

  // The last time we had low supply (denoted as less than 2x the increment size in spare capacity)
  private long mLastLowSupplyTimeMs;

  private T[] mPool;
  private int mSize;

  /**
   * Generic allocator interface for the pool.  Encapsulates the per-object logic needed to
   * instantiate, destroy, and do operations when an object is allocated from / returned to the pool
   * The call sequence for a given object will look like:
   * obj1 = Allocator.create()
   * [
   *   Allocator.onAllocate(obj1)
   *   Allocator.onRelease(obj1)
   * ] (zero or more times)
   * Allocator.onDestroy(obj1) // if the object is freed from the pool
   * @param <T>
   */
  public interface Allocator<T> {

    /**
     * Create and return a new instance of T
     * @return new instance of T suitable for pooling
     */
    public T create();

    /**
     * Handler for when an instance of T is allocated from the pool
     * @param obj instance of T that is being allocated
     */
    public void onAllocate(T obj);

    /**
     * Handler for when an instance of T is returned to the pool
     * @param obj instance of T that is being returned
     */
    public void onRelease(T obj);

  }

  /**
   * Basic implementation of an Allocator.  Uses new to create the object instance
   * @param <T>
   */
  public static class BasicAllocator<T> implements Allocator<T> {

    Class<T> mClazz;

    public BasicAllocator(Class<T> clazz) {
      mClazz = clazz;
    }

    @Override
    public T create() {
      try {
        return mClazz.newInstance();
      } catch (InstantiationException e) {
        FLog.e(TAG, "Couldn't instantiate object", e);
      } catch (IllegalAccessException e) {
        FLog.e(TAG, "Couldn't instantiate object", e);
      }
      return null;
    }

    @Override
    public void onAllocate(T obj) {
    }

    @Override
    public void onRelease(T obj) {
    }
  }

  @SuppressWarnings("unchecked")
  public ObjectPool(Class<T> clazz, int minSize, int maxSize, int incrementSize,
      long compactionDelay, Allocator<T> alloc, MonotonicClock clock) {
    mClazz = clazz;
    mMinSize = Math.max(minSize, 0);
    mMaxSize = Math.max(mMinSize, maxSize);
    mIncrementSize = Math.max(incrementSize, 1);
    mCompactionDelayMs = compactionDelay;
    mAllocator = alloc;
    mClock = clock;
    mPool = (T[]) Array.newInstance(mClazz, mMinSize);
  }

  /**
   * Return a free instance of T for use.  Tries to return an already allocated instance, or creates
   * a new one if none exist
   * @return an instance of T for use
   */
  public synchronized T allocate() {
    T obj;
    if (mSize > 0) {
      --mSize;
      obj = mPool[mSize];
      mPool[mSize] = null;
    } else {
      obj = mAllocator.create();
    }
    mAllocator.onAllocate(obj);
    return obj;
  }

  /**
   * Return a previously allocated object back to the pool
   * @param obj object to return to the pool
   */
  public synchronized void release(T obj) {
    // Check usage at the beginning of release since it represents the low point
    checkUsage();

    mAllocator.onRelease(obj);
    if (mSize < mMaxSize) {
      if (mSize + 1 > mPool.length) {
        resizePool(Math.min(mMaxSize, mPool.length + mIncrementSize));
      }
      mPool[mSize++] = obj;
    }
  }

  /**
   * Checks whether the compaction policies set for this pool have been satisfied
   */
  public synchronized void checkUsage() {
    final long now = mClock.now();

    // this check prevents compaction from occurring by setting the last timestamp
    // to right now when the size is less than 2x the increment size (default
    // ObjectPoolBuilder.DEFAULT_INCREMENT_SIZE).
    if (mSize < 2*mIncrementSize) {
      mLastLowSupplyTimeMs = now;
    }

    if (now - mLastLowSupplyTimeMs > mCompactionDelayMs) {
      FLog.d(TAG, "ObjectPool.checkUsage is compacting the pool.");
      compactUsage();
    }
  }

  /**
   * Forcibly compacts the pool by the set increment size, regardless of the compaction policy.
   */
  public synchronized void compactUsage() {
    int newSize = Math.max(mPool.length - mIncrementSize, mMinSize);
    if (newSize != mPool.length) {
      resizePool(newSize);
    }
  }

  /**
   * Get the number of free objects currently in the pool.
   * @return the number of free objects
   */
  public int getPooledObjectCount() {
    return mSize;
  }

  public int getMinimumSize() {
    return mMinSize;
  }

  public int getMaximumSize() {
    return mMaxSize;
  }

  public int getIncrementSize() {
    return mIncrementSize;
  }

  public long getCompactionDelayMs() {
    return mCompactionDelayMs;
  }

  /**
   * Get the total size of the array backing the pool.  This will always be >= getPooledObjectCount
   * @return the pool size
   */
  public int getPoolSize() {
    return mPool.length;
  }

  @SuppressWarnings("unchecked")
  private void resizePool(int newSize) {
    T[] newArr = (T[]) Array.newInstance(mClazz, newSize);
    System.arraycopy(mPool, 0, newArr, 0, Math.min(mPool.length, newSize));
    mPool = newArr;
    mSize = Math.min(mSize, newSize);
  }
}
