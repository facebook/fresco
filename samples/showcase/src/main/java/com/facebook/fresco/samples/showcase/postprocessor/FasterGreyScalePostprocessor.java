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
 * Applies a grey-scale effect on the bitmap using the more efficient {@link Bitmap#setPixels(int[],
 * int, int, int, int, int, int)} method.
 */
public class FasterGreyScalePostprocessor extends BasePostprocessor {

  @Override
  public void process(Bitmap bitmap) {
    final int w = bitmap.getWidth();
    final int h = bitmap.getHeight();
    final int[] pixels = new int[w * h];

    /*
     * Using {@link Bitmap#getPixels} reduces the number of Java-JNI calls and passes all the image
     * pixels in one call. This allows us to edit all the data in the Java world and then hand back
     * the final result later.
     */
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        final int offset = y * w + x;
        pixels[offset] = SlowGreyScalePostprocessor.getGreyColor(pixels[offset]);
      }
    }

    bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
  }
}
