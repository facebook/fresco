/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.memory;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.Nullable;
import com.facebook.imageutils.BitmapUtil;

public class BitmapPoolBackend extends LruBucketsPoolBackend<Bitmap> {
  @Nullable
  @Override
  public Bitmap get(int size) {
    Bitmap bitmap = super.get(size);
    if (bitmap != null) {
      bitmap.eraseColor(Color.TRANSPARENT);
    }
    return bitmap;
  }

  @Override
  public int getSize(Bitmap bitmap) {
    return BitmapUtil.getSizeInBytes(bitmap);
  }
}
