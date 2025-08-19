/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.graphics.Bitmap
import android.graphics.Color
import com.facebook.common.internal.Preconditions
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.imageutils.BitmapUtil
import kotlin.math.ceil

/**
 * Manages a pool of bitmaps. This allows us to reuse bitmaps instead of constantly allocating them
 * (and pressuring the Java GC to garbage collect unused bitmaps).
 *
 * The pool supports a get/release paradigm. get() allows for a bitmap in the pool to be reused if
 * it matches the desired dimensions; if no such bitmap is found in the pool, a new one is
 * allocated. release() returns a bitmap to the pool.
 */
class BucketsBitmapPool(
    memoryTrimmableRegistry: MemoryTrimmableRegistry?,
    poolParams: PoolParams?,
    poolStatsTracker: PoolStatsTracker?,
    ignoreHardCap: Boolean,
) :
    BasePool<Bitmap>(memoryTrimmableRegistry!!, poolParams!!, poolStatsTracker!!, ignoreHardCap),
    BitmapPool {
  /**
   * Creates an instance of a bitmap pool.
   *
   * @param memoryTrimmableRegistry the memory manager to register with
   * @param poolParams pool parameters
   */
  init {
    initialize()
  }

  /**
   * Allocate a bitmap that has a backing memory allocation of 'size' bytes. This is configuration
   * agnostic so the size is the actual size in bytes of the bitmap.
   *
   * @param size the 'size' in bytes of the bitmap
   * @return a new bitmap with the specified size in memory
   */
  public override fun alloc(size: Int): Bitmap {
    return Bitmap.createBitmap(
        1,
        ceil(size / BitmapUtil.RGB_565_BYTES_PER_PIXEL.toDouble()).toInt(),
        Bitmap.Config.RGB_565,
    )
  }

  /**
   * Frees the bitmap
   *
   * @param value the bitmap to free
   */
  override fun free(value: Bitmap) {
    value.recycle()
  }

  /**
   * Gets the bucketed size (typically something the same or larger than the requested size)
   *
   * @param requestSize the logical request size
   * @return the 'bucketed' size
   */
  public override fun getBucketedSize(requestSize: Int): Int {
    return requestSize
  }

  /**
   * Gets the bucketed size of the value. We don't check the 'validity' of the value (beyond the
   * not-null check). That's handled in [.isReusable]
   *
   * @param value the value
   * @return bucketed size of the value
   */
  override fun getBucketedSizeForValue(value: Bitmap): Int {
    return value.allocationByteCount
  }

  /**
   * Gets the size in bytes for the given bucketed size
   *
   * @param bucketedSize the bucketed size
   * @return size in bytes
   */
  public override fun getSizeInBytes(bucketedSize: Int): Int {
    return bucketedSize
  }

  /**
   * Determine if this bitmap is reusable (i.e.) if subsequent [.get] requests can use this value.
   * The bitmap is reusable if - it has not already been recycled AND - it is mutable
   *
   * @param value the value to test for reusability
   * @return true, if the bitmap can be reused
   */
  override fun isReusable(value: Bitmap): Boolean {
    Preconditions.checkNotNull(value)
    return !value.isRecycled && value.isMutable
  }

  protected override fun getValue(bucket: Bucket<Bitmap>): Bitmap? {
    val result = super.getValue(bucket)
    result?.eraseColor(Color.TRANSPARENT)
    return result
  }
}
