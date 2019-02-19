/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.memory;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.facebook.common.logging.FLog;
import com.facebook.imageutils.BitmapUtil;
import javax.annotation.Nullable;

public class BitmapPoolBackend extends LruBucketsPoolBackend<Bitmap> {

  private static final String TAG = "BitmapPoolBackend";

  @Override
  public void put(Bitmap bitmap) {
    if (isReusable(bitmap)) {
      super.put(bitmap);
    }
  }

  @Nullable
  @Override
  public Bitmap get(int size) {
    Bitmap bitmap = super.get(size);
    if (bitmap != null && isReusable(bitmap)) {
      bitmap.eraseColor(Color.TRANSPARENT);
      return bitmap;
    }
    return null;
  }

  @Override
  public int getSize(Bitmap bitmap) {
    return BitmapUtil.getSizeInBytes(bitmap);
  }

  protected boolean isReusable(@Nullable Bitmap bitmap) {
    if (bitmap == null) {
      return false;
    }
    if (bitmap.isRecycled()) {
      FLog.wtf(TAG, "Cannot reuse a recycled bitmap: %s", bitmap);
      return false;
    }
    if (!bitmap.isMutable()) {
      FLog.wtf(TAG, "Cannot reuse an immutable bitmap: %s", bitmap);
      return false;
    }
    return true;
  }
}
