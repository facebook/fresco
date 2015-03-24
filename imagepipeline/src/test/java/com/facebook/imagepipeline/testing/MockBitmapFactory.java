/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.testing;

import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.nativecode.Bitmaps;

import static org.mockito.Mockito.*;

/**
 * Helper class for creating bitmap mocks in tests.
 */
public class MockBitmapFactory {
  public static int DEFAULT_BITMAP_WIDTH = 3;
  public static int DEFAULT_BITMAP_HEIGHT = 4;
  public static int DEFAULT_BITMAP_PIXELS = DEFAULT_BITMAP_WIDTH * DEFAULT_BITMAP_HEIGHT;
  public static int DEFAULT_BITMAP_SIZE = bitmapSize(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT);

  public static Bitmap create() {
    return create(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT);
  }

  public static Bitmap create(int width, int height) {
    return create(width, height, Bitmaps.BITMAP_CONFIG);
  }

  public static Bitmap createForSize(int size) {
    Preconditions.checkArgument(size % Bitmaps.BYTES_PER_PIXEL == 0);
    return create(1, size / Bitmaps.BYTES_PER_PIXEL, Bitmaps.BITMAP_CONFIG);
  }

  public static Bitmap create(int width, int height, Bitmap.Config config) {
    Preconditions.checkArgument(width > 0);
    Preconditions.checkArgument(height > 0);
    Preconditions.checkNotNull(config);
    Bitmap bitmap = mock(Bitmap.class);
    when(bitmap.getWidth()).thenReturn(width);
    when(bitmap.getHeight()).thenReturn(height);
    when(bitmap.getConfig()).thenReturn(config);
    when(bitmap.isMutable()).thenReturn(true);
    when(bitmap.getRowBytes()).thenReturn(width * Bitmaps.BYTES_PER_PIXEL);
    when(bitmap.getByteCount()).thenReturn(bitmapSize(width, height));
    return bitmap;
  }

  public static int bitmapSize(int width, int height) {
    return width * height * Bitmaps.BYTES_PER_PIXEL;
  }
}
