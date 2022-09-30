/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import com.facebook.common.memory.ByteArrayPool
import com.facebook.common.memory.MemoryTrimmableRegistry
import javax.annotation.concurrent.ThreadSafe

/**
 * A pool of byte arrays. The pool manages a number of byte arrays of a predefined set of sizes.
 * This set of sizes is typically, but not required to be, based on powers of 2. The pool supports a
 * get/release paradigm. On a get request, the pool attempts to find an existing byte array whose
 * size is at least as big as the requested size. On a release request, the pool adds the byte array
 * to the appropriate bucket. This byte array can then be used for a subsequent get request.
 */
@ThreadSafe
open class GenericByteArrayPool(
    memoryTrimmableRegistry: MemoryTrimmableRegistry,
    poolParams: PoolParams,
    poolStatsTracker: PoolStatsTracker
) : BasePool<ByteArray?>(memoryTrimmableRegistry, poolParams, poolStatsTracker), ByteArrayPool {

  private val bucketSizes: IntArray

  /**
   * Gets the smallest buffer size supported by the pool
   *
   * @return the smallest buffer size supported by the pool
   */
  val minBufferSize: Int
    get() = if (bucketSizes.size > 0) bucketSizes[0] else 0

  /**
   * Creates a new instance of the GenericByteArrayPool class
   *
   * @param memoryTrimmableRegistry the memory manager to register with
   * @param poolParams provider for pool parameters
   * @param poolStatsTracker
   */
  init {
    val bucketSizes = poolParams.bucketSizes
    if (bucketSizes != null) {
      this.bucketSizes = IntArray(bucketSizes.size())
      for (i in 0 until bucketSizes.size()) {
        this.bucketSizes[i] = bucketSizes.keyAt(i)
      }
    } else {
      this.bucketSizes = IntArray(0)
    }
    initialize()
  }

  /**
   * Allocate a buffer greater than or equal to the specified size
   *
   * @param bucketedSize size of the buffer requested
   * @return a byte array of the specified or larger size. Null if the size is invalid
   */
  override fun alloc(bucketedSize: Int): ByteArray = ByteArray(bucketedSize)

  /**
   * Frees the 'value'
   *
   * @param value the value to free
   */
  override fun free(value: ByteArray) {
    checkNotNull(value)
    // do nothing. Let the GC take care of this
  }

  /**
   * Gets the size in bytes for the given 'bucketed' size
   *
   * @param bucketedSize the bucketed size
   * @return size in bytes
   */
  override fun getSizeInBytes(bucketedSize: Int): Int = bucketedSize

  /**
   * Get the 'bucketed' size for the given request size. The 'bucketed' size is a size that is the
   * same or larger than the request size. We walk through our list of pre-defined bucket sizes, and
   * use that to determine the smallest bucket size that is larger than the requested size. If no
   * such 'bucketedSize' is found, then we simply return "requestSize"
   *
   * @param requestSize the logical request size
   * @return the bucketed size
   * @throws InvalidSizeException, if the requested size was invalid
   */
  override fun getBucketedSize(requestSize: Int): Int {
    if (requestSize <= 0) {
      throw InvalidSizeException(requestSize)
    }

    // find the smallest bucketed size that is larger than the requested size
    for (bucketedSize in bucketSizes) {
      if (bucketedSize >= requestSize) {
        return bucketedSize
      }
    }

    // requested size doesn't match our existing buckets - just return the requested size
    // this will eventually translate into a plain alloc/free paradigm
    return requestSize
  }

  /**
   * Gets the bucketed size of the value
   *
   * @param value the value
   * @return just the length of the value
   */
  override fun getBucketedSizeForValue(value: ByteArray): Int {
    checkNotNull(value)
    return value.size
  }
}
