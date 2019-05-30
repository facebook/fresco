/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Sets;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.imageutils.BitmapUtil;
import java.util.Set;

public class DummyTrackingInUseBitmapPool implements BitmapPool {

  /** An Identity hash-set to keep track of values by reference equality */
private final Set<Bitmap> mInUseValues;

  public DummyTrackingInUseBitmapPool() {
    mInUseValues = Sets.newIdentityHashSet();
  }

  @Override
  public void trim(MemoryTrimType trimType) {
    // nop
  }

  @Override
  public Bitmap get(int size) {
    final Bitmap result =
        Bitmap.createBitmap(
            1,
            (int) Math.ceil(size / (double) BitmapUtil.RGB_565_BYTES_PER_PIXEL),
            Bitmap.Config.RGB_565);
    mInUseValues.add(result);
    return result;
  }

  @Override
  public void release(Bitmap value) {
    Preconditions.checkNotNull(value);
    mInUseValues.remove(value);
    value.recycle();
  }
}
