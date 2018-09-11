/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.filter;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.imageutils.BitmapUtil;
import java.util.Locale;

public abstract class IterativeBoxBlurFilter {
  private static final String TAG = "IterativeBoxBlurFilter";

  /**
   * An in-place iterative box blur algorithm that runs faster than a traditional box blur.
   *
   * <p>The individual box blurs are split up in vertical and horizontal direction. That allows us
   * to use a moving average implementation for blurring individual rows and columns.
   *
   * <p>The runtime is: O(iterations * width * height) and therefore linear in the number of pixels
   *
   * <p>The required memory is: 2 * radius * 256 * 4 Bytes + max(width, height) * 4 Bytes + width *
   * height * 4 Bytes (+constant)
   *
   * @param bitmap The {@link Bitmap} containing the image. The bitmap dimension need to be smaller
   *     than {@link BitmapUtil#MAX_BITMAP_SIZE}
   * @param iterations The number of iterations of the blurring algorithm > 0.
   * @param radius The radius of the blur with a supported range 0 < radius <= {@link
   *     RenderScriptBlurFilter#BLUR_MAX_RADIUS}
   */
  public static void boxBlurBitmapInPlace(
      final Bitmap bitmap, final int iterations, final int radius) {
    Preconditions.checkNotNull(bitmap);
    Preconditions.checkArgument(bitmap.isMutable());
    Preconditions.checkArgument(bitmap.getHeight() <= BitmapUtil.MAX_BITMAP_SIZE);
    Preconditions.checkArgument(bitmap.getWidth() <= BitmapUtil.MAX_BITMAP_SIZE);
    Preconditions.checkArgument(radius > 0 && radius <= RenderScriptBlurFilter.BLUR_MAX_RADIUS);
    Preconditions.checkArgument(iterations > 0);
    try {
      fastBoxBlur(bitmap, iterations, radius);
    } catch (OutOfMemoryError oom) {
      FLog.e(
          TAG,
          String.format(
              (Locale) null,
              "OOM: %d iterations on %dx%d with %d radius",
              iterations,
              bitmap.getWidth(),
              bitmap.getHeight(),
              radius));
      throw oom;
    }
  }

  private static void fastBoxBlur(final Bitmap bitmap, final int iterations, final int radius) {
    final int w = bitmap.getWidth();
    final int h = bitmap.getHeight();
    final int[] pixels = new int[w * h];
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

    // The information written to an output pixels `x` are from `[x-radius, x+radius]` (inclusive)
    final int diameter = radius + 1 + radius;
    // Pre-compute division table: speed-up by factor 5(!)
    final int[] div = new int[256 * diameter];

    // The following lines will fill-up at least the first `255 * diameter` entries with the mapping
    // `div[x] = (x + r) / d` (i.e. division of x by d rounded to the nearest number).
    int ptr = radius + 1;
    for (int b = 1; b <= 255; b++) {
      for (int d = 0; d < diameter; d++) {
        div[ptr] = b;
        ptr++;
      }
    }

    final int[] tempRowOrColumn = new int[Math.max(w, h)];

    for (int i = 0; i < iterations; i++) {
      // Blur rows one-by-one
      for (int row = 0; row < h; row++) {
        internalHorizontalBlur(pixels, tempRowOrColumn, w, row, diameter, div);

        System.arraycopy(tempRowOrColumn, 0, pixels, row * w, w);
      }

      // Blur columns one-by-one
      for (int col = 0; col < w; col++) {
        internalVerticalBlur(pixels, tempRowOrColumn, w, h, col, diameter, div);

        int pos = col;
        for (int row = 0; row < h; row++) {
          pixels[pos] = tempRowOrColumn[row];
          pos += w;
        }
      }
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
  }

  /**
   * Creates a blurred version of the given {@code row} of {@code pixel}. It uses a moving average
   * algorithm such that it reads every pixel of the row just once. The edge pixels are repeated to
   * avoid artifacts.
   *
   * <p>Requires a pre-computed {@code div} table of size (255 * diameter) that maps x -> (x /
   * diameter) (can be rounded)
   */
  private static void internalHorizontalBlur(
      int[] pixels, int[] outRow, int w, int row, int diameter, int[] div) {
    final int firstInByte = w * row;
    final int lastInByte = w * (row + 1) - 1;
    final int radius = diameter >> 1;

    int a = 0, r = 0, g = 0, b = 0;
    int pixel;

    // Iterate over relative position to first pixel of row
    for (int i = -radius; i < w + radius; i++) {
      final int ii = bound(firstInByte + i, firstInByte, lastInByte);
      pixel = pixels[ii];
      r += (pixel >> 16) & 0xFF;
      g += (pixel >> 8) & 0xFF;
      b += pixel & 0xFF;
      a += pixel >>> 24;

      if (i >= radius) {
        final int outOffset = i - radius;
        outRow[outOffset] = (div[a] << 24) | (div[r] << 16) | (div[g] << 8) | div[b];

        final int j = i - (diameter - 1);
        final int jj = bound(firstInByte + j, firstInByte, lastInByte);
        pixel = pixels[jj];
        r -= (pixel >> 16) & 0xFF;
        g -= (pixel >> 8) & 0xFF;
        b -= pixel & 0xFF;
        a -= pixel >>> 24;
      }
    }
  }

  /**
   * Creates a blurred version of the given {@code col} of {@code pixels}. It uses a moving average
   * algorithm such that it reads every pixel of the column just once. The edge pixels are repeated
   * to avoid artifacts.
   *
   * <p>Requires a pre-computed {@code div} table of size (255 * diameter) that maps x -> (x /
   * diameter) (can be rounded)
   */
  private static void internalVerticalBlur(
      int[] pixels, int[] outCol, int w, int h, int col, int diameter, int[] div) {
    final int lastInByte = w * (h - 1) + col;
    final int radiusTimesW = (diameter >> 1) * w;
    final int diameterMinusOneTimesW = (diameter - 1) * w;

    int a = 0, r = 0, g = 0, b = 0;
    int pixel;
    int outColPos = 0;

    // iterate over absolute positions in `pixelsIn`; `w` is the step width for moving down one row
    for (int i = col - radiusTimesW; i <= lastInByte + radiusTimesW; i += w) {
      final int ii = bound(i, col, lastInByte);
      pixel = pixels[ii];
      r += (pixel >> 16) & 0xFF;
      g += (pixel >> 8) & 0xFF;
      b += pixel & 0xFF;
      a += pixel >>> 24;

      final int outPos = i - radiusTimesW;
      if (outPos >= col) {
        outCol[outColPos] = (div[a] << 24) | (div[r] << 16) | (div[g] << 8) | div[b];
        outColPos++;

        final int j = i - diameterMinusOneTimesW;
        final int jj = bound(j, col, lastInByte);
        pixel = pixels[jj];
        r -= (pixel >> 16) & 0xFF;
        g -= (pixel >> 8) & 0xFF;
        b -= pixel & 0xFF;
        a -= pixel >>> 24;
      }
    }
  }

  private static int bound(int x, int l, int h) {
    return x < l ? l : (x > h ? h : x);
  }
}
