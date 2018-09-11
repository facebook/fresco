/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.ThreadSafe;

public class BitmapCounterProvider {
  private static final Class<?> TAG = BitmapCounterProvider.class;

  private static final long KB = 1024;
  private static final long MB = 1024 * KB;

  /**
   * Our Bitmaps live in ashmem, meaning that they are pinned in Android's shared native memory.
   *
   * <p> Therefore, we are not constrained by the max heap size of the dalvik heap, but we want to
   * make sure we don't use too much memory on low end devices, so that we don't force other
   * background process to be killed.
   */
  public static final int MAX_BITMAP_TOTAL_SIZE = getMaxSizeHardCap();

  private static int sMaxBitmapCount = BitmapCounterConfig.DEFAULT_MAX_BITMAP_COUNT;

  private static volatile BitmapCounter sBitmapCounter;

  private static int getMaxSizeHardCap() {
    final int maxMemory = (int) Math.min(Runtime.getRuntime().maxMemory(), Integer.MAX_VALUE);
    if (maxMemory > 16 * MB) {
      return maxMemory / 4 * 3;
    } else {
      return maxMemory / 2;
    }
  }

  public static void initialize(BitmapCounterConfig bitmapCounterConfig) {
    if (sBitmapCounter != null) {
      throw new IllegalStateException("BitmapCounter has already been created! `BitmapCounterProvider.initialize(...)` should only be called before `BitmapCounterProvider.get()` or not at all!");
    } else {
      sMaxBitmapCount = bitmapCounterConfig.getMaxBitmapCount();
    }
  }

  @ThreadSafe
  public static BitmapCounter get() {
    if (sBitmapCounter == null) {
      synchronized (BitmapCounterProvider.class) {
        if (sBitmapCounter == null) {
          sBitmapCounter = new BitmapCounter(sMaxBitmapCount, MAX_BITMAP_TOTAL_SIZE);
        }
      }
    }
    return sBitmapCounter;
  }
}
