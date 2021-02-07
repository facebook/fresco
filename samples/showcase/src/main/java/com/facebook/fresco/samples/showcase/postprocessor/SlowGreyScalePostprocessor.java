/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.postprocessor;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.request.BasePostprocessor;

/**
 * Applies a grey-scale effect on the bitmap using the slow {@link Bitmap#setPixel(int, int, int)}
 * method.
 */
public class SlowGreyScalePostprocessor extends BasePostprocessor {

  static int getGreyColor(int color) {
    final int r = (color >> 16) & 0xFF;
    final int g = (color >> 8) & 0xFF;
    final int b = color & 0xFF;

    // see: https://en.wikipedia.org/wiki/Relative_luminance
    final int luminance = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);

    return 0xFF000000 | luminance << 16 | luminance << 8 | luminance;
  }

  @Override
  public void process(Bitmap bitmap) {
    final int w = bitmap.getWidth();
    final int h = bitmap.getHeight();

    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        /*
         * Using {@link Bitmap#getPixel} when iterating about an entire bitmap is inefficient as it
         * causes many JNI calls. This is only done here to provide a comparison to
         * {@link FasterGreyScalePostprocessor}.
         */
        final int color = bitmap.getPixel(x, y);
        bitmap.setPixel(x, y, SlowGreyScalePostprocessor.getGreyColor(color));
      }
    }
  }
}
