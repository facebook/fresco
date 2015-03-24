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

import android.util.SparseIntArray;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.MemoryTrimmableRegistry;

/**
 * A pool of byte arrays.
 * The pool manages a number of byte arrays of a predefined set of sizes. This set of sizes is
 * typically, but not required to be, based on powers of 2.
 * The pool supports a get/release paradigm.
 * On a get request, the pool attempts to find an existing byte array whose size
 * is at least as big as the requested size.
 * On a release request, the pool adds the byte array to the appropriate bucket.
 * This byte array can then be used for a subsequent get request.
 */
@ThreadSafe
public class GenericByteArrayPool extends BasePool<byte[]> implements ByteArrayPool {
  private final int[] mBucketSizes;

  /**
   * Creates a new instance of the GenericByteArrayPool class
   * @param memoryTrimmableRegistry the memory manager to register with
   * @param poolParams provider for pool parameters
   * @param poolStatsTracker
   */
  public GenericByteArrayPool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams poolParams,
      PoolStatsTracker poolStatsTracker) {
    super(memoryTrimmableRegistry, poolParams, poolStatsTracker);
    final SparseIntArray bucketSizes = poolParams.bucketSizes;
    mBucketSizes = new int[bucketSizes.size()];
    for (int i = 0; i < bucketSizes.size(); ++i) {
      mBucketSizes[i] = bucketSizes.keyAt(i);
    }
    initialize();
  }

  /**
   * Gets the smallest buffer size supported by the pool
   * @return the smallest buffer size supported by the pool
   */
  public int getMinBufferSize() {
    return mBucketSizes[0];
  }

  /**
   * Allocate a buffer greater than or equal to the specified size
   * @param bucketedSize size of the buffer requested
   * @return a byte array of the specified or larger size. Null if the size is invalid
   */
  @Override
  protected byte[] alloc(int bucketedSize) {
    return new byte[bucketedSize];
  }

  /**
   * Frees the 'value'
   * @param value the value to free
   */
  @Override
  protected void free(byte[] value) {
    Preconditions.checkNotNull(value);
    // do nothing. Let the GC take care of this
  }

  /**
   * Gets the size in bytes for the given 'bucketed' size
   * @param bucketedSize the bucketed size
   * @return size in bytes
   */
  @Override
  protected int getSizeInBytes(int bucketedSize) {
    return bucketedSize;
  }

  /**
   * Get the 'bucketed' size for the given request size. The 'bucketed' size is a size that is
   * the same or larger than the request size. We walk through our list of pre-defined bucket
   * sizes, and use that to determine the smallest bucket size that is larger than the requested
   * size.
   * If no such 'bucketedSize' is found, then we simply return "requestSize"
   * @param requestSize the logical request size
   * @return the bucketed size
   * @throws InvalidSizeException, if the requested size was invalid
   */
  @Override
  protected int getBucketedSize(int requestSize) {
    int intRequestSize = requestSize;
    if (intRequestSize <= 0) {
      throw new InvalidSizeException(requestSize);
    }

    // find the smallest bucketed size that is larger than the requested size
    for (int bucketedSize : mBucketSizes) {
      if (bucketedSize >= intRequestSize) {
        return bucketedSize;
      }
    }

    // requested size doesn't match our existing buckets - just return the requested size
    // this will eventually translate into a plain alloc/free paradigm
    return requestSize;
  }

  /**
   * Gets the bucketed size of the value
   * @param value the value
   * @return just the length of the value
   */
  @Override
  protected int getBucketedSizeForValue(byte[] value) {
    Preconditions.checkNotNull(value);
    return value.length;
  }
}

