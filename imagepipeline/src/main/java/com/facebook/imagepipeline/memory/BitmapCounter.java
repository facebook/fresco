/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import android.graphics.Bitmap;
import android.os.Build;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.common.TooManyBitmapsException;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imageutils.BitmapUtil;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;

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
  private final ResourceReleaser<Bitmap> mUnpooledBitmapsReleaser;

  public BitmapCounter(int maxCount, int maxSize) {
    Preconditions.checkArgument(maxCount > 0);
    Preconditions.checkArgument(maxSize > 0);
    mMaxCount = maxCount;
    mMaxSize = maxSize;
    mUnpooledBitmapsReleaser = new ResourceReleaser<Bitmap>() {
      @Override
      public void release(Bitmap value) {
        try {
          decrease(value);
        } finally {
          value.recycle();
        }
      }
    };
  }

  /**
   * Includes given bitmap in the bitmap count. The bitmap is included only if doing so
   * does not violate configured limit
   *
   * @param bitmap to include in the count
   * @return true if and only if bitmap is successfully included in the count
   */
  public synchronized boolean increase(Bitmap bitmap) {
    final int bitmapSize = BitmapUtil.getSizeInBytes(bitmap);
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
    final int bitmapSize = BitmapUtil.getSizeInBytes(bitmap);
    Preconditions.checkArgument(mCount > 0, "No bitmaps registered.");
    Preconditions.checkArgument(
        bitmapSize <= mSize,
        "Bitmap size bigger than the total registered size: %d, %d",
        bitmapSize,
        mSize);
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

  public synchronized int getMaxCount() {
    return mMaxCount;
  }

  public synchronized int getMaxSize() {
    return mMaxSize;
  }

  public ResourceReleaser<Bitmap> getReleaser() {
    return mUnpooledBitmapsReleaser;
  }
}
