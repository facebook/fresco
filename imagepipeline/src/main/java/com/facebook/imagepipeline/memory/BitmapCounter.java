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

import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;

/**
 * Counts bitmaps - keeps track of both, count and total size in bytes.
 */
public class BitmapCounter {

  @GuardedBy("this")
  private int mCount;

  @GuardedBy("this")
  private long mSize;

  private final int mMaxCount;
  private final int mMaxSize;

  public BitmapCounter(int maxCount, int maxSize) {
    Preconditions.checkArgument(maxCount > 0);
    Preconditions.checkArgument(maxSize > 0);
    mMaxCount = maxCount;
    mMaxSize = maxSize;
  }

  /**
   * Includes given bitmap in the bitmap count. The bitmap is included only if doing so does not
   * violate configured limit
   *
   * @param bitmap to include in the count
   * @return true if and only if bitmap is successfully included in the count
   */
  public synchronized boolean increase(Bitmap bitmap) {
    final int bitmapSize = getBitmapSize(bitmap);
    if (mCount >= mMaxCount || mSize + bitmapSize > mMaxSize) {
      return false;
    }

    mCount++;
    mSize += bitmapSize;
    return true;
  }

  /**
   * Excludes given bitmap from the count.
   *
   * @param bitmap to be excluded from the count
   */
  public synchronized void decrease(Bitmap bitmap) {
    final int bitmapSize = getBitmapSize(bitmap);
    Preconditions.checkArgument(bitmapSize <= mSize);
    Preconditions.checkArgument(mCount > 0);

    mSize -= bitmapSize;
    mCount--;
  }

  /**
   * @return number of counted bitmaps
   */
  public synchronized int getCount() {
    return mCount;
  }

  /**
   * @return total size in bytes of counted bitmaps
   */
  public synchronized long getSize() {
    return mSize;
  }

  public static int getBitmapSize(Bitmap bitmap) {
    Preconditions.checkNotNull(bitmap);
    return bitmap.getRowBytes() * bitmap.getHeight();
  }
}
