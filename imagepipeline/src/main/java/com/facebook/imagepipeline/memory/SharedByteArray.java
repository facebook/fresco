/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import javax.annotation.concurrent.ThreadSafe;

import java.util.concurrent.Semaphore;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmable;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.OOMSoftReference;
import com.facebook.common.references.ResourceReleaser;

/**
 * Maintains a shareable reference to a byte array.
 *
 * <p> When accessing the shared array proper synchronization is guaranteed.
 * Under hood the get method acquires an exclusive lock, which is released
 * whenever the returned CloseableReference is closed.
 *
 * <p> If the currently available byte array is too small for a request
 * it is replaced with a bigger one.
 *
 * <p> This class will also release the byte array if it is unused and
 * collecting it can prevent an OOM.
 */
@ThreadSafe
public class SharedByteArray implements MemoryTrimmable {
  @VisibleForTesting
  final int mMinByteArraySize;
  @VisibleForTesting
  final int mMaxByteArraySize;

  /**
   * The underlying byte array.
   *
   * <p> If we receive a memory trim notification, or the runtime runs pre-OOM gc
   * it will be cleared to reduce memory pressure.
   */
  @VisibleForTesting
  final OOMSoftReference<byte[]> mByteArraySoftRef;

  /**
   * Synchronization primitive used by this implementation
   */
  @VisibleForTesting
  final Semaphore mSemaphore;

  private final ResourceReleaser<byte[]> mResourceReleaser;

  public SharedByteArray(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams params) {
    Preconditions.checkNotNull(memoryTrimmableRegistry);
    Preconditions.checkArgument(params.minBucketSize > 0);
    Preconditions.checkArgument(params.maxBucketSize >= params.minBucketSize);

    mMaxByteArraySize = params.maxBucketSize;
    mMinByteArraySize = params.minBucketSize;
    mByteArraySoftRef = new OOMSoftReference<byte[]>();
    mSemaphore = new Semaphore(1);
    mResourceReleaser = new ResourceReleaser<byte[]>() {
      @Override
      public void release(byte[] unused) {
        mSemaphore.release();
      }
    };

    memoryTrimmableRegistry.registerMemoryTrimmable(this);
  }

  /**
   * Get exclusive access to the byte array of size greater or equal to the passed one.
   *
   * <p> Under the hood this method acquires an exclusive lock that is released when
   * the returned reference is closed.
   */
  public CloseableReference<byte[]> get(int size) {
    Preconditions.checkArgument(size > 0, "Size must be greater than zero");
    Preconditions.checkArgument(size <= mMaxByteArraySize, "Requested size is too big");
    mSemaphore.acquireUninterruptibly();
    try {
      byte[] byteArray = getByteArray(size);
      return CloseableReference.of(byteArray, mResourceReleaser);
    } catch (Throwable t) {
      mSemaphore.release();
      throw Throwables.propagate(t);
    }
  }

  private byte[] getByteArray(int requestedSize) {
    final int bucketedSize = getBucketedSize(requestedSize);
    byte[] byteArray = mByteArraySoftRef.get();
    if (byteArray == null || byteArray.length < bucketedSize) {
      byteArray = allocateByteArray(bucketedSize);
    }
    return byteArray;
  }

  /**
   * Responds to memory pressure by simply 'discarding' the local byte array if it is not used
   * at the moment.
   *
   * @param trimType kind of trimming to perform (ignored)
   */
  @Override
  public void trim(MemoryTrimType trimType) {
    if (!mSemaphore.tryAcquire()) {
      return;
    }
    try {
      mByteArraySoftRef.clear();
    } finally {
      mSemaphore.release();
    }
  }

  @VisibleForTesting
  int getBucketedSize(int size) {
    size = Math.max(size, mMinByteArraySize);
    return Integer.highestOneBit(size - 1) * 2;
  }

  private synchronized byte[] allocateByteArray(int size) {
    // Start with clearing reference and releasing currently owned byte array
    mByteArraySoftRef.clear();
    byte[] byteArray = new byte[size];
    mByteArraySoftRef.set(byteArray);
    return byteArray;
  }
}
