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
package com.facebook.samples.animation2.bitmap;

import javax.annotation.Nullable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.facebook.cache.common.CacheKey;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.backend.AnimationInformation;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;

/**
 * Bitmap animation factory to create backends for the sample app.
 */
public class ExampleBitmapAnimationFactory {

  public static BitmapAnimationBackend createColorBitmapAnimationBackend(
      final int[] colors,
      final int animationDurationMs,
      final BitmapFrameCache bitmapFrameCache) {
    BitmapAnimationBackend bitmapAnimationBackend = new BitmapAnimationBackend(
        Fresco.getImagePipelineFactory().getPlatformBitmapFactory(),
        bitmapFrameCache,
        new ColorListAnimationInformation(colors, animationDurationMs),
        new ColorAndFrameNumberRenderer(colors));
    bitmapAnimationBackend.setFrameListener(new DebugBitmapAnimationFrameListener());
    return bitmapAnimationBackend;
  }

  public static CountingMemoryCache<CacheKey, CloseableImage> createMemoryCache() {
    return Fresco.getImagePipelineFactory().getBitmapCountingMemoryCache();
  }

  public static class ColorListAnimationInformation implements AnimationInformation {

    private final int[] mColors;
    private final int mAnimationDurationMs;

    public ColorListAnimationInformation(int[] colors, int animationDurationMs) {
      mColors = colors;
      mAnimationDurationMs = animationDurationMs;
    }

    @Override
    public int getFrameCount() {
      return mColors.length;
    }

    @Override
    public int getFrameDurationMs(int frameNumber) {
      return mAnimationDurationMs;
    }

    @Override
    public int getLoopCount() {
      return LOOP_COUNT_INFINITE;
    }
  }

  /**
   * Renderer that draws a color and the frame number on top
   */
  public static class ColorAndFrameNumberRenderer implements BitmapFrameRenderer {

    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int[] mColors;

    public ColorAndFrameNumberRenderer(int[] colors) {
      mColors = colors;
      mTextPaint.setColor(Color.WHITE);
    }

    @Override
    public boolean renderFrame(int frameNumber, Bitmap targetBitmap) {
      Canvas canvas = new Canvas(targetBitmap);
      canvas.drawColor(mColors[frameNumber]);
      int textSize = targetBitmap.getWidth() / 4;

      mTextPaint.setTextSize(textSize);

      String text = "F " + frameNumber;

      float textWidth = mTextPaint.measureText(text, 0, text.length());

      canvas.drawText(
          text,
          (targetBitmap.getWidth() - textWidth) / 2,
          (targetBitmap.getHeight() + textSize) / 2,
          mTextPaint);
      return true;
    }

    @Override
    public void setBounds(@Nullable Rect bounds) {
      // we always use the full bitmap
    }

    @Override
    public int getIntrinsicWidth() {
      // we always use the full bitmap
      return AnimationBackend.INTRINSIC_DIMENSION_UNSET;
    }

    @Override
    public int getIntrinsicHeight() {
      // we always use the full bitmap
      return AnimationBackend.INTRINSIC_DIMENSION_UNSET;
    }
  }
}
