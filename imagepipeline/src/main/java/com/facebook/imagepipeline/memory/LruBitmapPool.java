/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import android.graphics.Bitmap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.MemoryTrimType;

public class LruBitmapPool implements BitmapPool {

  protected final PoolBackend<Bitmap> mStrategy = new BitmapPoolBackend();
  private final int mMaxSize;
  private final PoolStatsTracker mPoolStatsTracker;
  private int mCurrentSize;

  public LruBitmapPool(int maxSize, PoolStatsTracker poolStatsTracker) {
    mMaxSize = maxSize;
    mPoolStatsTracker = poolStatsTracker;
  }

  @Override
  public void trim(MemoryTrimType trimType) {
    trimTo((int) (mMaxSize * (1f - trimType.getSuggestedTrimRatio())));
  }

  private synchronized void trimTo(int maxSize) {
    while (mCurrentSize > maxSize) {
      Bitmap removed = mStrategy.pop();
      if (removed == null) {
        break;
      } else {
        final int size = mStrategy.getSize(removed);
        mCurrentSize -= size;
        mPoolStatsTracker.onFree(size);
      }
    }
  }

  @Override
  public synchronized Bitmap get(int size) {
    Bitmap cached = mStrategy.get(size);
    if (cached != null) {
      final int reusedSize = mStrategy.getSize(cached);
      mCurrentSize -= reusedSize;
      mPoolStatsTracker.onValueReuse(reusedSize);
      return cached;
    }
    return alloc(size);
  }

  @VisibleForTesting
  private Bitmap alloc(int size) {
    mPoolStatsTracker.onAlloc(size);
    return Bitmap.createBitmap(1, size, Bitmap.Config.ALPHA_8);
  }

  @Override
  public synchronized void release(Bitmap value) {
    final int size = mStrategy.getSize(value);
    mPoolStatsTracker.onValueRelease(size);
    mStrategy.put(value);
    mCurrentSize += size;
    if (mCurrentSize > mMaxSize) {
      trimTo(mMaxSize);
    }
  }
}
