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

import android.annotation.TargetApi;
import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.imagepipeline.nativecode.Bitmaps;

/**
 * Manages a pool of bitmaps. This allows us to reuse bitmaps instead of constantly allocating
 * them (and pressuring the Java GC to garbage collect unused bitmaps).
 * <p>
 * The pool supports a get/release paradigm.
 * get() allows for a bitmap in the pool to be reused if it matches the desired
 * dimensions; if no such bitmap is found in the pool, a new one is allocated.
 * release() returns a bitmap to the pool.
 */
@ThreadSafe
@TargetApi(21)
public class BitmapPool extends BasePool<Bitmap> {

  /**
   * Creates an instance of a bitmap pool.
   * @param memoryTrimmableRegistry the memory manager to register with
   * @param poolParams pool parameters
   */
  public BitmapPool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams poolParams,
      PoolStatsTracker poolStatsTracker) {
    super(memoryTrimmableRegistry, poolParams, poolStatsTracker);
    initialize();
  }

  /**
   * Allocate a bitmap with the specified width and height.
   * The bitmap's config is controlled by the BITMAP_CONFIG we've defined above.
   * @param size the 'size' of the bitmap
   * @return a new bitmap with the specified dimensions
   */
  @Override
  protected Bitmap alloc(int size) {
    return Bitmap.createBitmap(1, size, Bitmaps.BITMAP_CONFIG);
  }

  /**
   * Frees the bitmap
   * @param value the bitmap to free
   */
  @Override
  protected void free(Bitmap value) {
    Preconditions.checkNotNull(value);
    value.recycle();
  }

  /**
   * Gets the bucketed size (typically something the same or larger than the requested size)
   * @param requestSize the logical request size
   * @return the 'bucketed' size
   */
  @Override
  protected int getBucketedSize(int requestSize) {
    return requestSize;
  }

  /**
   * Gets the bucketed size of the value.
   * We don't check the 'validity' of the value (beyond the not-null check). That's handled
   * in {@link #isReusable(Bitmap)}
   * @param value the value
   * @return bucketed size of the value
   */
  @Override
  protected int getBucketedSizeForValue(Bitmap value) {
    Preconditions.checkNotNull(value);
    final int allocationByteCount = value.getAllocationByteCount();
    return allocationByteCount / Bitmaps.BYTES_PER_PIXEL;
  }

  /**
   * Gets the size in bytes for the given bucketed size
   * This will use the BYTES_PER_PIXEL constant defined above (which is dependent on the specific
   * BITMAP_CONFIG above)
   * @param bucketedSize the bucketed size
   * @return size in bytes
   */
  @Override
  protected int getSizeInBytes(int bucketedSize) {
    return Bitmaps.BYTES_PER_PIXEL * bucketedSize;
  }

  /**
   * Determine if this bitmap is reusable (i.e.) if subsequent {@link #get(int)} requests can
   * use this value.
   * The bitmap is reusable if
   *  - it has not already been recycled AND
   *  - it is mutable AND
   *  - it has the desired bitmap-config
   * @param value the value to test for reusability
   * @return true, if the bitmap can be reused
   */
  @Override
  protected boolean isReusable(Bitmap value) {
    Preconditions.checkNotNull(value);
    return !value.isRecycled() &&
        value.isMutable() &&
        Bitmaps.BITMAP_CONFIG.equals(value.getConfig());
  }
}
