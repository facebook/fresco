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

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.os.Build;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.common.TooManyBitmapsException;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imageutils.BitmapUtil;

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

  public ResourceReleaser<Bitmap> getReleaser() {
    return mUnpooledBitmapsReleaser;
  }

  /**
   * Associates bitmaps with the bitmap counter. <p/> <p>If this method throws
   * TooManyBitmapsException, the code will have called {@link Bitmap#recycle} on the
   * bitmaps.</p>
   *
   * @param bitmaps the bitmaps to associate
   * @return the references to the bitmaps that are now tied to the bitmap pool
   * @throws TooManyBitmapsException if the pool is full
   */
  public List<CloseableReference<Bitmap>> associateBitmapsWithBitmapCounter(
      final List<Bitmap> bitmaps) {
    int countedBitmaps = 0;
    try {
      for (; countedBitmaps < bitmaps.size(); ++countedBitmaps) {
        final Bitmap bitmap = bitmaps.get(countedBitmaps);
        // 'Pin' the bytes of the purgeable bitmap, so it is now not purgeable
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
          Bitmaps.pinBitmap(bitmap);
        }
        if (!increase(bitmap)) {
          throw new TooManyBitmapsException();
        }
      }
      List<CloseableReference<Bitmap>> ret = new ArrayList<>();
      for (Bitmap bitmap : bitmaps) {
        ret.add(CloseableReference.of(bitmap, mUnpooledBitmapsReleaser));
      }
      return ret;
    } catch (Exception exception) {
      if (bitmaps != null) {
        for (Bitmap bitmap : bitmaps) {
          if (countedBitmaps-- > 0) {
            decrease(bitmap);
          }
          bitmap.recycle();
        }
      }
      throw Throwables.propagate(exception);
    }
  }
}
