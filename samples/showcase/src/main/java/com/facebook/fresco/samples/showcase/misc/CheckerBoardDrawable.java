/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
