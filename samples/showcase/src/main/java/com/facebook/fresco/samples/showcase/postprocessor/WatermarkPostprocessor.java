/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.postprocessor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.facebook.imagepipeline.request.BasePostprocessor;
import java.util.Random;

/** Adds a watermark at random positions to the bitmap using {@link Canvas}. */
public class WatermarkPostprocessor extends BasePostprocessor {

  private static final int TEXT_COLOR = 0xBBFFFFFF;
  private static final int FONT_SIZE = 80;

  protected final int mCount;
  protected final String mWatermarkText;
  private final Random mRandom = new Random();
  private final Paint mPaint = new Paint();

  public WatermarkPostprocessor(int count, String watermarkText) {
    mCount = count;
    mWatermarkText = watermarkText;
  }

  @Override
  public void process(Bitmap bitmap) {
    final int w = bitmap.getWidth();
    final int h = bitmap.getHeight();

    final Canvas canvas = new Canvas(bitmap);

    mPaint.setAntiAlias(true);
    mPaint.setColor(TEXT_COLOR);
    mPaint.setFakeBoldText(true);
    mPaint.setTextSize(FONT_SIZE);

    for (int c = 0; c < mCount; c++) {
      final int x = mRandom.nextInt(w);
      final int y = mRandom.nextInt(h);
      canvas.drawText(mWatermarkText, x, y, mPaint);
    }
  }
}
