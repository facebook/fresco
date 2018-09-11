/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing;

import static org.mockito.Mockito.*;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.imageutils.BitmapUtil;

/**
 * Helper class for creating bitmap mocks in tests.
 */
public class MockBitmapFactory {
  public static int DEFAULT_BITMAP_WIDTH = 3;
  public static int DEFAULT_BITMAP_HEIGHT = 4;
  public static int DEFAULT_BITMAP_PIXELS = DEFAULT_BITMAP_WIDTH * DEFAULT_BITMAP_HEIGHT;
  public static int DEFAULT_BITMAP_SIZE = bitmapSize(
      DEFAULT_BITMAP_WIDTH,
      DEFAULT_BITMAP_HEIGHT,
      Bitmap.Config.ARGB_8888);

  public static Bitmap create() {
    return create(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
  }

  public static Bitmap createForSize(int size, Bitmap.Config config) {
    Preconditions.checkArgument(size % BitmapUtil.getPixelSizeForBitmapConfig(config) == 0);
    return create(1, size / BitmapUtil.getPixelSizeForBitmapConfig(config), config);
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
    when(bitmap.getRowBytes()).thenReturn(width * BitmapUtil.getPixelSizeForBitmapConfig(config));
    when(bitmap.getByteCount()).thenReturn(bitmapSize(width, height, config));
    return bitmap;
  }

  public static int bitmapSize(int width, int height, Bitmap.Config config) {
    return BitmapUtil.getSizeInByteForBitmap(width, height, config);
  }
}
