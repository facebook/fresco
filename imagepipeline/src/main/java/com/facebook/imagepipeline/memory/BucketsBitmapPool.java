/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.imageutils.BitmapUtil;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

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
public class BucketsBitmapPool extends BasePool<Bitmap> implements BitmapPool {

  /**
   * Creates an instance of a bitmap pool.
   * @param memoryTrimmableRegistry the memory manager to register with
   * @param poolParams pool parameters
   */
  public BucketsBitmapPool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams poolParams,
      PoolStatsTracker poolStatsTracker) {
    super(memoryTrimmableRegistry, poolParams, poolStatsTracker);
    initialize();
  }

  /**
   * Allocate a bitmap that has a backing memory allocation of 'size' bytes.
   * This is configuration agnostic so the size is the actual size in bytes of the bitmap.
   * @param size the 'size' in bytes of the bitmap
   * @return a new bitmap with the specified size in memory
   */
  @Override
  protected Bitmap alloc(int size) {
    return Bitmap.createBitmap(
        1,
        (int) Math.ceil(size / (double) BitmapUtil.RGB_565_BYTES_PER_PIXEL),
        Bitmap.Config.RGB_565);
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
    return value.getAllocationByteCount();
  }

  /**
   * Gets the size in bytes for the given bucketed size
   * @param bucketedSize the bucketed size
   * @return size in bytes
   */
  @Override
  protected int getSizeInBytes(int bucketedSize) {
    return  bucketedSize;
  }

  /**
   * Determine if this bitmap is reusable (i.e.) if subsequent {@link #get(int)} requests can
   * use this value.
   * The bitmap is reusable if
   *  - it has not already been recycled AND
   *  - it is mutable
   * @param value the value to test for reusability
   * @return true, if the bitmap can be reused
   */
  @Override
  protected boolean isReusable(Bitmap value) {
    Preconditions.checkNotNull(value);
    return !value.isRecycled() &&
        value.isMutable();
  }

  @Nullable
  @Override
  protected Bitmap getValue(Bucket<Bitmap> bucket) {
    Bitmap result = super.getValue(bucket);
    if (result != null) {
      result.eraseColor(Color.TRANSPARENT);
    }
    return result;
  }
}
