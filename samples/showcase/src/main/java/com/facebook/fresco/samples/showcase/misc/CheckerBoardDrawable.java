/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.misc;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import com.facebook.fresco.samples.showcase.R;

/**
 * A simple checker board drawable that creates a two-colored pattern. It is used to highlight that
 * images are indeed transparent.
 */
public class CheckerBoardDrawable extends Drawable {

  private final Paint mPaint = new Paint();

  private int mColorLight;
  private int mColorDark;
  private int mSquareSize;

  public CheckerBoardDrawable(Resources resources) {
    //noinspection deprecation
    mColorLight = resources.getColor(R.color.checker_board_light);
    //noinspection deprecation
    mColorDark = resources.getColor(R.color.checker_board_dark);

    mSquareSize = resources.getDimensionPixelSize(R.dimen.checker_board_square_size);
  }

  @Override
  public void draw(Canvas canvas) {
    final int w = canvas.getWidth();
    final int h = canvas.getHeight();

    for (int x = 0; x < w; x += mSquareSize) {

      boolean b = (x / mSquareSize) % 2 == 0;
      for (int y = 0; y < h; y += mSquareSize) {

        mPaint.setColor(b ? mColorDark : mColorLight);
        canvas.drawRect(x, y, x + mSquareSize, y + mSquareSize, mPaint);

        b = !b;
      }
    }
  }

  @Override
  public void setAlpha(int alpha) {
    // ignore
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    // ignore
  }

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }
}
