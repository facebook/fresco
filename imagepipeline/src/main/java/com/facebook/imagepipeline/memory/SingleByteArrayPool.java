/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.references.OOMSoftReference;


/**
 * A simple class that holds (at-most) one byte-array for use.
 * This buffer is allocated on first use (i.e.) when the {@link #get(int)} operation is called.
 * If the current byte-array is smaller than what's required for the {@link #get(int)}
 * operation, then the current byte-array is discarded and a new byte-array is allocated.
 * <p>
 * NOTE: There can be at most one user of the byte-array returned from this class at any
 * one time. This fits well with DecodeOperation, where we've synchronized the decode
 * call, so that only one decodeXXX call is active at any time. However, if that assumption
 * changes, we will need to change this as well.
 * This is currently enforced by the {@link #mInUse} member variable.
 * <p>
 * This looks a bit like a Pool; however, it is an extremely degenerate case because there can
 * be at most one byte array active at any time.
 */
@ThreadSafe
public class SingleByteArrayPool implements ByteArrayPool {
  private static final Class<?> TAG = SingleByteArrayPool.class;

  @VisibleForTesting final int mMinByteArraySize;
  @VisibleForTesting final int mMaxByteArraySize;
  @VisibleForTesting final SingleByteArrayPoolStatsTracker mSingleByteArrayPoolStatsTracker;

  /**
   * Soft references to the single byte array maintained by the pool.
   */
  @GuardedBy("this")
  @VisibleForTesting
  OOMSoftReference<byte[]> mByteArraySoftRef;

  /**
   * Indicates if the single byte array above is currently in use
   */
  @GuardedBy("this")
  @VisibleForTesting
  boolean mInUse = false;

  /**
   * Creates an instance of the SingleByteArrayPool class, and registers it
   * @param memoryTrimmableRegistry the memory resource manager
   * @param singleByteArrayPoolStatsTracker stats tracker for the pool
   * @param params params for this pool
   */
  public SingleByteArrayPool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams params,
      SingleByteArrayPoolStatsTracker singleByteArrayPoolStatsTracker) {
    this(
        memoryTrimmableRegistry,
        singleByteArrayPoolStatsTracker,
        params.minBucketSize,
        params.maxBucketSize);
  }

  /**
   * Creates an instance of the SingleByteArrayPool class, and registers it
   * @param memoryTrimmableRegistry the memory resource manager
   * @param singleByteArrayPoolStatsTracker stats tracker for the pool
   * @param minByteArraySize size of the smallest byte array we will create
   * @param maxByteArraySize size of the largest byte array we will create
   */
  @VisibleForTesting
  SingleByteArrayPool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      SingleByteArrayPoolStatsTracker singleByteArrayPoolStatsTracker,
      int minByteArraySize,
      int maxByteArraySize) {
    Preconditions.checkNotNull(memoryTrimmableRegistry);
    Preconditions.checkNotNull(singleByteArrayPoolStatsTracker);
    Preconditions.checkState(minByteArraySize > 0 && maxByteArraySize >= minByteArraySize);

    mSingleByteArrayPoolStatsTracker = singleByteArrayPoolStatsTracker;
    mMaxByteArraySize = maxByteArraySize;
    mMinByteArraySize = minByteArraySize;
    mByteArraySoftRef = new OOMSoftReference<byte[]>();

    memoryTrimmableRegistry.registerMemoryTrimmable(this);
  }

  /**
   * Returns a byte array of the desired size.
   * If the locally held byte array {@link #mByteArraySoftRef} is larger than the desired size,
   * then the buffer is returned. Otherwise, a new byte array is allocated, and the current byte
   * array is discarded.
   * NOTE: The byte-array must not be in use already.
   * @param size the size of the byte array that's needed
   * @return a byte array of the desired size
   */
  @Override
  public synchronized byte[] get(int size) {
    Preconditions.checkArgument(size > 0, "Invalid size %s", size);
    Preconditions.checkState(!mInUse, "Byte-array currently in use");

    int bucketedSize = getBucketedSize(size);
    mSingleByteArrayPoolStatsTracker.onBucketedSizeRequested(bucketedSize);
    if (bucketedSize > mMaxByteArraySize) {
      throw new IllegalArgumentException("Size too large: " + size);
    }

    byte[] byteArray = mByteArraySoftRef.get();
    if (byteArray == null || byteArray.length < bucketedSize) {
      bucketedSize = Math.max(bucketedSize, mMinByteArraySize);
      if (FLog.isLoggable(FLog.VERBOSE)) {
        FLog.v(
            TAG,
            "get (alloc) size = %d. Previous buffer size = %d",
            bucketedSize,
            (byteArray == null) ? 0 : byteArray.length);
      }
      if (byteArray != null) {
        byteArray = null;
        clearByteArraySoftRef();
      }
      allocateByteArraySoftRef(bucketedSize);
      byteArray = mByteArraySoftRef.get();
    }
    mInUse = true;
    return byteArray;
  }

  /**
   * 'Releases' the byte array, so another {@link #get(int)} call can use it.
   * If the 'value' parameter is not the same as {@link #mByteArraySoftRef}, we do nothing.
   * Otherwise, we mark {@link #mInUse} as false, and return true.
   */
  @Override
  public synchronized void release(byte[] value) {
    Preconditions.checkNotNull(value);
    // if this value is not the
    if (mByteArraySoftRef.get() == value) {
      mInUse = false;
    }
  }

  /**
   * Responds to memory pressure by simply 'discarding' the local byte array
   * @param trimType kind of trimming to perform (ignored)
   */
  @Override
  public synchronized void trim(MemoryTrimType trimType) {
    if (!mInUse && mByteArraySoftRef.get() != null) {
      if (FLog.isLoggable(FLog.VERBOSE)) {
        FLog.v(TAG, "Discarding existing buffer of size %d", mByteArraySoftRef.get().length);
      }
      mSingleByteArrayPoolStatsTracker.onMemoryTrimmed(mByteArraySoftRef.get().length);
      clearByteArraySoftRef();
    }
  }

  /**
   * Gets the 'bucketed' size - the nearest power of 2 that's larger than or equal to 'size'
   * @param size the requested size
   * @return the 'bucketed' size that's greater than or equal to size
   */
  @VisibleForTesting
  int getBucketedSize(int size) {
    int powerOfTwo = Integer.highestOneBit(size);
    return (size > powerOfTwo) ? powerOfTwo * 2 : powerOfTwo;
  }

  /**
   * Allocates byte array and sets soft references to it
   */
  private synchronized void allocateByteArraySoftRef(int bucketedSize) {
    byte[] byteArray = new byte[bucketedSize];
    mByteArraySoftRef.set(byteArray);
    mSingleByteArrayPoolStatsTracker.onMemoryAlloc(bucketedSize);
  }

  /**
   * Clear byte array and the soft references to it
   */
  private synchronized void clearByteArraySoftRef() {
    mByteArraySoftRef.clear();
  }
}
