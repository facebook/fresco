/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.filter;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.imageutils.BitmapUtil;

/**
 * Modified midpoint circle algorithm. Clients that look for better performances should use the
 * native implementation of this algorithm in NativeRoundingFilter.
 */
public final class InPlaceRoundFilter {

  private InPlaceRoundFilter() {}

  /**
   * An implementation for rounding a given bitmap to a circular shape. The underlying
   * implementation uses a modified midpoint circle algorithm but instead of drawing a circle, it
   * clears all pixels starting from the circle all the way to the bitmap edges.
   *
   * @param bitmap The input {@link Bitmap}
   */
  public static void roundBitmapInPlace(Bitmap bitmap) {
    Preconditions.checkNotNull(bitmap);
    final int w = bitmap.getWidth();
    final int h = bitmap.getHeight();
    final int radius = Math.min(w, h) / 2;
    final int centerX = w / 2;
    final int centerY = h / 2;
    // Nothing to do if the radius is equal to 0.
    if (radius == 0) {
      return;
    }
    Preconditions.checkArgument(radius >= 1);
    Preconditions.checkArgument(w > 0 && w <= BitmapUtil.MAX_BITMAP_SIZE);
    Preconditions.checkArgument(h > 0 && h <= BitmapUtil.MAX_BITMAP_SIZE);
    Preconditions.checkArgument(centerX > 0 && centerX < w);
    Preconditions.checkArgument(centerY > 0 && centerY < h);

    final int[] pixels = new int[w * h];
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

    int x = radius - 1;
    int y = 0;

    final int maxX = centerX + x;
    final int maxY = centerY + x;
    final int minX = centerX - x;
    final int minY = centerY - x;
    Preconditions.checkArgument(minX >= 0 && minY >= 0 && maxX < w && maxY < h);

    int dx = 1;
    int dy = 1;
    final int rInc = -radius * 2;
    final int[] transparentColor = new int[w];
    int err = dx + rInc;

    int cXpX;
    int cXmX;
    int cXpY;
    int cXmY;

    int cYpX;
    int cYmX;
    int cYpY;
    int cYmY;

    int offA;
    int offB;
    int offC;
    int offD;

    while (x >= y) {
      cXpX = centerX + x;
      cXmX = centerX - x;
      cXpY = centerX + y;
      cXmY = centerX - y;

      cYpX = centerY + x;
      cYmX = centerY - x;
      cYpY = centerY + y;
      cYmY = centerY - y;

      Preconditions.checkArgument(x >= 0 && cXpY < w && cXmY >= 0 && cYpY < h && cYmY >= 0);

      offA = w * cYpY;
      offB = w * cYmY;
      offC = w * cYpX;
      offD = w * cYmX;

      // Clear left
      System.arraycopy(transparentColor, 0, pixels, offA, cXmX);
      System.arraycopy(transparentColor, 0, pixels, offB, cXmX);
      System.arraycopy(transparentColor, 0, pixels, offC, cXmY);
      System.arraycopy(transparentColor, 0, pixels, offD, cXmY);

      // Clear right
      System.arraycopy(transparentColor, 0, pixels, offA + cXpX, w - cXpX);
      System.arraycopy(transparentColor, 0, pixels, offB + cXpX, w - cXpX);
      System.arraycopy(transparentColor, 0, pixels, offC + cXpY, w - cXpY);
      System.arraycopy(transparentColor, 0, pixels, offD + cXpY, w - cXpY);

      if (err <= 0) {
        y++;

        dy += 2;
        err += dy;
      }
      if (err > 0) {
        x--;

        dx += 2;
        err += dx + rInc;
      }
    }

    // Clear top / bottom if height > width
    for (int i = centerY - radius; i >= 0; i--) {
      System.arraycopy(transparentColor, 0, pixels, i * w, w);
    }

    for (int i = centerY + radius; i < h; i++) {
      System.arraycopy(transparentColor, 0, pixels, i * w, w);
    }

    bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
  }
}
